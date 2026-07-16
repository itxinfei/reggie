const memberPage = (params) => $axios({ url: '/api/member/member/page', method: 'get', params })
const memberStats = () => $axios({ url: '/api/member/member/stats', method: 'get' })
const addMember = (params) => $axios({ url: '/api/member/member', method: 'post', data: params })
const updateMember = (params) => $axios({ url: '/api/member/member', method: 'put', data: params })
const getMember = (id) => $axios({ url: `/api/member/member/${id}`, method: 'get' })
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
