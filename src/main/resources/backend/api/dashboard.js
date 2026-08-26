// 数据概览 API
const dashboardAll = (params) => $axios({ url: '/api/dashboard/all', method: 'get', params })
const dashboardTrend = (params) => $axios({ url: '/api/dashboard/trend', method: 'get', params })
const redisStatus = () => $axios({ url: '/api/dashboard/redis-status', method: 'get' })