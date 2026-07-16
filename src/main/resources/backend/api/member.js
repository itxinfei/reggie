// 修改点：筛选下拉选项（动态加载员工姓名）
const employeeOptions = () => $axios({ url: '/employee/options', method: 'get' })

function getMemberList (params) {
  return $axios({
    url: '/employee/page',
    method: 'get',
    params
  })
}

// 修改点：员工统计聚合接口（后端 count 查询），替代前端 pageSize:1000 全量拉取
function getMemberStats () {
  return $axios({
    url: '/employee/stats',
    method: 'get'
  })
}

// 修改---启用禁用接口
function enableOrDisableEmployee (params) {
  return $axios({
    url: '/employee/status',
    method: 'put',
    data: { id: params.id, status: params.status }
  })
}

// 新增---添加员工
function addEmployee (params) {
  return $axios({
    url: '/employee',
    method: 'post',
    data: { ...params }
  })
}

// 修改---添加员工
function editEmployee (params) {
  return $axios({
    url: '/employee',
    method: 'put',
    data: { ...params }
  })
}

// 修改页面反查详情接口
function queryEmployeeById (id) {
  return $axios({
    url: `/employee/${id}`,
    method: 'get'
  })
}