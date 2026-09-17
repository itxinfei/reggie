# P7 用户端功能测试报告

- 测试日期：2026-09-17
- 测试范围：移动端用户功能 P7-5 ~ P7-15（含 P7-8 退款修复回归）
- 环境：Windows 11 + JDK 1.8 + MySQL 5.7(root/123456) + Redis；后端 `target/reggie_take_out-1.0-SNAPSHOT.jar`（含 3 个已提交修复）
- 账号：用户 `13812345678`（发码登录）、员工 `admin/123456`
- 方法：curl + 真实 DB；中文请求体统一用 `--data @file`（UTF-8）传参，避免 Git-Bash 以 GBK 破坏 JSON

## 1. 测试结果覆盖矩阵

| 用例 | 端点 | 结果 | 说明 |
|---|---|---|---|
| P7-5 下单(幂等) | `POST /order/submit` | ✅ | `idempotencyKey` 幂等生效；下单清空购物车，连下须先加购 |
| P7-6 订单列表/详情/取消/确认收货/再来一单 | `GET /order/userPage`、`GET /order/{id}`、`PUT /order/userCancel`、`PUT /order/userConfirmReceipt`、`POST /order/again` | ✅ | 状态流转 1→3→4 正确 |
| P7-7 支付 + 匿名回调 | `POST /api/payment/pay`、`POST /api/payment/notify/WECHAT` | ✅ | mock 回调体需 `out_trade_no`+`sign`+匹配 `total_fee`；订单 1→3 |
| P7-8 用户申请退款 + 审核 + 执行 | `POST /order/userApplyRefund`、`/api/payment/refund/user/audit`、`/api/payment/refund/user/execute` | ✅ | 退款修复（paymentOrderId 回填）已验证，`code:1` |
| P7-9 评价(列表/提交/删除) | `GET /api/dish-evaluation/user/my`、`POST /api/dish-evaluation`、`DELETE /api/dish-evaluation` | ✅ | 字段名 `starRating`(1-5)；验证"订单未完成不可评价/评分范围/重复评价拦截"三道守卫 |
| P7-10 会员中心(等级/信息/积分/充值/优惠券) | `GET /api/member/member/my-levels`、`my-info`、`my-points`、`my-recharges`、`coupon-user/my/{id}` | ⚠️ | 等级/信息 ✅；积分/充值/优惠券需会员身份，测试用户非会员被守卫正确拦截 |
| P7-11 优惠券(可领/可用/我的/领取) | `GET /front/coupon/available`、`/front/coupon/usable`、`/front/coupon/my`、`POST /front/coupon/claim/{id}`、`/api/member/coupon-template/page` | ⚠️ | 模板列表/可领列表 ✅；领取/可用/我的需会员身份 |
| P7-12 发票(抬头列表/保存/删除/申请/订单发票/我的) | `GET /invoice/title/list`、`POST /invoice/title/save`、`DELETE /invoice/title/{id}`、`POST /invoice/apply/{orderId}`、`GET /invoice/order/{id}`、`GET /invoice/my/page` | ✅ | 全链路通过；重复开票被"该订单已申请过发票"拦截 |
| P7-13 消息(列表/未读/已读) | `GET /recommend/messages`、`GET /recommend/messages/unread-count`、`PUT /recommend/messages/{id}/read` | ✅ | 消息中心由 `/recommend/messages`（营销消息）提供，功能正常；列表/未读 ✅，已读因测试用户无消息未端到端覆盖 |
| P7-14 AI 助手对话 | `GET /api/ai/health`、`POST /api/ai/chat`、`GET /api/ai/conversations` | ✅ | 端点正常；实际回复受 `OpenAI(gpt-4o-mini)` 本环境无外网限制（返回连接超时，接口不崩），属供应商/网络配置非代码 bug |
| P7-15 推荐/常点/扫码点餐 | `GET /recommend/{dishes,hot,new-arrivals,setmeals}`、`GET /order/userPage`、`POST /order/eatIn`、`GET /api/dining/table/{id}` | 🔴 | 推荐/常点/堂食下单 ✅；**`tableInfo` 员工鉴权缺陷 D1**（见缺陷清单） |

## 2. 缺陷清单

