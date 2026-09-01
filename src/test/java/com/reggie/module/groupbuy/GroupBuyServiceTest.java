package com.reggie.module.groupbuy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.groupbuy.mapper.GroupBuyParticipationMapper;
import com.reggie.module.groupbuy.model.GroupBuyCampaign;
import com.reggie.module.groupbuy.model.GroupBuyParticipation;
import com.reggie.module.groupbuy.service.GroupBuyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 拼团服务单元测试
 * <p>覆盖：创建/更新/删除活动、加入拼团、成团校验、支付回调幂等、定时到期关闭。</p>
 *
 * @author 心飞为你飞
 * @since 2026-09-01
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-groupbuy-withdraw.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class GroupBuyServiceTest {

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private GroupBuyParticipationMapper participationMapper;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void createCampaign_validParams_returnsOpen() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7));
        GroupBuyCampaign saved = groupBuyService.createCampaign(campaign);
        assertNotNull(saved.getId());
        assertEquals("OPEN", saved.getStatus());
        assertEquals(1L, saved.getTenantId());
    }

    @Test
    void createCampaign_noTenant_throws() {
        BaseContext.remove();
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7));
        assertThrows(RuntimeException.class, () -> groupBuyService.createCampaign(campaign));
    }

    @Test
    void updateCampaign_validUpdatesStatus() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7));
        GroupBuyCampaign saved = groupBuyService.createCampaign(campaign);

        saved.setName("新名称");
        saved.setGroupPrice(new BigDecimal("9.90"));
        GroupBuyCampaign updated = groupBuyService.updateCampaign(saved);
        assertEquals("新名称", updated.getName());
        assertEquals(new BigDecimal("9.90"), updated.getGroupPrice());
    }

    @Test
    void deleteCampaign_removesRecord() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7));
        GroupBuyCampaign saved = groupBuyService.createCampaign(campaign);
        Long id = saved.getId();

        groupBuyService.deleteCampaign(id);
        assertNull(groupBuyService.getById(id));
    }

    @Test
    void joinGroupBuy_validJoin_returnsParticipation() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));
        groupBuyService.createCampaign(campaign);
        Long campaignId = campaign.getId();

        GroupBuyParticipation participation = groupBuyService.joinGroupBuy(
                campaignId, 999L, 100L);
        assertNotNull(participation.getId());
        assertEquals("JOINED", participation.getStatus());
        assertEquals(campaignId, participation.getGroupBuyId());
        assertEquals(999L, participation.getOrderId());
    }

    @Test
    void joinGroupBuy_campaignClosed_throws() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(1));
        groupBuyService.createCampaign(campaign);

        assertThrows(RuntimeException.class, () ->
                groupBuyService.joinGroupBuy(campaign.getId(), 1L, 1L));
    }

    @Test
    void joinGroupBuy_outOfTimeRange_throws() {
        // 开始时间在未来
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5));
        groupBuyService.createCampaign(campaign);

        assertThrows(RuntimeException.class, () ->
                groupBuyService.joinGroupBuy(campaign.getId(), 1L, 1L));
    }

    @Test
    void checkGroupBuyStatus_notEnoughParticipants_returnsFalse() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));
        campaign.setMinMembers(3);
        groupBuyService.createCampaign(campaign);

        assertFalse(groupBuyService.checkGroupBuyStatus(campaign.getId()));
    }

    @Test
    void checkGroupBuyStatus_enoughParticipants_returnsTrue() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));
        campaign.setMinMembers(2);
        groupBuyService.createCampaign(campaign);

        // 加入两个参与者
        groupBuyService.joinGroupBuy(campaign.getId(), 1L, 1L);
        groupBuyService.joinGroupBuy(campaign.getId(), 2L, 2L);

        assertTrue(groupBuyService.checkGroupBuyStatus(campaign.getId()));
    }

    @Test
    void markParticipationPaid_joinedMarkedAsPaid() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));
        groupBuyService.createCampaign(campaign);
        GroupBuyParticipation participation = groupBuyService.joinGroupBuy(
                campaign.getId(), 100L, 1L);

        groupBuyService.markParticipationPaid(participation.getOrderId());
        // REQUIRES_NEW 事务中更新，本地缓存未刷新，需重新查询
        List<GroupBuyParticipation> participations = participationMapper.selectList(
                new LambdaQueryWrapper<GroupBuyParticipation>()
                        .eq(GroupBuyParticipation::getOrderId, 100L));
        assertFalse(participations.isEmpty());
        assertEquals("PAID", participations.get(0).getStatus());
    }

    @Test
    void markParticipationPaid_nonGroupBuyOrder_isIdempotent() {
        // 非拼团单（无 JOINED 记录），应静默跳过不抛异常
        groupBuyService.markParticipationPaid(999L);
    }

    @Test
    void autoCloseExpiredCampaigns_closesExpired() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(1));
        campaign.setStatus("OPEN");
        groupBuyService.createCampaign(campaign);

        int closed = groupBuyService.autoCloseExpiredCampaigns();
        assertEquals(1, closed);

        GroupBuyCampaign closedCampaign = groupBuyService.getById(campaign.getId());
        assertEquals("ENDED", closedCampaign.getStatus());
    }

    @Test
    void autoCloseExpiredCampaigns_activeNotClosed() {
        GroupBuyCampaign campaign = buildCampaign(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));
        campaign.setStatus("OPEN");
        groupBuyService.createCampaign(campaign);

        int closed = groupBuyService.autoCloseExpiredCampaigns();
        assertEquals(0, closed);

        GroupBuyCampaign stillOpen = groupBuyService.getById(campaign.getId());
        assertEquals("OPEN", stillOpen.getStatus());
    }

    private GroupBuyCampaign buildCampaign(LocalDateTime start, LocalDateTime end) {
        GroupBuyCampaign c = new GroupBuyCampaign();
        c.setName("测试拼团");
        c.setDescription("拼团测试");
        c.setStartTime(start);
        c.setEndTime(end);
        c.setMinMembers(2);
        c.setMaxMembers(10);
        c.setOriginalPrice(new BigDecimal("15.00"));
        c.setGroupPrice(new BigDecimal("9.90"));
        c.setDishId(1L);
        c.setDishName("红烧肉");
        return c;
    }
}
