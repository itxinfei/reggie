package com.reggie.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.reggie.common.BaseContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * MyBatis-Plus配置类，配置多租户插件和分页插件。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 不需要租户隔离的表：
     * - tenant: 租户表本身不需要 tenant_id
     * - employee: 在 EmployeeController 中手动添加了租户过滤
     * - shopping_cart: 暂无 tenant_id 列，通过 userId 关联隔离
     * - ai_provider_config: 系统级AI大模型配置表，暂无 tenant_id 列，不需要租户隔离
     * - dish_evaluation: 暂无 tenant_id 列，通过 userId 关联隔离
     * - permission: 全局权限目录表，无 tenant_id 列
     * - role_permission: 角色-权限关联表，无 tenant_id 列
     * 修改点：permission/role_permission 两个表均无 tenant_id 列，若不加入忽略表，
     * TenantLineInnerInterceptor 会对其追加 WHERE tenant_id = ? 导致 Unknown column 异常。
     */
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
        "tenant", "employee", "shopping_cart", "ai_provider_config", "dish_evaluation",
        "permission", "role_permission"
    ));

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = BaseContext.getCurrentTenantId();
                if (tenantId == null) {
                    // MP 3.4.2的TenantLineInnerInterceptor不检查null，
                    // 直接add到SQL中导致WHERE tenant_id = null（永远false），
                    // 因此必须在ignoreTable中拦截所有表来跳过租户过滤。
                    return null;
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 无租户上下文时全局跳过租户隔离，避免MP 3.4.2生成tenant_id = null
                if (BaseContext.getCurrentTenantId() == null) {
                    return true;
                }
                return IGNORE_TABLES.contains(tableName);
            }
        }));
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return mybatisPlusInterceptor;
    }
}
