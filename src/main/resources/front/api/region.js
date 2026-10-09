// 地区 API 模块

// 获取子地区列表（按父级ID）
function getRegionChildren(parentId) {
  return $axios({
    url: '/region/children',
    method: 'get',
    params: { parentId: parentId || 0 }
  })
}
