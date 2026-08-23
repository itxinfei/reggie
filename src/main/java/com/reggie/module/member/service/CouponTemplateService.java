package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.CouponEffectVO;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.ExpiringByTemplateVO;
import com.reggie.module.member.model.ExpiringCouponVO;
import com.reggie.module.member.model.IssuedMemberVO;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 优惠券模板服务接口
 * </p>
 * <p>提供优惠券领取、使用、过期清理等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface CouponTemplateService extends IService<CouponTemplate> {

    /**
     * 领取优惠券
     *
     * @param memberId   会员ID
     * @param templateId 优惠券模板ID
     * @return 是否领取成功
     */
    boolean claimCoupon(Long memberId, Long templateId);

    /**
     * 批量清理过期优惠券
     */
    void expireCoupons();

    /**
     * 优惠券模板统计：总数、启用/禁用/已领完数量、累计发放/领取数、使用率
     * 修改点：替代前端 pageSize=1000 拉全量后在浏览器聚合的统计方式，改为后端聚合
     * @return 统计结果 Map
     */
    Map<String, Object> getStats();

    /**
     * 批量定向发券（按会员ID列表发放）
     *
     * @param templateId 优惠券模板ID
     * @param memberIds  会员ID列表
     * @return 发放结果：successCount/failCount/alreadyIssuedCount/total
     */
    Map<String, Object> batchIssue(Long templateId, List<Long> memberIds);

    /**
     * 条件定向发券（按条件筛选会员后发放）
     *
     * @param templateId 优惠券模板ID
     * @param condition  筛选条件（levelId/status/minPoints/maxPoints/minConsumption/maxConsumption/newMemberDays）
     * @return 发放结果：successCount/failCount/alreadyIssuedCount/total
     */
    Map<String, Object> issueByCondition(Long templateId, Map<String, Object> condition);

    /**
     * 分页查询某模板的发放会员明细
     *
     * @param page     分页参数
     * @param templateId 模板ID
     * @return 发放会员分页列表（含会员姓名/手机/等级/用券状态/领取/使用时间）
     */
    Page<IssuedMemberVO> issuedMembers(Page<IssuedMemberVO> page, Long templateId);

    /**
     * 查询某模板的投放效果聚合指标
     *
     * @param templateId 模板ID
     * @return 投放效果 VO（发放率/使用率/活跃率/状态分布）
     */
    CouponEffectVO effect(Long templateId);

    /**
     * 查询即将到期优惠券明细（分页）
     * <p>
     * 查询条件：status='unused' 且 expireTime 在 [NOW, NOW+days] 范围内。
     * 支持按 templateId 筛选、按到期时间排序、按会员手机模糊查询。
     * </p>
     *
     * @param page      分页参数
     * @param days      预警天数窗口（如 3 = 未来 3 天）
     * @param templateId 优惠券模板ID（可选）
     * @param phone      会员手机（可选，模糊查询）
     * @return 即将到期优惠券分页列表
     */
    Page<ExpiringCouponVO> expiringCoupons(Page<ExpiringCouponVO> page, int days, Long templateId, String phone);

    /**
     * 查询已过期优惠券明细（分页）
     * <p>
     * 查询条件：status='expired'，按 expireTime 倒序。
     * 支持按 templateId 筛选、按会员手机模糊查询。
     * </p>
     *
     * @param page      分页参数
     * @param templateId 优惠券模板ID（可选）
     * @param phone      会员手机（可选，模糊查询）
     * @return 已过期优惠券分页列表
     */
    Page<ExpiringCouponVO> expiredCoupons(Page<ExpiringCouponVO> page, Long templateId, String phone);

    /**
     * 优惠券到期预警统计
     * <p>
     * 返回各模板的即将到期/已过期数量与优惠总额，按即将到期数量倒序。
     * </p>
     *
     * @param days 预警天数窗口
     * @return 按模板聚合的预警统计列表
     */
    List<ExpiringByTemplateVO> expiringStats(int days);

    /**
     * 批量延期优惠券
     * <p>
     * 对指定 coupon_user 列表（仅 unused 状态），将 expireTime 向后延长指定天数。
     * 忽略非 unused 状态的记录，返回处理结果统计。
     * </p>
     *
     * @param couponUserIds 用户优惠券ID列表
     * @param extendDays    延长天数（必须 > 0）
     * @return 处理结果：successCount/invalidCount/total
     */
    Map<String, Object> batchExtend(List<Long> couponUserIds, int extendDays);

    /**
     * 新增优惠券模板（租户安全）
     * <p>tenantId 从 BaseContext 强制取得，前端无法通过 DTO 字段篡改租户归属。</p>
     *
     * @param name            模板名称
     * @param type            优惠券类型
     * @param conditionAmount 满减条件金额
     * @param discountAmount  优惠金额
     * @param discountRate    折扣率
     * @param totalCount      发放总数
     * @param remainCount     剩余可领数量
     * @param validDays       有效天数
     * @param status          状态
     * @return 是否创建成功
     */
    boolean addTenantCouponTemplate(String name, String type, java.math.BigDecimal conditionAmount,
                                     java.math.BigDecimal discountAmount, java.math.BigDecimal discountRate,
                                     Integer totalCount, Integer remainCount, Integer validDays, Integer status);

    /**
     * 更新优惠券模板（租户安全）
     * <p>先通过 id 查询确认归属，再仅更新业务字段，绕过全实体覆盖漏洞。</p>
     *
     * @param id              模板ID
     * @param name            新模板名称
     * @param type            新优惠券类型
     * @param conditionAmount 新满减条件金额
     * @param discountAmount  新优惠金额
     * @param discountRate    新折扣率
     * @param totalCount      新发放总数
     * @param remainCount     新剩余可领数量
     * @param validDays       新有效天数
     * @param status          新状态
     * @return 是否更新成功
     */
    boolean updateTenantCouponTemplate(Long id, String name, String type, java.math.BigDecimal conditionAmount,
                                       java.math.BigDecimal discountAmount, java.math.BigDecimal discountRate,
                                       Integer totalCount, Integer remainCount, Integer validDays, Integer status);

    /**
     * 删除优惠券模板（租户安全）
     * <p>先查询确认该模板属于当前租户，再删除，防止跨租户删除。</p>
     *
     * @param id 模板ID
     * @return 是否删除成功
     */
    boolean deleteTenantCouponTemplate(Long id);
}