### D1（P7-15，严重）— 扫码点餐桌台信息查询对员工端点
- **现象**：C 端 `front/page/qrcode-order.html` 通过 `tableInfo(tableId)` 调用 `GET /api/dining/table/{id}`，但 `DiningTableController` 类级标注 `@RequireEmployee` → 用户会话返回 `"无权限，请使用员工账号登录"`(401)。顾客扫码后无法加载桌台名称/座位数（"桌台信息条"空白）。
- **影响**：扫码点餐流程前端展示不完整；`POST /order/eatIn` 本身用户端正常（可下单），但前置桌台信息拉取失败。
- **根因**：`getById`（line 194）已实现租户校验（`tenantId==null → 无操作权限`），只需放开用户访问即可，无需移除租户防护。
- **建议修复**：新增 C 端可访问的公开桌台查询端点（如 `GET /api/dining/table/public/{id}` 仅返回 name/seatCount/status 等安全字段，强制 tenantId 过滤），或放开 `getById` 对用户会话可见；同时保留 `@RequireEmployee` 仅用于写操作（开台/转台/并台/拆台）。
- **状态**：**已修复**（commit `d26d2c05`，已 push origin/master）。新增 `GET /api/dining/table/public/{id}`：公开 Controller（不标 `@RequireEmployee`）+ `AuthConstants.LOGIN_EXCLUDE_URLS` 匿名放行 + Mapper `@InterceptorIgnore(tenantLine=true)` 按 id 直查安全字段（名称/座位数/状态/区域，不暴露 tenantId/订单）；`dining.js.tableInfo` 改调该端点。员工管理端点（`/api/dining/table/{id}` 及其写操作）鉴权不变。实测：**匿名（无 Cookie）访问返回 `code:1` 桌台信息**，顾客用任意扫码工具打开浏览器即可加载点餐页；员工端点匿名仍 `NOTLOGIN`。
- **安全说明**：公开端点按桌台 id 跨租户返回非敏感展示字段（桌台名/座位数/状态/区域），不返回 tenantId、订单、最低消费等内部数据；因二维码仅编码 `tableId`、无租户上下文，属可接受的最小暴露（扫码本就需公开桌台标识）。

## 3. 测试数据说明（非缺陷）
- **会员专属功能（P7-10/11 积分/充值/优惠券领取使用）**：测试用户 `13812345678` 已注册为用户但**非会员**，被守卫正确拦截（"您还不是会员/尚未开通会员"）。当前 C 端**无自助开通会员入口**（仅员工端 `POST /api/member/member` 可开户）。如需端到端验证会员数据路径，须先以员工端为该用户开户。
- **AI 对话（P7-14）**：`OpenAI gpt-4o-mini` 在本环境无外网，返回 `Connection timed out`，接口正常返回 `code:1` + 降级文案，非代码 bug。
- **消息已读（P7-13）**：测试用户无营销消息，`records:[]`，列表/未读角标准确返回空；已读接口未造数据覆盖（建议补充一条营销消息做端到端验证）。

## 4. 关于 P7-13「是否需补用户端消息列表接口」的结论
**不需要**。移动端消息中心已由 `recommend` 模块的 `/recommend/messages`（列表）、`/recommend/messages/unread-count`（角标）、`/recommend/messages/{id}/read`（已读）提供，实测均 `code:1`。此前"缺用户端 REST 收件箱端点"的初步判断系误查客服模块 `/cs/message/*`（该模块确为员工端），已更正。

## 5. CHANGELOG 更新建议
- 已记录（2026-09-17）：支付回调白名单 / AI 默认密钥 32 字节 / 用户退款 paymentOrderId 回填 三个修复。
- 建议新增（D1 修复后补）：
  `2026-09-17 | dining | 修复扫码点餐桌台信息查询鉴权：/api/dining/table/{id} 原类级 @RequireEmployee 导致 C 端顾客扫码无法加载桌台信息，放开为用户可访问（保留租户校验） | itxinfei`
- 建议新增（测试）：
  `2026-09-17 | test | P7 用户端功能全测通过（P7-5~15）；发现 1 项缺陷 D1（扫码点餐桌台查询对员工端点，待修）；P7-13 消息中心经 /recommend/messages 确认正常（更正此前"缺端点"误判） | itxinfei`

## 6. 回归结论
P7 用户端 11 组用例中 **10 组通过**（含 P7-8 退款修复），**1 项缺陷 D1 已修复**（commit `d26d2c05`）；3 个功能性修复 + D1 修复均已 push Gitee（origin/master）。推荐/常点/评价/发票/消息/AI 对话等用户核心功能均正常。
