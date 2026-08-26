// ============================================================
// 会员留存 (Retention) API 模块
// 后端基路径: /api/retention
// 覆盖 RetentionController 全部 7 个端点
// ============================================================

// 获取会员留存概览（分层统计）
const getRetentionOverview = () => {
  return $axios({
    url: '/api/retention/overview',
    method: 'get'
  })
}

// 获取会员列表（支持等级/状态筛选）
const getMemberList = (params) => {
  return $axios({
    url: '/api/retention/list',
    method: 'get',
    params: params
  })
}

// 获取积分排行榜 Top 20
const getPointsRanking = () => {
  return $axios({
    url: '/api/retention/ranking',
    method: 'get'
  })
}

// 获取流失预警会员（>30 天未下单）
const getChurnWarning = () => {
  return $axios({
    url: '/api/retention/warning',
    method: 'get'
  })
}

// 获取智能优惠券推荐
const getSmartRecommend = () => {
  return $axios({
    url: '/api/retention/recommend',
    method: 'get'
  })
}

// 向单个会员发送优惠券
const sendCoupon = (memberId) => {
  return $axios({
    url: '/api/retention/send',
    method: 'post',
    params: { memberId: memberId }
  })
}

// 批量发送优惠券
const batchSendCoupon = (memberIds) => {
  return $axios({
    url: '/api/retention/send-batch',
    method: 'post',
    data: memberIds
  })
}