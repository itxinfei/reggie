// 配送追踪相关接口
// 查询配送订单详情（用于追踪页面）
function queryDeliveryTracking(orderId) {
  return $axios({
    url: '/api/delivery/tracking/' + orderId,
    method: 'get'
  })
}
