package com.reggie.module.address.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 地址簿
 */
@Data
@TableName("address_book")
@Schema(description = "收货地址")
public class AddressBook implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "地址ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "收货人", example = "张三", required = true)
    @NotBlank(message = "收货人不能为空")
    @Size(max = 30, message = "收货人姓名不能超过30个字符")
    private String consignee;

    @Schema(description = "手机号", example = "13800138000", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "性别：0=女，1=男", example = "1")
    @Min(value = 0, message = "性别值不正确")
    @Max(value = 1, message = "性别值不正确")
    private Integer sex;

    @Schema(description = "省级区划编号", example = "110000", required = true)
    @NotBlank(message = "省级区划编号不能为空")
    private String provinceCode;

    @Schema(description = "省级名称", example = "北京市", required = true)
    @NotBlank(message = "省级名称不能为空")
    private String provinceName;

    @Schema(description = "市级区划编号", example = "110100", required = true)
    @NotBlank(message = "市级区划编号不能为空")
    private String cityCode;

    @Schema(description = "市级名称", example = "北京市", required = true)
    @NotBlank(message = "市级名称不能为空")
    private String cityName;

    @Schema(description = "区级区划编号", example = "110105", required = true)
    @NotBlank(message = "区级区划编号不能为空")
    private String districtCode;

    @Schema(description = "区级名称", example = "朝阳区", required = true)
    @NotBlank(message = "区级名称不能为空")
    private String districtName;

    // ---- 结构化地址：街道/乡镇 + 小区/大厦 + 楼栋 + 单元 + 楼层 + 门牌号 ----
    // 省/市/区由联动选择器保证规范；以下字段文本手填，用于骑手精确定位，均独立入库。

    @Schema(description = "街道/乡镇（文本，选填）", example = "三里屯街道")
    @Size(max = 50, message = "街道/乡镇不能超过50个字符")
    private String streetName;

    @Schema(description = "小区/大厦/学校等", example = "幸福里小区")
    @Size(max = 100, message = "小区/大厦名称不能超过100个字符")
    private String community;

    @Schema(description = "楼栋", example = "3栋")
    @Size(max = 20, message = "楼栋不能超过20个字符")
    private String building;

    @Schema(description = "单元", example = "2单元")
    @Size(max = 20, message = "单元不能超过20个字符")
    private String unit;

    @Schema(description = "楼层", example = "15层")
    @Size(max = 20, message = "楼层不能超过20个字符")
    private String floor;

    @Schema(description = "门牌号", example = "1503室")
    @Size(max = 20, message = "门牌号不能超过20个字符")
    private String roomNo;

    // detail 为冗余的完整拼接地址（后端按上述字段规范化生成），供订单快照/配送/打印等既有展示复用。
    @Schema(description = "详细地址（后端按结构化字段规范化拼接）", example = "三里屯街道幸福里小区3栋2单元15层1503室")
    @Size(max = 200, message = "详细地址不能超过200个字符")
    private String detail;

    @Schema(description = "标签", example = "家")
    @Size(max = 100, message = "标签不能超过100个字符")
    private String label;

    @Schema(description = "经度（高德坐标系，GCJ-02）", example = "116.397428")
    private BigDecimal longitude;

    @Schema(description = "纬度（高德坐标系，GCJ-02）", example = "39.90923")
    private BigDecimal latitude;

    @Schema(description = "是否默认地址：0=否，1=是", example = "1")
    public static final int NOT_DEFAULT = 0;
    public static final int IS_DEFAULT = 1;
    private Integer isDefault;

    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID", example = "1")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是", example = "0")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}

