/**
 * 员工管理 API
 * 修改点：使用 $axios 统一请求拦截器
 */
const employeePage = (params) => $axios({ url: '/employee/page', method: 'get', params })
const addEmployee = (params) => $axios({ url: '/employee', method: 'post', data: { ...params } })
const editEmployee = (params) => $axios({ url: '/employee', method: 'put', data: { ...params } })
const queryEmployeeById = (id) => $axios({ url: `/employee/${id}`, method: 'get' })
const deleteEmployee = (ids) => $axios({ url: '/employee', method: 'delete', params: { ids } })
const enableOrDisableEmployee = (params) => $axios({ url: '/employee/status', method: 'put', data: { id: params.id, status: params.status } })
const employeeOptions = () => $axios({ url: '/employee/options', method: 'get' })

// 修改密码
const updatePassword = (data) => $axios({ url: '/employee/password', method: 'put', data: data })
