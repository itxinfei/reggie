/**
 * 骑手端请求封装：axios 实例 + CSRF 自动带头 + 统一响应/未登录处理。
 * 依赖全局 axios、vant（Vant2 UMD）；CSRF 存取与请求拦截复用 /shared/js/request-core.js。
 */
document.write('<script src="/shared/js/request-core.js?v=20260930"><\/script>');
(function (win) {
  var service = axios.create({
    baseURL: '/',
    timeout: 15000
  });

  function toast(msg) {
    if (win.vant && typeof win.vant.Toast === 'function') {
      win.vant.Toast(msg);
    }
  }

  function goLogin() {
    win.ReggieCsrf.clear();
    if (win.location.pathname.indexOf('/rider/login.html') === -1) {
      win.location.href = '/rider/login.html';
    }
  }

  // 请求拦截：写操作带 CSRF 头（统一实现见 request-core.js）。
  // document.write 注入的脚本在本文件顶层之后执行，core 未就绪时登记 pending 由其补挂。
  if (win.ReggieCsrf) {
    win.ReggieCsrf.attachRequestInterceptor(service, win.ReggieCsrf.get);
  } else {
    (win.__reggieCsrfPending = win.__reggieCsrfPending || []).push(service);
  }

  // 响应拦截
  service.interceptors.response.use(function (res) {
    var t = res.headers['x-csrf-token'];
    if (t) { win.ReggieCsrf.save(t); }

    var body = res.data || {};
    if (body.code === 1) {
      return body;
    }
    // HTTP 200 但接口内判定未登录（非经 LoginCheckFilter 的场景）→ 跳登录
    if (body.msg === 'NOTLOGIN') {
      goLogin();
      return Promise.reject(new Error('NOTLOGIN'));
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
        win.ReggieCsrf.save(newToken);
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
