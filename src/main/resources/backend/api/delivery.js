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

/** 同步菜品到外卖平台（platform 必传，dishes 可选） */
const deliverySyncMenu = (data) => $axios({ url: '/api/delivery/sync/menu', method: 'post', data: data || {} })

/** 同步库存到外卖平台（platform 必传，stock 可选） */
const deliverySyncStock = (data) => $axios({ url: '/api/delivery/sync/stock', method: 'post', data: data || {} })

/** 骑手列表（status 可选：1空闲 2配送中；不传返回全部） */
const getRiderListApi = (params) => $axios({ url: '/delivery/tracking/rider/list', method: 'get', params })

// ==================== 骑手账号管理（后台 /api/delivery/rider） ====================

/** 骑手分页（page/pageSize 上限 100，name/phone 模糊，status：0-离线 1-在线 2-忙碌） */
const riderPage = (params) => $axios({ url: '/api/delivery/rider/page', method: 'get', params })

/** 新增骑手（name/phone/password，密码 BCrypt 加密入库） */
const addRider = (data) => $axios({ url: '/api/delivery/rider', method: 'post', data })

/** 编辑骑手资料（id/name/phone/avatar，不含密码） */
const updateRider = (data) => $axios({ url: '/api/delivery/rider', method: 'put', data })

/** 重置骑手密码（id + 新密码，6-20 位） */
const resetRiderPassword = (id, password) => $axios({ url: '/api/delivery/rider/' + id + '/password', method: 'put', data: { password: password } })

/** 删除骑手 */
const deleteRiderApi = (id) => $axios({ url: '/api/delivery/rider/' + id, method: 'delete' })
