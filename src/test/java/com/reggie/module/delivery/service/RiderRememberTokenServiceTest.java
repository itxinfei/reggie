package com.reggie.module.delivery.service;

import com.reggie.module.delivery.model.RiderRememberToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 骑手记住登录令牌服务集成测试（P2-2）：
 * 颁发/校验、重复颁发覆盖旧令牌、过期令牌失效、未知令牌拒绝、按骑手吊销。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-rider.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RiderRememberTokenServiceTest {

    private static final long RIDER_ID = 2002L;
    private static final long TENANT_ID = 999L;

    @Autowired
    private RiderRememberTokenService riderRememberTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM rider_remember_token WHERE rider_id = ?", RIDER_ID);
    }

    @Test
    void issueThenValidateSuccess() {
        RiderRememberToken token = riderRememberTokenService.issue(RIDER_ID, TENANT_ID);
        assertNotNull(token.getToken());
        assertEquals(32, token.getToken().length());

        RiderRememberToken validated = riderRememberTokenService.validate(token.getToken());
        assertNotNull(validated);
        assertEquals(RIDER_ID, validated.getRiderId());
        assertEquals(TENANT_ID, validated.getTenantId());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rider_remember_token WHERE rider_id = ?",
                Integer.class, RIDER_ID);
        assertEquals(1, count);
    }

    @Test
    void issueTwiceOldTokenRevoked() {
        // 每个骑手仅一条：重新登录颁发后旧令牌必须失效
        RiderRememberToken first = riderRememberTokenService.issue(RIDER_ID, TENANT_ID);
        RiderRememberToken second = riderRememberTokenService.issue(RIDER_ID, TENANT_ID);

        assertNull(riderRememberTokenService.validate(first.getToken()));
        assertNotNull(riderRememberTokenService.validate(second.getToken()));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rider_remember_token WHERE rider_id = ?",
                Integer.class, RIDER_ID);
        assertEquals(1, count);
    }

    @Test
    void validateExpiredReturnsNull() {
        RiderRememberToken token = riderRememberTokenService.issue(RIDER_ID, TENANT_ID);
        jdbcTemplate.update("UPDATE rider_remember_token SET expire_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE) "
                + "WHERE rider_id = ?", RIDER_ID);
        assertNull(riderRememberTokenService.validate(token.getToken()));
    }

    @Test
    void validateUnknownTokenReturnsNull() {
        riderRememberTokenService.issue(RIDER_ID, TENANT_ID);
        assertNull(riderRememberTokenService.validate("no-such-token-value"));
        assertNull(riderRememberTokenService.validate(""));
        assertNull(riderRememberTokenService.validate(null));
    }

    @Test
    void revokeByRiderRemovesToken() {
        RiderRememberToken token = riderRememberTokenService.issue(RIDER_ID, TENANT_ID);
        riderRememberTokenService.revokeByRider(RIDER_ID);
        assertNull(riderRememberTokenService.validate(token.getToken()));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rider_remember_token WHERE rider_id = ?",
                Integer.class, RIDER_ID);
        assertEquals(0, count);
    }
}
