// 数据概览 API
const dashboardAll = (params) => $axios({ url: '/api/dashboard/all', method: 'get', params })
const dashboardTrend = (params) => $axios({ url: '/api/dashboard/trend', method: 'get', params })