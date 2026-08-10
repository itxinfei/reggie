// 配送增强 API

// 配送范围管理
const getRangeRules = () => $axios({ url: '/delivery/enhanced/range/list', method: 'get' })
const getRangeRuleById = (id) => $axios({ url: `/delivery/enhanced/range/${id}`, method: 'get' })
const saveRangeRule = (data) => $axios({ url: '/delivery/enhanced/range', method: 'post', data })
const updateRangeRule = (data) => $axios({ url: '/delivery/enhanced/range', method: 'put', data })
const deleteRangeRule = (id) => $axios({ url: `/delivery/enhanced/range/${id}`, method: 'delete' })

// 配送费阶梯管理
const getFeeSteps = (params) => $axios({ url: '/delivery/enhanced/fee-step/list', method: 'get', params })
const saveFeeStep = (data) => $axios({ url: '/delivery/enhanced/fee-step', method: 'post', data })
const updateFeeStep = (data) => $axios({ url: '/delivery/enhanced/fee-step', method: 'put', data })
const deleteFeeStep = (id) => $axios({ url: `/delivery/enhanced/fee-step/${id}`, method: 'delete' })
const batchSaveFeeSteps = (data) => $axios({ url: '/delivery/enhanced/fee-step/batch', method: 'post', data })

// 配送范围校验
const isInRange = (params) => $axios({ url: '/delivery/enhanced/range/check', method: 'post', params })
const findMatchingRule = (params) => $axios({ url: '/delivery/enhanced/range/find', method: 'post', params })

// 配送费计算
const calculateDeliveryFee = (params) => $axios({ url: '/delivery/enhanced/fee/calculate', method: 'post', params })
const calculateFee = (params) => $axios({ url: '/delivery/enhanced/fee/auto-calculate', method: 'post', params })
const calculateDistance = (params) => $axios({ url: '/delivery/enhanced/distance/calculate', method: 'post', params })

// 统计分析
const getDeliveryStatistics = () => $axios({ url: '/delivery/enhanced/statistics', method: 'get' })
const getRangeCoverage = () => $axios({ url: '/delivery/enhanced/coverage', method: 'get' })
