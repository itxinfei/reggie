// User Experience API

// WebSocket Notifications
const sendOrderNotification = (params) => $axios({ url: '/ux/notification/order', method: 'post', params })
const sendKitchenNotification = (params) => $axios({ url: '/ux/notification/kitchen', method: 'post', params })
const sendSystemNotification = (params) => $axios({ url: '/ux/notification/system', method: 'post', params })

// Voice Broadcast
const getNewOrderVoice = (params) => $axios({ url: '/ux/voice/new-order', method: 'get', params })
const getOrderReminderVoice = (params) => $axios({ url: '/ux/voice/order-reminder', method: 'get', params })
const getPaymentReceivedVoice = (params) => $axios({ url: '/ux/voice/payment-received', method: 'get', params })
const getQueueCallVoice = (params) => $axios({ url: '/ux/voice/queue-call', method: 'get', params })
