// ==================== 支付 API ====================
// 注意：PaymentController 映射在 /api/payment（与 /order 控制器不同），
// 且 /api/payment/** 不在登录白名单内，必须保持用户登录态才能调用。

// 创建支付订单并获取支付链接/二维码（PayResponse: tradeNo / payUrl / qrCodeUrl / errorMsg）
function paymentCreateApi(data) {
  return $axios({
    'url': '/api/payment/pay',
    'method': 'post',
    data
  })
}

// 沙箱模拟支付完成：回调通知接口（仅 mock-mode=true 的开发/演示环境会受理，
// 生产环境 mock-mode=false 时由微信/支付宝服务端真实回调，前端不应调用此方法）。
// params 需带 out_trade_no(商户单号)、sign(非空) 及对应渠道金额字段：
// 微信 total_fee(分)、支付宝 total_amount(元)
function paymentNotifyMockApi(channel, params) {
  return $axios({
    'url': '/api/payment/notify/' + channel,
    'method': 'post',
    data: params
  })
}
