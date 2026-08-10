// 营销活动 API

// 满减规则管理
const getFullReductionRules = (params) => $axios({ url: '/marketing/full-reduction/list', method: 'get', params })
const saveFullReductionRule = (data) => $axios({ url: '/marketing/full-reduction', method: 'post', data })
const updateFullReductionRule = (data) => $axios({ url: '/marketing/full-reduction', method: 'put', data })
const deleteFullReductionRule = (id) => $axios({ url: `/marketing/full-reduction/${id}`, method: 'delete' })
const batchSaveFullReductionRules = (data) => $axios({ url: '/marketing/full-reduction/batch', method: 'post', data })

// 折扣规则管理
const getDiscountRules = (params) => $axios({ url: '/marketing/discount/list', method: 'get', params })
const saveDiscountRule = (data) => $axios({ url: '/marketing/discount', method: 'post', data })
const updateDiscountRule = (data) => $axios({ url: '/marketing/discount', method: 'put', data })
const deleteDiscountRule = (id) => $axios({ url: `/marketing/discount/${id}`, method: 'delete' })
const batchSaveDiscountRules = (data) => $axios({ url: '/marketing/discount/batch', method: 'post', data })

// 营销计算
const calculateFullReduction = (params) => $axios({ url: '/marketing/calculate/full-reduction', method: 'post', params })
const calculateDiscount = (params, data) => $axios({ url: '/marketing/calculate/discount', method: 'post', params, data })
const calculateBestDiscount = (params, data) => $axios({ url: '/marketing/calculate/best', method: 'post', params, data })

// 使用记录
const getUsageRecords = (params) => $axios({ url: '/marketing/usage/list', method: 'get', params })
const getUserUsageCount = (params) => $axios({ url: '/marketing/usage/count', method: 'get', params })

// 统计分析
const getMarketingStatistics = (params) => $axios({ url: '/marketing/statistics', method: 'get', params })
const getFullReductionEffect = (campaignId) => $axios({ url: `/marketing/effect/full-reduction/${campaignId}`, method: 'get' })
const getDiscountEffect = (campaignId) => $axios({ url: `/marketing/effect/discount/${campaignId}`, method: 'get' })
const getMarketingTrend = (params) => $axios({ url: '/marketing/trend', method: 'get', params })
const getTopActivities = (params) => $axios({ url: '/marketing/top', method: 'get', params })
