// 修改点：筛选下拉选项（动态加载区域名称）
const areaOptions = () => $axios({ url: '/api/dining/area/options', method: 'get' })

const areaPage = (params) => $axios({ url: '/api/dining/area/page', method: 'get', params })
const areaList = () => $axios({ url: '/api/dining/area/list', method: 'get' })
const addArea = (params) => $axios({ url: '/api/dining/area', method: 'post', data: params })
const updateArea = (params) => $axios({ url: '/api/dining/area', method: 'put', data: params })
const deleteArea = (id) => $axios({ url: `/api/dining/area/${id}`, method: 'delete' })
const getArea = (id) => $axios({ url: `/api/dining/area/${id}`, method: 'get' })

const tablePage = (params) => $axios({ url: '/api/dining/table/page', method: 'get', params })
// 修改点：桌台区域聚合统计（SQL 分组，替代前端 pageSize:999 拉全量分组）
const tableAreaStats = () => $axios({ url: '/api/dining/table/area-stats', method: 'get' })
// 域⑩-A：桌台统计（按状态分类计数，替代 pageSize:1 多状态并发查询 hack）
const tableStats = () => $axios({ url: '/api/dining/table/stats', method: 'get' })
const addTable = (params) => $axios({ url: '/api/dining/table', method: 'post', data: params })
const updateTable = (params) => $axios({ url: '/api/dining/table', method: 'put', data: params })
const deleteTable = (id) => $axios({ url: `/api/dining/table/${id}`, method: 'delete' })
const getTable = (id) => $axios({ url: `/api/dining/table/${id}`, method: 'get' })
const updateTableStatus = (params) => $axios({ url: '/api/dining/table/status', method: 'put', data: params })
const tableQrcode = (id) => $axios({ url: `/api/dining/table/qrcode/${id}`, method: 'get' })
const tableInfo = (id) => $axios({ url: `/api/dining/table/${id}`, method: 'get' })

const queuePage = (params) => $axios({ url: '/api/dining/queue/page', method: 'get', params })
const queueTake = (params) => $axios({ url: '/api/dining/queue/take', method: 'post', data: params })
const queueCall = (params) => $axios({ url: '/api/dining/queue/call', method: 'put', data: params })
const queueCancel = (id) => $axios({ url: `/api/dining/queue/cancel/${id}`, method: 'put' })
// 域⑩-B：排队统计 + 安排入座 + 退回/恢复
const queueStats = () => $axios({ url: '/api/dining/queue/stats', method: 'get' })
const queueSeat = (data) => $axios({ url: '/api/dining/queue/seat', method: 'put', data })
const recallQueue = (id) => $axios({ url: `/api/dining/queue/recall/${id}`, method: 'put' })
const reactivateQueue = (id) => $axios({ url: `/api/dining/queue/reactivate/${id}`, method: 'put' })

const reservationPage = (params) => $axios({ url: '/api/dining/reservation/page', method: 'get', params })
const addReservation = (params) => $axios({ url: '/api/dining/reservation', method: 'post', data: params })
const confirmReservation = (id) => $axios({ url: `/api/dining/reservation/confirm/${id}`, method: 'put' })
const cancelReservation = (id) => $axios({ url: `/api/dining/reservation/cancel/${id}`, method: 'put' })
const arriveReservation = (id) => $axios({ url: `/api/dining/reservation/arrive/${id}`, method: 'put' })


