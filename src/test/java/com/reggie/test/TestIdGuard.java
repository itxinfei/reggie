package com.reggie.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 测试硬编码主键防护（单库隔离语义配套，2026-10-06 起）。
 *
 * <p><b>背景</b>：测试与开发共用本地 <code>reggie</code> 单库，测试里显式 setId 后 insert 的
 * 主键如果与演示数据（租户 1）的既有主键撞库，表现为难以定位的 DuplicateKey 异常；
 * 若撞上的是 DELETE 条件覆盖不到的行，还可能把脏数据留进演示态。</p>
 *
 * <p><b>用法</b>：测试在显式主键 insert 之前调用
 * {@link #assertAbsent(String, long[])}，主键被占用时立刻 fail-fast 并给出处理指引：</p>
 * <pre>{@code
 * @Autowired
 * private TestIdGuard testIdGuard;
 *
 * testIdGuard.assertAbsent("shopping_cart", 990101L, 990102L);
 * }</pre>
 *
 * <p><b>选 ID 约定</b>：新测试的显式主键优先选 <code>990000</code> 以上的高位段，
 * 并先对照 <code>db/reggie.sql</code> 确认该段在目标表无演示数据（已知演示段：
 * cashier_record 6000-6024、user 990002-990009、shopping_cart 101-143、orders 800002001+）。</p>
 *
 * @since 2026-10-06
 */
@Component
public class TestIdGuard {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 断言指定主键在表中不存在（为随后的显式主键 INSERT 让路）。
     * 任一主键已被占用即抛异常：租户 1 行说明撞了演示数据须换 ID；
     * 租户 999 行说明上次测试残留未清，须先跑对应 @Sql / cleanTables 清理。
     *
     * @param table 表名
     * @param ids   待插入的显式主键
     */
    public void assertAbsent(String table, long... ids) {
        for (long id : ids) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM " + table + " WHERE id = ?",
                    Integer.class, id);
            if (count != null && count > 0) {
                throw new IllegalStateException(
                        "TestIdGuard: 表 [" + table + "] 主键 [" + id + "] 已被占用（可能是演示数据或测试残留）。"
                                + "请换用 db/reggie.sql 中未占用的测试 ID（约定 990000+ 高位段），"
                                + "或先清理该残留再跑本测试。");
            }
        }
    }
}
