/**
 * 前端订单状态单一真源（与后端 enums.OrderStatus / DeliveryOrderStatus 对齐）。
 * 三端（顾客端 / 骑手端 / 配送追踪）共用本字典，杜绝状态文案各自硬编码导致的不一致。
 * 仅做状态文案 / 样式映射，不包含任何业务逻辑。
 *
 * 修改点(S0 三端状态机统一)：新增本文件作为三端共用字典，替代各页零散内联映射。
 */
(function (win) {
  'use strict';

  // 主单状态：严格对齐 com.reggie.enums.OrderStatus
  var ORDER = {
    1: { text: '待付款', cls: 'status-pending' },
    2: { text: '待接单', cls: 'status-accepted' },
    3: { text: '配送中', cls: 'status-shipped' },
    4: { text: '已完成', cls: 'status-completed' },
    5: { text: '已取消', cls: 'status-cancelled' },
    6: { text: '已退款', cls: 'status-refunding' },
    7: { text: '已分账', cls: 'status-completed' }
  };

  // 配送状态：对齐后端 DeliveryOrderStatus（取餐中 / 配送中 由 pickup 区分）
  var DELIVERY = {
    PENDING:   { text: '待接单', cls: 'status-pending',   desc: '商家正在确认订单', icon: 'ri-time-line' },
    ACCEPTED:  { text: '已接单', cls: 'status-accepted',  desc: '商家已接单，正在准备', icon: 'ri-restaurant-line' },
    PICKING:   { text: '取餐中', cls: 'status-accepted',  desc: '骑手正在取餐',     icon: 'ri-store-2-line' },
    DELIVERING:{ text: '配送中', cls: 'status-shipped',   desc: '骑手正在配送',     icon: 'ri-motorbike-line' },
    DELIVERED: { text: '已送达', cls: 'status-completed', desc: '订单已送达',       icon: 'ri-check-line' },
    CANCELLED: { text: '已取消', cls: 'status-cancelled', desc: '订单已取消',       icon: 'ri-close-line' }
  };

  function orderText(code) {
    var m = ORDER[code];
    return m ? m.text : '未知';
  }

  function orderClass(code) {
    var m = ORDER[code];
    return m ? m.cls : '';
  }

  // 配送态文案（未取餐的 PICKING 在列表场景可归并为「配送中」，与主单 3 一致）
  function deliveryText(key) {
    var m = DELIVERY[key];
    return m ? m.text : '未知状态';
  }

  function deliveryClass(key) {
    var m = DELIVERY[key];
    return m ? m.cls : '';
  }

  function deliveryDesc(key) {
    var m = DELIVERY[key];
    return m ? m.desc : '';
  }

  function deliveryIcon(key) {
    var m = DELIVERY[key];
    return m ? m.icon : 'ri-map-pin-line';
  }

  win.OrderStatusDict = {
    ORDER: ORDER,
    DELIVERY: DELIVERY,
    orderText: orderText,
    orderClass: orderClass,
    deliveryText: deliveryText,
    deliveryClass: deliveryClass,
    deliveryDesc: deliveryDesc,
    deliveryIcon: deliveryIcon
  };
})(window);
