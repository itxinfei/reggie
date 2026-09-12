package com.reggie.module.ai.controller;

import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.service.AiProviderConfigService;
import com.reggie.module.ai.util.AiKeyEncryptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI供应商配置管理控制器（后台管理）
 * 管理员可在此配置/切换不同的大模型供应商
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai/provider")
@RequireEmployee
@Tag(name = "AI供应商管理", description = "配置和切换大模型供应商")
public class AiProviderController {

    @Resource
    private AiProviderConfigService providerConfigService;

    @Resource
    private AiProviderManager aiProviderManager;

    /**
     * 查询列表。
     * @return 返回结果
     */
    @GetMapping("/list")
    @Operation(summary = "供应商列表", description = "获取所有AI供应商配置")
    public R<List<AiProviderConfig>> list() {
        List<AiProviderConfig> list = providerConfigService.list();
        // 脱敏API密钥
        list.forEach(p -> maskSensitiveFields(p));
        return R.success(list);
    }

    /**
     * 获取 active。
     * @return 返回结果
     */
    @GetMapping("/active")
    @Operation(summary = "当前激活供应商", description = "获取当前正在使用的AI供应商")
    public R<AiProviderConfig> getActive() {
        AiProviderConfig config = providerConfigService.getActiveProvider();
        if (config != null) maskSensitiveFields(config);
        return R.success(config);
    }

