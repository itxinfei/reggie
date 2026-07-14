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
     * - order_detail: 暂无 tenant_id 列，通过 orderId 关联隔离
     * - ai_provider_config: 系统级AI大模型配置表，暂无 tenant_id 列，不需要租户隔离
     */
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
        "tenant", "employee", "shopping_cart", "order_detail", "ai_provider_config", "dish_evaluation"
    ));

    /**
     * 配置MyBatis-Plus拦截器
     * 包含多租户拦截器和分页拦截器
     *
     * @return MyBatis-Plus拦截器
     */
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
