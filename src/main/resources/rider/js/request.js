/**
 * 骑手端请求封装：axios 实例 + CSRF 自动带头 + 统一响应/未登录处理。
 * 依赖全局 axios、vant（Vant2 UMD）。
 */
(function (win) {
  var service = axios.create({
    baseURL: '/',
    timeout: 15000
  });

  // ---- CSRF Token 存取 ----
  function getCsrfToken() {
    var cookies = document.cookie.split(';');
    for (var i = 0; i < cookies.length; i++) {
      var c = cookies[i].trim();
      if (c.indexOf('csrfToken=') === 0) {
        return decodeURIComponent(c.substring('csrfToken='.length));
      }
    }
    try { return sessionStorage.getItem('csrfToken'); } catch (e) { return null; }
  }

  function saveCsrfToken(token) {
    if (!token) return;
    try {
      sessionStorage.setItem('csrfToken', token);
      var expires = new Date(Date.now() + 30 * 60 * 1000).toUTCString();
      // HTTPS 下补 Secure，防止中间人窃取 CSRF token
      var secure = win.location.protocol === 'https:' ? '; Secure' : '';
      document.cookie = 'csrfToken=' + encodeURIComponent(token) +
        '; expires=' + expires + '; path=/; SameSite=Strict' + secure;
    } catch (e) { /* 忽略存储异常 */ }
  }

  function clearCsrfToken() {
    try {
      sessionStorage.removeItem('csrfToken');
      document.cookie = 'csrfToken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    } catch (e) { /* 忽略 */ }
  }

  function toast(msg) {
    if (win.vant && typeof win.vant.Toast === 'function') {
      win.vant.Toast(msg);
    }
  }

  function goLogin() {
    clearCsrfToken();
    if (win.location.pathname.indexOf('/rider/login.html') === -1) {
      win.location.href = '/rider/login.html';
    }
  }

  // 请求拦截：写操作带 CSRF 头
  service.interceptors.request.use(function (config) {
    var method = (config.method || 'get').toLowerCase();
    if (method === 'post' || method === 'put' || method === 'delete' || method === 'patch') {
      var token = getCsrfToken();
      if (token) { config.headers['X-CSRF-Token'] = token; }
    }
    return config;
  }, function (error) { return Promise.reject(error); });

  // 响应拦截
  service.interceptors.response.use(function (res) {
    var t = res.headers['x-csrf-token'];
    if (t) { saveCsrfToken(t); }

    var body = res.data || {};
    if (body.code === 1) {
      return body;
    }
    // 业务失败
    toast(body.msg || '操作失败');
    return Promise.reject(new Error(body.msg || '业务失败'));
  }, function (error) {
    var status = error.response ? error.response.status : 0;
    var data = error.response ? error.response.data : null;
    // CSRF token 过期：后端在 403 响应头下发轮转的新 token，保存后自动重放本次请求一次
    var cfg = error.config;
    if (status === 403 && cfg && !cfg.__csrfRetried) {
      var newToken = error.response.headers['x-csrf-token'];
      if (newToken) {
        saveCsrfToken(newToken);
        cfg.__csrfRetried = true;
        cfg.headers['X-CSRF-Token'] = newToken;
        return service(cfg);
      }
    }
    if (status === 401 || (data && data.msg === 'NOTLOGIN')) {
      goLogin();
      return Promise.reject(new Error('NOTLOGIN'));
    }
    var msg;
    if (data && data.msg) {
      msg = data.msg;
    } else if (error.message === 'Network Error') {
      msg = '网络异常，请检查网络后重试';
    } else if (error.message && error.message.indexOf('timeout') !== -1) {
      msg = '请求超时，请重试';
    } else {
      msg = '服务异常，请稍后再试';
    }
    toast(msg);
    return Promise.reject(new Error(msg));
  });

  win.$axios = service;
})(window);
