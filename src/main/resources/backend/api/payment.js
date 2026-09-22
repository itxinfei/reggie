const paymentPage = (params) => $axios({ url: '/api/payment/page', method: 'get', params })
const paymentCreate = (params) => $axios({ url: '/api/payment/pay', method: 'post', data: params })
const paymentRefund = (params) => $axios({ url: '/api/payment/refund', method: 'post', data: params })
const paymentQuery = (tradeNo) => $axios({ url: `/api/payment/query/${tradeNo}`, method: 'get' })
const paymentRefundStats = () => $axios({ url: '/api/payment/refund/stats', method: 'get' })
const reconcilePendingCount = () => $axios({ url: '/api/payment/reconcile/pending-count', method: 'get' })

// 支付渠道配置 API（按租户隔离；密钥加密落库，列表/详情仅返回掩码）
window.paymentChannelApi = {
  // 分页列表
  list: function (params) {
    return $axios({ url: '/admin/payment/channel/list', method: 'get', params: params })
  },
  // 详情（密钥掩码）
  detail: function (id) {
    return $axios({ url: '/admin/payment/channel/detail', method: 'get', params: { id: id } })
  },
  // 新增
  add: function (data) {
    return $axios({ url: '/admin/payment/channel/add', method: 'post', data: data })
  },
  // 更新（密钥留空不修改）
  update: function (data) {
    return $axios({ url: '/admin/payment/channel/update', method: 'post', data: data })
  },
  // 删除（逻辑删除）
  remove: function (id) {
    return $axios({ url: '/admin/payment/channel/delete', method: 'post', params: { id: id } })
  },
  // 启用 / 停用
  toggle: function (id, enabled) {
    return $axios({ url: '/admin/payment/channel/toggle', method: 'post',
      params: { id: id, enabled: enabled } })
  },
  // 统计
  stats: function () {
    return $axios({ url: '/admin/payment/channel/stats', method: 'get' })
  },
  // 测试连通性（不写库、不写缓存）
  testConnection: function (data) {
    return $axios({ url: '/admin/payment/channel/test-connection', method: 'post', data: data })
  }
}
