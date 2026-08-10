// Customer Service API

// Session Management
const createCsSession = (params) => $axios({ url: '/cs/session/create', method: 'post', params })
const getCsSessionList = (params) => $axios({ url: '/cs/session/list', method: 'get', params })
const getCsSessionById = (id) => $axios({ url: `/cs/session/${id}`, method: 'get' })
const assignAgent = (id, params) => $axios({ url: `/cs/session/${id}/assign`, method: 'post', params })
const closeCsSession = (id, params) => $axios({ url: `/cs/session/${id}/close`, method: 'post', params })

// Message Management
const sendCsMessage = (params) => $axios({ url: '/cs/message/send', method: 'post', params })
const getCsMessages = (sessionId) => $axios({ url: `/cs/message/list/${sessionId}`, method: 'get' })
const getUnreadCsMessageCount = (sessionId, params) => $axios({ url: `/cs/message/unread/${sessionId}`, method: 'get', params })
const markCsMessagesAsRead = (sessionId, params) => $axios({ url: `/cs/message/read/${sessionId}`, method: 'post', params })

// Complaint Management
const createComplaint = (data) => $axios({ url: '/cs/complaint/create', method: 'post', data })
const getComplaintList = (params) => $axios({ url: '/cs/complaint/list', method: 'get', params })
const getComplaintById = (id) => $axios({ url: `/cs/complaint/${id}`, method: 'get' })
const handleComplaint = (id, params) => $axios({ url: `/cs/complaint/${id}/handle`, method: 'post', params })
const closeComplaint = (id) => $axios({ url: `/cs/complaint/${id}/close`, method: 'post' })
const rateComplaint = (id, params) => $axios({ url: `/cs/complaint/${id}/rate`, method: 'post', params })

// Statistics
const getCsStatistics = (params) => $axios({ url: '/cs/statistics', method: 'get', params })
const getComplaintStatistics = (params) => $axios({ url: '/cs/complaint/statistics', method: 'get', params })
const getAgentWorkload = (agentId, params) => $axios({ url: `/cs/agent/${agentId}/workload`, method: 'get', params })
