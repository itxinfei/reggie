/**
 * 智能推荐模块 - 前端API
 * 菜品推荐、浏览记录、营销活动相关接口
 */
var recommendApi = {
    // ==================== 菜品推荐 ====================

    /**
     * 获取个性化菜品推荐
     * @param {number} limit - 推荐数量
     */
    getDishRecommend: function(limit) {
        return $axios.get('/recommend/dishes', { params: { limit: limit || 10 } });
    },

    /**
     * 获取热门排行
     * @param {number} limit - 数量
     */
    getHotRank: function(limit) {
        return $axios.get('/recommend/hot', { params: { limit: limit || 10 } });
    },

    /**
     * 获取新品尝鲜推荐
     * @param {number} limit - 数量
     */
    getNewArrivals: function(limit) {
        return $axios.get('/recommend/new-arrivals', { params: { limit: limit || 6 } });
    },

    /**
     * 获取套餐推荐
     * @param {number} limit - 数量
     */
    getSetmealRecommend: function(limit) {
        return $axios.get('/recommend/setmeals', { params: { limit: limit || 6 } });
    },

    // ==================== 浏览记录 ====================

    /**
     * 记录浏览行为
     * @param {Object} data - {targetType, targetId, targetName, duration, actionType}
     */
    recordBrowse: function(data) {
        return $axios.post('/recommend/browse', data);
    },

    /**
     * 获取浏览历史
     * @param {number} limit - 条数
     */
    getBrowseHistory: function(limit) {
        return $axios.get('/recommend/browse-history', { params: { limit: limit || 20 } });
    },

    // ==================== 推荐反馈 ====================

    /**
     * 记录推荐反馈
     * @param {Object} data - {dishId, feedbackType, recommendCacheId}
     */
    recordFeedback: function(data) {
        return $axios.post('/recommend/feedback', data);
    },

    /**
     * 刷新推荐缓存
     */
    refreshCache: function() {
        return $axios.post('/recommend/refresh-cache');
    },

    // ==================== 营销活动 ====================

    /**
     * 获取匹配用户的活动
     */
    getMatchedCampaigns: function() {
        return $axios.get('/recommend/campaigns');
    },

    /**
     * 获取未读营销消息
     */
    getUnreadMessages: function() {
        return $axios.get('/recommend/messages/unread');
    },

    /**
     * 标记消息已读
     * @param {number} id - 消息ID
     */
    markMessageRead: function(id) {
        return $axios.put('/recommend/messages/' + id + '/read');
    },

    // 修改点：新增消息列表和未读数查询API

    /**
     * 获取用户所有消息列表（分页）
     * @param {number} page - 页码
     * @param {number} pageSize - 每页数量
     */
    getUserMessages: function(page, pageSize) {
        return $axios.get('/recommend/messages', {
            params: { page: page || 1, pageSize: pageSize || 20 }
        });
    },

    /**
     * 获取未读消息数量（用于角标）
     */
    getUnreadCount: function() {
        return $axios.get('/recommend/messages/unread-count');
    }
};
