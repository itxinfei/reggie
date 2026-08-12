package com.reggie.module.ai.controller;

import com.reggie.common.R;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.service.AiProviderConfigService;
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
@Tag(name = "AI供应商管理", description = "配置和切换大模型供应商")
public class AiProviderController {

    @Resource
    private AiProviderConfigService providerConfigService;

    @Resource
    private AiProviderManager aiProviderManager;

    @GetMapping("/list")
    @Operation(summary = "供应商列表", description = "获取所有AI供应商配置")
    public R<List<AiProviderConfig>> list() {
        List<AiProviderConfig> list = providerConfigService.list();
        // 脱敏API密钥
        list.forEach(p -> {
            if (p.getApiKey() != null && p.getApiKey().length() > 8) {
                p.setApiKey(p.getApiKey().substring(0, 4) + "****" + p.getApiKey().substring(p.getApiKey().length() - 4));
            }
        });
        return R.success(list);
    }

    @GetMapping("/active")
    @Operation(summary = "当前激活供应商", description = "获取当前正在使用的AI供应商")
    public R<AiProviderConfig> getActive() {
        AiProviderConfig config = providerConfigService.getActiveProvider();
        if (config != null && config.getApiKey() != null && config.getApiKey().length() > 8) {
            config.setApiKey(config.getApiKey().substring(0, 4) + "****" + config.getApiKey().substring(config.getApiKey().length() - 4));
        }
        return R.success(config);
    }

    @PostMapping("/add")
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
        log.info("{}AI供应商: code={}, name={}, id={}", action, saved.getProviderCode(), saved.getProviderName(), saved.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("action", action);
        result.put("msg", action + "成功");
        return R.success(result);
    }

