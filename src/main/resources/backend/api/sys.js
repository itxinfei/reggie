// 系统管理模块 API
const sysApi = {
    // ==================== 角色管理 ====================
    rolePage(params) {
        return $axios.get('/sys/role/page', { params })
    },
    roleList() {
        return $axios.get('/sys/role/list')
    },
    roleAdd(data) {
        return $axios.post('/sys/role', data)
    },
    roleUpdate(data) {
        return $axios.put('/sys/role', data)
    },
    roleDelete(id) {
        return $axios.delete('/sys/role/' + id)
    },
    rolePermissions(id) {
        return $axios.get('/sys/role/' + id + '/permissions')
    },
    assignPermissions(id, permissionIds) {
        return $axios.put('/sys/role/' + id + '/permissions', { permissionIds })
    },
    permissionTree() {
        return $axios.get('/sys/role/permissions/tree')
    },

    // ==================== 系统配置 ====================
    configPage(params) {
        return $axios.get('/sys/config/page', { params })
    },
    configList() {
        return $axios.get('/sys/config/list')
    },
    configGet(key) {
        return $axios.get('/sys/config/' + key)
    },
    configAdd(data) {
        return $axios.post('/sys/config', data)
    },
    configUpdate(data) {
        return $axios.put('/sys/config', data)
    },
    configBatchUpdate(data) {
        return $axios.put('/sys/config/batch', data)
    },
    configDelete(id) {
        return $axios.delete('/sys/config/' + id)
    },

    // ==================== 通知模板 ====================
    templatePage(params) {
        return $axios.get('/sys/template/page', { params })
    },
    templateList(bizType) {
        return $axios.get('/sys/template/list', { params: { bizType } })
    },
    templateDetail(id) {
        return $axios.get('/sys/template/' + id)
    },
    templateAdd(data) {
        return $axios.post('/sys/template', data)
    },
    templateUpdate(data) {
        return $axios.put('/sys/template', data)
    },
    templateToggle(id, status) {
        return $axios.put('/sys/template/' + id + '/toggle', null, { params: { status } })
    },
    templateDelete(id) {
        return $axios.delete('/sys/template/' + id)
    },

    // ==================== 操作日志 ====================
    logPage(params) {
        return $axios.get('/sys/log/page', { params })
    },
    logStats() {
        return $axios.get('/sys/log/stats')
    },
    logByBiz(tableName, bizId) {
        return $axios.get('/sys/log/biz', { params: { tableName, bizId } })
    }
}
