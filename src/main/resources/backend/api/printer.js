// 打印模块接口（门店 PC 本地打印）
// 打印终端（门店 PC 打印代理）
const printerTerminalPage = (params) => $axios({ url: '/printer/terminal/page', method: 'get', params })
const printerTerminalStats = () => $axios({ url: '/printer/terminal/stats', method: 'get' })
const printerTerminalStatus = (id, status) => $axios({ url: `/printer/terminal/status/${id}`, method: 'put', params: { status } })
const printerTerminalTest = (id) => $axios({ url: `/printer/terminal/test/${id}`, method: 'post' })
const printerTerminalDelete = (id) => $axios({ url: `/printer/terminal/${id}`, method: 'delete' })

// 打印任务（门店 PC 打印代理执行流水）
const printerTaskPage = (params) => $axios({ url: '/printer/task/page', method: 'get', params })
const printerTaskStats = () => $axios({ url: '/printer/task/stats', method: 'get' })

// 订单打印（入队到门店终端）
const printerPrint = (orderId, type) => $axios({ url: `/printer/print/${orderId}`, method: 'post', params: { type } })
