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
