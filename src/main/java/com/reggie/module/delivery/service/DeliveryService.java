package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.delivery.model.DeliveryOrder;
import java.util.List;
import java.util.Map;

/**
 * 外卖配送服务接口
 * 提供第三方平台订单对接、菜单同步、库存同步等功能
 *
 * @author reggie
 * @since 2026-07-09
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
     * 接受第三方平台订单
     *
     * @param platform        平台类型
     * @param platformOrderId 平台订单ID
     * @return 是否接受成功
     */
    boolean acceptOrder(String platform, String platformOrderId);

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
     * 处理第三方平台回调
     *
     * @param platform 平台类型
     * @param params   回调参数
     * @return 处理结果
     */
    String handleCallback(String platform, Map<String, String> params);
}
