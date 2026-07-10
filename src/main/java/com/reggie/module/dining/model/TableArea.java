package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 桌台区域信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("dining_area")
public class TableArea implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 区域名称 */
    private String name;

    /** 排序号 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
