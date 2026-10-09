// 枚举字典接口：后端 /api/meta/enums 是状态码的唯一真源
// 背景：订单状态此前在两端各自硬编码且互不一致（后台 0 基、C 端 1 基但 2/3 语义相反），
// 现统一由后端下发。页面采用「本地兜底 + 远程覆盖」：接口失败时兜底值也是正确的 1 基。
const metaEnums = () => $axios({ url: '/api/meta/enums', method: 'get' })

/**
 * 将 [{code,label}] 转为 { '1': '待付款', ... } 便于 O(1) 查找。
 * 注意：后端枚举 value 有 int 与 String 两种（如订单状态为 int、积分类型为 "IN"），
 * 故统一用 String(code) 作为 key，页面比较时也用 String(x)。
 */
function toEnumMap(items) {
  const map = {}
  if (!Array.isArray(items)) return map
  items.forEach(function (it) {
    if (it && it.code !== undefined && it.code !== null) map[String(it.code)] = it.label
  })
  return map
}
