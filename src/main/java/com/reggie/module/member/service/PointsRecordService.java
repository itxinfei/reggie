package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.PointsRecord;

/**
 * <p>
 * 积分记录服务接口
 * </p>
 * <p>记录会员积分的获取和消费流水</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface PointsRecordService extends IService<PointsRecord> {

    /**
     * 查询会员的积分流水
     *
     * @param memberId 会员ID
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页积分流水
     */
    Page<PointsRecord> listByMember(Long memberId, int page, int pageSize);

    /**
     * 查询会员的积分余额
     *
     * @param memberId 会员ID
     * @return 积分余额
     */
    int getBalance(Long memberId);
}
