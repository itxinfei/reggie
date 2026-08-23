const memberPage = (params) => $axios({ url: '/api/member/member/page', method: 'get', params })
const memberStats = () => $axios({ url: '/api/member/member/stats', method: 'get' })
const addMember = (params) => $axios({ url: '/api/member/member', method: 'post', data: params })
const updateMember = (params) => $axios({ url: '/api/member/member', method: 'put', data: params })
const getMember = (id) => $axios({ url: `/api/member/member/${id}`, method: 'get' })
const memberByPhone = (phone) => $axios({ url: `/api/member/member/by-phone?phone=${phone}`, method: 'get' })
const memberRecharge = (params) => $axios({ url: '/api/member/member/recharge', method: 'post', data: params })
const memberDeductBalance = (params) => $axios({ url: '/api/member/member/deduct-balance', method: 'post', data: params })
const memberMyInfo = () => $axios({ url: '/api/member/member/my-info', method: 'get' })

const levelPage = (params) => $axios({ url: '/api/member/level/page', method: 'get', params })
const addLevel = (params) => $axios({ url: '/api/member/level', method: 'post', data: params })
const updateLevel = (params) => $axios({ url: '/api/member/level', method: 'put', data: params })
const deleteLevel = (id) => $axios({ url: `/api/member/level/${id}`, method: 'delete' })
const getLevel = (id) => $axios({ url: `/api/member/level/${id}`, method: 'get' })
// 修改点：会员等级聚合统计（后端聚合，替代前端仅当前页 records 计算口径偏差）
const levelStats = () => $axios({ url: '/api/member/level/stats', method: 'get' })

const rechargePage = (params) => $axios({ url: '/api/member/recharge/page', method: 'get', params })
const rechargeStats = () => $axios({ url: '/api/member/recharge/stats', method: 'get' })

const pointsPage = (params) => $axios({ url: '/api/member/points/page', method: 'get', params })
const pointsStats = () => $axios({ url: '/api/member/points/stats', method: 'get' })

const couponTemplatePage = (params) => $axios({ url: '/api/member/coupon-template/page', method: 'get', params })
const couponStats = () => $axios({ url: '/api/member/coupon-template/stats', method: 'get' })
const addCouponTemplate = (params) => $axios({ url: '/api/member/coupon-template', method: 'post', data: params })
const updateCouponTemplate = (params) => $axios({ url: '/api/member/coupon-template', method: 'put', data: params })
const deleteCouponTemplate = (id) => $axios({ url: `/api/member/coupon-template/${id}`, method: 'delete' })
const getCouponTemplate = (id) => $axios({ url: `/api/member/coupon-template/${id}`, method: 'get' })

const couponUserPage = (params) => $axios({ url: '/api/member/coupon-user/page', method: 'get', params })
const couponMy = (memberId) => $axios({ url: `/api/member/coupon-user/my/${memberId}`, method: 'get' })
// 修改点：收银台选券——按用户ID与订单金额查询可用优惠券
const memberAvailableCoupons = (userId, orderAmount) => $axios({ url: `/api/member/coupon-user/available?userId=${userId}&orderAmount=${orderAmount}`, method: 'get' })

// 修改点：域⑥ 会员营销-无定向发券
const couponBatchIssue = (params) => $axios({ url: '/api/member/coupon-template/batch-issue', method: 'post', data: params })
const couponIssueByCondition = (params) => $axios({ url: '/api/member/coupon-template/issue-by-condition', method: 'post', data: params })
const couponTemplateList = () => $axios({ url: '/api/member/coupon-template/page?pageSize=100', method: 'get' })

// 修改点：域⑦ 会员营销-投放明细与效果追溯
const couponIssued = (templateId, params) => $axios({ url: `/api/member/coupon-template/${templateId}/issued`, method: 'get', params })
const couponEffect = (templateId) => $axios({ url: `/api/member/coupon-template/${templateId}/effect`, method: 'get' })

// 修改点：域⑧ 会员营销-到期预警与批量延期
const couponExpiring = (params) => $axios({ url: '/api/member/coupon-template/expiring', method: 'get', params })
const couponExpired = (params) => $axios({ url: '/api/member/coupon-template/expired', method: 'get', params })
const couponExpiringStats = (days) => $axios({ url: `/api/member/coupon-template/expiring-stats?days=${days}`, method: 'get' })
const couponBatchExtend = (params) => $axios({ url: '/api/member/coupon-template/batch-extend', method: 'post', data: params })
