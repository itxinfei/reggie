package com.reggie.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.ai.model.AiProviderConfig;

import java.util.List;

/**
 * AI供应商配置服务接口
 * 管理员可在后台配置/切换不同的大模型供应商
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface AiProviderConfigService extends IService<AiProviderConfig> {

    /**
     * 获取当前激活的供应商配置
     */
    AiProviderConfig getActiveProvider();

    /**
     * 切换激活供应商
     */
    boolean activateProvider(Long id);

    /**
     * 获取所有启用的供应商列表
     */
    List<AiProviderConfig> listEnabled();

    /**
     * 测试供应商连通性
     */
    String testProvider(Long id);

    /**
     * 修改点：按 providerCode 执行 upsert——存在则更新，不存在则插入
     * <p>解决重复添加同一 provider_code 时的 DuplicateKeyException</p>
     *
     * @param config 供应商配置（新增时 id 为 null，更新时需在内部回填已有 id）
     * @return 持久化后的配置实体
     */
    AiProviderConfig saveOrUpdateByCode(AiProviderConfig config);

    /**
     * 修改点：从供应商 API 拉取可用模型列表
     * <p>调用 GET {baseUrl}/models（OpenAI 兼容格式），解析返回的模型 ID 列表</p>
     *
     * @param baseUrl API 基础地址
     * @param apiKey  API 密钥
     * @return 模型 ID 列表，失败时返回空列表
     */
    List<String> fetchModelList(String baseUrl, String apiKey);
}
