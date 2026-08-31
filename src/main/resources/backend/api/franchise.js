// ==================== 加盟分账管理 API ====================

// 加盟商
const franchiseePage = (params) => $axios({ url: '/api/franchise/franchisee/page', method: 'get', params })
const franchiseeList = () => $axios({ url: '/api/franchise/franchisee/list', method: 'get' })
const getFranchisee = (id) => $axios({ url: `/api/franchise/franchisee/${id}`, method: 'get' })
const addFranchisee = (data) => $axios({ url: '/api/franchise/franchisee', method: 'post', data })
const updateFranchisee = (data) => $axios({ url: '/api/franchise/franchisee', method: 'put', data })
const deleteFranchisee = (ids) => $axios({ url: `/api/franchise/franchisee?ids=${ids}`, method: 'delete' })
const franchiseeStats = () => $axios({ url: '/api/franchise/franchisee/stats', method: 'get' })

// 加盟合同（含抽成规则）
const contractPage = (params) => $axios({ url: '/api/franchise/contract/page', method: 'get', params })
const contractList = () => $axios({ url: '/api/franchise/contract/list', method: 'get' })
const getContract = (id) => $axios({ url: `/api/franchise/contract/${id}`, method: 'get' })
const addContract = (data) => $axios({ url: '/api/franchise/contract', method: 'post', data })
const updateContract = (data) => $axios({ url: '/api/franchise/contract', method: 'put', data })
const deleteContract = (ids) => $axios({ url: `/api/franchise/contract?ids=${ids}`, method: 'delete' })
const contractStats = () => $axios({ url: '/api/franchise/contract/stats', method: 'get' })

// 分账结算单
const settlementPage = (params) => $axios({ url: '/api/franchise/settlement/page', method: 'get', params })
const getSettlement = (id) => $axios({ url: `/api/franchise/settlement/${id}`, method: 'get' })
const generateSettlement = (contractId, settlePeriod) => $axios({ url: `/api/franchise/settlement/generate?contractId=${contractId}&settlePeriod=${settlePeriod}`, method: 'post' })
const confirmSettlement = (id) => $axios({ url: `/api/franchise/settlement/confirm/${id}`, method: 'put' })
const settleSettlement = (id) => $axios({ url: `/api/franchise/settlement/settle/${id}`, method: 'put' })
const settlementStats = () => $axios({ url: '/api/franchise/settlement/stats', method: 'get' })
