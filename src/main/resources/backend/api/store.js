/**
 * 多门店管理 - 后台API
 * 修改点：
 *   1. 新增分页搜索、详情获取、编辑门店、批量状态、导出方法
 *   2. 将原生axios替换为$axios，统一走request.js响应拦截器
 *   3. 确保NOTLOGIN自动跳转登录页、网络异常统一弹窗提示
 *
 * @author reggie
 * @since 2026-07-09
 */
var storeApi = {
    // ==================== 门店管理 ====================

    /**
     * 获取所有门店列表（兼容旧接口）
     */
    listStores: function() {
        return $axios.get('/store/list');
    },

    /**
     * 修改点：门店统计聚合（SQL 聚合，替代前端 /store/list 拉全量 filter 统计）
     */
    storeStats: function() {
        return $axios.get('/store/stats');
    },

    /**
     * 分页搜索门店列表（支持多条件筛选与排序）
     * @param {Object} params - { keyword, storeType, status, page, pageSize, sortBy, sortOrder }
     */
    pageStores: function(params) {
        return $axios.post('/store/page', params);
    },

    /**
     * 获取门店详情（编辑回显用）
     * @param {Number} tenantId 门店ID
     */
    getStoreDetail: function(tenantId) {
        return $axios.get('/store/detail/' + tenantId);
    },

    /**
     * 获取分店列表
     * @param {Number} parentTenantId 上级总店ID
     */
    listBranches: function(parentTenantId) {
        return $axios.get('/store/branches', { params: { parentTenantId: parentTenantId } });
    },

    /**
     * 创建门店
     * @param {Object} data 门店信息
     */
    createStore: function(data) {
        return $axios.post('/store/create', data);
    },

    /**
     * 编辑门店
     * @param {Number} tenantId 门店ID
     * @param {Object} data 更新数据
     */
    updateStore: function(tenantId, data) {
        return $axios.put('/store/update/' + tenantId, data);
    },

    /**
     * 切换门店
     * @param {Number} tenantId 目标门店ID
     */
    switchStore: function(tenantId) {
        return $axios.post('/store/switch/' + tenantId);
    },

    /**
     * 更新门店状态
     * @param {Number} tenantId 门店ID
     * @param {Number} status 状态码 (1启用, 0停用)
     */
    updateStatus: function(tenantId, status) {
        return $axios.put('/store/' + tenantId + '/status', null, {
            params: { status: status }
        });
    },

    /**
     * 批量更新门店状态（上下架）
     * @param {Array<Number>} tenantIds 门店ID列表
     * @param {Number} status 目标状态
     */
    batchUpdateStatus: function(tenantIds, status) {
        return $axios.put('/store/batch/status', {
            tenantIds: tenantIds,
            status: status
        });
    },

    /**
     * 导出门店数据为CSV
     * @param {Object} params - { keyword, storeType, status }
     */
    exportStores: function(params) {
        return $axios.post('/store/export', params, {
            responseType: 'blob',
            timeout: 60000
        });
    },

    /**
     * 获取门店今日概况
     * @param {Number} tenantId 门店ID
     */
    getTodaySummary: function(tenantId) {
        return $axios.get('/store/summary/today', { params: { tenantId: tenantId } });
    },

    // ==================== 数据同步 ====================

    /**
     * 同步菜品到目标门店
     * @param {Object} data - { sourceTenantId, targetTenantId, dishIds, operatorId }
     */
    syncDishes: function(data) {
        return $axios.post('/store/sync/dishes', data);
    },

    /**
     * 同步分类到目标门店
     * @param {Object} data - { sourceTenantId, targetTenantId, operatorId }
     */
    syncCategories: function(data) {
        return $axios.post('/store/sync/categories', data);
    },

    /**
     * 同步套餐到目标门店
     * @param {Object} data - { sourceTenantId, targetTenantId, setmealIds, operatorId }
     */
    syncSetmeals: function(data) {
        return $axios.post('/store/sync/setmeals', data);
    },

    /**
     * 查询同步日志
     * @param {Number} sourceTenantId 源门店ID
     * @param {Number} page 页码
     * @param {Number} pageSize 每页条数
     */
    getSyncLogs: function(sourceTenantId, page, pageSize) {
        return $axios.get('/store/sync/logs', {
            params: { sourceTenantId: sourceTenantId, page: page || 1, pageSize: pageSize || 10 }
        });
    },

    // ==================== 总部控制台 ====================

    /**
     * 获取总部控制台聚合数据
     */
    getDashboardOverview: function() {
        return $axios.get('/store/dashboard/overview');
    },

    /**
     * 获取门店排行
     */
    getStoreRanking: function() {
        return $axios.get('/store/dashboard/ranking');
    }
};
