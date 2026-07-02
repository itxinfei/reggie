## Task 1: API 封装层

创建 7 个 API JS 文件，每个文件封装对应模块的所有后端接口调用。

### printer.js

```javascript
const printerConfigPage = (params) => $axios({ url: '/printer/config/page', method: 'get', params })
const printerConfigList = (params) => $axios({ url: '/printer/config/list', method: 'get', params })
const addPrinterConfig = (params) => $axios({ url: '/printer/config', method: 'post', data: params })
const updatePrinterConfig = (params) => $axios({ url: '/printer/config', method: 'put', data: params })
const deletePrinterConfig = (id) => $axios({ url: `/printer/config/${id}`, method: 'delete' })
const getPrinterConfig = (id) => $axios({ url: `/printer/config/${id}`, method: 'get' })
const printerTest = (id) => $axios({ url: `/printer/test/${id}`, method: 'post' })
const printerStatus = (id) => $axios({ url: `/printer/status/${id}`, method: 'get' })
const printerPrint = (orderId, type) => $axios({ url: `/printer/print/${orderId}`, method: 'post', params: { type } })
const printerLogPage = (params) => $axios({ url: '/printer/log/page', method: 'get', params })
```

### payment.js

```javascript
const paymentPage = (params) => $axios({ url: '/api/payment/page', method: 'get', params })
const paymentCreate = (params) => $axios({ url: '/api/payment/pay', method: 'post', data: params })
const paymentRefund = (params) => $axios({ url: '/api/payment/refund', method: 'post', data: params })
const paymentQuery = (tradeNo) => $axios({ url: `/api/payment/query/${tradeNo}`, method: 'get' })
```

### delivery.js

```javascript
const deliveryOrderPage = (params) => $axios({ url: '/api/delivery/orders', method: 'get', params })
const deliveryAccept = (params) => $axios({ url: '/api/delivery/accept', method: 'post', data: params })
const deliverySyncMenu = () => $axios({ url: '/api/delivery/sync/menu', method: 'post' })
const deliverySyncStock = () => $axios({ url: '/api/delivery/sync/stock', method: 'post' })
```

### dining.js

```javascript
const areaPage = (params) => $axios({ url: '/api/dining/area/page', method: 'get', params })
const areaList = () => $axios({ url: '/api/dining/area/list', method: 'get' })
const addArea = (params) => $axios({ url: '/api/dining/area', method: 'post', data: params })
const updateArea = (params) => $axios({ url: '/api/dining/area', method: 'put', data: params })
const deleteArea = (id) => $axios({ url: `/api/dining/area/${id}`, method: 'delete' })
const getArea = (id) => $axios({ url: `/api/dining/area/${id}`, method: 'get' })

const tablePage = (params) => $axios({ url: '/api/dining/table/page', method: 'get', params })
const addTable = (params) => $axios({ url: '/api/dining/table', method: 'post', data: params })
const updateTable = (params) => $axios({ url: '/api/dining/table', method: 'put', data: params })
const deleteTable = (id) => $axios({ url: `/api/dining/table/${id}`, method: 'delete' })
const getTable = (id) => $axios({ url: `/api/dining/table/${id}`, method: 'get' })
const updateTableStatus = (params) => $axios({ url: '/api/dining/table/status', method: 'put', data: params })
const tableQrcode = (id) => $axios({ url: `/api/dining/table/qrcode/${id}`, method: 'get' })

const queuePage = (params) => $axios({ url: '/api/dining/queue/page', method: 'get', params })
const queueTake = (params) => $axios({ url: '/api/dining/queue/take', method: 'post', data: params })
const queueCall = () => $axios({ url: '/api/dining/queue/call', method: 'put' })
const queueCancel = (id) => $axios({ url: `/api/dining/queue/cancel/${id}`, method: 'put' })

const reservationPage = (params) => $axios({ url: '/api/dining/reservation/page', method: 'get', params })
const addReservation = (params) => $axios({ url: '/api/dining/reservation', method: 'post', data: params })
const confirmReservation = (id) => $axios({ url: `/api/dining/reservation/confirm/${id}`, method: 'put' })
const cancelReservation = (id) => $axios({ url: `/api/dining/reservation/cancel/${id}`, method: 'put' })
const arriveReservation = (id) => $axios({ url: `/api/dining/reservation/arrive/${id}`, method: 'put' })
```

### inventory.js

