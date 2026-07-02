## Task 4: 外卖平台页面

### order-list.html 关键差异

- 表格列：platformOrderId, platform(美团/饿了么/抖音标签), dishSummary, amount, userName, address, status, orderTime
- 搜索：platform(下拉), status, date range
- 顶部按钮：同步菜品、同步库存（调用 deliverySyncMenu / deliverySyncStock）
- 操作：手动接单（调用 deliveryAccept）
- 引用的 API 文件：`../../api/delivery.js`

### Steps

- [ ] **Step 1: 创建 `delivery/order-list.html`**
- [ ] **Step 2: 提交**

```bash
git add src/main/resources/backend/page/delivery/order-list.html
git commit -m "feat(frontend): add delivery platform order list page"
```
