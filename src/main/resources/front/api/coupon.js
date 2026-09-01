// 优惠券 API 模块（C端 /front/coupon 系列）
// 注意：函数名统一加 Front 后缀，避免与 member-center.js 同名函数（/api/member/coupon-* 系列）冲突

// 获取下单可用优惠券列表
function getUsableCoupons(orderAmount) {
  return $axios({
    url: '/front/coupon/usable',
    method: 'get',
    params: { orderAmount: orderAmount }
  })
}

// 获取我的优惠券列表
function getMyCouponsFront() {
  return $axios({
    url: '/front/coupon/my',
    method: 'get'
  })
}

// 获取可领取优惠券列表
function getAvailableCouponsFront() {
  return $axios({
    url: '/front/coupon/available',
    method: 'get'
  })
}

// 领取优惠券
function claimCouponFront(templateId) {
  return $axios({
    url: '/front/coupon/claim/' + templateId,
    method: 'post'
  })
}
