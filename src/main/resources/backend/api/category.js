// 修改点：筛选下拉选项（动态加载分类名称）
const categoryOptions = () => $axios({ url: '/category/options', method: 'get' })

// 查询列表接口
const getCategoryPage = (params) => {
  return $axios({
    url: '/category/page',
    method: 'get',
    params
  })
}

// 修改点：分类统计数据（后端直接查DB，准确无分页限制）
const getCategoryStats = () => $axios({ url: '/category/stats', method: 'get' })

// 编辑页面反查详情接口
const queryCategoryById = (id) => {
  return $axios({
    url: `/category/${id}`,
    method: 'get'
  })
}

// 删除当前列的接口
const deleCategory = (id) => {
  return $axios({
    url: `/category/${id}`,
    method: 'delete'
  })
}

// 修改接口
const editCategory = (params) => {
  return $axios({
    url: '/category',
    method: 'put',
    data: { ...params }
  })
}

// 新增接口
const addCategory = (params) => {
  return $axios({
    url: '/category',
    method: 'post',
    data: { ...params }
  })
}