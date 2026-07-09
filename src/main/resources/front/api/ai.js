/**
 * AI智能助手 - 用户端API
 * 修改点：新增AI点餐助手相关接口
 */
var aiApi = {
    /**
     * 智能点餐推荐
     * @param {string} message - 用户自然语言输入
     * @param {number} userId - 用户ID（可选）
     */
    orderAssistant: function(message, userId) {
        return $axios.post('/api/ai/order-assistant', {
            message: message,
            userId: userId || null
        });
    },

    /**
     * 通用AI对话
     * @param {string} message - 用户消息
     * @param {string} scene - 场景：order_assistant/dish_desc/business_analysis/marketing
     */
    chat: function(message, scene) {
        return $axios.post('/api/ai/chat', {
            message: message,
            scene: scene || 'order_assistant'
        });
    },

    /**
     * AI健康检查
     */
    health: function() {
        return $axios.get('/api/ai/health');
    }
};
