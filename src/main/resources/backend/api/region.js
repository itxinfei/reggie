// 修改点：筛选下拉选项（动态加载地区名称）
const regionOptions = () => $axios({ url: '/region/options', method: 'get' })

// 获取地区树形数据
const regionTree = () => $axios({ url: '/region/tree', method: 'get' })

// 根据父级ID查子级
const regionChildren = (parentId) => $axios({ url: '/region/children', method: 'get', params: { parentId } })

// 分页查询
const regionPage = (params) => $axios({ url: '/region/page', method: 'get', params })

// 详情
const queryRegionById = (id) => $axios({ url: `/region/${id}`, method: 'get' })

// 新增
const addRegion = (params) => $axios({ url: '/region', method: 'post', data: params })

// 修改
const editRegion = (params) => $axios({ url: '/region', method: 'put', data: params })

// 删除
const deleteRegion = (id) => $axios({ url: `/region/${id}`, method: 'delete' })
