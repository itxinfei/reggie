/**
 * 数据导出模块 - 后台API
 * 支持Excel和PDF两种格式的数据导出
 */
const exportApi = {
    /**
     * 导出订单Excel
     * @param {Object} params - { startDate, endDate, status }
     */
    exportOrdersExcel(params) {
        return axios.get('/export/orders/excel', {
            params,
            responseType: 'blob'
        });
    },

    /**
     * 导出订单PDF
     */
    exportOrdersPdf(params) {
        return axios.get('/export/orders/pdf', {
            params,
            responseType: 'blob'
        });
    },

    /**
     * 导出菜品Excel
     */
    exportDishesExcel(params) {
        return axios.get('/export/dishes/excel', {
            params,
            responseType: 'blob'
        });
    },

    /**
     * 导出菜品PDF
     */
    exportDishesPdf(params) {
        return axios.get('/export/dishes/pdf', {
            params,
            responseType: 'blob'
        });
    },

    /**
     * 导出员工Excel
     */
    exportEmployeesExcel(params) {
        return axios.get('/export/employees/excel', {
            params,
            responseType: 'blob'
        });
    },

    /**
     * 通用下载方法 - 处理Blob响应
     * @param {Promise} apiCall - axios返回的Promise
     * @param {string} fileName - 默认文件名
     */
    downloadFile(apiCall, fileName) {
        apiCall.then(res => {
            const blob = new Blob([res.data]);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;

            // 从响应头中提取文件名
            const disposition = res.headers['content-disposition'];
            if (disposition) {
                const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (match) {
                    a.download = decodeURIComponent(match[1].replace(/['"]/g, ''));
                } else {
                    a.download = fileName;
                }
            } else {
                a.download = fileName;
            }

            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        }).catch(err => {
            console.error('文件下载失败:', err);
            alert('文件导出失败，请重试');
        });
    }
};
