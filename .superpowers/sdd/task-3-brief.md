## Task 3: 支付管理页面

### order-list.html 关键差异

- 表格列：id(tradeNo), orderId, channel(支付宝/微信标签), amount(￥前缀), status(待支付/成功/失败/已退款标签), paidTime
- 搜索：orderId, channel(下拉), status(下拉), date range (el-date-picker type=datetimerange)
- 操作：查看详情(弹框)、退款(仅status=SUCCESS时显示，二次确认弹框含退款金额和原因)
- 引用的 API 文件：`../../api/payment.js`

### Steps

- [ ] **Step 1: 创建 `payment/order-list.html`**
- [ ] **Step 2: 提交**

```bash
git add src/main/resources/backend/page/payment/order-list.html
git commit -m "feat(frontend): add payment order list page"
```
