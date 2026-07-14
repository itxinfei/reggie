package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberTag;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 会员标签服务接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface MemberTagService extends IService<MemberTag> {

    /**
     * 根据会员ID查询标签列表
     *
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @return 标签列表
     */
    List<MemberTag> listByMemberId(Long tenantId, Long memberId);

    /**
     * 根据标签类型查询标签列表
     *
     * @param tenantId 租户ID
     * @param tagType  标签类型（1手动添加 2自动生成）
     * @return 标签列表
     */
    List<MemberTag> listByTagType(Long tenantId, String tagType);

    /**
     * 根据业务标签查询标签列表
     *
     * @param tenantId 租户ID
     * @param bizTag   业务标签
     * @return 标签列表
     */
    List<MemberTag> listByBizTag(Long tenantId, String bizTag);

    /**
     * 按业务标签分组统计标签数量
     *
     * @param tenantId 租户ID
     * @return 业务标签数量统计 Map<bizTag, count>
     */
    Map<String, Long> countByBizTag(Long tenantId);

    /**
     * 为会员添加标签
     *
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param tagName  标签名称
     * @param bizTag   业务标签
     * @param tagColor 标签颜色
     * @return 是否添加成功
     */
    boolean addTag(Long tenantId, Long memberId, String tagName, String bizTag, String tagColor);

    /**
     * 批量删除会员标签
     *
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param tagIds   标签ID列表
     * @return 是否删除成功
     */
    boolean batchRemoveTags(Long tenantId, Long memberId, List<Long> tagIds);

    /**
     * 自动生成会员标签
     *
     * @param tenantId 租户ID
     * @return 生成的标签数量
     */
    int autoGenerateTags(Long tenantId);
}
