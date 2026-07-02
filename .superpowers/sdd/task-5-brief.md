## Task 5: 堂食管理页面

### area-list.html

- 表格列：name, sort
- 弹框：name, sort
- API 引用：`../../api/dining.js`（areaPage, addArea, updateArea, deleteArea）

### table-list.html

- 表格列：name, areaId(显示区域名), seatCount, status(绿色空闲/红色占用/橙色预留标签), minAmount(￥), sort
- 弹框：name, areaId(下拉加载 areaList), seatCount, minAmount, status, sort
- 操作：编辑、状态切换(点击标签弹出确认)、生成二维码(调用 tableQrcode(id) 下载)
- API 引用：`../../api/dining.js`（tablePage, addTable, updateTable, deleteTable, updateTableStatus, tableQrcode, areaList）

### queue-list.html

- 表格列：queueNo, phone, seatCount, status(等待/已叫号/已入座/已取消标签), createdTime
- 搜索：phone, status(下拉)
- 操作：叫号(queueCall)、取消(queueCancel)
- 弹框：取号弹框(phone, seatCount) 调用 queueTake
- API 引用：`../../api/dining.js`（queuePage, queueTake, queueCall, queueCancel）

### reservation-list.html

- 表格列：customerName, phone, tableId(显示桌名), reservedTime, seatCount, status(待确认/已确认/已到店/已取消标签), remark
- 搜索：customerName, phone, status, date range
- 操作：确认(confirmReservation)、到店(arriveReservation)、取消(cancelReservation)
- API 引用：`../../api/dining.js`（reservationPage, confirmReservation, cancelReservation, arriveReservation）

### Steps

- [ ] **Step 1: 创建 `dining/area-list.html`**
- [ ] **Step 2: 创建 `dining/table-list.html`**
- [ ] **Step 3: 创建 `dining/queue-list.html`**
- [ ] **Step 4: 创建 `dining/reservation-list.html`**
- [ ] **Step 5: 提交**

```bash
git add src/main/resources/backend/page/dining/
git commit -m "feat(frontend): add dining management pages (area, table, queue, reservation)"
```
