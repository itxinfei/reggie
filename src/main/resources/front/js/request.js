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

  // 响应拦截器
  service.interceptors.response.use(res => {
      // 修改点：防御性检查res和res.data，防止异常响应导致TypeError
      if (res && res.data && res.data.code === 0 && res.data.msg === 'NOTLOGIN') {
        // 修改点：本项目不使用iframe，直接用window.location
        window.location.href = '/front/page/login.html'
        return Promise.reject(new Error('NOTLOGIN'))
      } else if (res && res.data) {
        return res.data
      }
      return Promise.reject(new Error('Invalid response'))
    },
    error => {
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
      // 修改点：网络异常时自动跳转断网页（排除已在断网页/登录页的情况，避免死循环）
      var currentPage = window.location.pathname;
      if ((message === "Network Error" || message === "后端接口连接异常" || message.includes("timeout"))
          && !currentPage.includes('no-wify')
          && !currentPage.includes('login')) {
        window.location.href = '/front/page/no-wify.html'
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
