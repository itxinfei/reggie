package com.reggie.test;

import com.reggie.common.BaseContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * 测试租户上下文监听器。
 *
 * <p>为每个测试方法在执行前设置默认租户 {@link TestDatabaseCleaner#TEST_TENANT_ID}（999），
 * 使 MyBatis-Plus 租户插件自动把 SQL 限定到测试租户，测试只见自己造的数据；
 * 方法结束后清理 ThreadLocal，避免线程复用造成租户串号。</p>
 *
 * <p>通过 META-INF/spring.factories 注册，对全部 Spring 测试默认生效；
 * 需要跨租户的用例可在自身 @BeforeEach 显式 setCurrentTenantId 覆盖。</p>
 *
 * @author reggie
 * @since 2026-09-25
 */
public class TenantTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        BaseContext.setCurrentTenantId(TestDatabaseCleaner.TEST_TENANT_ID);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        BaseContext.remove();
    }
}
