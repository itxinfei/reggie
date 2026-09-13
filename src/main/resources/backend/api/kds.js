// 后厨出餐大屏 KDS API
// 对应后端 com.reggie.module.kds.controller.KdsController（/api/kds，员工鉴权）
window.kdsApi = {
  // 出餐大屏看板：autoPull=true 时先自动拉取已下单订单生成工单
  board: function (autoPull) {
    return $axios({
      url: '/api/kds/board',
      method: 'get',
      params: { autoPull: autoPull === false ? false : true }
    })
  },
  // 手动拉取新订单生成工单，返回新生成数量
  pull: function () {
    return $axios({ url: '/api/kds/pull', method: 'post' })
  },
  start: function (id) {
    return $axios({ url: '/api/kds/start/' + id, method: 'put' })
  },
  ready: function (id) {
    return $axios({ url: '/api/kds/ready/' + id, method: 'put' })
  },
  finish: function (id) {
    return $axios({ url: '/api/kds/finish/' + id, method: 'put' })
  },
  cancel: function (id) {
    return $axios({ url: '/api/kds/cancel/' + id, method: 'put' })
  },
  toggleUrgent: function (id) {
    return $axios({ url: '/api/kds/urgent/' + id, method: 'put' })
  },
  // 工单历史分页 params={page,pageSize,status}
  page: function (params) {
    return $axios({ url: '/api/kds/page', method: 'get', params: params })
  }
}
