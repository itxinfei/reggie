/**
 * 消息通知模块 - 后台API
 * 提供通知模板管理、消息发送、发送记录查询
 */
const notificationApi = {
    // ==================== 模板管理 ====================

    /** 分页查询通知模板 */
    getTemplatePage(params) {
        return axios.get('/notification/template/page', { params });
    },

    /** 获取模板详情 */
    getTemplate(id) {
        return axios.get('/notification/template/' + id);
    },

    /** 新增通知模板 */
    addTemplate(data) {
        return axios.post('/notification/template', data);
    },

    /** 修改通知模板 */
    updateTemplate(data) {
        return axios.put('/notification/template', data);
    },

    /** 启用/停用模板 */
    toggleTemplate(id, status) {
        return axios.put('/notification/template/' + id + '/status/' + status);
    },

    /** 删除通知模板 */
    deleteTemplate(id) {
        return axios.delete('/notification/template/' + id);
    },

    // ==================== 消息发送 ====================

    /** 发送通知 */
    sendNotification(data) {
        return axios.post('/notification/send', data);
    },

    /** 批量发送通知 */
    batchSend(data) {
        return axios.post('/notification/batch-send', data);
    },

    // ==================== 发送记录 ====================

    /** 分页查询发送记录 */
    getRecordPage(params) {
        return axios.get('/notification/record/page', { params });
    },

    /** 获取发送详情 */
    getRecordDetail(id) {
        return axios.get('/notification/record/' + id);
    },

    /** 获取业务类型枚举 */
    getBizTypes() {
        return axios.get('/notification/biz-types');
    }
};
