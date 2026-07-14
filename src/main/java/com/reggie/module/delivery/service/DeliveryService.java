package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.delivery.model.DeliveryOrder;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 外卖配送服务接口
 * </p>
 * <p>提供第三方平台订单对接、菜单同步、库存同步、状态流转、筛选选项、统计等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface DeliveryService {

    /**
     * 根据ID获取配送订单
     *
     * @param id 订单ID
     * @return 配送订单信息
     */
    DeliveryOrder getById(String id);

    /**
     * 分页查询配送订单列表
     *
     * @param page      页码
     * @param pageSize  每页条数
     * @param platform  平台类型（如美团、饿了么）
     * @param status    订单状态
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 分页订单列表
     */
    Page<DeliveryOrder> pageOrders(int page, int pageSize, String platform, String status, String startDate, String endDate);

    /**
     * 接受第三方平台订单（PENDING → ACCEPTED）
     *
     * @param platform        平台类型
     * @param platformOrderId 平台订单ID
     * @return 是否接受成功
     */
    boolean acceptOrder(String platform, String platformOrderId);

    /**
     * 更新配送订单状态（支持完整生命周期：接单→取餐→配送→送达→取消）
     *
     * @param id     订单主键ID
     * @param status 目标状态
     * @param remark 操作备注（如取消原因）
     * @return 是否更新成功
     */
    boolean updateOrderStatus(Long id, String status, String remark);

    /**
     * 获取筛选选项（平台列表、状态下拉数据）
     *
     * @param platform 按指定平台筛选选项（可选）
     * @return 包含 platformOptions、statusOptions 的 Map
     */
    Map<String, Object> getFilterOptions(String platform);

    /**
     * 获取配送统计数据
     *
     * @param platform  平台筛选（可选）
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 含今日订单数、各状态数量、总金额的 Map
     */
    Map<String, Object> getDeliveryStats(String platform, String startDate, String endDate);

    /**
     * 同步菜品到第三方平台
     *
     * @param platform 平台类型
     * @param dishes   菜品数据列表
     * @return 是否同步成功
     */
    boolean syncMenu(String platform, List<Map<String, Object>> dishes);

    /**
     * 同步库存到第三方平台
     *
     * @param platform 平台类型
     * @param stock    库存数据，key为菜品ID，value为库存数量
     * @return 是否同步成功
     */
    boolean syncStock(String platform, Map<Long, Integer> stock);

    /**
     * 根据平台订单号查询配送订单
     *
     * @param platformOrderId 平台订单号
     * @return 配送订单信息
     */
    DeliveryOrder getByPlatformOrderId(String platformOrderId);

    /**
     * 处理第三方平台回调（新订单通知、状态变更、退款通知）
     *
     * @param platform 平台类型
     * @param params   回调参数
     * @return 处理结果
     */
    String handleCallback(String platform, Map<String, String> params);
}
