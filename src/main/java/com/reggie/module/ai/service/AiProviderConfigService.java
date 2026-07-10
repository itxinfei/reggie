package com.reggie.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.ai.model.AiProviderConfig;

import java.util.List;

/**
 * AI供应商配置服务
 * 管理员可在后台配置/切换不同的大模型供应商
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
}
