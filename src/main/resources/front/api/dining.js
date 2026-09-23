// C端堂食API

// 获取桌台信息（扫码点餐公开端点，支持顾客未登录/非员工匿名访问）
const tableInfo = (id) => $axios({ url: '/api/dining/table/public/' + id, method: 'get' })

// 获取桌台所属门店的公开菜单（分类 + 在售菜品，扫码点餐匿名访问）
const getTableMenu = (id) => $axios({ url: '/api/dining/table/public/' + id + '/menu', method: 'get' })

// 堂食下单
const submitEatInOrder = (data) => $axios({ url: '/order/eatIn', method: 'post', data: data })
