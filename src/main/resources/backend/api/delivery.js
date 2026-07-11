// ==================== 外卖配送 API 模块 ====================
// 修改点：完善所有 API 接口，新增状态流转/详情/筛选选项/统计

/** 分页查询外卖订单 */
const deliveryOrderPage = (params) => $axios({ url: '/api/delivery/orders', method: 'get', params })

/** 查询外卖订单详情 */
const deliveryOrderDetail = (id) => $axios({ url: '/api/delivery/orders/' + id, method: 'get' })

/** 接单（PENDING → ACCEPTED） */
const deliveryAccept = (params) => $axios({ url: '/api/delivery/accept', method: 'post', data: params })

/** 更新配送状态（完整生命周期：取餐/配送/送达/取消） */
const deliveryUpdateStatus = (params) => $axios({ url: '/api/delivery/status', method: 'put', params })

/** 获取筛选选项（平台、状态） */
const deliveryFilterOptions = (params) => $axios({ url: '/api/delivery/options', method: 'get', params })

/** 获取配送统计数据 */
const deliveryStats = (params) => $axios({ url: '/api/delivery/stats', method: 'get', params })

/** 同步菜品到外卖平台 */
const deliverySyncMenu = () => $axios({ url: '/api/delivery/sync/menu', method: 'post' })

/** 同步库存到外卖平台 */
const deliverySyncStock = () => $axios({ url: '/api/delivery/sync/stock', method: 'post' })
