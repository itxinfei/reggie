package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("supplier")
@Schema(description = "供应商")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "供应商ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "供应商名称", example = "北京蔬菜批发市场")
    private String name;

    @Schema(description = "联系人", example = "李经理")
    private String contact;

    @Schema(description = "联系电话", example = "010-12345678")
    private String phone;

    @Schema(description = "地址", example = "北京市朝阳区xxx")
    private String address;

    @Schema(description = "状态：0=禁用，1=正常", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    private LocalDateTime updatedTime;
}
