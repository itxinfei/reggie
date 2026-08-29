/**
 * 数据导出模块 - 后台API
 * 支持Excel(.xlsx)和PDF两种格式
 *
 * 使用独立axios实例，绕过$axios的JSON拦截器
 * 原因：导出返回Blob二进制流，$axios拦截器会尝试解析为JSON导致报错
 *
 * 修改点：添加response拦截器处理NOTLOGIN和Token过期场景
 *       session过期时自动跳转登录页
 */
var exportApi = (function() {
    var service = axios.create({
        baseURL: '/',
        timeout: 120000
    });

    service.interceptors.request.use(function(config) {
        return config;
    }, function(error) {
        return Promise.reject(error);
    });

    // 修改点：添加响应拦截器，处理session过期跳转
    service.interceptors.response.use(function(res) {
        // 如果响应Content-Type是JSON（说明后端返回了错误信息而非文件流）
        if (res.headers['content-type'] && res.headers['content-type'].indexOf('application/json') !== -1
                && res.data instanceof Blob) {
            return new Promise(function(_, reject) {
                var reader = new FileReader();
                reader.onload = function() {
                    try {
                        var errData = JSON.parse(reader.result);
                        if (errData.code === 0 && errData.msg === 'NOTLOGIN') {
                            localStorage.removeItem('userInfo');
                            // 修正：iframe 内 window.top 会被 sandbox 拦截，改用 postMessage 通知顶层跳登录
                            if (window.self !== window.top) { try { window.parent.postMessage({ type: 'REGGIE_NOTLOGIN' }, '*'); } catch(e) {} }
                            else { window.location.href = '/backend/page/login/login.html'; }
                        }
                        reject(new Error(errData.msg || '导出失败'));
                    } catch(e) {
                        reject(new Error('导出失败，服务器返回异常'));
                    }
                };
                reader.readAsText(res.data);
            });
        }
        return res;
    }, function(error) {
        var message = error.message || '';
        if (message === 'Network Error') {
            message = '后端接口连接异常';
        } else if (message.indexOf('timeout') !== -1) {
            message = '系统接口请求超时';
        }
        window.ELEMENT && window.ELEMENT.Message({
            message: message,
            type: 'error',
            duration: 5000
        });
        return Promise.reject(error);
    });

    // ==================== 订单导出 ====================
    var exportOrdersExcel = function(params) {
        return service.get('/export/orders/excel', { params: params, responseType: 'blob' });
    };
    var exportOrdersPdf = function(params) {
        return service.get('/export/orders/pdf', { params: params, responseType: 'blob' });
    };

    // ==================== 菜品导出 ====================
    var exportDishesExcel = function(params) {
        return service.get('/export/dishes/excel', { params: params, responseType: 'blob' });
    };
    var exportDishesPdf = function(params) {
        return service.get('/export/dishes/pdf', { params: params, responseType: 'blob' });
    };

    // ==================== 员工导出 ====================
    var exportEmployeesExcel = function(params) {
        return service.get('/export/employees/excel', { params: params, responseType: 'blob' });
    };
    var exportEmployeesPdf = function(params) {
        return service.get('/export/employees/pdf', { params: params, responseType: 'blob' });
    };

    // ==================== 报表导出 ====================
    var exportReportExcel = function(params) {
        return service.get('/api/report/export', { params: Object.assign({}, params, { format: 'excel' }), responseType: 'blob' });
    };
    var exportReportPdf = function(params) {
        return service.get('/api/report/export', { params: Object.assign({}, params, { format: 'pdf' }), responseType: 'blob' });
    };

    // ==================== 通用下载 ====================
    var downloadFile = function(apiCall, fileName) {
        return apiCall.then(function(res) {
            var blob = res.data instanceof Blob ? res.data : new Blob([res.data]);

            if (blob.type && blob.type.indexOf('application/json') !== -1) {
                return new Promise(function(_, reject) {
                    var reader = new FileReader();
                    reader.onload = function() {
                        try {
                            var errData = JSON.parse(reader.result);
                            reject(new Error(errData.msg || '导出失败，请重新登录'));
                        } catch(e) {
                            reject(new Error('导出失败，服务器返回异常'));
                        }
                    };
                    reader.readAsText(blob);
                });
            }

            var url = window.URL.createObjectURL(blob);
            var a = document.createElement('a');
            a.href = url;
            a.style.display = 'none';

            var disposition = res.headers['content-disposition'];
            if (disposition) {
                var match = disposition.match(/filename[^;=\\n]*=((['"]).*?\\2|[^;\\n]*)/);
                if (match && match[1]) {
                    a.download = decodeURIComponent(match[1].replace(/['"]/g, ''));
                } else {
                    a.download = fileName;
                }
            } else {
                a.download = fileName;
            }

            document.body.appendChild(a);
            a.click();
            setTimeout(function() {
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
            }, 200);
        }).catch(function(err) {
            console.error('文件下载失败:', err);
            throw err;
        });
    };

    return {
        exportOrdersExcel: exportOrdersExcel,
        exportOrdersPdf: exportOrdersPdf,
        exportDishesExcel: exportDishesExcel,
        exportDishesPdf: exportDishesPdf,
        exportEmployeesExcel: exportEmployeesExcel,
        exportEmployeesPdf: exportEmployeesPdf,
        exportReportExcel: exportReportExcel,
        exportReportPdf: exportReportPdf,
        downloadFile: downloadFile
    };
})();
