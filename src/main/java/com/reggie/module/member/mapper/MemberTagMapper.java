package com.reggie.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.member.model.MemberTag;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 会员标签 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface MemberTagMapper extends BaseMapper<MemberTag> {

    /**
     * 根据租户ID和业务标签分组统计数量
     *
     * @param tenantId 租户ID
     * @return 各业务标签数量统计
     */
    List<Map<String, Object>> countByBizTag(Long tenantId);
}
