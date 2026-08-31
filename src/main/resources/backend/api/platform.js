// 外卖平台接入配置 API
window.platformApi = {
  // 列表
  list: function (params) {
    return $axios({
      url: '/admin/platform/config/list',
      method: 'get',
      params: params
    })
  },
  // 详情
  detail: function (id) {
    return $axios({
      url: '/admin/platform/config/detail',
      method: 'get',
      params: { id: id }
    })
  },
  // 新增
  add: function (data) {
    return $axios({
      url: '/admin/platform/config/add',
      method: 'post',
      data: data
    })
  },
  // 更新
  update: function (data) {
    return $axios({
      url: '/admin/platform/config/update',
      method: 'post',
      data: data
    })
  },
  // 删除
  remove: function (id) {
    return $axios({
      url: '/admin/platform/config/delete',
      method: 'post',
      params: { id: id }
    })
  },
  // 启用/停用
  toggle: function (id, enabled) {
    return $axios({
      url: '/admin/platform/config/toggle',
      method: 'post',
      params: { id: id, enabled: enabled }
    })
  },
  // 即时拉单：从指定平台拉取最近订单并落库
  pull: function (platformType, minutes) {
    return $axios({
      url: '/api/platform/pull',
      method: 'get',
      params: { platformType: platformType, minutes: minutes || 30 }
    })
  },
  // 回传订单状态到平台（accept/reject/prepare/complete/cancel）
  pushStatus: function (platformType, platformOrderId, action) {
    return $axios({
      url: '/api/platform/pushStatus',
      method: 'post',
      params: { platformType: platformType, platformOrderId: platformOrderId, action: action }
    })
  },
  // 同步菜品上/下架到平台
  syncDish: function (platformType, dishId, action) {
    return $axios({
      url: '/api/platform/syncDish',
      method: 'post',
      params: { platformType: platformType, dishId: dishId, action: action }
    })
  },
  // 同步库存到平台
  syncStock: function (platformType, platformDishId, remainQty) {
    return $axios({
      url: '/api/platform/syncStock',
      method: 'post',
      params: { platformType: platformType, platformDishId: platformDishId, remainQty: remainQty }
    })
  },
  // 同步营业状态到平台
  syncBusiness: function (platformType, open) {
    return $axios({
      url: '/api/platform/syncBusiness',
      method: 'post',
      params: { platformType: platformType, open: open }
    })
  },
  // 菜品平台映射分页
  mappingPage: function (params) {
    return $axios({
      url: '/admin/platform/mapping/page',
      method: 'get',
      params: params
    })
  }
}
