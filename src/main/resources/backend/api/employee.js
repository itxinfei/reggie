// 员工管理 API（原 api/member.js——文件名与内容不符，已按内容正名为 employee.js）
// 修改点：getMemberList→getEmployeePage、getMemberStats→getEmployeeStats（原函数名误导，实际查询的是员工）

// 筛选下拉选项（动态加载员工姓名）
const employeeOptions = () => $axios({ url: '/employee/options', method: 'get' })

// 员工分页列表
function getEmployeePage (params) {
  return $axios({
    url: '/employee/page',
    method: 'get',
    params
  })
}

// 员工统计聚合接口（后端 count 查询），替代前端 pageSize:1000 全量拉取
function getEmployeeStats () {
  return $axios({
    url: '/employee/stats',
    method: 'get'
  })
}

// 启用/禁用员工
function enableOrDisableEmployee (params) {
  return $axios({
    url: '/employee/status',
    method: 'put',
    data: { id: params.id, status: params.status }
  })
}

// 添加员工
function addEmployee (params) {
  return $axios({
    url: '/employee',
    method: 'post',
    data: { ...params }
  })
}

// 修改员工
function editEmployee (params) {
  return $axios({
    url: '/employee',
    method: 'put',
    data: { ...params }
  })
}

// 编辑页反查员工详情
function queryEmployeeById (id) {
  return $axios({
    url: `/employee/${id}`,
    method: 'get'
  })
}

// 删除员工（支持批量）
function deleteEmployee (ids) {
  return $axios({
    url: '/employee',
    method: 'delete',
    params: { ids: ids }
  })
}
