// 菜品规格 API

// 规格组管理
const getSpecGroups = () => $axios({ url: '/dish/spec/group/list', method: 'get' })
const getSpecGroupById = (id) => $axios({ url: `/dish/spec/group/${id}`, method: 'get' })
const saveSpecGroup = (data) => $axios({ url: '/dish/spec/group', method: 'post', data })
const updateSpecGroup = (data) => $axios({ url: '/dish/spec/group', method: 'put', data })
const deleteSpecGroup = (id) => $axios({ url: `/dish/spec/group/${id}`, method: 'delete' })

// 规格选项管理
const getSpecOptions = (params) => $axios({ url: '/dish/spec/option/list', method: 'get', params })
const saveSpecOption = (data) => $axios({ url: '/dish/spec/option', method: 'post', data })
const updateSpecOption = (data) => $axios({ url: '/dish/spec/option', method: 'put', data })
const deleteSpecOption = (id) => $axios({ url: `/dish/spec/option/${id}`, method: 'delete' })
const batchSaveSpecOptions = (data) => $axios({ url: '/dish/spec/option/batch', method: 'post', data })

// 菜品规格关联
const getDishSpecGroups = (dishId) => $axios({ url: `/dish/spec/dish/${dishId}`, method: 'get' })
const setDishSpecGroups = (dishId, data) => $axios({ url: `/dish/spec/dish/${dishId}`, method: 'post', data })
const deleteDishSpecRelations = (dishId) => $axios({ url: `/dish/spec/dish/${dishId}`, method: 'delete' })

// 规格价格计算
const calculateSpecPrice = (params, data) => $axios({ url: '/dish/spec/price/calculate', method: 'post', params, data })
const getDishSpecDetail = (dishId) => $axios({ url: `/dish/spec/detail/${dishId}`, method: 'get' })

// 统计分析
const getSpecStatistics = () => $axios({ url: '/dish/spec/statistics', method: 'get' })
