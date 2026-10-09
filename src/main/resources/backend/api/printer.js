// 打印模块接口（员工在浏览器手动调本地打印机打印）

// 打印记录（员工发起打印的流水）
const printerTaskPage = (params) => $axios({ url: '/printer/task/page', method: 'get', params })
const printerTaskStats = () => $axios({ url: '/printer/task/stats', method: 'get' })

// 渲染订单小票并保存一条打印记录，返回小票纯文本供浏览器 window.print() 打印
// type: BILL-收银小票、KITCHEN-厨房制作单、DELIVERY-配送单
const printerPrint = (orderId, type) => $axios({ url: `/printer/print/${orderId}`, method: 'post', params: { type } })
