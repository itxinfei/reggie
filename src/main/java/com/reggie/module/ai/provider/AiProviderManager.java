package com.reggie.module.ai.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.ai.adapter.AiModelAdapter.StreamCallback;
import com.reggie.module.ai.adapter.AbortableStreamCallback;
import com.reggie.module.ai.adapter.AiModelAdapter;
import com.reggie.module.ai.adapter.AnthropicAdapter;
import com.reggie.module.ai.adapter.BaiduAdapter;
import com.reggie.module.ai.adapter.OpenAICompatibleAdapter;
import com.reggie.module.ai.failover.AiCallAction;
import com.reggie.module.ai.failover.AiFailoverExecutor;
import com.reggie.module.ai.failover.AiFailureType;
import com.reggie.module.ai.failover.AiProviderException;
import com.reggie.module.ai.failover.FailoverAttempt;
import com.reggie.module.ai.failover.FailoverResult;
import com.reggie.module.ai.failover.FirstTokenGuard;
import com.reggie.module.ai.service.CircuitBreakerService;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.mapper.AiProviderConfigMapper;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.model.ModelTurn;
import com.reggie.module.ai.tool.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
public class AiProviderManager {

    @Resource
    private AiProviderConfigMapper providerConfigMapper;

    @Resource
    private AIConfigProperties aiConfig;

    /** 熔断降级服务 */
    @Resource
    private CircuitBreakerService circuitBreakerService;

    /** 多供应商故障转移执行器 */
    @Resource
    private AiFailoverExecutor failoverExecutor;

    // ==================== 适配器注册表 ====================

    /**
     * 适配器注册表：formatId → 适配器实例
     * <p>使用 LinkedHashMap 保持注册顺序</p>
     */
    private final Map<String, AiModelAdapter> adapterRegistry = new LinkedHashMap<>();

    /** 能力 JSON 解析（capabilities 列是简单 JSON，无需注入 Spring 托管 Mapper） */
    private static final ObjectMapper CAPS_MAPPER = new ObjectMapper();

    /** 能力键：视觉多模态 */
    public static final String CAP_VISION = "vision";
    /** 能力键：函数调用 */
    public static final String CAP_TOOLS = "tools";
    /** 能力键：向量嵌入 */
    public static final String CAP_EMBEDDING = "embedding";

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

    /** 候选供应商列表（已校验，按 is_active DESC、sort ASC），volatile 保证可见性 */
    private volatile List<AiProviderConfig> candidateConfigs = Collections.emptyList();

    /** reInit 锁对象，防止并发重新加载时多次查库 */
    private final Object reloadLock = new Object();

    /** 最近一次重新加载的时间戳，用于避免高频 reload */
    private volatile long lastReloadTime = 0L;

    /**
     * 初始化。
     */
    @PostConstruct
    public void init() {
        try {
            reloadConfig();
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("AI供应商配置加载失败（可能是测试环境缺少数据表），已跳过。错误", e);
            this.candidateConfigs = Collections.emptyList();
        }
        List<AiProviderConfig> ready = resolveCandidates();
        log.info("AI供应商管理器初始化完成，候选供应商 {} 个，首选: {}",
                ready.size(), ready.isEmpty() ? "无（AI不可用）" : ready.get(0).getProviderCode());
    }

