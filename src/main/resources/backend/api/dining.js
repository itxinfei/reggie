// 堂食模块 API（桌台 / 区域 / 排队 / 预订）
// 注意：函数名以后台页面实际调用为准（table-page 族），
// 旧命名（getTableList 等）仍被 cashier/index.html 引用，保留勿删。

/* ── 桌台（旧命名，收银台 cashier/index.html 在用） ── */
const getTableList = () => $axios({ url: '/api/dining/table/list', method: 'get' })

// 桌台统计（按状态分类计数）
const getTableStats = () => $axios({ url: '/api/dining/table/stats', method: 'get' })

// 桌台明细（含订单+菜品列表，收银台用）
const getTableDetail = (tableId) => $axios({ url: '/api/dining/table/detail/' + tableId, method: 'get' })

// 加菜（为已有订单追加菜品）
const addItemsToTable = (data) => $axios({ url: '/api/dining/table/addItems', method: 'post', data: data })

// 转台
const transferTable = (data) => $axios({ url: '/api/dining/table/transfer', method: 'post', data: data })

// 并台
const mergeTables = (data) => $axios({ url: '/api/dining/table/merge', method: 'post', data: data })

// 拆台
const splitTable = (params) => $axios({ url: '/api/dining/table/split', method: 'post', params: params })

// 一键开台
const openWithOrder = (params) => $axios({ url: '/api/dining/table/openWithOrder', method: 'post', params: params })

// 结账（复用订单接口）
const checkoutOrder = (id, payMethod) => $axios({ url: '/order/checkout', method: 'post', data: { id: id, payMethod: payMethod } })

/* ── 桌台（页面命名，table-list.html 等） ── */
// 桌台分页（page/pageSize/name/areaId/status: FREE|OCCUPIED|RESERVED|CLEANING；pageSize 上限 100）
const tablePage = (params) => $axios({ url: '/api/dining/table/page', method: 'get', params: params })

// 桌台统计（TableStatsVO: totalTables/freeTables/occupiedTables/reservedTables/cleaningTables）
const tableStats = () => $axios({ url: '/api/dining/table/stats', method: 'get' })

// 区域维度桌台聚合统计（桌台总数 + 最大容量区域名称/桌数）
const tableAreaStats = () => $axios({ url: '/api/dining/table/area-stats', method: 'get' })

// 桌台明细（TableDetailVO：桌台信息 + 关联订单 + 菜品明细）
const tableInfo = (tableId) => $axios({ url: '/api/dining/table/detail/' + tableId, method: 'get' })

// 桌台扫码二维码（data:image/png;base64）
const tableQrcode = (id) => $axios({ url: '/api/dining/table/qrcode/' + id, method: 'get' })

// 桌台列表（不分页，按 sort 排序，含 areaName）
const tableListAll = () => $axios({ url: '/api/dining/table/list', method: 'get' })

// 新增桌台（areaId 必填）
const addTable = (data) => $axios({ url: '/api/dining/table', method: 'post', data: data })

// 修改桌台（body 含 id）
const updateTable = (data) => $axios({ url: '/api/dining/table', method: 'put', data: data })

// 删除桌台
const deleteTable = (id) => $axios({ url: '/api/dining/table/' + id, method: 'delete' })

// 修改桌台状态（body: {id, status}）
const updateTableStatus = (data) => $axios({ url: '/api/dining/table/status', method: 'put', data: data })

// 一键开台（query: tableId 必填，customerCount/remark 可选；返回 {tableId, orderId, orderNumber}）
const openTableWithOrder = (params) => $axios({ url: '/api/dining/table/openWithOrder', method: 'post', params: params })

/* ── 区域（area-list.html） ── */
// 区域分页（仅 page/pageSize，无筛选条件）
const areaPage = (params) => $axios({ url: '/api/dining/area/page', method: 'get', params: params })

// 区域列表（不分页，按 sort 升序）
const areaList = () => $axios({ url: '/api/dining/area/list', method: 'get' })

// 区域名称下拉选项（{names: [...]}）
const areaOptions = () => $axios({ url: '/api/dining/area/options', method: 'get' })

// 新增区域
const addArea = (data) => $axios({ url: '/api/dining/area', method: 'post', data: data })

// 修改区域（body 含 id）
const updateArea = (data) => $axios({ url: '/api/dining/area', method: 'put', data: data })

// 删除区域
const deleteArea = (id) => $axios({ url: '/api/dining/area/' + id, method: 'delete' })

/* ── 排队（queue-list.html） ── */
// 排队分页（page/pageSize/status: WAITING|CALLED|SEATED|CANCELLED/phone 模糊）
const queuePage = (params) => $axios({ url: '/api/dining/queue/page', method: 'get', params: params })

// 排队统计（QueueStatsVO: totalQueues/waitingCount/calledCount/seatedCount/cancelledCount）
const queueStats = () => $axios({ url: '/api/dining/queue/stats', method: 'get' })

// 取号（body: {seatCount, phone}）
const queueTake = (data) => $axios({ url: '/api/dining/queue/take', method: 'post', data: data })

// 叫号（body 可选，含 seatCount 筛选；无等待顾客时返回 error）
const queueCall = (data) => $axios({ url: '/api/dining/queue/call', method: 'put', data: data })

// 取消排队
const queueCancel = (id) => $axios({ url: '/api/dining/queue/cancel/' + id, method: 'put' })

// 入座（body: {queueId, tableId}，CALLED → SEATED）
const queueSeat = (data) => $axios({ url: '/api/dining/queue/seat', method: 'put', data: data })

// 重新叫号（CALLED → WAITING）
const recallQueue = (id) => $axios({ url: '/api/dining/queue/recall/' + id, method: 'put' })

// 恢复排队（CANCELLED → WAITING）
const reactivateQueue = (id) => $axios({ url: '/api/dining/queue/reactivate/' + id, method: 'put' })

/* ── 预订（reservation-list.html） ── */
// 预订分页（page/pageSize/status: PENDING|CONFIRMED|ARRIVED|CANCELLED/customerName/phone/reservedDate/beginTime/endTime）
const reservationPage = (params) => $axios({ url: '/api/dining/reservation/page', method: 'get', params: params })

// 预订统计（totalReservations/pendingCount/confirmedCount/arrivedCount/cancelledCount）
const reservationStats = () => $axios({ url: '/api/dining/reservation/stats', method: 'get' })

// 新增预订（body: {customerName, phone, reservedTime, seatCount, tableId, remark}）
const addReservation = (data) => $axios({ url: '/api/dining/reservation', method: 'post', data: data })

// 修改预订（body 含 id，仅 PENDING/CONFIRMED 可改）
const updateReservation = (data) => $axios({ url: '/api/dining/reservation', method: 'put', data: data })

// 确认预订
const confirmReservation = (id) => $axios({ url: '/api/dining/reservation/confirm/' + id, method: 'put' })

// 取消预订
const cancelReservation = (id) => $axios({ url: '/api/dining/reservation/cancel/' + id, method: 'put' })

// 到店（标记已到店）
const arriveReservation = (id) => $axios({ url: '/api/dining/reservation/arrive/' + id, method: 'put' })

// 删除预订（仅 CANCELLED 状态可删）
const deleteReservation = (id) => $axios({ url: '/api/dining/reservation/' + id, method: 'delete' })
