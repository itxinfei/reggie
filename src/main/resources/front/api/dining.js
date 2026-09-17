// C端堂食API

// 获取桌台信息（扫码点餐公开端点，支持顾客未登录/非员工匿名访问）
const tableInfo = (id) => $axios({ url: '/api/dining/table/public/' + id, method: 'get' })

// 堂食下单
const submitEatInOrder = (data) => $axios({ url: '/order/eatIn', method: 'post', data: data })
