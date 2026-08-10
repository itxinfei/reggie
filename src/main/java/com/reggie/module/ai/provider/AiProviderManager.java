package com.reggie.module.ai.provider;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.ai.adapter.AiModelAdapter.StreamCallback;
import com.reggie.module.ai.adapter.AiModelAdapter;
import com.reggie.module.ai.adapter.AnthropicAdapter;
import com.reggie.module.ai.adapter.BaiduAdapter;
import com.reggie.module.ai.adapter.OpenAICompatibleAdapter;
import com.reggie.module.ai.service.CircuitBreakerService;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.mapper.AiProviderConfigMapper;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI供应商管理器（核心调度器），从数据库读取当前激活的供应商配置，
 * 通过适配器注册表将请求分发给对应格式的适配器。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-10
 */
@Slf4j
@Component
public class AiProviderManager implements AIClient {

    @Resource
    private AiProviderConfigMapper providerConfigMapper;

    @Resource
    private AIConfigProperties aiConfig;

    /** 熔断降级服务 */
    @Resource
    private CircuitBreakerService circuitBreakerService;

    // ==================== 适配器注册表 ====================

    /**
     * 适配器注册表：formatId → 适配器实例
     * <p>使用 LinkedHashMap 保持注册顺序</p>
     */
    private final Map<String, AiModelAdapter> adapterRegistry = new LinkedHashMap<>();

    /**
     * 注册所有内置适配器
     * <p>如需扩展，在此方法中添加新适配器即可</p>
     */
    @PostConstruct
    public void initAdapters() {
        registerAdapter(new OpenAICompatibleAdapter());
        // openai_compatible 是历史兼容别名
        adapterRegistry.put("openai_compatible", adapterRegistry.get(OpenAICompatibleAdapter.FORMAT_ID));
        // 360 智脑也是 OpenAI 兼容格式
        adapterRegistry.put("360", adapterRegistry.get(OpenAICompatibleAdapter.FORMAT_ID));
        // custom 格式暂回退到 OpenAI 兼容
        adapterRegistry.put("custom", adapterRegistry.get(OpenAICompatibleAdapter.FORMAT_ID));

        registerAdapter(new AnthropicAdapter());
        registerAdapter(new BaiduAdapter());

        log.info("AI适配器注册完成，已注册 {} 种格式: {}", adapterRegistry.size(), adapterRegistry.keySet());
    }

    /**
     * 注册单个适配器
     */
    private void registerAdapter(AiModelAdapter adapter) {
        adapterRegistry.put(adapter.getFormatId(), adapter);
        log.info("注册AI适配器: formatId={}, displayName={}", adapter.getFormatId(), adapter.getDisplayName());
    }

    // ==================== 配置管理 ====================

    /** 缓存当前激活的供应商配置（volatile 保证可见性） */
    private volatile AiProviderConfig activeConfig;

    /** reInit 锁对象，防止并发重新加载时多次查库 */
    private final Object reloadLock = new Object();

    /** 最近一次重新加载的时间戳，用于避免高频 reload */
    private volatile long lastReloadTime = 0L;

    @PostConstruct
    public void init() {
        try {
            reloadConfig();
        } catch (Exception e) {
            log.warn("AI供应商配置加载失败（可能是测试环境缺少数据表），已跳过。错误: {}", e.getMessage());
            this.activeConfig = null;
        }
        log.info("AI供应商管理器初始化完成，当前供应商: {}",
                activeConfig != null ? activeConfig.getProviderCode() : "application.yml 配置");
    }

    /**
     * 从数据库重新加载激活的供应商配置（线程安全）
     */
    public void reloadConfig() {
        // 防抖动：2秒内不重复加载
        long now = System.currentTimeMillis();
        if (now - lastReloadTime < 2000 && activeConfig != null) {
            return;
        }

        synchronized (reloadLock) {
            // 双重检查，防止在等待锁时已被其他线程加载
            if (now - lastReloadTime < 2000 && activeConfig != null) {
                return;
            }

            AiProviderConfig config = providerConfigMapper.selectOne(
                    new LambdaQueryWrapper<AiProviderConfig>()
                            .eq(AiProviderConfig::getEnabled, true)
                            .eq(AiProviderConfig::getIsActive, true)
                            .eq(AiProviderConfig::getIsDeleted, 0)
                            .last("LIMIT 1")
            );

            if (config != null) {
                // 配置校验
                String validationError = validateConfig(config);
                if (validationError != null) {
                    log.error("AI供应商配置校验失败: provider={}, error={}",
                            config.getProviderCode(), validationError);
                    this.activeConfig = null;
                } else {
                    this.activeConfig = config;
                    log.info("AI供应商已切换: provider={}, model={}, format={}",
                            config.getProviderCode(), config.getModelName(), config.getApiFormat());
                }
            } else {
                this.activeConfig = null;
                log.warn("未找到激活的AI供应商配置，将使用 application.yml 默认配置");
            }

            lastReloadTime = System.currentTimeMillis();
        }
    }

