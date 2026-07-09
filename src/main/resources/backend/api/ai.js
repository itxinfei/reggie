/**
 * AI智能助手 - 管理端API
 * 修改点：新增AI相关管理接口
 */
var aiBackendApi = {
    /**
     * 通用AI对话
     */
    chat: function(message, scene) {
        return $axios.post('/api/ai/chat', {
            message: message,
            scene: scene || 'business_analysis'
        });
    },

    /**
     * 生成菜品描述
     */
    generateDishDesc: function(dishName, categoryName, ingredients) {
        return $axios.post('/api/ai/dish-description', {
            dishName: dishName,
            categoryName: categoryName || '',
            ingredients: ingredients || ''
        });
    },

    /**
     * 经营分析
     */
    analyzeBusiness: function(question, dataJson) {
        return $axios.post('/api/ai/business-analysis', {
            question: question,
            data: dataJson || '{}'
        });
    },

    /**
     * AI健康检查
     */
    health: function() {
        return $axios.get('/api/ai/health');
    }
};
