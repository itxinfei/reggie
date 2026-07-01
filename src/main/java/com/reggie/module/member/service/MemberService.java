package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.Member;
import java.math.BigDecimal;

public interface MemberService extends IService<Member> {
    Member registerByPhone(String phone, String name);
    boolean deductBalance(Long memberId, BigDecimal amount);
    void addPoints(Long memberId, int points, String bizType, Long bizId);
    BigDecimal calculateDiscount(Long memberId, BigDecimal amount);
}
