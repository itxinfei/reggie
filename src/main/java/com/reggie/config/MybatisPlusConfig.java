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

@Configuration
public class MybatisPlusConfig {

    /**
     * 不需要租户隔离的表：
     * - tenant: 租户表本身不需要 tenant_id
     * - employee: 在 EmployeeController 中手动添加了租户过滤
     * - shopping_cart: 暂无 tenant_id 列，通过 userId 关联隔离
     * - order_detail: 暂无 tenant_id 列，通过 orderId 关联隔离
     */
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
        "tenant", "employee", "shopping_cart", "order_detail"
    ));

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            // 修改点：记录是否有租户上下文（null表示无租户/未登录浏览场景）
            @Override
            public Expression getTenantId() {
                Long tenantId = BaseContext.getCurrentTenantId();
                if (tenantId == null) {
                    // 修改点：MP 3.4.2的TenantLineInnerInterceptor不检查null，
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
                // 修改点：无租户上下文时全局跳过租户隔离，避免MP 3.4.2生成tenant_id = null
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
