//提交订单
function  addOrderApi(data){
    return $axios({
        'url': '/order/submit',
        'method': 'post',
        data
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