/**
 * AI智能助手 - 用户端API
 * 包含：智能点餐推荐、通用AI对话、对话管理、反馈记录
 */
var aiApi = {
    // ==================== 核心对话 ====================

    /**
     * 智能点餐推荐
     * @param {string} message - 用户自然语言输入
     * @param {number|null} userId - 用户ID（可选，用于个性化推荐）
     * @param {string} conversationId - 对话ID（用于上下文）
     */
    orderAssistant: function(message, userId, conversationId) {
        return $axios.post('/api/ai/order-assistant', {
            message: message,
            userId: userId || null,
            conversationId: conversationId || null
        });
    },

    /**
     * 通用AI对话
     * @param {string} message - 用户消息
     * @param {string} scene - 场景：order_assistant/dish_desc/business_analysis/marketing
     * @param {string} conversationId - 对话ID
     */
    chat: function(message, scene, conversationId) {
        return $axios.post('/api/ai/chat', {
            message: message,
            scene: scene || 'order_assistant',
            conversationId: conversationId || null
        });
    },

    /**
     * AI健康检查
     */
    health: function() {
        return $axios.get('/api/ai/health');
    },

    // ==================== 对话管理 ====================

    /**
     * 获取对话历史列表
     * @param {number} page - 页码
     * @param {number} pageSize - 每页数量
     */
    getConversations: function(page, pageSize) {
        return $axios.get('/api/ai/conversations', {
            params: { page: page || 1, pageSize: pageSize || 20 }
        });
    },

    /**
     * 获取单个对话详情（含消息历史）
     * @param {string} conversationId - 对话ID
     */
    getConversation: function(conversationId) {
        return $axios.get('/api/ai/conversations/' + conversationId);
    },

    /**
     * 创建新对话
     * @param {string} title - 对话标题
     */
    createConversation: function(title) {
        return $axios.post('/api/ai/conversations', { title: title || '新对话' });
    },

    /**
     * 删除对话
     * @param {string} conversationId - 对话ID
     */
    deleteConversation: function(conversationId) {
        return $axios.delete('/api/ai/conversations/' + conversationId);
    },

    // ==================== 反馈记录 ====================

    /**
     * 记录推荐反馈
     * @param {Object} data - {messageId, dishId, feedbackType, scene}
     */
    recordFeedback: function(data) {
        return $axios.post('/api/ai/feedback', data);
    },

    // ==================== 用户画像 ====================

    /**
     * 获取用户画像摘要
     * @returns {Promise} {tags: string[], summary: string}
     */
    getProfileSummary: function() {
        return $axios.get('/api/ai/profile/summary');
    }
};