```javascript
// 食材分类
const matCategoryPage = (params) => $axios({ url: '/api/inventory/material-category/page', method: 'get', params })
const matCategoryList = () => $axios({ url: '/api/inventory/material-category/list', method: 'get' })
const addMatCategory = (params) => $axios({ url: '/api/inventory/material-category', method: 'post', data: params })
const updateMatCategory = (params) => $axios({ url: '/api/inventory/material-category', method: 'put', data: params })
const deleteMatCategory = (id) => $axios({ url: `/api/inventory/material-category/${id}`, method: 'delete' })

// 供应商
const supplierPage = (params) => $axios({ url: '/api/inventory/supplier/page', method: 'get', params })
const supplierList = () => $axios({ url: '/api/inventory/supplier/list', method: 'get' })
const addSupplier = (params) => $axios({ url: '/api/inventory/supplier', method: 'post', data: params })
const updateSupplier = (params) => $axios({ url: '/api/inventory/supplier', method: 'put', data: params })
const deleteSupplier = (id) => $axios({ url: `/api/inventory/supplier/${id}`, method: 'delete' })

// 食材
const materialPage = (params) => $axios({ url: '/api/inventory/material/page', method: 'get', params })
const materialList = () => $axios({ url: '/api/inventory/material/list', method: 'get' })
const addMaterial = (params) => $axios({ url: '/api/inventory/material', method: 'post', data: params })
const updateMaterial = (params) => $axios({ url: '/api/inventory/material', method: 'put', data: params })
const deleteMaterial = (id) => $axios({ url: `/api/inventory/material/${id}`, method: 'delete' })
const materialWarning = () => $axios({ url: '/api/inventory/material/warning', method: 'get' })

// 采购单
const purchasePage = (params) => $axios({ url: '/api/inventory/purchase-order/page', method: 'get', params })
const addPurchase = (params) => $axios({ url: '/api/inventory/purchase-order', method: 'post', data: params })
const getPurchase = (id) => $axios({ url: `/api/inventory/purchase-order/${id}`, method: 'get' })
const addPurchaseDetail = (params) => $axios({ url: '/api/inventory/purchase-order/addDetail', method: 'post', data: params })
const receivePurchase = (id) => $axios({ url: `/api/inventory/purchase-order/receive/${id}`, method: 'put' })
const cancelPurchase = (id) => $axios({ url: `/api/inventory/purchase-order/cancel/${id}`, method: 'put' })
const purchaseDetailList = (orderId) => $axios({ url: `/api/inventory/purchase-order-detail/list/${orderId}`, method: 'get' })

// 库存记录
const stockRecordPage = (params) => $axios({ url: '/api/inventory/stock-record/page', method: 'get', params })
const stockIn = (params) => $axios({ url: '/api/inventory/stock-record/stockIn', method: 'post', data: params })
const stockOut = (params) => $axios({ url: '/api/inventory/stock-record/stockOut', method: 'post', data: params })

// 盘点
const stockCheckPage = (params) => $axios({ url: '/api/inventory/stock-check/page', method: 'get', params })
const addStockCheck = (params) => $axios({ url: '/api/inventory/stock-check', method: 'post', data: params })
const completeStockCheck = (id) => $axios({ url: `/api/inventory/stock-check/complete/${id}`, method: 'put' })
```

### member-center.js

```javascript
const memberPage = (params) => $axios({ url: '/api/member/member/page', method: 'get', params })
const addMember = (params) => $axios({ url: '/api/member/member', method: 'post', data: params })
const updateMember = (params) => $axios({ url: '/api/member/member', method: 'put', data: params })
const getMember = (id) => $axios({ url: `/api/member/member/${id}`, method: 'get' })
const memberRecharge = (params) => $axios({ url: '/api/member/member/recharge', method: 'post', data: params })
const memberDeductBalance = (params) => $axios({ url: '/api/member/member/deduct-balance', method: 'post', data: params })

const levelPage = (params) => $axios({ url: '/api/member/level/page', method: 'get', params })
const addLevel = (params) => $axios({ url: '/api/member/level', method: 'post', data: params })
const updateLevel = (params) => $axios({ url: '/api/member/level', method: 'put', data: params })
const deleteLevel = (id) => $axios({ url: `/api/member/level/${id}`, method: 'delete' })
const getLevel = (id) => $axios({ url: `/api/member/level/${id}`, method: 'get' })

const rechargePage = (params) => $axios({ url: '/api/member/recharge/page', method: 'get', params })
const pointsPage = (params) => $axios({ url: '/api/member/points/page', method: 'get', params })

const couponTemplatePage = (params) => $axios({ url: '/api/member/coupon-template/page', method: 'get', params })
const addCouponTemplate = (params) => $axios({ url: '/api/member/coupon-template', method: 'post', data: params })
const updateCouponTemplate = (params) => $axios({ url: '/api/member/coupon-template', method: 'put', data: params })
const deleteCouponTemplate = (id) => $axios({ url: `/api/member/coupon-template/${id}`, method: 'delete' })
const getCouponTemplate = (id) => $axios({ url: `/api/member/coupon-template/${id}`, method: 'get' })

const couponUserPage = (params) => $axios({ url: '/api/member/coupon-user/page', method: 'get', params })
const couponMy = (memberId) => $axios({ url: `/api/member/coupon-user/my/${memberId}`, method: 'get' })
```

### report.js

```javascript
const reportDaily = (params) => $axios({ url: '/api/report/daily', method: 'get', params })
const reportDishRanking = (params) => $axios({ url: '/api/report/dish-ranking', method: 'get', params })
const reportTimeSlot = (params) => $axios({ url: '/api/report/time-slot', method: 'get', params })
const reportPayment = (params) => $axios({ url: '/api/report/payment', method: 'get', params })
const reportExport = (params) => $axios({ url: '/api/report/export', method: 'get', params, responseType: 'blob' })
```

### Steps

- [ ] **Step 1: 创建 `backend/api/printer.js`**
- [ ] **Step 2: 创建 `backend/api/payment.js`**
- [ ] **Step 3: 创建 `backend/api/delivery.js`**
- [ ] **Step 4: 创建 `backend/api/dining.js`**
- [ ] **Step 5: 创建 `backend/api/inventory.js`**
- [ ] **Step 6: 创建 `backend/api/member-center.js`**
- [ ] **Step 7: 创建 `backend/api/report.js`**
- [ ] **Step 8: 提交**

```bash
git add src/main/resources/backend/api/printer.js src/main/resources/backend/api/payment.js src/main/resources/backend/api/delivery.js src/main/resources/backend/api/dining.js src/main/resources/backend/api/inventory.js src/main/resources/backend/api/member-center.js src/main/resources/backend/api/report.js
git commit -m "feat(frontend): add API layer for 7 business modules"
```
