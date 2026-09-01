// ============================================================
// 发票管理 (Invoice) API 模块
// 后端基路径: /invoice
// 覆盖 InvoiceController 后台管理端点（用户端抬头/申请接口暂由 C 端调用）
// ============================================================

const invoiceApi = {
  // 发票列表（后台，分页）：status 可选 0待申请/1已申请/2已开具/3已作废
  listRecords: function(page, pageSize, status) {
    var params = { page: page || 1, pageSize: pageSize || 10 }
    if (status !== '' && status !== null && status !== undefined) {
      params.status = status
    }
    return $axios({
      url: '/invoice/list',
      method: 'get',
      params: params
    })
  },
  // 发票状态统计（后台统计卡，全量）
  listStats: function() {
    return $axios({
      url: '/invoice/stats',
      method: 'get'
    })
  },
  // 开具发票：发票号码/代码/PDF地址
  issue: function(recordId, data) {
    return $axios({
      url: '/invoice/issue/' + recordId,
      method: 'post',
      params: data
    })
  },
  // 作废发票
  voidRecord: function(recordId) {
    return $axios({
      url: '/invoice/void/' + recordId,
      method: 'post'
    })
  }
}
