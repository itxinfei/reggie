// ============================================================
// 催单管理 (Urgency) API 模块
// 后端基路径: /api/urgency
// 覆盖 UrgencyController 全部 6 个端点
// ============================================================

// 获取催单概览（催单中订单数/超时可催/平均等待时间/最长等待时间）
const getUrgencyOverview = () => {
  return $axios({
    url: '/api/urgency/overview',
    method: 'get'
  })
}

// 获取催单列表（支持状态筛选：COOKING/WAITING_CALL/COMPLETED）
const getUrgencyList = (params) => {
  return $axios({
    url: '/api/urgency/list',
    method: 'get',
    params: params
  })
}

// 催单操作
const callOrder = (orderId) => {
  return $axios({
    url: '/api/urgency/call/' + orderId,
    method: 'post'
  })
}

// 查看催单详情（含制作进度和预估时间）
const getUrgencyDetail = (orderId) => {
  return $axios({
    url: '/api/urgency/detail/' + orderId,
    method: 'get'
  })
}

// 获取叫号排队列表
const getUrgencyQueue = () => {
  return $axios({
    url: '/api/urgency/queue',
    method: 'get'
  })
}

// 获取催单统计汇总（今日）
const getUrgencySummary = () => {
  return $axios({
    url: '/api/urgency/summary',
    method: 'get'
  })
}