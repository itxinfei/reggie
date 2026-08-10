// Finance API

// Withdrawal Management
const getWithdrawalList = (params) => $axios({ url: '/finance/withdrawal/list', method: 'get', params })
const getWithdrawalById = (id) => $axios({ url: `/finance/withdrawal/${id}`, method: 'get' })
const createWithdrawal = (data) => $axios({ url: '/finance/withdrawal', method: 'post', data })
const reviewWithdrawal = (id, params) => $axios({ url: `/finance/withdrawal/${id}/review`, method: 'post', params })
const processWithdrawalPayment = (id, params) => $axios({ url: `/finance/withdrawal/${id}/payment`, method: 'post', params })
const cancelWithdrawal = (id) => $axios({ url: `/finance/withdrawal/${id}/cancel`, method: 'post' })
const deleteWithdrawal = (id) => $axios({ url: `/finance/withdrawal/${id}`, method: 'delete' })

// Reconciliation Management
const getReconciliationList = (params) => $axios({ url: '/finance/reconciliation/list', method: 'get', params })
const getReconciliationById = (id) => $axios({ url: `/finance/reconciliation/${id}`, method: 'get' })
const generateReconciliation = (params) => $axios({ url: '/finance/reconciliation/generate', method: 'post', params })
const confirmReconciliation = (id) => $axios({ url: `/finance/reconciliation/${id}/confirm`, method: 'post' })
const deleteReconciliation = (id) => $axios({ url: `/finance/reconciliation/${id}`, method: 'delete' })

// Profit Analysis
const getProfitAnalysisList = (params) => $axios({ url: '/finance/profit/list', method: 'get', params })
const getProfitAnalysisByDate = (date) => $axios({ url: `/finance/profit/date/${date}`, method: 'get' })
const generateProfitAnalysis = (params) => $axios({ url: '/finance/profit/generate', method: 'post', params })
const getProfitTrend = (params) => $axios({ url: '/finance/profit/trend', method: 'get', params })
const getProfitStructure = (params) => $axios({ url: '/finance/profit/structure', method: 'get', params })

// Statistics
const getFinanceStatistics = (params) => $axios({ url: '/finance/statistics', method: 'get', params })
const getWithdrawalStatistics = () => $axios({ url: '/finance/withdrawal/statistics', method: 'get' })
