# 瑞吉外卖功能改进实施计划

**Goal:** 在当前技术栈上完善核心商业功能，提升与大厂的竞争力

**Architecture:** 保持现有架构不变，通过添加新的HTML页面、API接口和前端逻辑来扩展功能

**Tech Stack:** Vue 2.x, Vant 2.x, Axios, 原生HTML/JS, Remix Icon

## 全局约束

- 技术栈不变：Vue 2 + Vant + 原生HTML/JS
- 不引入构建工具：保持原生HTML页面
- 不引入TypeScript：保持JavaScript
- 保持现有代码风格和目录结构

---

## 已完成功能（2026-07-14 审计）

### P0 - 核心商业功能（已完成）

| 功能 | 状态 | 实现位置 |
|------|------|----------|
| 在线支付 | ✅ 完成 | `module/payment/` - PaymentOrder/RefundRecord 模型，微信/支付宝支付通道，`/api/payment` 控制器，测试覆盖 |
| 配送追踪 | ✅ 完成 | `module/delivery/` - DeliveryOrder 模型，美团/饿了么/抖音平台适配，`/api/delivery` 控制器，测试覆盖 |
| 优惠券体系 | ✅ 完成 | `module/member/` - CouponTemplate/CouponUser/PointsRecord/MemberLevel 模型，多控制器，测试覆盖 |

### P1 - 用户体验优化（部分完成）

| 功能 | 状态 | 备注 |
|------|------|------|
| 会员体系 | ✅ 完成 | 等级、积分、优惠券、充值全部实现 |
| 订单评价 | ✅ 后端完成 | DishEvaluation 实体 + evaluation.js API + my-evaluations.html 页面 |
| 常购清单 | ❌ 未实现 | 需要前端页面 + 后端接口 |

### P2 - 功能增强（未实现）

| 功能 | 状态 | 备注 |
|------|------|------|
| 语音搜索 | ❌ 未实现 | 移动端 Speech Recognition API |
| 分享功能 | ❌ 未实现 | Web Share API / 自定义分享 |
| 预约下单 | ❌ 未实现 | 预约配送时间（已有基础时间选择） |

---

## 执行进展

### 已完成（2026-07-14）

| 功能 | 状态 | 改动文件 |
|------|------|----------|
| 配送追踪前端页面 | ✅ 完成 | `api/tracking.js`, `page/tracking.html`, `DeliveryController` + `/tracking/{orderId}`, `order.html` 添加追踪入口 |
| 常购清单 | ✅ 完成 | `api/frequent.js`, `page/frequent-orders.html`, `user.html` 添加入口 |
| 订单评价完善 | ✅ 完成 | `my-evaluations.html` 添加写评价入口栏, `order.html` 添加评价按钮 |
| 语音搜索 | ✅ 完成 | `index.html` 搜索栏添加麦克风按钮, 基于 Web Speech API |
| 分享功能 | ✅ 完成 | `pay-success.html` + `order.html` 添加分享按钮, 基于 Web Share API |
| 过期文件清理 | ✅ 完成 | 删除 5 个旧计划 + 5 个旧设计文档 + 2 个过期测试文档 |
| 测试 | ✅ 完成 | 修复 delivery 测试数据, 7/7 通过 |

## 待实现任务

### Task 6: 预约下单

### Task 1: 配送追踪前端页面

**现状：** 后端 `module/delivery/` 已完成，但缺少移动端前端页面

**Files:**
- Create: `src/main/resources/front/page/tracking.html` - 配送追踪页面
- Create: `src/main/resources/front/api/tracking.js` - 配送追踪前端API
- Modify: `src/main/resources/front/page/order.html` - 添加追踪入口

---

### Task 2: 常购清单功能

**Files:**
- Create: `src/main/resources/front/page/frequent-orders.html` - 常购清单页面
- Create: `src/main/resources/front/api/frequent.js` - 常购清单API
- Create: 后端 API 接口（或复用现有订单查询）

---

### Task 3: 订单评价完善

**现状：** 后端评价 API 已完成，前端有 `my-evaluations.html`

**Files:**
- Modify: `src/main/resources/front/page/order.html` - 添加评价入口
- Modify: 评价提交流程优化

---

### Task 4: 语音搜索

**Files:**
- Modify: `src/main/resources/front/index.html` - 首页搜索栏添加语音按钮
- Create: 语音搜索逻辑（Web Speech API）

---

### Task 5: 分享功能

**Files:**
- Modify: 订单详情/菜品页面添加分享按钮
- Create: 分享逻辑（Web Share API）

---

## 执行优先级

1. **Task 1: 配送追踪前端页面** - 后端已就绪，补齐前端即可
2. **Task 2: 常购清单** - 快速复购，提升转化率
3. **Task 3: 订单评价完善** - 已有基础，优化体验
4. **Task 4: 语音搜索** - 差异化功能
5. **Task 5: 分享功能** - 社交传播
