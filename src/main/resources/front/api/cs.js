// 在线客服 / 投诉 API（C端 /cs/portal 系列，仅需登录态）

// 创建客服会话
function csCreateSession(sessionType, orderId) {
  return $axios({
    url: '/cs/portal/session/create',
    method: 'post',
    params: { sessionType: sessionType || 1, orderId: orderId }
  })
}

// 我的会话列表
function csMySessions(status) {
  return $axios({
    url: '/cs/portal/session/list',
    method: 'get',
    params: { status: status }
  })
}

// 关闭会话
function csCloseSession(sessionId, rating, feedback) {
  return $axios({
    url: '/cs/portal/session/' + sessionId + '/close',
    method: 'post',
    params: { rating: rating, feedback: feedback }
  })
}

// 发送消息（data: sessionId/messageType/content/imageUrl）
function csSendMessage(data) {
  return $axios({
    url: '/cs/portal/message/send',
    method: 'post',
    data: data
  })
}

// 会话消息列表（opts.silent=true 时网络瞬断不跳断网页/不弹横幅，供 4s 后台轮询使用）
function csListMessages(sessionId, opts) {
  var silent = opts && opts.silent;
  return $axios({
    url: '/cs/portal/message/list/' + sessionId,
    method: 'get',
    skipNoWifiRedirect: !!silent,
    silent: !!silent
  })
}

// 标记该会话客服消息已读
function csMarkRead(sessionId) {
  return $axios({
    url: '/cs/portal/message/read/' + sessionId,
    method: 'post'
  })
}

// 提交投诉（data: complaintType/title/content/imageUrls/orderId/orderNumber）
function csCreateComplaint(data) {
  return $axios({
    url: '/cs/portal/complaint/create',
    method: 'post',
    data: data
  })
}

// 我的投诉列表
function csMyComplaints(status, type) {
  return $axios({
    url: '/cs/portal/complaint/list',
    method: 'get',
    params: { status: status, type: type }
  })
}

// 评价投诉处理
function csRateComplaint(complaintId, satisfaction, feedback) {
  return $axios({
    url: '/cs/portal/complaint/' + complaintId + '/rate',
    method: 'post',
    params: { satisfaction: satisfaction, feedback: feedback }
  })
}
