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
        // 修改点：本项目不使用iframe，直接用window.location
        clearCsrfToken();
        window.location.href = '/front/page/login.html'
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
          clearCsrfToken();
          var curPage = window.location.pathname;
          if (!curPage.includes('login')) {
            window.location.href = '/front/page/login.html';
          }
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
      if (!isMutating
          && (message === "Network Error" || message === "后端接口连接异常" || message.includes("timeout"))
          && !currentPage.includes('no-wifi')
          && !currentPage.includes('login')) {
        window.location.href = '/front/page/no-wifi.html'
      }
      // 修改点：防御性检查vant是否加载
      if(window.vant && window.vant.Notify){
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

  // 全局错误捕获，防止STATUS_ACCESS_VIOLATION等浏览器底层崩溃
  window.addEventListener('unhandledrejection', function(event) {
    console.error('[Unhandled Rejection]', event.reason)
    event.preventDefault()
  })
})(window);
