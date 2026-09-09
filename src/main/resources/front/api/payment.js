// ==================== 支付 API ====================
// 注意：PaymentController 映射在 /api/payment（与 /order 控制器不同），
// 且 /api/payment/** 不在登录白名单内，必须保持用户登录态才能调用。

// 创建支付订单并获取支付链接/二维码（PayResponse: payUrl / qrCodeUrl / errorMsg）
function paymentCreateApi(data) {
  return $axios({
    'url': '/api/payment/pay',
    'method': 'post',
    data
  })
}
