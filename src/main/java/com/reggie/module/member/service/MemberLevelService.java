package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.MemberLevel;

/**
 * <p>
 * 会员等级服务接口
 * </p>
 * <p>管理会员等级体系（根据累计积分自动升级）</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface MemberLevelService extends IService<MemberLevel> {

    /**
     * 获取默认会员等级（新注册用户初始等级）
     *
     * @return 默认会员等级
     */
    MemberLevel getDefaultLevel();

    /**
     * 根据累计积分查找对应等级
     *
     * @param points 累计积分
     * @return 匹配的会员等级
     */
    MemberLevel findLevelByPoints(Long points);
}
