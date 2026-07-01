package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.MemberLevel;

public interface MemberLevelService extends IService<MemberLevel> {
    MemberLevel getDefaultLevel();
    MemberLevel findLevelByPoints(Long points);
}