    /**
     * 新增。
     * @param config 参数 config
     * @return 返回结果
     */
    @PostMapping("/add")
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "添加或更新供应商", description = "新增AI供应商配置，若providerCode已存在则自动更新")
    public R<Map<String, Object>> add(
            @Parameter(description = "供应商配置信息", required = true) @RequestBody AiProviderConfig config) {
        String validation = validateProviderConfig(config);
        if (validation != null) {
            return R.error(validation);
        }
        // 新增时不允许直接设为激活
        if (config.getIsActive() == null || config.getIsActive()) {
            config.setIsActive(false);
        }
        if (config.getTimeout() == null) config.setTimeout(60);
        if (config.getMaxTokens() == null) config.setMaxTokens(2048);
        if (config.getTemperature() == null) config.setTemperature(0.7);
        if (config.getApiFormat() == null || config.getApiFormat().isEmpty()) {
            config.setApiFormat("openai_compatible");
        }
        if (config.getEnabled() == null) config.setEnabled(false);

        // 修改点：使用 upsert 逻辑，providerCode 已存在则更新而非抛 DuplicateKeyException
        AiProviderConfig saved = providerConfigService.saveOrUpdateByCode(config);
        String action = (saved.getId() != null && saved.getId().equals(config.getId())) ? "更新" : "新增";
        log.info("{}AI供应商: code={}, name={}, id={}", action, saved.getProviderCode(), saved.getProviderName(), saved
                .getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("action", action);
        result.put("msg", action + "成功");
        return R.success(result);
    }

    /**
     * 更新。
     * @param config 参数 config
     * @return 返回结果
     */
    @PostMapping("/update")
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "更新供应商配置", description = "修改AI供应商配置信息")
    public R<String> update(
            @Parameter(description = "供应商配置信息", required = true) @RequestBody AiProviderConfig config) {
        if (config.getId() == null) {
            return R.error("ID不能为空");
        }
        String validation = validateProviderConfig(config);
        if (validation != null) {
            return R.error(validation);
        }
        // 不允许通过更新修改 isActive，需通过 activate 接口
        AiProviderConfig existing = providerConfigService.getById(config.getId());
        if (existing == null) {
            return R.error("供应商配置不存在");
        }
        config.setIsActive(existing.getIsActive());
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            config.setApiKey(existing.getApiKey());
        } else {
            // 修复 P0-6：存入数据库前加密 apiKey
            String encrypted = AiKeyEncryptor.encrypt(config.getApiKey());
            if (encrypted == null) {
                return R.error("API密钥加密失败，请检查 REGGIE_AI_KEY 环境变量");
            }
            config.setApiKey(encrypted);
        }
        providerConfigService.updateById(config);

        if (Boolean.TRUE.equals(existing.getIsActive())) {
            aiProviderManager.reloadConfig();
            log.info("已更新激活的供应商配置并刷新缓存: code={}", existing.getProviderCode());
        }
        return R.success("更新成功");
    }

    /**
     * 删除。
     * @param id 参数 id
     * @return 返回结果
     */
    @DeleteMapping("/delete/{id}")
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "删除供应商", description = "软删除AI供应商配置，不能删除当前激活的供应商")
    public R<String> delete(
            @Parameter(description = "供应商配置ID", required = true) @PathVariable Long id) {
        AiProviderConfig config = providerConfigService.getById(id);
        if (config != null && Boolean.TRUE.equals(config.getIsActive())) {
            return R.error("不能删除当前激活的供应商，请先切换其他供应商");
        }
        providerConfigService.removeById(id);
        return R.success("删除成功");
    }

    /**
     * 处理 activate。
     * @param id 参数 id
     * @return 返回结果
     */
    @PostMapping("/activate/{id}")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "切换供应商", description = "激活指定供应商（切换后AI将使用该供应商）")
    public R<String> activate(@Parameter(description = "供应商配置ID", required = true) @PathVariable Long id) {
        AiProviderConfig target = providerConfigService.getById(id);
        if (target == null) {
            return R.error("供应商配置不存在");
        }
        String validation = validateProviderConfig(target);
        if (validation != null) {
            return R.error("供应商配置不完整，无法激活：" + validation);
        }

        boolean success = providerConfigService.activateProvider(id);
        if (success) {
            // 通知 ProviderManager 重新从数据库加载配置
            aiProviderManager.reloadConfig();
            log.info("供应商已切换，ID={}, code={}", id, target.getProviderCode());
            return R.success("切换成功，AI将使用「" + target.getProviderName() + "」");
        }
        return R.error("切换失败");
    }

    /**
     * 获取 detail。
     * @param id 参数 id
     * @return 返回结果
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "获取单个供应商", description = "获取指定供应商的配置（API密钥已脱敏）")
    public R<AiProviderConfig> getDetail(@Parameter(description = "供应商配置ID", required = true) @PathVariable Long id) {
        AiProviderConfig config = providerConfigService.getById(id);
        if (config == null) {
            return R.error("供应商配置不存在");
        }
        maskSensitiveFields(config);
        return R.success(config);
    }

    /**
     * 处理 test。
     * @param id 参数 id
     * @return 返回结果
     */
    @GetMapping("/test/{id}")
    @RateLimit(maxRequestsPerSecond = 2)
    @Operation(summary = "测试连通性", description = "测试指定AI供应商的连接是否正常")
    public R<Map<String, String>> test(@Parameter(description = "供应商配置ID", required = true) @PathVariable Long id) {
        String result = providerConfigService.testProvider(id);
        Map<String, String> resp = new HashMap<>();
        resp.put("result", result);
        resp.put("success", result.startsWith("SUCCESS") ? "true" : "false");
        return R.success(resp);
    }

    // ==================== 修改点：从供应商API拉取模型列表 ====================

    /**
     * 处理 fetch models。
     * @param params 参数 params
     * @return 返回结果
     */
    @PostMapping("/fetch-models")
    @RateLimit(maxRequestsPerSecond = 2)
    @Operation(summary = "拉取模型列表", description = "调用AI供应商的 /models 接口获取可用模型列表（参考ChatBox/NextChat交互模式）")
    public R<List<String>> fetchModels(@Parameter(description = "请求参数（baseUrl API地址、apiKey 密钥）", required =
            true) @RequestBody Map<String, String> params) {
        String baseUrl = params.get("baseUrl");
        String apiKey = params.get("apiKey");

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return R.error("API 地址不能为空");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return R.error("API 密钥不能为空");
        }

        log.info("拉取模型列表: baseUrl={}", baseUrl);
        List<String> models = providerConfigService.fetchModelList(baseUrl.trim(), apiKey.trim());

        if (models.isEmpty()) {
            return R.error("未能获取到模型列表，请检查 API 地址和密钥是否正确。提示：支持 OpenAI 兼容格式的 /models 接口。");
        }

        return R.success(models);
    }

    /**
     * 校验供应商配置完整性
     */
    private String validateProviderConfig(AiProviderConfig config) {
        if (config.getProviderCode() == null || config.getProviderCode().trim().isEmpty()) {
            return "供应商编码不能为空";
        }
        if (config.getProviderName() == null || config.getProviderName().trim().isEmpty()) {
            return "供应商名称不能为空";
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
            return "API基础URL不能为空";
        }
        if (config.getModelName() == null || config.getModelName().trim().isEmpty()) {
            return "模型名称不能为空";
        }
        return null;
    }

    /**
     * 脱敏敏感字段：API密钥、extraHeaders（可能含API Key等凭证）
     */
    private void maskSensitiveFields(AiProviderConfig config) {
        if (config.getApiKey() != null && config.getApiKey().length() > 8) {
            config.setApiKey(config.getApiKey().substring(0, 4) + "****"
                    + config.getApiKey().substring(config.getApiKey().length() - 4));
        }
        // extraHeaders 可能包含 {"api-key": "xxx"} 等凭证信息，直接置空防止泄露
        if (config.getExtraHeaders() != null && !config.getExtraHeaders().isEmpty()) {
            config.setExtraHeaders("");
        }
    }

    // ==================== 预设供应商快捷初始化 ====================

    /**
     * 初始化 presets。
     * @return 返回结果
     */
    @PostMapping("/init-presets")
    @RateLimit(maxRequestsPerSecond = 1)
    @Operation(summary = "初始化预设供应商", description = "批量添加国产大模型预设配置（仅当无配置时生效）")
    public R<String> initPresets() {
        if (providerConfigService.count() > 0) {
            return R.error("已存在供应商配置，如需重置请先清空");
        }

        List<AiProviderConfig> presets = getPresetProviders();
        for (AiProviderConfig preset : presets) {
            providerConfigService.save(preset);
        }
        return R.success("已初始化 " + presets.size() + " 个预设供应商，请在后台配置API密钥后启用");
    }

    /**
     * 预置国产大模型配置
     */
    private List<AiProviderConfig> getPresetProviders() {
        List<AiProviderConfig> list = new ArrayList<>();

        // 1. DeepSeek（默认不激活，需管理员配置 API Key 后手动激活）
        list.add(preset("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat",
                "openai_compatible", 2048, 1, "DeepSeek V3，性价比高，支持128K上下文"));
        // 2. 通义千问 Qwen
        list.add(preset("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-turbo",
                "openai_compatible", 2048, 2, "阿里通义千问，DashScope API，国内稳定"));
        // 3. 智谱 AI GLM
        list.add(preset("zhipu", "智谱AI", "https://open.bigmodel.cn/api/paas/v4", "glm-4",
                "openai_compatible", 2048, 3, "智谱清言 GLM-4，清华技术背景"));
        // 4. 百度文心一言
        list.add(preset("ernie", "文心一言",
                "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat", "ernie-4.0",
                "baidu", 2048, 4, "百度文心一言 ERNIE 4.0"));
        // 5. 百川智能
        list.add(preset("baichuan", "百川智能", "https://api.baichuan-ai.com/v1", "Baichuan2-Turbo",
                "openai_compatible", 2048, 5, "百川智能 Baichuan2，开源模型"));
        // 6. 月之暗面 Moonshot
        list.add(preset("moonshot", "月之暗面", "https://api.moonshot.cn/v1", "moonshot-v1-8k",
                "openai_compatible", 2048, 6, "月之暗面 Kimi，长文本能力突出"));
        // 7. MiniMax
        list.add(preset("minimax", "MiniMax", "https://api.minimax.chat/v1", "minimax/MiniMax-M1-80k",
                "openai_compatible", 2048, 7, "MiniMax M1 系列"));
        // 8. 360智脑
        list.add(preset("360", "360智脑", "https://api.360.cn/v1/chat", "360gpt-turbo",
                "360", 2048, 8, "360智脑"));
        // 9. Anthropic Claude
        list.add(preset("anthropic", "Anthropic Claude", "https://api.anthropic.com/v1",
                "claude-sonnet-4-20250514", "anthropic", 4096, 9,
                "Anthropic Claude Sonnet 4，支持200K上下文，需海外API Key"));

        return list;
    }

    /**
     * 构造单个预置供应商配置（等价抽取，降低方法长度）。
     *
     * @param code 供应商编码
     * @param name 供应商名称
     * @param baseUrl 接口地址
     * @param model 模型名
     * @param apiFormat 接口格式
     * @param maxTokens 最大 token
     * @param sort 排序
     * @param remark 备注
     * @return 预置配置
     */
    private AiProviderConfig preset(String code, String name, String baseUrl, String model,
            String apiFormat, int maxTokens, int sort, String remark) {
        AiProviderConfig c = new AiProviderConfig();
        c.setProviderCode(code);
        c.setProviderName(name);
        c.setBaseUrl(baseUrl);
        c.setModelName(model);
        c.setApiFormat(apiFormat);
        c.setTimeout(60);
        c.setMaxTokens(maxTokens);
        c.setTemperature(0.7);
        c.setEnabled(true);
        c.setIsActive(false);
        c.setSort(sort);
        c.setRemark(remark);
        return c;
    }
}


