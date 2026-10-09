// 餐补管理 API（企业内部订餐）
const subsidyApi = {
    accountPage(params) {
        return $axios.get('/subsidy/account/page', { params })
    },
    accountStats() {
        return $axios.get('/subsidy/account/stats')
    },
    grant(data) {
        return $axios.post('/subsidy/account/grant', data)
    },
    recordPage(params) {
        return $axios.get('/subsidy/account/record/page', { params })
    },
    reconciliation(params) {
        return $axios.get('/subsidy/account/reconciliation', { params })
    }
}
