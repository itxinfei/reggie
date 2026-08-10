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
  // 修改点：移除手动GET参数拼接代码，axios原生支持params序列化，无需手动处理
  // 修改点：自动携带CSRF Token（从Cookie或SessionStorage获取）
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
   * 优先从Cookie获取，其次从SessionStorage获取
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
   * 保存CSRF Token到Cookie和SessionStorage
   */
  function saveCsrfToken(token) {
    if (!token) return;
    try {
      // 保存到SessionStorage
      sessionStorage.setItem('csrfToken', token);
      // 保存到Cookie（有效期30分钟，与后端同步）
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
      // 修改点：保存后端返回的CSRF Token
      var csrfToken = res.headers['x-csrf-token'];
      if (csrfToken) {
        saveCsrfToken(csrfToken);
      }
      // 修改点：统一code判断，code===0为业务失败，code===1为成功
      const code = res.data ? res.data.code : undefined;
      // NOTLOGIN状态码处理：返回登录页面
      if (code === 0 && res.data.msg === 'NOTLOGIN') {
        localStorage.removeItem('userInfo')
        clearCsrfToken();
        // 修改点：后端页面在iframe中加载，须用window.top导航顶层窗口到登录页
        try {
          window.top.location.href = '/backend/page/login/login.html'
        } catch (e) {
          window.location.href = '/backend/page/login/login.html'
        }
        return Promise.reject(new Error('NOTLOGIN'))  // 修改点：阻止Promise继续进入then回调
      } else {
        return res.data
      }
    },
    error => {
      let { message } = error;
      // 修改点：尝试从响应体中提取详细的错误信息
      if (error.response && error.response.data) {
        const respData = error.response.data;
        // 后端统一响应格式：{ code: 0, msg: "..." }
        if (respData.msg && typeof respData.msg === 'string' && respData.msg.startsWith('参数校验失败')) {
          message = respData.msg;
        } else if (respData.msg) {
          message = respData.msg;
        }
      }
      if (message === "Network Error") {
        message = "后端接口连接异常";
      }
      else if (message.includes("timeout")) {
        message = "系统接口请求超时";
      }
      else if (message.includes("Request failed with status code")) {
        message = "系统接口" + message.substring(message.length - 3) + "异常";
      }
      if (window.ELEMENT && window.ELEMENT.Message) {
        window.ELEMENT.Message({
          message: message,
          type: 'error',
          duration: 5 * 1000
        })
      }
      return Promise.reject(error)
    }
  )
  win.$axios = service;

  /* ===== ReggieUI 统一交互反馈（挂载于 window.ReggieUI） =====
     规范（前端二次审查 2026-07-17）：所有页面的 toast / loading / confirm /
     notify 必须经由本模块，禁止在业务页混用 this.$message / ElMessage /
     Notification 直写，确保提示样式与交互反馈一致、可统一管控。
     优先走 Vue.prototype.$message 等原型方法，缺失时降级 window.ELEMENT。 */
  (function (global) {
    'use strict';
    function getVue() {
      return global.Vue || (global.top && global.top.Vue);
    }
    function callMethod(name, fallback) {
      var Vue = getVue();
      var args = Array.prototype.slice.call(arguments, 2);
      if (Vue && Vue.prototype && typeof Vue.prototype[name] === 'function') {
        return Vue.prototype[name].apply(Vue.prototype, args);
      }
      if (typeof fallback === 'function') {
        return fallback.apply(null, args);
      }
      return null;
    }
    var ReggieUI = {
      /** 兼容 Element $message 对象形式，如 ReggieUI.message({ type:'success', message:'x' }) */
      message: function (options) {
        var res = callMethod('$message', global.ELEMENT && global.ELEMENT.Message, options);
        if (res === null && options && options.message) { global.alert(options.message); }
      },
      /** 轻提示：type 可取 success/warning/info/error；duration 毫秒 */
      toast: function (message, type, duration) {
        var res = callMethod('$message', global.ELEMENT && global.ELEMENT.Message, {
          message: message,
          type: type || 'info',
          duration: (duration == null) ? 2000 : duration
        });
        if (res === null) { global.alert(message); }
      },
      success: function (m, d) { this.toast(m, 'success', d); },
      error: function (m, d) { this.toast(m, 'error', d); },
      warning: function (m, d) { this.toast(m, 'warning', d); },
      info: function (m, d) { this.toast(m, 'info', d); },
      /** 全屏加载态，返回带 close() 的句柄 */
      loading: function (text) {
        var opts = {
          text: text || '加载中…',
          fullscreen: true,
          background: 'rgba(255, 255, 255, 0.7)'
        };
        var inst = callMethod('$loading', global.ELEMENT && global.ELEMENT.Loading && global.ELEMENT.Loading.service, opts);
        if (inst) { return inst; }
        return { close: function () {} };
      },
      /** 确认框：签名兼容 Element this.$confirm(message, title, options)，返回 Promise */
      confirm: function (message, title, options) {
        var opts = options || {};
        if (opts.confirmButtonText == null) { opts.confirmButtonText = '确定'; }
        if (opts.cancelButtonText == null) { opts.cancelButtonText = '取消'; }
        if (opts.type == null) { opts.type = 'warning'; }
        var fb = global.ELEMENT && global.ELEMENT.MessageBox && global.ELEMENT.MessageBox.confirm;
        var p = callMethod('$confirm', fb, message, title || '提示', opts);
        if (p) { return p; }
        return global.confirm(message) ? Promise.resolve() : Promise.reject();
      },
      /** 通知（右上角）：签名兼容 Element this.$notify(options) */
      notify: function (options) {
        var res = callMethod('$notify', global.ELEMENT && global.ELEMENT.Notification, options);
        if (res === null && options && options.message) { global.alert(options.message); }
      }
    };
    global.ReggieUI = ReggieUI;
  })(window);
})(window);
