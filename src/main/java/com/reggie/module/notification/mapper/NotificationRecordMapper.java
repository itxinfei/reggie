package com.reggie.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.notification.model.NotificationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 通知发送记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface NotificationRecordMapper extends BaseMapper<NotificationRecord> {
}
