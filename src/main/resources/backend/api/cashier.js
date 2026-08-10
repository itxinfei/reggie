// 收银管理 API

// 收银记录管理
const getCashierRecordList = (params) => $axios({ url: '/cashier/record/list', method: 'get', params })
const getCashierRecordByOrderId = (orderId) => $axios({ url: `/cashier/record/order/${orderId}`, method: 'get' })
const saveCashierRecord = (data) => $axios({ url: '/cashier/record', method: 'post', data })
const cashPayment = (params) => $axios({ url: '/cashier/cash-payment', method: 'post', params })
const deleteCashierRecord = (id) => $axios({ url: `/cashier/record/${id}`, method: 'delete' })

// 日结管理
const getDailySettlementList = (params) => $axios({ url: '/cashier/settlement/list', method: 'get', params })
const getDailySettlementByDate = (date) => $axios({ url: `/cashier/settlement/date/${date}`, method: 'get' })
const executeDailySettlement = (params) => $axios({ url: '/cashier/settlement/execute', method: 'post', params })
const cancelDailySettlement = (params) => $axios({ url: '/cashier/settlement/cancel', method: 'post', params })
const deleteDailySettlement = (id) => $axios({ url: `/cashier/settlement/${id}`, method: 'delete' })

// 统计分析
const getCashierStatistics = (params) => $axios({ url: '/cashier/statistics', method: 'get', params })
const getPaymentTypeStatistics = (params) => $axios({ url: '/cashier/statistics/payment-type', method: 'get', params })
const getCashierTrend = (params) => $axios({ url: '/cashier/trend', method: 'get', params })
const getDailySettlementSummary = (params) => $axios({ url: '/cashier/settlement/summary', method: 'get', params })
