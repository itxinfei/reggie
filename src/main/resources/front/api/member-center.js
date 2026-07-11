// 获取当前用户的会员信息（含等级、积分、余额、优惠券数量）
function getMyMemberInfo() {
  return $axios({ url: '/api/member/member/my-info', method: 'get' })
}

// 获取我的积分记录（分页）
function getMyPointsList(params) {
  return $axios({ url: '/api/member/points/page', method: 'get', params: params })
}

// 获取我的充值记录（分页）
function getMyRechargeList(params) {
  return $axios({ url: '/api/member/recharge/page', method: 'get', params: params })
}

// 获取我的优惠券列表
function getMyCoupons(memberId) {
  return $axios({ url: '/api/member/coupon-user/my/' + memberId, method: 'get' })
}

// 领取优惠券
function claimCoupon(data) {
  return $axios({ url: '/api/member/coupon-template/claim', method: 'post', data: data })
}

// 获取可领取优惠券列表
function getAvailableCoupons() {
  return $axios({ url: '/api/member/coupon-template/page', method: 'get', params: { page: 1, pageSize: 100, status: 1 } })
}
