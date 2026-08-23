// 成本核算 API

// 菜品成本管理
const getDishCostList = () => $axios({ url: '/cost/dish/list', method: 'get' })
const getDishCostByDishId = (dishId) => $axios({ url: `/cost/dish/${dishId}`, method: 'get' })
const saveDishCost = (data) => $axios({ url: '/cost/dish', method: 'post', data })
const updateDishCost = (data) => $axios({ url: '/cost/dish', method: 'put', data })
const deleteDishCost = (id) => $axios({ url: `/cost/dish/${id}`, method: 'delete' })
const batchUpdateDishCost = (data) => $axios({ url: '/cost/dish/batch', method: 'post', data })

// 成本记录管理
const getCostRecordList = (params) => $axios({ url: '/cost/record/list', method: 'get', params })
const saveCostRecord = (data) => $axios({ url: '/cost/record', method: 'post', data })
const deleteCostRecord = (id) => $axios({ url: `/cost/record/${id}`, method: 'delete' })

// 人工成本管理
const getLaborCostList = (params) => $axios({ url: '/cost/labor/list', method: 'get', params })
const saveLaborCost = (data) => $axios({ url: '/cost/labor', method: 'post', data })
const updateLaborCost = (data) => $axios({ url: '/cost/labor', method: 'put', data })
const deleteLaborCost = (id) => $axios({ url: `/cost/labor/${id}`, method: 'delete' })
const batchSaveLaborCost = (data) => $axios({ url: '/cost/labor/batch', method: 'post', data })

// 其他成本管理
const getOtherCostList = (params) => $axios({ url: '/cost/other/list', method: 'get', params })
const saveOtherCost = (data) => $axios({ url: '/cost/other', method: 'post', data })
const updateOtherCost = (data) => $axios({ url: '/cost/other', method: 'put', data })
const deleteOtherCost = (id) => $axios({ url: `/cost/other/${id}`, method: 'delete' })

// 成本统计分析
const getCostSummary = (params) => $axios({ url: '/cost/summary', method: 'get', params })
const getCostTrend = (params) => $axios({ url: '/cost/trend', method: 'get', params })
const getCostStructure = (params) => $axios({ url: '/cost/structure', method: 'get', params })
const getDishCostRanking = (params) => $axios({ url: '/cost/dish/ranking', method: 'get', params })
const getCostRanking = getDishCostRanking // 别名：页面调用 getCostRanking
const calculateProfitRate = (dishId) => $axios({ url: `/cost/dish/profit-rate/${dishId}`, method: 'get' })
const getCostAlert = (params) => $axios({ url: '/cost/alert', method: 'get', params })
const getCostAlerts = getCostAlert // 别名：页面调用 getCostAlerts
