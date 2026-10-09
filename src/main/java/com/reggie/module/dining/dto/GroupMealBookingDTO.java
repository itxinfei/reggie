package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 团餐预订创建 DTO
 * <p>菜单条目仅传类型/ID/份数，名称与单价由服务端从数据库快照（防篡改、与订单链路口径一致）。</p>
 */
@Data
@Schema(description = "团餐预订创建请求")
public class GroupMealBookingDTO {

    @Schema(description = "部门ID（可选，未传表示全公司）")
    private Long departmentId;

    @Schema(description = "联系人", example = "张三", required = true)
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @Schema(description = "联系电话", example = "13900000000", required = true)
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    @Schema(description = "用餐日期 yyyy-MM-dd", required = true)
    @NotBlank(message = "用餐日期不能为空")
    private String mealDate;

    @Schema(description = "餐段：LUNCH/DINNER", example = "LUNCH")
    private String mealType;

    @Schema(description = "备注", example = "少辣")
    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    @Schema(description = "菜单条目", required = true)
    @NotEmpty(message = "请至少选择一个菜品或套餐")
    @Valid
    private List<GroupMealBookingItemDTO> items;

    @Data
    @Schema(description = "团餐菜单条目")
    public static class GroupMealBookingItemDTO {

        @Schema(description = "条目类型：DISH/SETMEAL", example = "DISH", required = true)
        @NotBlank(message = "条目类型不能为空")
        private String itemType;

        @Schema(description = "菜品/套餐ID", required = true)
        @NotNull(message = "条目ID不能为空")
        private Long itemId;

        @Schema(description = "份数", example = "10", required = true)
        @NotNull(message = "份数不能为空")
        private Integer quantity;
    }
}
