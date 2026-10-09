// 用户管理 API
const userPage = (params) => $axios({ url: '/user/page', method: 'get', params })
const userStats = () => $axios({ url: '/user/stats', method: 'get' })
const userStatus = (params) => $axios({ url: '/user/status', method: 'put', params })
const userOptions = () => $axios({ url: '/user/options', method: 'get' })
const userDelete = (id) => $axios({ url: '/user', method: 'delete', params: { id } })