# Task 5: 堂食管理页面 — 完成报告

## 完成情况

已创建 4 个页面文件在 `src/main/resources/backend/page/dining/`：

| 文件 | 状态 | 说明 |
|------|------|------|
| `area-list.html` | ✅ | 区域管理：搜索、CRUD、分页 |
| `table-list.html` | ✅ | 桌台管理：按区域搜索、状态标签（空闲/占用/预留）、状态切换、生成二维码、区域下拉加载 |
| `queue-list.html` | ✅ | 排队取号：按手机号/状态搜索、取号弹框、叫号、取消 |
| `reservation-list.html` | ✅ | 预订管理：按姓名/手机/状态/时间范围搜索、确认、到店、取消 |

## API 引用

所有页面均引用 `../../api/dining.js`，使用已定义的函数：
- area: `areaPage`, `addArea`, `updateArea`, `deleteArea`
- table: `tablePage`, `addTable`, `updateTable`, `deleteTable`, `updateTableStatus`, `tableQrcode`, `areaList`
- queue: `queuePage`, `queueTake`, `queueCall`, `queueCancel`
- reservation: `reservationPage`, `confirmReservation`, `cancelReservation`, `arriveReservation`

## 测试

`mvn test -q` — 通过（exit code 0）
