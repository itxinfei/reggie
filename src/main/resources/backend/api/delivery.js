const deliveryOrderPage = (params) => $axios({ url: '/api/delivery/orders', method: 'get', params })
const deliveryAccept = (params) => $axios({ url: '/api/delivery/accept', method: 'post', data: params })
const deliverySyncMenu = () => $axios({ url: '/api/delivery/sync/menu', method: 'post' })
const deliverySyncStock = () => $axios({ url: '/api/delivery/sync/stock', method: 'post' })
