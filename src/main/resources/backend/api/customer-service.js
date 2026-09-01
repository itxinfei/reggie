// ============================================================
// 客服管理 API 模块
// 后端基路径: /cs
// 覆盖 CustomerServiceController 全部 16 个端点
// ============================================================

// ---------- 会话管理 (Session) ----------

// 创建客服会话
const createSession = (params) => {
  return $axios({
    url: '/cs/session/create',
    method: 'post',
    params: params
  })
}

// 查询会话列表
const getSessionList = (params) => {
  return $axios({
    url: '/cs/session/list',
    method: 'get',
    params: params
  })
}

// 查询会话详情
const getSessionById = (id) => {
  return $axios({
    url: '/cs/session/' + id,
    method: 'get'
  })
}

// 分配客服
const assignSession = (id, params) => {
  return $axios({
    url: '/cs/session/' + id + '/assign',
    method: 'post',
    params: params
  })
}

// 关闭会话
const closeSession = (id, params) => {
  return $axios({
    url: '/cs/session/' + id + '/close',
    method: 'post',
    params: params
  })
}

// ---------- 消息管理 (Message) ----------

// 发送消息
const sendMessage = (params) => {
  return $axios({
    url: '/cs/message/send',
    method: 'post',
    params: params
  })
}

// 查询会话消息列表
const getSessionMessages = (sessionId) => {
  return $axios({
    url: '/cs/message/list/' + sessionId,
    method: 'get'
  })
}

// 查询未读消息数
const getUnreadCount = (sessionId, params) => {
  return $axios({
    url: '/cs/message/unread/' + sessionId,
    method: 'get',
    params: params
  })
}

// 标记消息已读
const markMessagesRead = (sessionId, params) => {
  return $axios({
    url: '/cs/message/read/' + sessionId,
    method: 'post',
    params: params
  })
}

// ---------- 投诉管理 (Complaint) ----------

// 创建投诉
const createComplaint = (data) => {
  return $axios({
    url: '/cs/complaint/create',
    method: 'post',
    data: data
  })
}

// 查询投诉列表
const getComplaintList = (params) => {
  return $axios({
    url: '/cs/complaint/list',
    method: 'get',
    params: params
  })
}

// 查询投诉详情
const getComplaintById = (id) => {
  return $axios({
    url: '/cs/complaint/' + id,
    method: 'get'
  })
}

// 处理投诉
const handleComplaint = (id, params) => {
  return $axios({
    url: '/cs/complaint/' + id + '/handle',
    method: 'post',
    params: params
  })
}

// 关闭投诉
const closeComplaint = (id) => {
  return $axios({
    url: '/cs/complaint/' + id + '/close',
    method: 'post'
  })
}

// 投诉评价
const rateComplaint = (id, params) => {
  return $axios({
    url: '/cs/complaint/' + id + '/rate',
    method: 'post',
    params: params
  })
}

// ---------- 统计 ----------

// 客服综合统计
const getCustomerServiceStatistics = (params) => {
  return $axios({
    url: '/cs/statistics',
    method: 'get',
    params: params
  })
}

// 投诉统计
const getComplaintStatistics = (params) => {
  return $axios({
    url: '/cs/complaint/statistics',
    method: 'get',
    params: params
  })
}

// 客服工作量统计
const getAgentWorkload = (agentId, params) => {
  return $axios({
    url: '/cs/agent/' + agentId + '/workload',
    method: 'get',
    params: params
  })
}

// 获取可选客服列表（员工列表）
const getAgentOptions = () => {
  return $axios({
    url: '/employee/list',
    method: 'get'
  })
}
