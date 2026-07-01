const paymentPage = (params) => $axios({ url: '/api/payment/page', method: 'get', params })
const paymentCreate = (params) => $axios({ url: '/api/payment/pay', method: 'post', data: params })
const paymentRefund = (params) => $axios({ url: '/api/payment/refund', method: 'post', data: params })
const paymentQuery = (tradeNo) => $axios({ url: `/api/payment/query/${tradeNo}`, method: 'get' })
