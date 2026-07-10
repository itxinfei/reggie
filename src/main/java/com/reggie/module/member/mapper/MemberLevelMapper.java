package com.reggie.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.member.model.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {
}
