package com.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * <p>
 * 自定义元数据对象处理器，实现 MyBatis-Plus 自动填充功能
 * </p>
 * <p>
 * 自动填充字段：createTime、updateTime、createUser、updateUser、tenantId
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Component
@Slf4j
public class MyMetaObjecthandler implements MetaObjectHandler {
    /**
     * 插入操作，自动填充公共字段
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("公共字段自动填充[insert]");

        if (metaObject.hasSetter("createTime")) {
            metaObject.setValue("createTime", LocalDateTime.now());
        }
        if (metaObject.hasSetter("createdTime")) {
            metaObject.setValue("createdTime", LocalDateTime.now());
        }
        if (metaObject.hasSetter("updateTime")) {
            metaObject.setValue("updateTime", LocalDateTime.now());
        }

        if (metaObject.hasSetter("createUser")) {
            metaObject.setValue("createUser", BaseContext.getCurrentId());
        }
        if (metaObject.hasSetter("updateUser")) {
            metaObject.setValue("updateUser", BaseContext.getCurrentId());
        }
        if (metaObject.hasSetter("tenantId")) {
            metaObject.setValue("tenantId", BaseContext.getCurrentTenantId());
        }
    }

    /**
     * 更新操作，自动填充公共字段
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("公共字段自动填充[update]");

        if (metaObject.hasSetter("updateTime")) {
            metaObject.setValue("updateTime", LocalDateTime.now());
        }

        if (metaObject.hasSetter("updateUser")) {
            metaObject.setValue("updateUser", BaseContext.getCurrentId());
        }
    }
}
