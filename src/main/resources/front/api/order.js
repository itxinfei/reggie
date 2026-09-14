//提交订单
function  addOrderApi(data){
    return $axios({
        'url': '/order/submit',
        'method': 'post',
        data
      })
}

// 查询订单详情（C 端用于支付结果回查等场景）
function getOrderDetailApi(id) {
  return $axios({
      'url': '/order/' + id,
      'method': 'get'
  })
}

//分页查询订单
function orderPagingApi(data) {
  return $axios({
      'url': '/order/userPage',
      'method': 'get',
      params:{...data}
  })
}

//再来一单
function orderAgainApi(data) {
  return $axios({
      'url': '/order/again',
      'method': 'post',
      data
  })
}

// 修改点：取消订单
function cancelOrderApi(data) {
  return $axios({
      'url': '/order/userCancel',
      'method': 'put',
      params: { id: data && data.id }
  })
}

// 用户确认收货
function confirmReceiptApi(data) {
  return $axios({
      'url': '/order/userConfirmReceipt',
      'method': 'put',
      params: { id: data && data.id }
  })
}

// 用户申请售后
function applyRefundApi(data) {
  return $axios({
      'url': '/order/userApplyRefund',
      'method': 'post',
      params: { id: data && data.id, reason: data && data.reason }
  })
}

// 查询订单售后记录
function listRefundRecordsApi(data) {
  return $axios({
      'url': '/order/userRefundRecords',
      'method': 'get',
      params: { id: data && data.id }
  })
}