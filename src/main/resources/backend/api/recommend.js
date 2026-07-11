/**
 * 智能推荐 & 营销管理 - 后台API
 * 修改点：将原生axios替换为$axios，统一走request.js响应拦截器
 *       确保NOTLOGIN自动跳转登录页、网络异常统一弹窗提示
 */
var recommendBackendApi = {
    // ==================== 营销活动管理 ====================

    /**
     * 分页查询营销活动
     */
    getCampaignsPage: function(params) {
        return $axios.get('/marketing/campaigns/page', { params: params });
    },

    // 修改点：筛选下拉选项（动态加载活动名称）
    campaignOptions: function() {
        return $axios.get('/marketing/campaigns/options');
    },

    /**
     * 创建营销活动
     */
    createCampaign: function(data) {
        return $axios.post('/marketing/campaigns', data);
    },

    /**
     * 更新营销活动
     */
    updateCampaign: function(data) {
        return $axios.put('/marketing/campaigns', data);
    },

    /**
     * 删除营销活动
     */
    deleteCampaign: function(id) {
        return $axios.delete('/marketing/campaigns/' + id);
    },

    /**
     * 修改点(2026-07-10)：批量删除营销活动
     */
    batchDeleteCampaigns: function(data) {
        return $axios.post('/marketing/campaigns/batch-delete', data);
    },

    /**
     * 查询营销活动详情
     */
    getCampaign: function(id) {
        return $axios.get('/marketing/campaigns/' + id);
    },

    /**
     * 发布活动
     */
    publishCampaign: function(id) {
        return $axios.put('/marketing/campaigns/' + id + '/publish');
    },

    /**
     * 暂停活动
     */
    pauseCampaign: function(id) {
        return $axios.put('/marketing/campaigns/' + id + '/pause');
    },

    /**
     * 推送营销消息
     */
    pushMessage: function(campaignId, userId, pushType) {
        return $axios.post('/marketing/push/' + campaignId + '/' + userId, null, {
            params: { pushType: pushType || 1 }
        });
    },

    /**
     * 自动发放优惠券
     */
    autoDispatchCoupons: function(userId) {
        return $axios.post('/marketing/auto-dispatch-coupons', null, {
            params: { userId: userId }
        });
    },

    // ==================== 推荐数据 ====================

    /**
     * 获取热门推荐数据（后台查看用）
     */
    getHotRecommend: function(limit) {
        return $axios.get('/recommend/hot', { params: { limit: limit || 10 } });
    },

    /**
     * 修改点：获取推荐引擎真实统计数据
     */
    getStats: function() {
        return $axios.get('/recommend/stats');
    },

    /**
     * 修改点：批量推送营销消息
     */
    batchPush: function(campaignId, data) {
        return $axios.post('/marketing/batch-push/' + campaignId, data);
    }
};
