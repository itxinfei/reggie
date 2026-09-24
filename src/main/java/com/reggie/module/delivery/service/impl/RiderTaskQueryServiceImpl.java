package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.CustomException;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.module.delivery.dto.RiderTaskVO;
import com.reggie.module.delivery.model.DeliveryTimeRecord;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import com.reggie.module.delivery.service.RiderTaskQueryService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 骑手任务查询服务实现。
 * <p>
 * 订单/明细/地址簿均在骑手租户上下文内由多租户拦截器自动过滤；
 * tenant 表在拦截器白名单中，按订单上的 tenantId 显式查询。
 * </p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@Service
public class RiderTaskQueryServiceImpl implements RiderTaskQueryService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    @Override
    public List<RiderTaskVO> listMine(Long riderId, String scope) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getRiderId, riderId);
        if ("todo".equals(scope)) {
            wrapper.eq(Orders::getStatus, Orders.STATUS_ORDERED);
            wrapper.orderByAsc(Orders::getDispatchTime);
        } else if ("history".equals(scope)) {
            wrapper.in(Orders::getStatus,
                    Arrays.asList(Orders.STATUS_COMPLETED, Orders.STATUS_REFUNDED, Orders.STATUS_CANCELLED));
            wrapper.orderByDesc(Orders::getOrderTime);
            wrapper.last("LIMIT 100");
        } else {
            // 默认展示配送中
            wrapper.eq(Orders::getStatus, Orders.STATUS_DELIVERING);
            wrapper.orderByAsc(Orders::getOrderTime);
        }
        return enrich(orderService.list(wrapper));
    }

    @Override
    public List<RiderTaskVO> listHall() {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, Orders.STATUS_ORDERED);
        wrapper.isNull(Orders::getRiderId);
        wrapper.orderByAsc(Orders::getOrderTime);
        // 积压保护：大厅单量过大时最多返回 200 单，避免一次性全量返回拖垮接口
        wrapper.last("LIMIT 200");
        List<Orders> orders = orderService.list(wrapper);
        return enrich(orders);
    }

    @Override
    public RiderTaskVO getDetail(Long orderId, Long riderId) {
        Orders order = orderService.getById(orderId);
        if (order == null) {
            // 业务异常走 GlobalExceptionHandler（422），避免 IllegalArgumentException 落入兜底 500
            throw new CustomException("订单不存在");
        }
        boolean mine = order.getRiderId() != null && order.getRiderId().equals(riderId);
        boolean inHall = order.getRiderId() == null
                && order.getStatus() != null && order.getStatus() == Orders.STATUS_ORDERED;
        if (!mine && !inHall) {
            throw new CustomException("无权查看该任务");
        }
        return toVO(order);
    }

    /** 批量组装，避免逐条查租户/门店。 */
    private List<RiderTaskVO> enrich(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.List<RiderTaskVO> result = new java.util.ArrayList<RiderTaskVO>(orders.size());
        for (Orders order : orders) {
            result.add(toVO(order));
        }
        return result;
    }

    /** 单订单组装完整 VO。 */
    private RiderTaskVO toVO(Orders order) {
        RiderTaskVO vo = new RiderTaskVO();
        vo.setId(order.getId());
        vo.setNumber(order.getNumber());
        vo.setStatus(order.getStatus());
        vo.setRiderId(order.getRiderId());
        vo.setAmount(order.getAmount());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setRemark(order.getRemark());
        vo.setExpectDeliveryTime(order.getExpectDeliveryTime());
        vo.setOrderTime(order.getOrderTime());

        // 送达地址快照
        vo.setConsignee(order.getConsignee());
        vo.setPhone(order.getPhone());
        vo.setAddress(order.getAddress());

        // 取餐门店：名称/地址/电话取租户，坐标取门店档案
        Tenant tenant = tenantService.getById(order.getTenantId());
        StoreInfo store = storeService.findByTenantId(order.getTenantId());
        if (tenant != null) {
            vo.setStoreName(tenant.getName());
            vo.setStoreAddress(tenant.getAddress());
            vo.setStorePhone(tenant.getPhone());
        }
        if (store != null) {
            vo.setStoreLongitude(store.getLongitude());
            vo.setStoreLatitude(store.getLatitude());
            if (vo.getStorePhone() == null || vo.getStorePhone().isEmpty()) {
                vo.setStorePhone(store.getContactPhone());
            }
        }

        // 商品明细（同租户，拦截器已过滤）
        List<OrderDetail> items = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, order.getId()));
        vo.setItems(items);

        // 收货经纬度：按地址簿补全，用于导航
        if (order.getAddressBookId() != null) {
            AddressBook book = addressBookService.getById(order.getAddressBookId());
            if (book != null) {
                vo.setDestLongitude(book.getLongitude());
                vo.setDestLatitude(book.getLatitude());
            }
        }

        // 动作时间戳
        DeliveryTimeRecord record = deliveryTrackingService.getDeliveryTimeByOrderId(order.getId());
        if (record != null) {
            vo.setAcceptTime(record.getAcceptTime());
            vo.setPickupTime(record.getPickupTime());
            vo.setDeliverTime(record.getDeliverTime());
        }
        return vo;
    }
}
