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
     * 使用 strictInsertFill：仅在字段为 null 时填充，避免覆盖业务显式设置的值
     * （例如更新操作中 createUser/tenantId 不应被覆盖）
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("公共字段自动填充[insert]");

        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        Long currentId = BaseContext.getCurrentId();
        this.strictInsertFill(metaObject, "createUser", Long.class, currentId);
        this.strictInsertFill(metaObject, "updateUser", Long.class, currentId);
        this.strictInsertFill(metaObject, "tenantId", Long.class, BaseContext.getCurrentTenantId());
    }

    /**
     * 更新操作，自动填充公共字段
     * 使用 strictUpdateFill：仅在字段为 null 时填充，避免覆盖业务显式设置的值
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("公共字段自动填充[update]");

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateUser", Long.class, BaseContext.getCurrentId());
    }
}
