package com.reggie.module.recommend.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐结果缓存
 * 缓存用户个性化推荐结果，避免实时计算开销
 */
@Data
@TableName("recommendation_cache")
public class RecommendationCache implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 推荐类型 - 菜品推荐 */
    public static final int TYPE_DISH = 1;
    /** 推荐类型 - 套餐推荐 */
    public static final int TYPE_SETMEAL = 2;
    /** 推荐类型 - 新品尝鲜 */
    public static final int TYPE_NEW_ARRIVAL = 3;

    /** 算法 - 协同过滤 */
    public static final String ALGO_CF = "CF";
    /** 算法 - 基于内容 */
    public static final String ALGO_CONTENT = "ContentBased";
    /** 算法 - 混合推荐 */
    public static final String ALGO_HYBRID = "Hybrid";
    /** 算法 - 热门排行 */
    public static final String ALGO_HOT = "HotRank";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 推荐类型 1:菜品推荐 2:套餐推荐 3:新品尝鲜 */
    private Integer recommendType;

    /** 推荐菜品ID列表 JSON数组 */
    private String dishIds;

    /** 算法名称 */
    private String algorithm;

    /** 推荐置信度 */
    private BigDecimal score;

    /** 缓存过期时间 */
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
