# 瑞吉外卖功能改进实施计划

**Goal:** 在当前技术栈上完善核心商业功能，提升与大厂的竞争力

**Tech Stack:** Vue 2.x, Vant 2.x, Axios, 原生HTML/JS, Remix Icon

## 全局约束

- 技术栈不变：Vue 2 + Vant + 原生HTML/JS
- 不引入构建工具：保持原生HTML页面
- 不引入TypeScript：保持JavaScript
- 保持现有代码风格和目录结构

---

## 已完成功能（2026-07-14）

### P0 - 核心商业功能

| 功能 | 状态 | 实现位置 |
|------|------|----------|
| 在线支付 | ✅ 完成 | `module/payment/` - 微信/支付宝支付通道 |
| 配送追踪 | ✅ 完成 | `module/delivery/` + `page/tracking.html` |
| 优惠券体系 | ✅ 完成 | `module/member/` - CouponTemplate/CouponUser |

### P1 - 用户体验优化

| 功能 | 状态 | 实现位置 |
|------|------|----------|
| 会员体系 | ✅ 完成 | `module/member/` - 等级/积分/优惠券/充值 |
| 订单评价 | ✅ 完成 | `my-evaluations.html` + `evaluation.js` |
| 常购清单 | ✅ 完成 | `page/frequent-orders.html` + `frequent.js` |
| 配送追踪页面 | ✅ 完成 | `page/tracking.html` + `tracking.js` |

### P2 - 功能增强

| 功能 | 状态 | 实现位置 |
|------|------|----------|
| 语音搜索 | ✅ 完成 | `index.html` 搜索栏麦克风按钮 |
| 分享功能 | ✅ 完成 | `pay-success.html` + `order.html` |
| 预约下单 | ✅ 完成 | `add-order.html` 立即/预约切换 + 日期时段选择 |

---

## 执行进展

### 已完成（2026-07-14）

| 功能 | 状态 | 改动文件 |
|------|------|----------|
| 配送追踪前端页面 | ✅ | `api/tracking.js`, `page/tracking.html`, `DeliveryController` + `/tracking/{orderId}`, `order.html` 追踪入口 |
| 常购清单 | ✅ | `api/frequent.js`, `page/frequent-orders.html`, `user.html` 入口 |
| 订单评价完善 | ✅ | `my-evaluations.html` 写评价入口栏, `order.html` 评价按钮 |
| 语音搜索 | ✅ | `index.html` 搜索栏麦克风按钮, Web Speech API |
| 分享功能 | ✅ | `pay-success.html` + `order.html` 分享按钮, Web Share API |
| 预约下单 | ✅ | `add-order.html` 立即/预约切换 + 日期时段选择弹窗, 复用 `expectDeliveryTime` |
| 过期文件清理 | ✅ | 删除 5 个旧计划 + 5 个旧设计文档 + 2 个过期文档 |
| 测试 | ✅ | 修复 delivery 测试数据, 7/7 通过 |

---

## 全部完成

计划中所有功能已实现。后续可根据业务需求新增功能。
