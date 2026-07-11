// 修改点：筛选下拉选项（动态加载菜品名称，供评价报表页面使用）
const dishOptions = () => $axios({ url: '/dish/options', method: 'get' })

// 菜品评价管理接口
const getEvaluationPage = (params) => {
  return $axios({
    url: '/api/dish-evaluation/page',
    method: 'get',
    params
  })
}

// 获取待审核评价列表
const getPendingEvaluations = (params) => {
  return $axios({
    url: '/api/dish-evaluation/pending',
    method: 'get',
    params
  })
}

// 审核评价（通过/拒绝）
const auditEvaluation = (id, status) => {
  return $axios({
    url: `/api/dish-evaluation/${id}/status`,
    method: 'put',
    data: { status }
  })
}

// 商家回复评价
const replyEvaluation = (id, replyContent) => {
  return $axios({
    url: `/api/dish-evaluation/${id}/reply`,
    method: 'put',
    data: { replyContent }
  })
}
