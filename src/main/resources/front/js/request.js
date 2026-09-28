(function (win) {
  axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
  // 创建axios实例
  const service = axios.create({
    // axios中请求配置有baseURL选项，表示请求URL公共部分
    baseURL: '/',
    // 超时
    timeout: 30000
  })
  // request拦截器
  service.interceptors.request.use(config => {
    // 为POST/PUT/DELETE请求添加CSRF Token
    var method = (config.method || 'get').toLowerCase();
    if (method === 'post' || method === 'put' || method === 'delete') {
      var csrfToken = getCsrfToken();
      if (csrfToken) {
        config.headers['X-CSRF-Token'] = csrfToken;
      }
    }
    return config
  }, error => {
      return Promise.reject(error)
  })

  // 修改点(2026-09-18)：未登录跳登录页时携带当前地址，登录成功后回跳来源页（仅站内相对路径，登录页再做安全校验）
  function buildLoginUrl() {
    var back = window.location.pathname + window.location.search;
    return '/front/page/login.html?redirect=' + encodeURIComponent(back);
  }

  // 未登录统一处理：立即跳登录页（2026-09-26：去掉"先提示再延迟1.2s跳转"，
  // 打开页面必须是登录态，未登录直接进登录页；防重入，并发请求同时 401 只跳一次）
  var notLoginHandled = false;
  function redirectToLogin() {
    clearCsrfToken();
    if (window.location.pathname.indexOf('login') !== -1) { return; }
    if (notLoginHandled) { return; }
    notLoginHandled = true;
    window.location.replace(buildLoginUrl());
  }

  /**
   * 获取CSRF Token
   */
  function getCsrfToken() {
    // 尝试从Cookie获取
    var cookies = document.cookie.split(';');
    for (var i = 0; i < cookies.length; i++) {
      var cookie = cookies[i].trim();
      if (cookie.startsWith('csrfToken=')) {
        return cookie.substring('csrfToken='.length);
      }
    }
    // 尝试从SessionStorage获取
    try {
      return sessionStorage.getItem('csrfToken');
    } catch (e) {
      return null;
    }
  }

  /**
   * 保存CSRF Token到Cookie和SessionStorage（与后台 request.js 保持一致）
   * 修改点(2026-09-01)：C 端此前只读不存，导致所有 POST/PUT/DELETE 被 CsrfFilter 拦截 403
   */
  function saveCsrfToken(token) {
    if (!token) return;
    try {
      sessionStorage.setItem('csrfToken', token);
      var expires = new Date(Date.now() + 30 * 60 * 1000).toUTCString();
      document.cookie = 'csrfToken=' + encodeURIComponent(token) + '; expires=' + expires + '; path=/; SameSite=Strict';
    } catch (e) {
      console.warn('保存CSRF Token失败', e);
    }
  }

  /**
   * 清除CSRF Token
   */
  function clearCsrfToken() {
    try {
      sessionStorage.removeItem('csrfToken');
      document.cookie = 'csrfToken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    } catch (e) {
      console.warn('清除CSRF Token失败', e);
    }
  }

  // 响应拦截器
  service.interceptors.response.use(res => {
      // 修改点(2026-09-01)：保存后端通过响应头返回的CSRF Token
      var csrfToken = res.headers ? res.headers['x-csrf-token'] : null;
      if (csrfToken) {
        saveCsrfToken(csrfToken);
      }
      // 修改点：防御性检查res和res.data，防止异常响应导致TypeError
      if (res && res.data && res.data.code === 0 && res.data.msg === 'NOTLOGIN') {
        // 会话探测请求：留在原页，由调用方按游客态处理（与下方 401 分支开关一致）
        if (res.config && res.config.skipAuthRedirect) {
          return Promise.reject(new Error('NOTLOGIN'))
        }
        // 修改点：本项目不使用iframe，直接用window.location；提示后跳转
        redirectToLogin()
        return Promise.reject(new Error('NOTLOGIN'))
      } else if (res && res.data) {
        return res.data
      }
      return Promise.reject(new Error('Invalid response'))
    },
    error => {
      // 修改点：401 = 登录态缺失/会话失效（后端 LoginCheckFilter 返回 401 + {code:0,msg:'NOTLOGIN'}）。
      // 原逻辑只在「响应成功分支」判断 NOTLOGIN（要求后端返回 200），但本项目未登录返回的是 401，
      // 导致成功分支永远收不到、不会跳登录页，只能在受保护页面刷一堆 "系统接口401异常" 报错。
      // 在此统一处理 401 -> 跳登录页，避免未登录用户被困在页面且满屏报错。
      var errResp = error && error.response;
      if (errResp && errResp.status === 401) {
        var body = errResp.data;
        var isNotLogin = !body || (body.code === 0 && body.msg === 'NOTLOGIN');
        if (isNotLogin) {
          // 请求声明 skipAuthRedirect：会话探测场景（如首页静默恢复登录态），
          // 不强制跳登录页，reject 后由调用方按游客态处理
          if (error.config && error.config.skipAuthRedirect) {
            return Promise.reject(error);
          }
          redirectToLogin();
          return Promise.reject(error);
        }
      }
      let { message } = error || {};
      if (!message) message = '未知错误';
      if (message === "Network Error") {
        message = "后端接口连接异常";
      }
      else if (message.includes("timeout")) {
        message = "系统接口请求超时";
      }
      else if (message.includes("Request failed with status code")) {
        message = "系统接口" + message.substring(message.length - 3) + "异常";
      }
      // 修改点(2026-09-18)：仅 GET 请求网络异常才跳断网页。
      // 原实现不分方法一律 location.href=no-wifi，会卸载当前页，导致用户在下单/支付/加购/
      // 提交评价/保存地址等变更操作中途断网时，已填表单与进行中交易全部丢失且无法原地重试。
      // 变更类请求（POST/PUT/DELETE/PATCH）失败只提示并 reject，由调用方 try/catch 引导重试；
      // GET 不承载表单输入，首屏数据加载失败进断网页是合理兜底。
      var currentPage = window.location.pathname;
      var reqMethod = (error && error.config && error.config.method || '').toLowerCase();
      var isMutating = reqMethod === 'post' || reqMethod === 'put'
          || reqMethod === 'delete' || reqMethod === 'patch';
      // 单个请求可在 config 中声明 skipNoWifiRedirect（如后台轮询），
      // 瞬断时留在原页由调用方静默重试，避免整页跳走丢掉用户已填草稿
      var skipNoWifi = error && error.config && error.config.skipNoWifiRedirect;
      if (!skipNoWifi
          && !isMutating
          && (message === "Network Error" || message === "后端接口连接异常" || message.includes("timeout"))
          && !currentPage.includes('no-wifi')
          && !currentPage.includes('login')) {
        window.location.href = '/front/page/no-wifi.html'
      }
      // silent 请求（后台轮询等）连错误横幅也不弹，由调用方静默处理
      var isSilent = error && error.config && error.config.silent;
      // 修改点：防御性检查vant是否加载
      if(!isSilent && window.vant && window.vant.Notify){
        window.vant.Notify({
          message: message,
          type: 'warning',
          duration: 5 * 1000
        })
      }
      return Promise.reject(error)
    }
  )
  win.$axios = service

  // ===== C端 SSE 实时消息通道（2026-09-26）=====
  // 登录后建立一条同源 EventSource（自动带 JSESSIONID，无需 CSRF 头）。
  // 后台推送到达时：弹 Vant 提醒 → 拉最新未读数 → 广播 reggie:new-message 事件，
  // 供当前页面的 Vue（首页/消息中心）刷新角标与列表。
  function initSseChannel() {
    if (win.__reggieSSE || typeof win.EventSource === 'undefined') { return; }
    var es;
    try {
      es = new win.EventSource('/notification/sse/subscribe');
    } catch (e) { return; }
    win.__reggieSSE = es;

    es.addEventListener('message', function (ev) {
      var msg = null;
      try { msg = JSON.parse(ev.data); } catch (e) { return; }
      // 实时提醒，点击进入消息中心
      if (win.vant && win.vant.Notify) {
        win.vant.Notify({
          message: msg.title || msg.content || '您有一条新消息',
          type: 'primary',
          duration: 5000,
          onClick: function () { win.location.href = '/front/page/message.html'; }
        });
      }
      // 广播事件，让当前页 Vue 刷新（无论拉未读数成败都广播，页面自行兜底）
      var notifyPages = function () {
        try { win.dispatchEvent(new win.CustomEvent('reggie:new-message', { detail: msg })); } catch (e) {}
      };
      win.$axios({ url: '/recommend/messages/unread-count', method: 'get',
                   silent: true, skipAuthRedirect: true })
        .then(notifyPages, notifyPages);
    });

    es.onerror = function () {
      // 浏览器会自动重连；仅当本地登录态已清除（会话失效）时关闭，跳转交给登录守卫
      var phone = null;
      try { phone = sessionStorage.getItem('userPhone'); } catch (e) {}
      if (!phone) {
        try { es.close(); } catch (e) {}
        win.__reggieSSE = null;
      }
    };
  }

  // ===== 页面登录守卫（2026-09-26）：打开页面必须是用户登录状态 =====
  // 页面加载时立即探测服务端会话（/user/info），未登录立即跳登录页，无提示、无延迟。
  // 仅「确认未登录」（NOTLOGIN / 401）才跳转；断网、5xx 不跳，交给 no-wifi 页 / 错误提示兜底。
  function isNotLoginErr(err) {
    if (!err) { return false; }
    if (err.message === 'NOTLOGIN') { return true; }
    return !!(err.response && err.response.status === 401);
  }
  (function guardLoginOnPageOpen() {
    var path = win.location.pathname;
    // 登录页自身不守卫（否则死循环）；断网兜底页不守卫（探测必然失败，会误跳）
    if (path.indexOf('/front/page/login.html') !== -1 || path.indexOf('no-wifi') !== -1) { return; }
    win.$axios({ url: '/user/info', method: 'get', params: { full: 1 },
                 skipAuthRedirect: true, silent: true })
      .then(function (r) {
        var u = r && r.data;
        var ok = r && r.code === 1 && u && /^1\d{10}$/.test(u.phone);
        if (!ok) {
          try { sessionStorage.removeItem('userPhone'); } catch (e) {}
          win.location.replace(buildLoginUrl());
          return;
        }
        // 已登录：建立 SSE 实时消息通道（函数内部防重复）+ 客服未读守护
        initSseChannel();
        startCsUnreadGuard();
      })
      .catch(function (err) {
        if (isNotLoginErr(err)) { win.location.replace(buildLoginUrl()); }
      });
  })();

  // ===== 客服未读守护（2026-09-26）=====
  // 背景：C 端只会在「在线客服」页轮询消息，后台客服发完消息后，
  // 只要用户不打开该页就完全无感知 —— 表现为"后台显示发送成功，C 端收不到"。
  // 这里在任意已登录页面低频轮询客服未读数，有未读就弹提醒并可点击直达会话。
  function startCsUnreadGuard() {
    if (win.__csUnreadTimer) { return; }
    var lastNotified = 0;                 // 已提醒过的未读数，避免重复弹
    var notifiedSession = 0;
    var tick = function () {
      try {
        // 会话页自身已在轮询并会自动标记已读，无需重复提醒；
        // 修复(2026-09-27 审查P0-4)：消息中心页同样展示客服消息卡片，停留期间跳过轮询，
        // 避免用户正在看消息时通知条每 15s 盖顶、且离开后 unread 已清零不再骚扰
        if (win.location.pathname.indexOf('customer-service') !== -1
            || win.location.pathname.indexOf('message.html') !== -1) { return; }
        win.$axios({ url: '/cs/portal/session/list', method: 'get',
                     silent: true, skipAuthRedirect: true, skipNoWifiRedirect: true })
          .then(function (r) {
            if (!r || r.code !== 1 || !r.data || !r.data.length) { return; }
            var sid = null;
            for (var i = 0; i < r.data.length; i++) {
              var st = Number(r.data[i].status);
              if (st === 0 || st === 1) { sid = r.data[i].id; break; }   // 待分配 / 进行中
            }
            if (!sid) { lastNotified = 0; return; }
            return win.$axios({ url: '/cs/portal/message/unread/' + sid, method: 'get',
                                silent: true, skipAuthRedirect: true, skipNoWifiRedirect: true })
              .then(function (u) {
                var n = u && u.code === 1 ? Number(u.data) : 0;
                if (!n) { lastNotified = 0; notifiedSession = 0; return; }
                if (notifiedSession === sid && n <= lastNotified) { return; }
                notifiedSession = sid; lastNotified = n;
                if (!win.vant || !win.vant.Notify) { return; }
                win.vant.Notify({
                  message: '客服发来 ' + n + ' 条新消息',
                  type: 'primary',
                  duration: 5000,
                  onClick: function () { win.location.href = '/front/page/customer-service.html'; }
                });
              });
          })
          .catch(function () { /* 轮询失败静默，下次重试 */ });
      } catch (e) { /* 守护异常不得影响主流程 */ }
    };
    tick();
    win.__csUnreadTimer = win.setInterval(tick, 15000);
  }

  // 全局错误捕获，防止STATUS_ACCESS_VIOLATION等浏览器底层崩溃
  window.addEventListener('unhandledrejection', function(event) {
    console.error('[Unhandled Rejection]', event.reason)
    event.preventDefault()
  })
})(window);
