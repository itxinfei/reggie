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
