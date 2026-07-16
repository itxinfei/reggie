// 堂食扫码下单
const submitEatInOrder = (data) => {
  return $axios({
    url: '/order/eatIn',
    method: 'post',
    data: data
  })
}

// 查询列表页接口
const getOrderDetailPage = (params) => {
  return $axios({
    url: '/order/page',
    method: 'get',
    params
  })
}

// 查询所有订单（不分页）
const getOrderList = (params) => {
  return $axios({
    url: '/order/list',
    method: 'get',
    params
  })
}

// 查询用户订单分页
const getUserOrderPage = (params) => {
  return $axios({
    url: '/order/userPage',
    method: 'get',
    params
  })
}

// 查看接口
const queryOrderDetailById = (id) => {
  return $axios({
    url: `/order/${id}`,
    method: 'get'
  })
}

// 再来一单
const orderAgain = (data) => {
  return $axios({
    url: '/order/again',
    method: 'post',
    data: data
  })
}

// 订单统计
const getOrderStatistics = (params) => {
  return $axios({
    url: '/order/statistics',
    method: 'get',
    params
  })
}

// 取消订单
const cancelOrder = (params) => {
  return $axios({
    url: '/order/cancel',
    method: 'put',
    data: { ...params }
  })
}

// 确认订单（接单）
const confirmOrder = (params) => {
  return $axios({
    url: '/order/confirm',
    method: 'put',
    data: { ...params }
  })
}

// 拒绝订单
const rejectOrder = (params) => {
  return $axios({
    url: '/order/reject',
    method: 'put',
    data: { ...params }
  })
}

// 完成订单
const completeOrder = (params) => {
  return $axios({
    url: '/order/complete',
    method: 'put',
    data: { ...params }
  })
}

// 取消，派送，完成接口（通用状态更新）
const editOrderDetail = (params) => {
  return $axios({
    url: '/order',
    method: 'put',
    data: { ...params }
  })
}
