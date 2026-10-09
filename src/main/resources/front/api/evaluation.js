// C端评价相关接口
// 获取我的评价列表
const getMyEvaluations = (params) => {
  return $axios({
    url: '/api/dish-evaluation/user/my',
    method: 'get',
    params
  })
}

// 提交评价
const submitEvaluation = (data) => {
  return $axios({
    url: '/api/dish-evaluation',
    method: 'post',
    data
  })
}

// 获取菜品评价列表（公开，用于菜品详情页）
const getDishEvaluations = (dishId, page = 1, pageSize = 10) => {
  return $axios({
    url: '/api/dish-evaluation/dish/' + dishId,
    method: 'get',
    params: { page, pageSize }
  })
}

// 获取菜品评分统计
const getDishRatingStats = (dishId) => {
  return $axios({
    url: '/api/dish-evaluation/dish/' + dishId + '/stats',
    method: 'get'
  })
}

// 修改点：新增删除评价接口
const deleteMyEvaluation = (data) => {
  return $axios({
    url: '/api/dish-evaluation',
    method: 'delete',
    data
  })
}

// ==================== 骑手评价（P0-2，2026-09-28）====================
// 提交骑手评价
const submitRiderEvaluation = (data) => {
  return $axios({
    url: '/api/rider-evaluation',
    method: 'post',
    data
  })
}

// 按订单+骑手查询我的评价（判断是否已评价）
const getRiderEvaluationByOrder = (orderId, riderId) => {
  return $axios({
    url: '/api/rider-evaluation/order/' + orderId,
    method: 'get',
    params: { riderId: riderId }
  })
}

// 我的骑手评价列表
const getMyRiderEvaluations = (params) => {
  return $axios({
    url: '/api/rider-evaluation/my',
    method: 'get',
    params
  })
}

// 骑手评价公开列表
const getRiderEvaluationList = (riderId, params) => {
  return $axios({
    url: '/api/rider-evaluation/rider/' + riderId,
    method: 'get',
    params
  })
}

// 骑手评分统计
const getRiderEvaluationStats = (riderId) => {
  return $axios({
    url: '/api/rider-evaluation/rider/' + riderId + '/stats',
    method: 'get'
  })
}