    /**
     * 从数据库重新加载候选供应商（线程安全）。
     * <p>查全部 enabled 行，按 is_active DESC、sort ASC 排序；逐行校验，
     * 单行配置不完整不影响其他候选。</p>
     */
    public void reloadConfig() {
        // 防抖动：2秒内不重复加载
        long now = System.currentTimeMillis();
        if (now - lastReloadTime < 2000 && !candidateConfigs.isEmpty()) {
            return;
        }

        synchronized (reloadLock) {
            // 双重检查，防止在等待锁时已被其他线程加载
            if (now - lastReloadTime < 2000 && !candidateConfigs.isEmpty()) {
                return;
            }

            List<AiProviderConfig> rows = providerConfigMapper.selectList(
                    new LambdaQueryWrapper<AiProviderConfig>()
                            .eq(AiProviderConfig::getEnabled, true)
                            .eq(AiProviderConfig::getIsDeleted, 0)
                            .orderByDesc(AiProviderConfig::getIsActive)
                            .orderByAsc(AiProviderConfig::getSort)
            );

            List<AiProviderConfig> usable = new ArrayList<AiProviderConfig>();
            for (AiProviderConfig row : rows) {
                // 配置校验：不完整的行跳过，不拖垮其他候选
                String validationError = validateConfig(row);
                if (validationError != null) {
                    log.warn("AI供应商配置校验失败，已跳过该候选: provider={}, error={}",
                            row.getProviderCode(), validationError);
                    continue;
                }
                usable.add(row);
            }
            this.candidateConfigs = Collections.unmodifiableList(usable);
            log.info("AI候选供应商已加载: {} 个{}", usable.size(),
                    usable.isEmpty() ? "（将使用 application.yml 兜底配置）"
                            : "，首选=" + usable.get(0).getProviderCode());

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

    /**
     * 处理 chat。
     * @param messages 参数 messages
     * @param maxTokens 参数 maxTokens
     * @param temperature 参数 temperature
     * @return 返回结果
     */
    public AIChatResponse chat(List<AIMessage> messages, int maxTokens, double temperature) {
        if (messages == null || messages.isEmpty()) {
            return AIChatResponse.builder()
                    .content("消息列表为空，无法发起对话")
                    .model(aiConfig.getModel())
                    .build();
        }

        List<AiProviderConfig> candidates = resolveCandidates();
        if (candidates.isEmpty()) {
            return AIChatResponse.builder()
                    .content("AI功能未配置：没有启用的AI供应商。请前往后台管理 → AI供应商配置 中设置API密钥并启用供应商。")
                    .model("none")
                    .build();
        }

        // 多候选故障转移：单个供应商失败自动切换下一家
        FailoverResult<AIChatResponse> result = failoverExecutor.execute(candidates,
                new AiCallAction<AIChatResponse>() {
                    @Override
                    public AIChatResponse invoke(AiProviderConfig candidate) {
                        AiModelAdapter adapter = resolveAdapter(candidate);
                        if (adapter == null) {
                            // 未知 API 格式：按服务端错误抛出，跳过该候选
                            throw new AiProviderException(AiFailureType.SERVER_ERROR, 0,
                                    "供应商「" + candidate.getProviderName() + "」API格式不受支持: "
                                            + candidate.getApiFormat());
                        }
                        return adapter.chat(messages, maxTokens, temperature, candidate);
                    }
                });

        if (result.getOutcome() == FailoverResult.Outcome.SUCCESS) {
            return result.getValue();
        }
        // EXHAUSTED / NON_RETRYABLE：返回兜底错误体（POST_START 在非流式不会发生）
        return AIChatResponse.builder()
                .content(buildFailMessage(result))
                .model(resolveTerminalModel(result, candidates))
                .build();
    }

    /**
     * SSE 流式对话：多候选故障转移，首选流式建连失败自动切换下一家；
     * 首 token 发出后失败只终止，不切换。
     */
    public String streamChat(List<AIMessage> messages, int maxTokens, double temperature,
                             StreamCallback callback) {
        if (messages == null || messages.isEmpty()) {
            callback.onToken("消息列表为空，无法发起对话", true);
            return null;
        }
        List<AiProviderConfig> candidates = resolveCandidates();
        if (candidates.isEmpty()) {
            callback.onToken("AI功能未配置，请前往后台管理启用供应商", true);
            return null;
        }

        // 首 token 哨兵：内容开始前失败可切换，开始后失败只终止
        final FirstTokenGuard guard = new FirstTokenGuard(callback);
        FailoverResult<String> result = failoverExecutor.execute(candidates,
                new AiCallAction<String>() {
                    @Override
                    public String invoke(AiProviderConfig candidate) throws Exception {
                        AiModelAdapter adapter = resolveAdapter(candidate);
                        if (adapter == null) {
                            throw new AiProviderException(AiFailureType.SERVER_ERROR, 0,
                                    "供应商「" + candidate.getProviderName() + "」API格式不受支持: "
                                            + candidate.getApiFormat());
                        }
                        return doStreamCandidate(messages, maxTokens, temperature,
                                candidate, adapter, guard);
                    }
                });

        if (result.getOutcome() == FailoverResult.Outcome.SUCCESS) {
            return result.getValue();
        }
        if (guard.isAborted()) {
            // 用户中止：片段落库由服务层按 stopped 负责，不补发
            return null;
        }
        if (result.getOutcome() == FailoverResult.Outcome.POST_START_ERROR) {
            // 首 token 后中断：通知会话片段非完整，收尾时以 stopped 落库（不伪装成 completed）
            guard.onUpstreamInterrupted();
            // 内容已部分推送，不补发末帧（服务层收尾）
            return null;
        }
        // EXHAUSTED / NON_RETRYABLE：首 token 从未发出，安全发末帧错误
        callback.onToken(buildFailMessage(result), true);
        return null;
    }

    /**
     * P4 工具感知单轮对话：由 {@code AiToolOrchestrator} 驱动多轮循环。
     * <p>仅在「capabilities 勾选 tools 且适配器协议层支持工具调用」的候选间故障转移；
     * 文本增量经 textSink 推送（首 token 边界受哨兵保护），工具调用由适配器结构化返回。</p>
     */
    public ModelTurn chatTurn(List<AIMessage> messages, int maxTokens, double temperature,
                              List<ToolDefinition> tools, AbortableStreamCallback abort,
                              StreamCallback textSink) {
        if (messages == null || messages.isEmpty()) {
            return ModelTurn.error("消息列表为空，无法发起对话");
        }
        List<AiProviderConfig> candidates = resolveCandidates();
        if (candidates.isEmpty()) {
            return ModelTurn.error("AI功能未配置：没有启用的AI供应商，请在后台管理设置。");
        }

        // 工具候选过滤：勾选 tools 能力且适配器支持 function calling（Baidu 压平协议会被排除）
        List<AiProviderConfig> toolCandidates = new ArrayList<AiProviderConfig>();
        for (AiProviderConfig candidate : candidates) {
            AiModelAdapter adapter = resolveAdapter(candidate);
            if (adapter != null && adapter.supportsToolCalling()
                    && capEnabled(candidate, CAP_TOOLS)) {
                toolCandidates.add(candidate);
            }
        }
        if (toolCandidates.isEmpty()) {
            return ModelTurn.error("当前没有启用且支持工具调用的供应商，请在后台为供应商勾选「工具调用」能力。");
        }

        // textSink 首 token 哨兵
        final FirstTokenGuard sinkGuard = new FirstTokenGuard(textSink);
        FailoverResult<ModelTurn> result = failoverExecutor.execute(toolCandidates,
                new AiCallAction<ModelTurn>() {
                    @Override
                    public ModelTurn invoke(AiProviderConfig candidate) throws Exception {
                        AiModelAdapter adapter = resolveAdapter(candidate);
                        return adapter.chatTurn(messages, maxTokens, temperature,
                                candidate, tools, abort, sinkGuard);
                    }
                });

        if (result.getOutcome() == FailoverResult.Outcome.SUCCESS) {
            return result.getValue();
        }
        if (abort != null && abort.isAborted()) {
            return ModelTurn.builder().finishReason(ModelTurn.FINISH_STOP).build();
        }
        if (result.getOutcome() == FailoverResult.Outcome.POST_START_ERROR) {
            // 首 token 后中断：通知文本出口所属会话，片段收尾时以 stopped 落库
            sinkGuard.onUpstreamInterrupted();
            return ModelTurn.error("AI回答生成中断，请稍后重试。");
        }
        // NON_RETRYABLE / EXHAUSTED
        return ModelTurn.error(buildFailMessage(result));
    }

    /**
     * 针对单个候选执行流式逻辑。
     * <p>真流式建连/读取失败：首 token 前抛异常给执行器切换，首 token 后抛终止性异常；
     * 流式返回空内容（网关协议错配）时，同候选转一次非流式（协议纠错，非失败重试）。</p>
     */
    private String doStreamCandidate(List<AIMessage> messages, int maxTokens, double temperature,
                                     AiProviderConfig config, AiModelAdapter adapter,
                                     FirstTokenGuard guard) throws Exception {
        if (adapter.supportsStreaming()) {
            String content;
            try {
                content = adapter.chatStream(messages, maxTokens, temperature, config, guard);
            } catch (Exception e) {
                // 用户主动中止：安静返回，不切换
                if (guard.isAborted()) {
                    log.info("流式被用户中止: provider={}", config.getProviderCode());
                    return null;
                }
                // 首 token 已发出：终止性异常，不切换
                if (guard.isStarted()) {
                    throw AiProviderException.afterStarted(e);
                }
                // 首 token 前硬失败（429/5xx/网络/非200）：抛给执行器切下一家，
                // 不在同家做非流式重试，避免延迟翻倍
                throw AiProviderException.local(config, e);
            }

            // 用户中止：安静返回
            if (guard.isAborted()) {
                return null;
            }
            if (content != null && !content.isEmpty()) {
                return content;
            }
            // 流式 200 但无内容：网关协议错配（历史场景），同候选转非流式做协议纠错
            log.warn("流式返回空内容，同候选转非流式: provider={}", config.getProviderCode());
        }

        // 非流式 + 分块推送（adapter.chat 失败/空会抛异常 → 执行器切下一家）
        AIChatResponse response = adapter.chat(messages, maxTokens, temperature, config);
        String content = response.getContent();
        String[] chunks = splitIntoChunks(content, 20);
        for (int i = 0; i < chunks.length; i++) {
            if (guard.isAborted()) {
                return null;
            }
            guard.onToken(chunks[i], i == chunks.length - 1);
            try {
                Thread.sleep(30);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return content;
    }

    /**
     * 解析候选对应适配器；apiFormat 缺失按 openai 处理，未知格式返回 null。
     */
    private AiModelAdapter resolveAdapter(AiProviderConfig candidate) {
        String apiFormat = candidate.getApiFormat();
        if (apiFormat == null || apiFormat.isEmpty()) {
            apiFormat = OpenAICompatibleAdapter.FORMAT_ID;
        }
        return adapterRegistry.get(apiFormat.toLowerCase());
    }

    /**
     * 判断候选是否勾选某能力。兼容 capabilities 的对象形式 {"tools":true}
     * 与数组形式 ["chat","tools"]；解析失败或未声明返回 false。
     */
    private boolean capEnabled(AiProviderConfig candidate, String cap) {
        String json = candidate.getCapabilities();
        if (StrUtil.isBlank(json)) {
            return false;
        }
        try {
            JsonNode node = CAPS_MAPPER.readTree(json);
            if (node.isObject()) {
                return node.path(cap).asBoolean(false);
            }
            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (cap.equals(item.asText())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("能力JSON解析失败，按未勾选处理: capabilities={}", json);
        }
        return false;
    }

    /**
     * 构造失败兜底文案：NON_RETRYABLE 直接返回厂家原始错误（含厂家名）；
     * EXHAUSTED 提示全部供应商不可用。
     */
    private String buildFailMessage(FailoverResult<?> result) {
        List<FailoverAttempt> attempts = result.getAttempts();
        FailoverAttempt last = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
        // 原始 detail 可能携带供应商上游错误体/内部信息（最长500字），仅落日志；终端用户统一友好话术
        if (last != null && last.getDetail() != null && !last.getDetail().isEmpty()) {
            log.warn("AI最终失败 outcome={}, provider={}, detail={}", result.getOutcome(),
                    last.getProviderCode(), last.getDetail());
        }
        if (result.getOutcome() == FailoverResult.Outcome.EXHAUSTED) {
            return "【AI服务暂时不可用】所有供应商均无法响应，请稍后重试。";
        }
        return "AI服务暂时不可用，请稍后重试。";
    }

    /**
     * 解析最终尝试候选的模型名（兜底响应用）。
     */
    private String resolveTerminalModel(FailoverResult<?> result, List<AiProviderConfig> candidates) {
        List<FailoverAttempt> attempts = result.getAttempts();
        if (!attempts.isEmpty()) {
            String lastCode = attempts.get(attempts.size() - 1).getProviderCode();
            for (AiProviderConfig candidate : candidates) {
                if (candidate.getProviderCode().equals(lastCode)) {
                    return candidate.getModelName();
                }
            }
        }
        return aiConfig.getModel();
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

    // ==================== 配置获取 ====================

    /**
     * 获取候选供应商快照。DB 有可用行返回其副本；无可用行时退化为 yml 单候选；
     * 无任何配置返回空列表。
     */
    public List<AiProviderConfig> resolveCandidates() {
        List<AiProviderConfig> rows = candidateConfigs;
        if (!rows.isEmpty()) {
            return new ArrayList<AiProviderConfig>(rows);
        }
        AiProviderConfig yml = buildYmlCandidate();
        if (yml == null) {
            return Collections.emptyList();
        }
        List<AiProviderConfig> one = new ArrayList<AiProviderConfig>(1);
        one.add(yml);
        return one;
    }

    /**
     * 构建 application.yml 兜底候选（仅 DB 无可用行时使用）。
     */
    private AiProviderConfig buildYmlCandidate() {
        String ymlApiKey = aiConfig.getApiKey();
        if (ymlApiKey == null || ymlApiKey.trim().isEmpty()) {
            log.warn("application.yml 中未配置 reggie.ai.api-key，且无启用的DB供应商，AI功能不可用");
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
     * 获取首选供应商（候选列表首位，带缓存，线程安全）。
     */
    public AiProviderConfig getActiveConfig() {
        List<AiProviderConfig> all = resolveCandidates();
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * 解析当前激活供应商的能力开关。
     * <p>capabilities 列形如 {"chat":true,"vision":false,"tools":false,"embedding":false}；
     * 为 null/空/坏 JSON 时退化为仅 chat（兼容历史供应商与 yml 兜底配置）。</p>
     *
     * @return 不可变能力映射，键为 chat/vision/tools/embedding
     */
    public Map<String, Boolean> getCapabilities() {
        Map<String, Boolean> caps = new HashMap<>();
        caps.put("chat", Boolean.TRUE);
        caps.put(CAP_VISION, Boolean.FALSE);
        caps.put(CAP_TOOLS, Boolean.FALSE);
        caps.put(CAP_EMBEDDING, Boolean.FALSE);

        AiProviderConfig config;
        try {
            config = getActiveConfig();
        } catch (Exception e) {
            log.warn("读取供应商能力失败，按仅 chat 兜底: {}", e.getMessage());
            return Collections.unmodifiableMap(caps);
        }
        if (config == null) {
            return Collections.unmodifiableMap(caps);
        }
        String json = config.getCapabilities();
        if (StrUtil.isBlank(json)) {
            return Collections.unmodifiableMap(caps);
        }
        try {
            Map<?, ?> parsed = CAPS_MAPPER.readValue(json, Map.class);
            caps.put(CAP_VISION, Boolean.TRUE.equals(parsed.get(CAP_VISION)));
            caps.put(CAP_TOOLS, Boolean.TRUE.equals(parsed.get(CAP_TOOLS)));
            caps.put(CAP_EMBEDDING, Boolean.TRUE.equals(parsed.get(CAP_EMBEDDING)));
        } catch (Exception e) {
            log.warn("供应商能力JSON解析失败，按仅 chat 兜底: capabilities={}", json, e);
        }
        return Collections.unmodifiableMap(caps);
    }

    /** 当前激活供应商是否支持视觉多模态 */
    public boolean supportsVision() {
        return Boolean.TRUE.equals(getCapabilities().get(CAP_VISION));
    }

    /**
     * 当前激活供应商是否可走工具调用链路：配置 capabilities.tools 勾选，
     * 且对应适配器在协议层真正实现了 function calling（Baidu 压平协议不支持）。
     */
    public boolean supportsToolCalling() {
        if (!Boolean.TRUE.equals(getCapabilities().get(CAP_TOOLS))) {
            return false;
        }
        AiProviderConfig config;
        try {
            config = getActiveConfig();
        } catch (Exception e) {
            log.warn("读取供应商配置失败，工具能力按不支持处理: {}", e.getMessage());
            return false;
        }
        if (config == null) {
            return false;
        }
        String apiFormat = config.getApiFormat();
        if (apiFormat == null || apiFormat.isEmpty()) {
            apiFormat = OpenAICompatibleAdapter.FORMAT_ID;
        }
        AiModelAdapter adapter = adapterRegistry.get(apiFormat.toLowerCase());
        return adapter != null && adapter.supportsToolCalling();
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

    /** 健康状态时间格式 */
    private static final java.time.format.DateTimeFormatter HEALTH_TIME_FMT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构建 AI 服务完整健康状态（供 /api/ai/status 使用）。
     * <p>含首选供应商、全部候选实时状态（启用/首选标记/上次测试/熔断视图）与全耗尽标记。</p>
     */
    public Map<String, Object> buildHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        List<AiProviderConfig> candidates = resolveCandidates();
        boolean configured = !candidates.isEmpty();

        AiProviderConfig preferred = configured ? candidates.get(0) : null;
        if (preferred != null) {
            status.put("provider", preferred.getProviderName());
            status.put("model", preferred.getModelName());
            status.put("format", preferred.getApiFormat());
        } else {
            status.put("provider", "未配置");
            status.put("model", "N/A");
        }

        status.put("configured", configured);
        status.put("allExhausted", failoverExecutor.isLastOutcomeExhausted());
        status.put("lastExhaustedTime", formatEpochMillis(failoverExecutor.getLastExhaustedTime()));

        List<Map<String, Object>> candidateList = new ArrayList<Map<String, Object>>();
        for (AiProviderConfig candidate : candidates) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", candidate.getProviderCode());
            item.put("name", candidate.getProviderName());
            item.put("sort", candidate.getSort());
            item.put("enabled", candidate.getEnabled());
            item.put("isActive", candidate.getIsActive());
            item.put("lastTestResult", candidate.getLastTestResult());
            java.time.LocalDateTime testTime = candidate.getLastTestTime();
            item.put("lastTestTime", testTime == null ? null : testTime.format(HEALTH_TIME_FMT));
            item.put("breaker", buildBreakerView(candidate.getProviderCode()));
            candidateList.add(item);
        }
        status.put("candidates", candidateList);
        status.put("circuitBreaker", getCircuitBreakerStats());
        return status;
    }

    /** 单个候选的熔断状态视图（state/errorRate/冷却剩余） */
    private Map<String, Object> buildBreakerView(String code) {
        CircuitBreakerService.BreakerSnapshot snapshot = circuitBreakerService.describe(code);
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("state", snapshot.getState());
        view.put("errorRate", Math.round(snapshot.getErrorRate() * 100) / 100.0);
        view.put("cooldownRemainingMs", snapshot.getCooldownRemainingMs());
        return view;
    }

    /** 毫秒时间戳格式化（<=0 无记录返回 null） */
    private String formatEpochMillis(long epochMillis) {
        if (epochMillis <= 0L) {
            return null;
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(epochMillis));
    }
}
