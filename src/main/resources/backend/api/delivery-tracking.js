// Delivery Tracking API

// Rider Management
const getRiderList = (params) => $axios({ url: '/delivery/tracking/rider/list', method: 'get', params })
const getRiderById = (id) => $axios({ url: `/delivery/tracking/rider/${id}`, method: 'get' })
const saveRider = (data) => $axios({ url: '/delivery/tracking/rider', method: 'post', data })
const updateRider = (data) => $axios({ url: '/delivery/tracking/rider', method: 'put', data })
const deleteRider = (id) => $axios({ url: `/delivery/tracking/rider/${id}`, method: 'delete' })
const updateRiderStatus = (id, params) => $axios({ url: `/delivery/tracking/rider/${id}/status`, method: 'post', params })

// Location Tracking
const updateRiderLocation = (params) => $axios({ url: '/delivery/tracking/location/update', method: 'post', params })
const getRiderLocationHistory = (params) => $axios({ url: '/delivery/tracking/location/history', method: 'get', params })
const getOrderDeliveryTracking = (orderId) => $axios({ url: `/delivery/tracking/tracking/order/${orderId}`, method: 'get' })
const getRiderCurrentLocation = (riderId) => $axios({ url: `/delivery/tracking/tracking/rider/${riderId}`, method: 'get' })

// Delivery Time Management
const createDeliveryTimeRecord = (data) => $axios({ url: '/delivery/tracking/time/record', method: 'post', data })
const updateDeliveryTimeRecord = (data) => $axios({ url: '/delivery/tracking/time/record', method: 'put', data })
const getDeliveryTimeByOrderId = (orderId) => $axios({ url: `/delivery/tracking/time/order/${orderId}`, method: 'get' })
const estimateDeliveryTime = (params) => $axios({ url: '/delivery/tracking/time/estimate', method: 'post', params })
const getDeliveryTimeStatistics = (params) => $axios({ url: '/delivery/tracking/time/statistics', method: 'get', params })

// Statistics
const getRiderStatistics = (riderId, params) => $axios({ url: `/delivery/tracking/rider/${riderId}/statistics`, method: 'get', params })
const getDeliveryOverview = () => $axios({ url: '/delivery/tracking/overview', method: 'get' })
