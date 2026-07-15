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
  service.interceptors.request.use(config => {
    return config
  }, error => {
      return Promise.reject(error)
  })

  // 响应拦截器
  service.interceptors.response.use(res => {
      // 修改点：统一code判断，code===0为业务失败，code===1为成功
      const code = res.data ? res.data.code : undefined;
      // NOTLOGIN状态码处理：返回登录页面
      if (code === 0 && res.data.msg === 'NOTLOGIN') {
        localStorage.removeItem('userInfo')
        // 修改点：后端页面在iframe中加载，须用window.top导航顶层窗口到登录页
        window.top.location.href = '/backend/page/login/login.html'
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
        if (respData.msg && respData.msg.startsWith('参数校验失败')) {
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
      window.ELEMENT.Message({
        message: message,
        type: 'error',
        duration: 5 * 1000
      })
      return Promise.reject(error)
    }
  )
  win.$axios = service
})(window);
