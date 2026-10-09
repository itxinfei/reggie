/**
 * 跨端共享：CSRF Token 存取 + 请求拦截注入。
 * 单一真源，三端 request.js 共同依赖（消除三套重复实现、统一编码/解码行为）。
 * 引入方式：由各端 request.js 在文件顶部 document.write 本文件（与 util.js 引入
 * img-path.js / order-status.js 同机制）。注意：document.write 注入的脚本在本端
 * request.js 顶层代码「之后」执行，因此顶层需经 __reggieCsrfPending 握手（见文件末尾）。
 */
(function (win) {
  // ---- CSRF Token 存取（统一：保存时编码、读取时解码；HTTPS 下补 Secure） ----
  // 说明：此前三端实现不一致——骑手端编解码完整，后台/C 端「保存 encodeURIComponent、
  // 读取却按原始 substring」存在潜在编解码错位。此处统一为「保存编码、读取解码」，
  // 与后端 CsrfFilter 对 X-CSRF-Token 的解码预期一致；HTTPS 下补 Secure 防中间人窃取。
  function getCsrfToken() {
    var cookies = document.cookie.split(';');
    for (var i = 0; i < cookies.length; i++) {
      var c = cookies[i].trim();
      if (c.indexOf('csrfToken=') === 0) {
        try {
          return decodeURIComponent(c.substring('csrfToken='.length));
        } catch (e) {
          return c.substring('csrfToken='.length);
        }
      }
    }
    try {
      return sessionStorage.getItem('csrfToken');
    } catch (e) {
      return null;
    }
  }

  function saveCsrfToken(token) {
    if (!token) return;
    try {
      sessionStorage.setItem('csrfToken', token);
      var expires = new Date(Date.now() + 30 * 60 * 1000).toUTCString();
      // HTTPS 下补 Secure，防止中间人窃取 CSRF token（原仅骑手端实现，现全端统一）
      var secure = win.location.protocol === 'https:' ? '; Secure' : '';
      document.cookie = 'csrfToken=' + encodeURIComponent(token) +
        '; expires=' + expires + '; path=/; SameSite=Strict' + secure;
    } catch (e) {
      /* 忽略存储异常 */
    }
  }

  function clearCsrfToken() {
    try {
      sessionStorage.removeItem('csrfToken');
      document.cookie = 'csrfToken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    } catch (e) {
      /* 忽略 */
    }
  }

  /** 为 axios 实例挂载 CSRF 请求拦截：写操作（POST/PUT/DELETE/PATCH）自动带 X-CSRF-Token 头。 */
  function attachRequestInterceptor(service, getToken) {
    service.interceptors.request.use(function (config) {
      var method = (config.method || 'get').toLowerCase();
      if (method === 'post' || method === 'put' || method === 'delete' || method === 'patch') {
        var token = getToken();
        if (token) {
          config.headers['X-CSRF-Token'] = token;
        }
      }
      return config;
    }, function (error) {
      return Promise.reject(error);
    });
  }

  win.ReggieCsrf = {
    get: getCsrfToken,
    save: saveCsrfToken,
    clear: clearCsrfToken,
    attachRequestInterceptor: attachRequestInterceptor
  };

  // 各端 request.js 通过 document.write 引入本文件时，本文件必然在该脚本顶层代码
  // 执行完毕之后才运行（document.write 的插入点在当前标签之后），故客户端在
  // ReggieCsrf 未就绪时先把 axios 实例登记到 pending 列表，此处统一补挂拦截器。
  var pending = win.__reggieCsrfPending || [];
  for (var i = 0; i < pending.length; i++) {
    attachRequestInterceptor(pending[i], getCsrfToken);
  }
  win.__reggieCsrfPending = [];
})(window);
