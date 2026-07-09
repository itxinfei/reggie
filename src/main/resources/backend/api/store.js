/**
 * 多门店管理 - 后台API
 */
var storeApi = {
    // ==================== 门店管理 ====================

    /**
     * 获取所有门店列表
     */
    listStores: function() {
        return axios.get('/store/list');
    },

    /**
     * 获取分店列表
     */
    listBranches: function(parentTenantId) {
        return axios.get('/store/branches', { params: { parentTenantId: parentTenantId } });
    },

    /**
     * 创建门店
     */
    createStore: function(data) {
        return axios.post('/store/create', data);
    },

    /**
     * 切换门店
     */
    switchStore: function(tenantId) {
        return axios.post('/store/switch/' + tenantId);
    },

    /**
     * 更新门店状态
     */
    updateStatus: function(tenantId, status) {
        return axios.put('/store/' + tenantId + '/status', null, {
            params: { status: status }
        });
    },

    /**
     * 获取门店今日概况
     */
    getTodaySummary: function(tenantId) {
        return axios.get('/store/summary/today', { params: { tenantId: tenantId } });
    },

    // ==================== 数据同步 ====================

    /**
     * 同步菜品到目标门店
     */
    syncDishes: function(data) {
        return axios.post('/store/sync/dishes', data);
    },

    /**
     * 同步分类到目标门店
     */
    syncCategories: function(data) {
        return axios.post('/store/sync/categories', data);
    },

    /**
     * 同步套餐到目标门店
     */
    syncSetmeals: function(data) {
        return axios.post('/store/sync/setmeals', data);
    },

    /**
     * 查询同步日志
     */
    getSyncLogs: function(sourceTenantId, page, pageSize) {
        return axios.get('/store/sync/logs', {
            params: { sourceTenantId: sourceTenantId, page: page || 1, pageSize: pageSize || 10 }
        });
    },

    // ==================== 总部控制台 ====================

    /**
     * 获取总部控制台聚合数据
     */
    getDashboardOverview: function() {
        return axios.get('/store/dashboard/overview');
    },

    /**
     * 获取门店排行
     */
    getStoreRanking: function() {
        return axios.get('/store/dashboard/ranking');
    }
};
