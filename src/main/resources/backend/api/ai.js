/**
 * AI智能助手 - 管理端API
 * 修改点：使用IIFE模块模式，避免全局变量污染，挂载到 window 以兼容 iframe 架构
 * 修改点：chat() 支持 conversationId，实现多轮对话持久化
 * @author reggie
 * @since 2026-07-10
 */
(function() {
    'use strict';

    var aiBackendApi = {
        // ==================== 核心对话 ====================

        /**
         * 通用AI对话（非流式）
         * @param {string} message - 用户消息
         * @param {string} scene - 场景：business_analysis / dish_desc / marketing
         * @param {string} conversationId - 会话ID（可选，不传则后端自动创建）
         */
        chat: function(message, scene, conversationId) {
            var params = {
                message: message,
                scene: scene || 'business_analysis'
            };
            if (conversationId) {
                params.conversationId = conversationId;
            }
            return $axios.post('/api/ai/chat', params);
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
        },

        // ==================== 对话管理 ====================

        /**
         * 获取用户对话列表
         * @param {number} page - 页码，默认1
         * @param {number} pageSize - 每页条数，默认20
         */
        getConversations: function(page, pageSize) {
            return $axios.get('/api/ai/conversations', {
                params: { page: page || 1, pageSize: pageSize || 20 }
            });
        },

        /**
         * 获取对话详情（含消息历史）
         * @param {string} conversationId - 会话ID
         */
        getConversationDetail: function(conversationId) {
            return $axios.get('/api/ai/conversations/' + conversationId);
        },

        /**
         * 创建新对话
         * @param {string} title - 对话标题（可选）
         * @param {string} scene - 对话场景
         */
        createConversation: function(title, scene) {
            return $axios.post('/api/ai/conversations', {
                title: title || null,
                scene: scene || 'business_analysis'
            });
        },

        /**
         * 删除对话
         * @param {string} conversationId - 会话ID
         */
        deleteConversation: function(conversationId) {
            return $axios.delete('/api/ai/conversations/' + conversationId);
        },

        // ==================== 反馈 ====================

        /**
         * 记录用户反馈（有用/没用）
         * @param {object} params - { messageId, feedbackType }
         */
        recordFeedback: function(params) {
            return $axios.post('/api/ai/feedback', {
                messageId: params.messageId,
                feedbackType: params.feedbackType
            });
        },

        // ==================== 用户画像 ====================

        /**
         * 获取用户画像摘要
         */
        getProfile: function() {
            return $axios.get('/api/ai/profile/summary');
        }
    };

    // 挂载到全局（兼容 iframe 架构）
    window.aiBackendApi = aiBackendApi;
})();
