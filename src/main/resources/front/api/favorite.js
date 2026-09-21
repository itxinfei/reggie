// 用户收藏 API（C端 /api/favorite 系列）

// 切换收藏（已收藏取消、未收藏加入）
function toggleFavorite(targetType, targetId) {
  return $axios({
    url: '/api/favorite/toggle',
    method: 'post',
    data: { targetType: targetType, targetId: targetId }
  })
}

// 当前用户某类型全部已收藏对象ID（星标初始化）
function getFavoriteIds(targetType) {
  return $axios({
    url: '/api/favorite/ids',
    method: 'get',
    params: { targetType: targetType }
  })
}

// 收藏列表
function getFavoriteList(targetType) {
  return $axios({
    url: '/api/favorite/list',
    method: 'get',
    params: { targetType: targetType }
  })
}
