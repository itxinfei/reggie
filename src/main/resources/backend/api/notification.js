/**
 * 消息通知模块 - 后台API
 * 提供通知模板管理、消息发送、发送记录查询
 * 修改点：axios改为$axios，确保请求走统一拦截器
 */
const notificationApi = {
    // ==================== 模板管理 ====================

    /** 分页查询通知模板 */
    getTemplatePage(params) {
        return $axios({ url: '/notification/template/page', method: 'get', params });
    },

    /** 获取模板详情 */
    getTemplate(id) {
        return $axios({ url: '/notification/template/' + id, method: 'get' });
    },

    /** 新增通知模板 */
    addTemplate(data) {
        return $axios({ url: '/notification/template', method: 'post', data });
    },

    /** 修改通知模板 */
    updateTemplate(data) {
        return $axios({ url: '/notification/template', method: 'put', data });
    },

    /** 启用/停用模板 */
    toggleTemplate(id, status) {
        return $axios({ url: '/notification/template/' + id + '/status/' + status, method: 'put' });
    },

    /** 删除通知模板 */
    deleteTemplate(id) {
        return $axios({ url: '/notification/template/' + id, method: 'delete' });
    },

    // ==================== 消息发送 ====================

    /** 发送通知 */
    sendNotification(data) {
        return $axios({ url: '/notification/send', method: 'post', data });
    },

    /** 修改点：向全部用户发送通知 */
    sendToAllUsers(data) {
        return $axios({ url: '/notification/send-all', method: 'post', data });
    },

    /** 批量发送通知 */
    batchSend(data) {
        return $axios({ url: '/notification/batch-send', method: 'post', data });
    },

    // ==================== 发送记录 ====================

    /** 分页查询发送记录 */
    getRecordPage(params) {
        return $axios({ url: '/notification/record/page', method: 'get', params });
    },

    /** 获取发送详情 */
    getRecordDetail(id) {
        return $axios({ url: '/notification/record/' + id, method: 'get' });
    },

    /** 修改点：今日发送记录聚合统计（SQL 聚合，替代前端 pageSize:999 拉全量） */
    getRecordStats() {
        return $axios({ url: '/notification/record/stats', method: 'get' });
    },

    /** 获取业务类型枚举 */
    getBizTypes() {
        return $axios({ url: '/notification/biz-types', method: 'get' });
    },

    // ==================== 修改点：简易消息发送 ====================

    /**
     * 简易消息发送（不需要模板，直接发送内容）
     * @param {Object} data - { channel: 1|2, targets: ['13800138000'], content: '...', title: '...' }
     */
    sendSimpleMessage(data) {
        return $axios({ url: '/notification/send-simple', method: 'post', data });
    },

    /**
     * 获取通知服务健康状态
     */
    getHealth() {
        return $axios({ url: '/notification/health', method: 'get' });
    }
};
