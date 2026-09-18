// 堂食桌台管理 API
// 桌台列表（不分页）
const getTableList = () => $axios({ url: '/api/dining/table/list', method: 'get' })

// 桌台统计（按状态分类计数）
const getTableStats = () => $axios({ url: '/api/dining/table/stats', method: 'get' })

// 桌台明细（含订单+菜品列表，收银台用）
const getTableDetail = (tableId) => $axios({ url: '/api/dining/table/detail/' + tableId, method: 'get' })

// 加菜（为已有订单追加菜品）
const addItemsToTable = (data) => $axios({ url: '/api/dining/table/addItems', method: 'post', data: data })

// 转台
const transferTable = (data) => $axios({ url: '/api/dining/table/transfer', method: 'post', data: data })

// 并台
const mergeTables = (data) => $axios({ url: '/api/dining/table/merge', method: 'post', data: data })

// 拆台
const splitTable = (params) => $axios({ url: '/api/dining/table/split', method: 'post', params: params })

// 一键开台
const openWithOrder = (params) => $axios({ url: '/api/dining/table/openWithOrder', method: 'post', params: params })

// 结账（复用订单接口）
const checkoutOrder = (id, payMethod) => $axios({ url: '/order/checkout', method: 'post', data: { id: id, payMethod: payMethod } })