    @PostMapping("/update")
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
        }
        providerConfigService.updateById(config);

        if (Boolean.TRUE.equals(existing.getIsActive())) {
            aiProviderManager.reloadConfig();
            log.info("已更新激活的供应商配置并刷新缓存: code={}", existing.getProviderCode());
        }
        return R.success("更新成功");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除供应商", description = "软删除AI供应商配置，不能删除当前激活的供应商")
    public R<String> delete(
            @Parameter(description = "I d")
            @Parameter(description = "供应商配置ID", required = true) @PathVariable Long id) {
        AiProviderConfig config = providerConfigService.getById(id);
        if (config != null && Boolean.TRUE.equals(config.getIsActive())) {
            return R.error("不能删除当前激活的供应商，请先切换其他供应商");
        }
        providerConfigService.removeById(id);
        return R.success("删除成功");
    }

    @PostMapping("/activate/{id}")
    @Operation(summary = "切换供应商", description = "激活指定供应商（切换后AI将使用该供应商）")
    @Parameter(description = "I d")
    public R<String> activate(@PathVariable Long id) {
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

    @GetMapping("/get/{id}")
    @Operation(summary = "获取单个供应商", description = "获取指定供应商的配置（API密钥已脱敏）")
    @Parameter(description = "I d")
    public R<AiProviderConfig> getDetail(@PathVariable Long id) {
        AiProviderConfig config = providerConfigService.getById(id);
        if (config == null) {
            return R.error("供应商配置不存在");
        }
        // 脱敏：不返回完整 API 密钥
        if (config.getApiKey() != null && config.getApiKey().length() > 8) {
            config.setApiKey(config.getApiKey().substring(0, 4) + "****"
                    + config.getApiKey().substring(config.getApiKey().length() - 4));
        }
        return R.success(config);
    }

    @GetMapping("/test/{id}")
    @Operation(summary = "测试连通性", description = "测试指定AI供应商的连接是否正常")
    @Parameter(description = "I d")
    public R<Map<String, String>> test(@PathVariable Long id) {
        String result = providerConfigService.testProvider(id);
        Map<String, String> resp = new HashMap<>();
        resp.put("result", result);
        resp.put("success", result.startsWith("SUCCESS") ? "true" : "false");
        return R.success(resp);
    }

    // ==================== 修改点：从供应商API拉取模型列表 ====================

    @PostMapping("/fetch-models")
    @Operation(summary = "拉取模型列表", description = "调用AI供应商的 /models 接口获取可用模型列表（参考ChatBox/NextChat交互模式）")
    public R<List<String>> fetchModels(@RequestBody Map<String, String> params) {
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

    // ==================== 预设供应商快捷初始化 ====================

    @PostMapping("/init-presets")
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
     * 预置国产大模型配置
     */
    private List<AiProviderConfig> getPresetProviders() {
        List<AiProviderConfig> list = new ArrayList<>();

        // 1. DeepSeek（默认不激活，需管理员配置 API Key 后手动激活）
        AiProviderConfig ds = new AiProviderConfig();
        ds.setProviderCode("deepseek");
        ds.setProviderName("DeepSeek");
        ds.setBaseUrl("https://api.deepseek.com/v1");
        ds.setModelName("deepseek-chat");
        ds.setApiFormat("openai_compatible");
        ds.setTimeout(60);
        ds.setMaxTokens(2048);
        ds.setTemperature(0.7);
        ds.setEnabled(true);
        ds.setIsActive(false);
        ds.setSort(1);
        ds.setRemark("DeepSeek V3，性价比高，支持128K上下文");
        list.add(ds);

        // 2. 通义千问 Qwen
        AiProviderConfig qwen = new AiProviderConfig();
        qwen.setProviderCode("qwen");
        qwen.setProviderName("通义千问");
        qwen.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        qwen.setModelName("qwen-turbo");
        qwen.setApiFormat("openai_compatible");
        qwen.setTimeout(60);
        qwen.setMaxTokens(2048);
        qwen.setTemperature(0.7);
        qwen.setEnabled(true);
        qwen.setIsActive(false);
        qwen.setSort(2);
        qwen.setRemark("阿里通义千问，DashScope API，国内稳定");
        list.add(qwen);

        // 3. 智谱 AI GLM
        AiProviderConfig zhipu = new AiProviderConfig();
        zhipu.setProviderCode("zhipu");
        zhipu.setProviderName("智谱AI");
        zhipu.setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
        zhipu.setModelName("glm-4");
        zhipu.setApiFormat("openai_compatible");
        zhipu.setTimeout(60);
        zhipu.setMaxTokens(2048);
        zhipu.setTemperature(0.7);
        zhipu.setEnabled(true);
        zhipu.setIsActive(false);
        zhipu.setSort(3);
        zhipu.setRemark("智谱清言 GLM-4，清华技术背景");
        list.add(zhipu);

        // 4. 百度文心一言
        AiProviderConfig ernie = new AiProviderConfig();
        ernie.setProviderCode("ernie");
        ernie.setProviderName("文心一言");
        ernie.setBaseUrl("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat");
        ernie.setModelName("ernie-4.0");
        ernie.setApiFormat("baidu");
        ernie.setTimeout(60);
        ernie.setMaxTokens(2048);
        ernie.setTemperature(0.7);
        ernie.setEnabled(true);
        ernie.setIsActive(false);
        ernie.setSort(4);
        ernie.setRemark("百度文心一言 ERNIE 4.0");
        list.add(ernie);

        // 5. 百川智能
        AiProviderConfig baichuan = new AiProviderConfig();
        baichuan.setProviderCode("baichuan");
        baichuan.setProviderName("百川智能");
        baichuan.setBaseUrl("https://api.baichuan-ai.com/v1");
        baichuan.setModelName("Baichuan2-Turbo");
        baichuan.setApiFormat("openai_compatible");
        baichuan.setTimeout(60);
        baichuan.setMaxTokens(2048);
        baichuan.setTemperature(0.7);
        baichuan.setEnabled(true);
        baichuan.setIsActive(false);
        baichuan.setSort(5);
        baichuan.setRemark("百川智能 Baichuan2，开源模型");
        list.add(baichuan);

        // 6. 月之暗面 Moonshot
        AiProviderConfig moonshot = new AiProviderConfig();
        moonshot.setProviderCode("moonshot");
        moonshot.setProviderName("月之暗面");
        moonshot.setBaseUrl("https://api.moonshot.cn/v1");
        moonshot.setModelName("moonshot-v1-8k");
        moonshot.setApiFormat("openai_compatible");
        moonshot.setTimeout(60);
        moonshot.setMaxTokens(2048);
        moonshot.setTemperature(0.7);
        moonshot.setEnabled(true);
        moonshot.setIsActive(false);
        moonshot.setSort(6);
        moonshot.setRemark("月之暗面 Kimi，长文本能力突出");
        list.add(moonshot);

        // 7. MiniMax
        AiProviderConfig minimax = new AiProviderConfig();
        minimax.setProviderCode("minimax");
        minimax.setProviderName("MiniMax");
        minimax.setBaseUrl("https://api.minimax.chat/v1");
        minimax.setModelName("minimax/MiniMax-M1-80k");
        minimax.setApiFormat("openai_compatible");
        minimax.setTimeout(60);
        minimax.setMaxTokens(2048);
        minimax.setTemperature(0.7);
        minimax.setEnabled(true);
        minimax.setIsActive(false);
        minimax.setSort(7);
        minimax.setRemark("MiniMax M1 系列");
        list.add(minimax);

        // 8. 360智脑
        AiProviderConfig zhinv = new AiProviderConfig();
        zhinv.setProviderCode("360");
        zhinv.setProviderName("360智脑");
        zhinv.setBaseUrl("https://api.360.cn/v1/chat");
        zhinv.setModelName("360gpt-turbo");
        zhinv.setApiFormat("360");
        zhinv.setTimeout(60);
        zhinv.setMaxTokens(2048);
        zhinv.setTemperature(0.7);
        zhinv.setEnabled(true);
        zhinv.setIsActive(false);
        zhinv.setSort(8);
        zhinv.setRemark("360智脑");
        list.add(zhinv);

        // 9. Anthropic Claude
        AiProviderConfig claude = new AiProviderConfig();
        claude.setProviderCode("anthropic");
        claude.setProviderName("Anthropic Claude");
        claude.setBaseUrl("https://api.anthropic.com/v1");
        claude.setModelName("claude-sonnet-4-20250514");
        claude.setApiFormat("anthropic");
        claude.setTimeout(60);
        claude.setMaxTokens(4096);
        claude.setTemperature(0.7);
        claude.setEnabled(true);
        claude.setIsActive(false);
        claude.setSort(9);
        claude.setRemark("Anthropic Claude Sonnet 4，支持200K上下文，需海外API Key");
        list.add(claude);

        return list;
    }
}


