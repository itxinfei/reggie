// C端堂食API

// 获取桌台信息
const tableInfo = (id) => $axios({ url: '/api/dining/table/' + id, method: 'get' })

// 堂食下单
const submitEatInOrder = (data) => $axios({ url: '/order/eatIn', method: 'post', data: data })
