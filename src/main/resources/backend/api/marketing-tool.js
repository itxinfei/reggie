// Marketing Tool API

// New Customer Discount
const getNewCustomerDiscounts = () => $axios({ url: '/marketing/tool/new-customer/list', method: 'get' })
const saveNewCustomerDiscount = (data) => $axios({ url: '/marketing/tool/new-customer', method: 'post', data })
const updateNewCustomerDiscount = (data) => $axios({ url: '/marketing/tool/new-customer', method: 'put', data })
const deleteNewCustomerDiscount = (id) => $axios({ url: `/marketing/tool/new-customer/${id}`, method: 'delete' })
const calculateNewCustomerDiscount = (params) => $axios({ url: '/marketing/tool/new-customer/calculate', method: 'post', params })

// Buy Get Free
const getBuyGetFreeActivities = () => $axios({ url: '/marketing/tool/buy-get-free/list', method: 'get' })
const saveBuyGetFree = (data) => $axios({ url: '/marketing/tool/buy-get-free', method: 'post', data })
const updateBuyGetFree = (data) => $axios({ url: '/marketing/tool/buy-get-free', method: 'put', data })
const deleteBuyGetFree = (id) => $axios({ url: `/marketing/tool/buy-get-free/${id}`, method: 'delete' })
const calculateBuyGetFreeGift = (params) => $axios({ url: '/marketing/tool/buy-get-free/calculate', method: 'post', params })

// Flash Sale
const getFlashSales = () => $axios({ url: '/marketing/tool/flash-sale/list', method: 'get' })
const saveFlashSale = (data) => $axios({ url: '/marketing/tool/flash-sale', method: 'post', data })
const updateFlashSale = (data) => $axios({ url: '/marketing/tool/flash-sale', method: 'put', data })
const deleteFlashSale = (id) => $axios({ url: `/marketing/tool/flash-sale/${id}`, method: 'delete' })
const getActiveFlashSales = () => $axios({ url: '/marketing/tool/flash-sale/active', method: 'get' })
const calculateFlashSalePrice = (params) => $axios({ url: '/marketing/tool/flash-sale/calculate', method: 'post', params })

// Statistics
const getMarketingToolStatistics = () => $axios({ url: '/marketing/tool/statistics', method: 'get' })
