// Enhanced Report API

// Food Cost Report
const getFoodCostReport = (params) => $axios({ url: '/report/enhanced/food-cost/report', method: 'get', params })
const getFoodCostTrend = (params) => $axios({ url: '/report/enhanced/food-cost/trend', method: 'get', params })
const getFoodCostByCategory = (params) => $axios({ url: '/report/enhanced/food-cost/category', method: 'get', params })
const getFoodCostRanking = (params) => $axios({ url: '/report/enhanced/food-cost/ranking', method: 'get', params })

// Enhanced Sales Report
const getWeeklyReport = (params) => $axios({ url: '/report/enhanced/sales/weekly', method: 'get', params })
const getMonthlyReport = (params) => $axios({ url: '/report/enhanced/sales/monthly', method: 'get', params })
const getYearlyReport = (params) => $axios({ url: '/report/enhanced/sales/yearly', method: 'get', params })
const getSalesComparison = (params) => $axios({ url: '/report/enhanced/sales/comparison', method: 'get', params })
const getSalesTrend = (params) => $axios({ url: '/report/enhanced/sales/trend', method: 'get', params })
const getTopSellingItems = (params) => $axios({ url: '/report/enhanced/sales/top-items', method: 'get', params })
const getSalesByTimePeriod = (params) => $axios({ url: '/report/enhanced/sales/time-period', method: 'get', params })
const getCustomerAnalysis = (params) => $axios({ url: '/report/enhanced/sales/customer-analysis', method: 'get', params })
const getRevenueForecast = (params) => $axios({ url: '/report/enhanced/sales/revenue-forecast', method: 'get', params })
