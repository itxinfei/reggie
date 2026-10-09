package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 团餐预订明细实体
 * <p>名称与单价均为下单时快照：后续菜品改价/改名不影响历史预订对账。</p>
 */
@Data
@TableName("group_meal_booking_item")
@Schema(description = "团餐预订明细实体")
public class GroupMealBookingItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_DISH = "DISH";
    public static final String TYPE_SETMEAL = "SETMEAL";

    @Schema(description = "明细ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "预订ID", example = "1")
    private Long bookingId;

    @Schema(description = "条目类型：DISH/SETMEAL", example = "DISH")
    private String itemType;

    @Schema(description = "菜品/套餐ID", example = "1")
    private Long itemId;

    @Schema(description = "名称快照", example = "鱼香肉丝")
    private String itemName;

    @Schema(description = "单价快照", example = "20.00")
    private BigDecimal price;

    @Schema(description = "份数", example = "10")
    private Integer quantity;
}
