package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.dining.dto.GroupMealBookingDTO;
import com.reggie.module.dining.mapper.GroupMealBookingItemMapper;
import com.reggie.module.dining.mapper.GroupMealBookingMapper;
import com.reggie.module.dining.model.GroupMealBooking;
import com.reggie.module.dining.model.GroupMealBookingItem;
import com.reggie.module.dining.service.GroupMealBookingService;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.setmeal.service.SetmealService;
import com.reggie.module.sys.model.Department;
import com.reggie.module.sys.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 团餐预订 Service 实现
 * <p>
 * 价格与名称在下单时从菜品/套餐表快照（停售/删除的条目直接拒绝），总金额为快照合计；
 * 状态机：待确认(0)→已确认(1)→已完成(2)，0/1 可取消(3)；其余迁移一律拒绝。
 * 全部读写按租户隔离，跨租户条目/部门视为不存在。
 * </p>
 */
@Slf4j
@Service
public class GroupMealBookingServiceImpl extends ServiceImpl<GroupMealBookingMapper, GroupMealBooking>
        implements GroupMealBookingService {

    @Autowired
    private GroupMealBookingItemMapper itemMapper;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private DepartmentService departmentService;

    @Override
    @Transactional
    public Long createBooking(GroupMealBookingDTO dto, Long tenantId, Long operatorId) {
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法创建团餐预订");
        }
        LocalDate mealDate;
        try {
            mealDate = LocalDate.parse(dto.getMealDate());
        } catch (Exception e) {
            throw new CustomException("用餐日期格式应为 yyyy-MM-dd");
        }
        if (mealDate.isBefore(LocalDate.now())) {
            throw new CustomException("用餐日期不能早于今天");
        }
        String mealType = dto.getMealType() == null || dto.getMealType().isEmpty()
                ? GroupMealBooking.MEAL_LUNCH : dto.getMealType();
        if (!GroupMealBooking.MEAL_LUNCH.equals(mealType)
                && !GroupMealBooking.MEAL_DINNER.equals(mealType)) {
            throw new CustomException("餐段仅支持 LUNCH/DINNER");
        }

        // 部门归属校验（可选字段：不传表示全公司）
        String departmentName = null;
        if (dto.getDepartmentId() != null) {
            Department dept = departmentService.getOne(new LambdaQueryWrapper<Department>()
                    .eq(Department::getId, dto.getDepartmentId())
                    .eq(Department::getTenantId, tenantId)
                    .eq(Department::getIsDeleted, 0));
            if (dept == null) {
                throw new CustomException("部门不存在或不属于当前租户（id=" + dto.getDepartmentId() + "）");
            }
            departmentName = dept.getName();
        }

        // 条目快照：逐条校验在售状态并取名称/单价
        BigDecimal total = BigDecimal.ZERO;
        List<GroupMealBookingItem> items = new ArrayList<>(dto.getItems().size());
        for (GroupMealBookingDTO.GroupMealBookingItemDTO itemDTO : dto.getItems()) {
            GroupMealBookingItem item = snapshotItem(tenantId, itemDTO);
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            items.add(item);
        }

        GroupMealBooking booking = new GroupMealBooking();
        booking.setTenantId(tenantId);
        booking.setDepartmentId(dto.getDepartmentId());
        booking.setDepartmentName(departmentName);
        booking.setContactName(dto.getContactName());
        booking.setContactPhone(dto.getContactPhone());
        booking.setMealDate(mealDate);
        booking.setMealType(mealType);
        booking.setStatus(GroupMealBooking.STATUS_PENDING);
        booking.setTotalAmount(total);
        booking.setRemark(dto.getRemark());
        booking.setCreateUser(operatorId);
        booking.setUpdateUser(operatorId);
        this.save(booking);

        for (GroupMealBookingItem item : items) {
            item.setBookingId(booking.getId());
            itemMapper.insert(item);
        }
        log.info("[团餐] 预订创建: tenant={}, booking={}, 部门={}, 条目数={}, 总额={}",
                tenantId, booking.getId(), departmentName, items.size(), total);
        return booking.getId();
    }

    /** 条目快照：校验存在/在售/租户归属，取名称与单价。 */
    private GroupMealBookingItem snapshotItem(Long tenantId, GroupMealBookingDTO.GroupMealBookingItemDTO dto) {
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new CustomException("份数必须大于0");
        }
        GroupMealBookingItem item = new GroupMealBookingItem();
        item.setTenantId(tenantId);
        item.setItemType(dto.getItemType());
        item.setItemId(dto.getItemId());
        item.setQuantity(dto.getQuantity());
        if (GroupMealBookingItem.TYPE_DISH.equals(dto.getItemType())) {
            Dish dish = dishService.getById(dto.getItemId());
            if (dish == null || !tenantId.equals(dish.getTenantId())
                    || Integer.valueOf(1).equals(dish.getIsDeleted())) {
                throw new CustomException("菜品不存在或不属于当前租户（id=" + dto.getItemId() + "）");
            }
            if (dish.getStatus() == null || dish.getStatus() != 1) {
                throw new CustomException("菜品已停售：" + dish.getName());
            }
            item.setItemName(dish.getName());
            item.setPrice(dish.getPrice());
        } else if (GroupMealBookingItem.TYPE_SETMEAL.equals(dto.getItemType())) {
            Setmeal setmeal = setmealService.getById(dto.getItemId());
            if (setmeal == null || !tenantId.equals(setmeal.getTenantId())
                    || Integer.valueOf(1).equals(setmeal.getIsDeleted())) {
                throw new CustomException("套餐不存在或不属于当前租户（id=" + dto.getItemId() + "）");
            }
            if (setmeal.getStatus() == null || setmeal.getStatus() != 1) {
                throw new CustomException("套餐已停售：" + setmeal.getName());
            }
            item.setItemName(setmeal.getName());
            item.setPrice(setmeal.getPrice());
        } else {
            throw new CustomException("条目类型仅支持 DISH/SETMEAL");
        }
        return item;
    }

    @Override
    public void changeStatus(Long id, int targetStatus, Long tenantId, Long operatorId) {
        GroupMealBooking booking = getOwnedBooking(id, tenantId);
        int current = booking.getStatus();
        boolean allowed =
                (current == GroupMealBooking.STATUS_PENDING && targetStatus == GroupMealBooking.STATUS_CONFIRMED)
                        || (current == GroupMealBooking.STATUS_CONFIRMED && targetStatus == GroupMealBooking.STATUS_COMPLETED)
                        || ((current == GroupMealBooking.STATUS_PENDING || current == GroupMealBooking.STATUS_CONFIRMED)
                                && targetStatus == GroupMealBooking.STATUS_CANCELLED);
        if (!allowed) {
            throw new CustomException("预订状态不允许该操作（当前：" + statusName(current) + "）");
        }
        booking.setStatus(targetStatus);
        booking.setUpdateUser(operatorId);
        booking.setUpdateTime(LocalDateTime.now());
        this.updateById(booking);
    }

    @Override
    public Map<String, Object> detail(Long id, Long tenantId) {
        GroupMealBooking booking = getOwnedBooking(id, tenantId);
        if (booking == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("booking", booking);
        result.put("items", itemsOf(id, tenantId));
        return result;
    }

    @Override
    public IPage<GroupMealBooking> pageBookings(Long tenantId, int page, int pageSize,
                                                Integer status, String mealDate, Long departmentId) {
        Page<GroupMealBooking> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<GroupMealBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupMealBooking::getTenantId, tenantId)
               .eq(GroupMealBooking::getIsDeleted, 0)
               .eq(status != null, GroupMealBooking::getStatus, status)
               .eq(departmentId != null, GroupMealBooking::getDepartmentId, departmentId);
        if (mealDate != null && !mealDate.isEmpty()) {
            wrapper.eq(GroupMealBooking::getMealDate, LocalDate.parse(mealDate));
        }
        wrapper.orderByDesc(GroupMealBooking::getMealDate).orderByDesc(GroupMealBooking::getId);
        return this.page(pageInfo, wrapper);
    }

    @Override
    public List<GroupMealBookingItem> itemsOf(Long bookingId, Long tenantId) {
        return itemMapper.selectList(new LambdaQueryWrapper<GroupMealBookingItem>()
                .eq(GroupMealBookingItem::getBookingId, bookingId)
                .eq(GroupMealBookingItem::getTenantId, tenantId));
    }

    private GroupMealBooking getOwnedBooking(Long id, Long tenantId) {
        GroupMealBooking booking = this.getOne(new LambdaQueryWrapper<GroupMealBooking>()
                .eq(GroupMealBooking::getId, id)
                .eq(GroupMealBooking::getTenantId, tenantId)
                .eq(GroupMealBooking::getIsDeleted, 0));
        if (booking == null) {
            throw new CustomException("团餐预订不存在或不属于当前租户（id=" + id + "）");
        }
        return booking;
    }

    private String statusName(int status) {
        switch (status) {
            case GroupMealBooking.STATUS_PENDING: return "待确认";
            case GroupMealBooking.STATUS_CONFIRMED: return "已确认";
            case GroupMealBooking.STATUS_COMPLETED: return "已完成";
            case GroupMealBooking.STATUS_CANCELLED: return "已取消";
            default: return String.valueOf(status);
        }
    }
}
