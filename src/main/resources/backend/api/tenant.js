// 分页查询租户（平台超管，支持名称/状态筛选）
const tenantPage = (params) => $axios({ url: '/tenant/page', method: 'get', params })

// 查询租户详情
const getTenant = (id) => $axios({ url: `/tenant/${id}`, method: 'get' })

// 编辑租户基本信息与套餐（不含状态）
const updateTenant = (data) => $axios({ url: '/tenant', method: 'put', data })

// 启用/禁用租户
const updateTenantStatus = (id, status) =>
  $axios({ url: '/tenant/status', method: 'put', params: { id: id, status: status } })
