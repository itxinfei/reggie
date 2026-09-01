// ============================================================
// 发票 (Invoice) C 端 API 模块
// 后端基路径: /invoice
// 用户端：抬头管理 + 申请开票 + 查询订单发票
// userId/tenantId 由后端从登录会话获取，前端无需传参
// ============================================================

// 发票抬头列表
function invoiceTitleListApi() {
  return $axios({
    'url': '/invoice/title/list',
    'method': 'get'
  })
}

// 保存发票抬头
function invoiceTitleSaveApi(data) {
  return $axios({
    'url': '/invoice/title/save',
    'method': 'post',
    data
  })
}

// 删除发票抬头
function invoiceTitleDeleteApi(id) {
  return $axios({
    'url': '/invoice/title/' + id,
    'method': 'delete'
  })
}

// 申请开票
function invoiceApplyApi(orderId, data) {
  return $axios({
    'url': '/invoice/apply/' + orderId,
    'method': 'post',
    data
  })
}

// 查询订单发票记录
function orderInvoiceApi(orderId) {
  return $axios({
    'url': '/invoice/order/' + orderId,
    'method': 'get'
  })
}
