package com.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * 自定义元数据对象处理器，实现 MyBatis-Plus 自动填充功能
 * 自动填充字段：createTime、updateTime、createUser、updateUser、tenantId
 *
 * @author reggie
 * @since 2026-07-09
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作，自动填充公共字段
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("公共字段自动填充[insert]");

        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        Long currentId = BaseContext.getCurrentId();
        this.strictInsertFill(metaObject, "createUser", Long.class, currentId);
        this.strictInsertFill(metaObject, "updateUser", Long.class, currentId);
        this.strictInsertFill(metaObject, "tenantId", Long.class, BaseContext.getCurrentTenantId());
    }

    /**
     * 更新操作，自动填充公共字段
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("公共字段自动填充[update]");

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateUser", Long.class, BaseContext.getCurrentId());
    }
}
