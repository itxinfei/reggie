// SaaS 订阅计费 API（套餐目录 + 租户订阅/权益）
// 对应后端 com.reggie.module.billing.controller：
//   BillingPlanController        /api/billing/plan
//   TenantSubscriptionController /api/billing/subscription
window.billingApi = {
  // ============ 套餐目录（全局共享，维护需超管，后端 @RequiresAdmin 兜底） ============
  // 套餐分页：params = { page, pageSize, keyword, status }
  planPage: function (params) {
    return $axios({ url: '/api/billing/plan/page', method: 'get', params: params })
  },
  // 已上架套餐列表（开通/续费时下拉选择）
  planOnShelf: function () {
    return $axios({ url: '/api/billing/plan/onShelf', method: 'get' })
  },
  planDetail: function (id) {
    return $axios({ url: '/api/billing/plan/' + id, method: 'get' })
  },
  planAdd: function (data) {
    return $axios({ url: '/api/billing/plan', method: 'post', data: data })
  },
  planUpdate: function (data) {
    return $axios({ url: '/api/billing/plan', method: 'put', data: data })
  },
  // 上架/下架：status 1上架 0下架
  planStatus: function (id, status) {
    return $axios({ url: '/api/billing/plan/status/' + id, method: 'put', params: { status: status } })
  },
  planRemove: function (id) {
    return $axios({ url: '/api/billing/plan/' + id, method: 'delete' })
  },

  // ============ 租户订阅/权益 ============
  // 发起订阅/续费（生成待支付单）：data = { planId, billingCycle, payChannel, remark }
  subscribe: function (data) {
    return $axios({ url: '/api/billing/subscription/subscribe', method: 'post', data: data })
  },
  // 确认支付（模拟开通，MOCK/OFFLINE）
  pay: function (id) {
    return $axios({ url: '/api/billing/subscription/pay/' + id, method: 'put' })
  },
  // 取消待支付/生效中的订阅
  cancel: function (id) {
    return $axios({ url: '/api/billing/subscription/cancel/' + id, method: 'put' })
  },
  // 当前租户生效中的订阅
  active: function () {
    return $axios({ url: '/api/billing/subscription/active', method: 'get' })
  },
  // 当前租户权益视图（是否生效、到期时间、剩余天数、门店/员工额度）
  entitlement: function () {
    return $axios({ url: '/api/billing/subscription/entitlement', method: 'get' })
  },
  // 本租户订阅记录分页：params = { page, pageSize, status }
  myPage: function (params) {
    return $axios({ url: '/api/billing/subscription/page', method: 'get', params: params })
  },
  // 平台运营跨租户分页（超管）：params = { page, pageSize, status, tenantId }
  adminPage: function (params) {
    return $axios({ url: '/api/billing/subscription/admin/page', method: 'get', params: params })
  },
  // 手动触发过期扫描（超管），返回被置为过期的条数
  expire: function () {
    return $axios({ url: '/api/billing/subscription/expire', method: 'post' })
  }
}
