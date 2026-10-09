package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 团餐预订实体（企业内部订餐）
 * <p>企业按部门批量订餐：菜单条目快照存于 {@link GroupMealBookingItem}，
 * 总金额为下单时价格快照合计，后续改价不影响已有预订。</p>
 */
@Data
@TableName("group_meal_booking")
@Schema(description = "团餐预订实体")
public class GroupMealBooking implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CONFIRMED = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_CANCELLED = 3;

    public static final String MEAL_LUNCH = "LUNCH";
    public static final String MEAL_DINNER = "DINNER";

    @Schema(description = "预订ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "部门ID", example = "1")
    private Long departmentId;

    @Schema(description = "部门名快照", example = "技术部")
    private String departmentName;

    @Schema(description = "联系人", example = "张三")
    private String contactName;

    @Schema(description = "联系电话", example = "13900000000")
    private String contactPhone;

    @Schema(description = "用餐日期")
    private LocalDate mealDate;

    @Schema(description = "餐段：LUNCH/DINNER", example = "LUNCH")
    private String mealType;

    @Schema(description = "状态：0=待确认，1=已确认，2=已完成，3=已取消", example = "0")
    private Integer status;

    @Schema(description = "总金额（下单时快照）", example = "300.00")
    private BigDecimal totalAmount;

    @Schema(description = "备注", example = "少辣")
    private String remark;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是")
    @TableLogic
    private Integer isDeleted;
}
