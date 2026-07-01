package com.reggie.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.member.model.Member;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
