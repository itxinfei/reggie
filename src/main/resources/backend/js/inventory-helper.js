/**
 * 库存模块输入辅助（无构建环境，挂到 window.RgInventoryHelper）
 *   - queryMaterials：按名称远程模糊搜索食材，供 el-autocomplete 使用
 *   - queryUnits：常用计量单位本地候选（仍允许自由输入新单位）
 * 依赖页面已引入 inventory.js（materialPage）。
 */
window.RgInventoryHelper = {
  /** 常用计量单位本地候选 */
  commonUnits: ['瓶', '箱', '斤', '公斤', '千克', '克', '个', '袋', '包', '桶', '升', '毫升', '盒', '份', '打'],

  /** 员工姓名缓存（首次调用 /employee/options 后复用） */
  employeeNameCache: null,

  /**
   * 远程按名称模糊搜索食材
   * @param {string} query 名称关键字
   * @return {Promise<Array<{value:string, raw:object}>>}
   */
  queryMaterials: function (query) {
    if (typeof materialPage !== 'function') return Promise.resolve([])
    return materialPage({ page: 1, pageSize: 20, name: query }).then(function (res) {
      if (!res || String(res.code) !== '1' || !res.data) return []
      var records = res.data.records || []
      return records.map(function (r) { return { value: r.name, raw: r } })
    }).catch(function () { return [] })
  },

  /**
   * 单位本地候选（包含输入串即返回；空串返回全部常用单位）
   * @return {Array<{value:string}>}
   */
  queryUnits: function (query) {
    var q = String(query || '').trim()
    var list = this.commonUnits
    if (q) list = list.filter(function (u) { return u.indexOf(q) >= 0 })
    return list.map(function (u) { return { value: u } })
  },

  /**
   * 操作员/员工姓名候选（数据来自 /employee/options 的 names，首次后缓存）
   * @return {Promise<Array<{value:string}>>}
   */
  queryEmployees: function (query) {
    var self = this
    var promise = this.employeeNameCache
      ? Promise.resolve(this.employeeNameCache)
      : $axios({ url: '/employee/options', method: 'get' }).then(function (res) {
          if (res && String(res.code) === '1' && res.data) {
            // 仅成功才缓存，避免首次请求失败后把空数组长期缓存导致候选一直为空
            var names = res.data.names || []
            self.employeeNameCache = names
            return names
          }
          return []
        }).catch(function () { return [] })
    return promise.then(function (names) {
      var q = String(query || '').trim()
      var arr = names
      if (q) arr = arr.filter(function (n) { return n.indexOf(q) >= 0 })
      return arr.map(function (n) { return { value: n } })
    })
  }
}
