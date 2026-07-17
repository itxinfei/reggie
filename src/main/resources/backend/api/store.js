/**
 * 多门店管理 - 后台API
 * 修改点：
 *   1. 统一为 const 箭头函数风格，与其他 API 文件一致
 *   2. 确保NOTLOGIN自动跳转登录页、网络异常统一弹窗提示
 *
 * @author reggie
 * @since 2026-07-09
 */

// ==================== 门店管理 ====================

const listStores = function() {
    return $axios.get('/store/list');
}

const storeStats = function() {
    return $axios.get('/store/stats');
}

const pageStores = function(params) {
    return $axios.post('/store/page', params);
}

const getStoreDetail = function(tenantId) {
    return $axios.get('/store/detail/' + tenantId);
}

const listBranches = function(parentTenantId) {
    return $axios.get('/store/branches', { params: { parentTenantId: parentTenantId } });
}

const createStore = function(data) {
    return $axios.post('/store/create', data);
}

const updateStore = function(tenantId, data) {
    return $axios.put('/store/update/' + tenantId, data);
}

const switchStore = function(tenantId) {
    return $axios.post('/store/switch/' + tenantId);
}

const updateStatus = function(tenantId, status) {
    return $axios.put('/store/' + tenantId + '/status', null, {
        params: { status: status }
    });
}

const batchUpdateStatus = function(tenantIds, status) {
    return $axios.put('/store/batch/status', {
        tenantIds: tenantIds,
        status: status
    });
}

const exportStores = function(params) {
    return $axios.post('/store/export', params, {
        responseType: 'blob',
        timeout: 60000
    });
}

const getTodaySummary = function(tenantId) {
    return $axios.get('/store/summary/today', { params: { tenantId: tenantId } });
}

// ==================== 数据同步 ====================

const syncDishes = function(data) {
    return $axios.post('/store/sync/dishes', data);
}

const syncCategories = function(data) {
    return $axios.post('/store/sync/categories', data);
}

const syncSetmeals = function(data) {
    return $axios.post('/store/sync/setmeals', data);
}

const getSyncLogs = function(sourceTenantId, page, pageSize) {
    return $axios.get('/store/sync/logs', {
        params: { sourceTenantId: sourceTenantId, page: page || 1, pageSize: pageSize || 10 }
    });
}

// ==================== 总部控制台 ====================

const getDashboardOverview = function() {
    return $axios.get('/store/dashboard/overview');
}

const getStoreRanking = function() {
    return $axios.get('/store/dashboard/ranking');
}
