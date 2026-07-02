## Task 6: 进销存页面

### category-list.html

- 表格列：name, sort
- 弹框：name, sort
- API 引用：`../../api/inventory.js`（matCategoryPage, addMatCategory, updateMatCategory, deleteMatCategory）

### supplier-list.html

- 表格列：name, contact, phone, address, status
- 搜索：name, contact
- 弹框：name, contact, phone, address, status
- API 引用：`../../api/inventory.js`（supplierPage, addSupplier, updateSupplier, deleteSupplier）

### material-list.html

- 表格列：name, categoryId(分类名), unit, stockQty(红色高亮 if stockQty <= minStock), minStock, unitPrice(￥), supplierId(供应商名), barcode, status
- 搜索：name, categoryId(下拉加载 matCategoryList)
- 弹框表单：name, categoryId(下拉), unit, stockQty, minStock, unitPrice, supplierId(下拉加载 supplierList), barcode, status
- 顶部按钮：库存预警(弹框调用 materialWarning 显示低库存清单)
- API 引用：`../../api/inventory.js`（materialPage, addMaterial, updateMaterial, deleteMaterial, materialWarning, matCategoryList, supplierList）

### purchase-list.html

- 表格列：id(采购单号), supplierId(供应商名), totalAmount, status(待收货/已收货/已取消标签), createdTime
- 搜索：supplierId(下拉), status(下拉)
- 展开行：el-table 的 expand 行，加载 purchaseDetailList(orderId) 显示明细表格( materialId(食材名), quantity, unitPrice, subtotal )
- 操作：收货(receivePurchase)、取消(cancelPurchase)
- 弹框：新增采购单(supplierId + 明细行)
- API 引用：`../../api/inventory.js`（purchasePage, getPurchase, addPurchase, addPurchaseDetail, receivePurchase, cancelPurchase, purchaseDetailList, supplierList）

### stock-check.html

- 表格列：id(盘点单号), itemCount, profitLoss(盈亏金额，负值红色), status(进行中/已完成标签), createdTime
- 搜索：status, date range
- 展开行：el-table 的 expand 行显示盘点明细( materialId, bookQty, actualQty, diff )
- 操作：完成盘点(completeStockCheck)
- 弹框：新增盘点(materialId + actualQty)
- API 引用：`../../api/inventory.js`（stockCheckPage, addStockCheck, completeStockCheck, materialList）

### stock-record.html

- 表格列：materialId(食材名), type(入库蓝/出库红/盘点灰标签), quantity, unitPrice, totalAmount, remark, createdTime
- 搜索：materialId(下拉), type(下拉), date range
- 只读操作：无新增/编辑/删除
- API 引用：`../../api/inventory.js`（stockRecordPage, materialList）

### Steps

- [ ] **Step 1: 创建 `inventory/category-list.html`**
- [ ] **Step 2: 创建 `inventory/supplier-list.html`**
- [ ] **Step 3: 创建 `inventory/material-list.html`**
- [ ] **Step 4: 创建 `inventory/purchase-list.html`**
- [ ] **Step 5: 创建 `inventory/stock-check.html`**
- [ ] **Step 6: 创建 `inventory/stock-record.html`**
- [ ] **Step 7: 提交**

```bash
git add src/main/resources/backend/page/inventory/
git commit -m "feat(frontend): add inventory management pages"
```
