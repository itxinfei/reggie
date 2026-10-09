package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.dto.GroupMealBookingDTO;
import com.reggie.module.dining.model.GroupMealBooking;
import com.reggie.module.dining.model.GroupMealBookingItem;

import java.util.List;
import java.util.Map;

/**
 * 团餐预订 Service（企业内部订餐）
 * <p>状态机：待确认(0) → 已确认(1) → 已完成(2)；待确认/已确认可取消(3)。
 * 条目名称/单价以下单时快照为准。</p>
 */
public interface GroupMealBookingService extends IService<GroupMealBooking> {

    /**
     * 创建团餐预订（价格/名称快照，租户安全）。
     *
     * @param dto        预订请求
     * @param tenantId   租户ID
     * @param operatorId 操作人ID
     * @return 预订ID
     */
    Long createBooking(GroupMealBookingDTO dto, Long tenantId, Long operatorId);

    /**
     * 状态流转（确认/完成/取消），校验当前状态允许的迁移。
     *
     * @param id           预订ID
     * @param targetStatus 目标状态
     * @param tenantId     租户ID
     * @param operatorId   操作人ID
     */
    void changeStatus(Long id, int targetStatus, Long tenantId, Long operatorId);

    /**
     * 预订详情（含明细条目）。
     *
     * @return booking + items，键 booking / items；不存在或越权返回 null
     */
    Map<String, Object> detail(Long id, Long tenantId);

    /**
     * 分页查询（租户隔离，按用餐日期倒序）。
     */
    IPage<GroupMealBooking> pageBookings(Long tenantId, int page, int pageSize,
                                         Integer status, String mealDate, Long departmentId);

    /**
     * 明细列表（管理端展开展示用）。
     */
    List<GroupMealBookingItem> itemsOf(Long bookingId, Long tenantId);
}