    /**
     * 校验供应商配置完整性
     */
    private String validateConfig(AiProviderConfig config) {
        if (config.getProviderCode() == null || config.getProviderCode().trim().isEmpty()) {
            return "供应商编码不能为空";
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
            return "API基础URL不能为空";
        }
        if (config.getModelName() == null || config.getModelName().trim().isEmpty()) {
            return "模型名称不能为空";
        }
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            return "API密钥未配置，请在后台管理页面设置API Key";
        }
        return null;
    }

    // ==================== AIClient 接口实现 ====================

    @Override
    public AIChatResponse chat(List<AIMessage> messages, int maxTokens, double temperature) {
        AiProviderConfig config = getActiveConfig();

        // 参数校验
        if (messages == null || messages.isEmpty()) {
            return AIChatResponse.builder()
                    .content("消息列表为空，无法发起对话")
                    .model(config != null ? config.getModelName() : aiConfig.getModel())
                    .build();
        }

        // 配置校验
        if (config == null) {
            return AIChatResponse.builder()
                    .content("AI功能未配置：没有激活的AI供应商。请前往后台管理 → AI供应商配置 中设置API密钥并激活供应商。")
                    .model("none")
                    .build();
        }

        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AIChatResponse.builder()
                    .content("AI功能未就绪：供应商「" + config.getProviderName()
                            + "」未配置API密钥，请前往后台管理页面设置。")
                    .model(config.getModelName())
                    .build();
        }

        // 从适配器注册表中查找对应格式的适配器
        String apiFormat = config.getApiFormat();
        if (apiFormat == null || apiFormat.isEmpty()) {
            apiFormat = OpenAICompatibleAdapter.FORMAT_ID;
        }
        apiFormat = apiFormat.toLowerCase();

        AiModelAdapter adapter = adapterRegistry.get(apiFormat);
        if (adapter == null) {
            log.error("不支持的API格式: provider={}, format={}, 已注册: {}",
                    config.getProviderCode(), apiFormat, adapterRegistry.keySet());
            return AIChatResponse.builder()
                    .content("不支持的API格式「" + apiFormat
                            + "」，支持的格式: " + StrUtil.join(", ", adapterRegistry.keySet()))
                    .model(config.getModelName())
                    .build();
        }

        log.info("使用适配器 [{}] 处理请求: provider={}, model={}",
                adapter.getFormatId(), config.getProviderCode(), config.getModelName());

        // 委托给对应适配器（带熔断保护）
        String providerCode = config.getProviderCode();
        return circuitBreakerService.execute(providerCode,
                () -> adapter.chat(messages, maxTokens, temperature, config),
                (code, reason) -> AIChatResponse.builder()
                        .content("【AI服务暂时不可用】" + reason + "（" + config.getProviderName() + "），请稍后重试。")
                        .model(config.getModelName())
                        .build()
        );
    }

    /**
     * SSE 流式对话：优先使用适配器的流式能力，降级为非流式
     * <p>带熔断保护，熔断时直接降级。</p>
     */
    public String streamChat(List<AIMessage> messages, int maxTokens, double temperature,
                             StreamCallback callback) {
        AiProviderConfig config = getActiveConfig();

        if (messages == null || messages.isEmpty()) {
            callback.onToken("消息列表为空，无法发起对话", true);
            return null;
        }
        if (config == null) {
            callback.onToken("AI功能未配置，请前往后台管理设置", true);
            return null;
        }

        String apiFormat = config.getApiFormat();
        if (apiFormat == null || apiFormat.isEmpty()) {
            apiFormat = OpenAICompatibleAdapter.FORMAT_ID;
        }
        apiFormat = apiFormat.toLowerCase();

        AiModelAdapter adapter = adapterRegistry.get(apiFormat);
        if (adapter == null) {
            callback.onToken("不支持的API格式：" + apiFormat, true);
            return null;
        }

        String providerCode = config.getProviderCode();
        return circuitBreakerService.execute(providerCode,
                () -> doStream(messages, maxTokens, temperature, config, adapter, callback),
                (code, reason) -> {
                    callback.onToken("【服务暂时不可用】" + reason, true);
                    return null;
                }
        );
    }

    /**
     * 执行实际流式逻辑（被熔断保护包裹）
     */
    private String doStream(List<AIMessage> messages, int maxTokens, double temperature,
                            AiProviderConfig config, AiModelAdapter adapter, StreamCallback callback)
            throws Exception {
        if (adapter.supportsStreaming()) {
            try {
                return adapter.chatStream(messages, maxTokens, temperature, config, callback);
            } catch (Exception e) {
                log.warn("真流式失败，降级为分块流式: provider={}", config.getProviderCode(), e);
            }
        }

        // 降级：非流式 + 分块发送
        AIChatResponse response = adapter.chat(messages, maxTokens, temperature, config);
        if (response != null && response.getContent() != null) {
            String content = response.getContent();
            String[] chunks = splitIntoChunks(content, 20);
            for (int i = 0; i < chunks.length; i++) {
                callback.onToken(chunks[i], i == chunks.length - 1);
                try { Thread.sleep(30); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            return content;
        }
        return null;
    }

    private String[] splitIntoChunks(String text, int chunkSize) {
        if (text == null || text.isEmpty()) return new String[0];
        int len = text.length();
        int chunks = (int) Math.ceil((double) len / chunkSize);
        String[] result = new String[chunks];
        for (int i = 0; i < chunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, len);
            result[i] = text.substring(start, end);
        }
        return result;
    }

    @Override
    public String getProviderName() {
        AiProviderConfig config = getActiveConfig();
        return config != null ? config.getProviderCode() : aiConfig.getProvider();
    }

    @Override
    public String getDefaultModel() {
        AiProviderConfig config = getActiveConfig();
        return config != null ? config.getModelName() : aiConfig.getModel();
    }

    // ==================== 配置获取 ====================

    /**
     * 获取当前激活的供应商配置（带缓存，线程安全）
     */
    public AiProviderConfig getActiveConfig() {
        // 先读取 volatile 本地副本
        AiProviderConfig cached = activeConfig;

        // 如果缓存不为空但标记为禁用，重新加载
        if (cached != null && cached.getEnabled() != null && !cached.getEnabled()) {
            reloadConfig();
            cached = activeConfig;
        }

        if (cached != null) {
            return cached;
        }

        // 无数据库配置，使用 application.yml 兜底
        String ymlApiKey = aiConfig.getApiKey();
        if (ymlApiKey == null || ymlApiKey.trim().isEmpty()) {
            log.warn("application.yml 中未配置 reggie.ai.api-key，AI功能将无法使用");
            return null;
        }

        AiProviderConfig fallback = new AiProviderConfig();
        fallback.setProviderCode(aiConfig.getProvider());
        fallback.setProviderName(aiConfig.getProvider());
        fallback.setBaseUrl(aiConfig.getBaseUrl());
        fallback.setModelName(aiConfig.getModel());
        fallback.setApiKey(ymlApiKey);
        fallback.setTimeout(aiConfig.getTimeout());
        fallback.setMaxTokens(aiConfig.getMaxTokens());
        fallback.setTemperature(aiConfig.getTemperature());
        fallback.setApiFormat(OpenAICompatibleAdapter.FORMAT_ID);
        fallback.setEnabled(true);
        return fallback;
    }

    /**
     * 获取已注册的适配器数量（管理用）
     */
    public int getAdapterCount() {
        return adapterRegistry.size();
    }

    /**
     * 获取所有已注册的适配器信息（管理用）
     */
    public Map<String, String> getRegisteredAdapters() {
        Map<String, String> result = new LinkedHashMap<>();
        for (AiModelAdapter adapter : adapterRegistry.values()) {
            result.putIfAbsent(adapter.getFormatId(), adapter.getDisplayName());
        }
        return result;
    }

    /**
     * 获取熔断器统计信息
     */
    public Map<String, Object> getCircuitBreakerStats() {
        return circuitBreakerService.getStats();
    }
}
