// C端 排队取号 / 到店预订 接口

// ===== 排队取号 =====
// 取号
const takeQueue = (data) => {
  return $axios({
    url: '/api/dining/queue/customer/take',
    method: 'post',
    data
  })
}

// 查询我的排队（含前面等待桌数）
const getMyQueue = () => {
  return $axios({
    url: '/api/dining/queue/customer/my',
    method: 'get'
  })
}

// 取消我的排队
const cancelMyQueue = (id) => {
  return $axios({
    url: '/api/dining/queue/customer/' + id + '/cancel',
    method: 'put'
  })
}

// ===== 到店预订 =====
// 创建预订
const createReservation = (data) => {
  return $axios({
    url: '/api/dining/reservation/customer',
    method: 'post',
    data
  })
}

// 我的预订列表
const getMyReservations = () => {
  return $axios({
    url: '/api/dining/reservation/customer/my',
    method: 'get'
  })
}

// 取消我的预订
const cancelMyReservation = (id) => {
  return $axios({
    url: '/api/dining/reservation/customer/' + id + '/cancel',
    method: 'put'
  })
}
