package com.reggie.module.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.platform.model.PlatformConfig;

import java.util.List;

/**
 * 外卖平台接入配置服务接口
 *
 * @author reggie
 * @since 2026-08-24
 */
public interface PlatformConfigService extends IService<PlatformConfig> {

    /**
     * 分页列表查询（凭据脱敏，按更新时间倒序）
     */
    IPage<PlatformConfig> pageMasked(IPage<PlatformConfig> page);

    /**
     * 按 ID 查询（凭据脱敏）
     */
    PlatformConfig getMaskedById(Long id);

    /**
     * 新增配置：明文凭据在保存前加密
     *
     * @return 持久化后的实体
     */
    PlatformConfig addConfig(PlatformConfig config);

    /**
     * 更新配置：仅非空明文凭据重新加密，空值保留原密文
     */
    boolean updateConfig(PlatformConfig config);

    /**
     * 启用 / 停用
     *
     * @param enabled 1 启用 / 0 停用
     */
    boolean setEnabled(Long id, Integer enabled);

    /**
     * 校验同租户下是否存在相同平台类型 + 门店 ID 的配置
     */
    boolean existsByTypeAndShop(String platformType, String shopId, Long exceptId);

    /**
     * 查询所有启用的平台配置
     */
    List<PlatformConfig> listEnabledConfigs();

    /**
     * 按平台类型和租户查询配置
     *
     * @param platformType 平台类型
     * @param tenantId     租户ID
     * @return 平台配置，不存在返回 null
     */
    PlatformConfig getByPlatformType(String platformType, Long tenantId);
}
