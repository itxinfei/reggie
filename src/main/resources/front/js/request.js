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
    return config
  }, error => {
      return Promise.reject(error)
  })

  // 响应拦截器
  service.interceptors.response.use(res => {
      if (res.data.code === 0 && res.data.msg === 'NOTLOGIN') {// 返回登录页面
        // 修改点：本项目不使用iframe，直接用window.location
        window.location.href = '/front/page/login.html'
      } else {
        return res.data
      }
    },
    error => {
      let { message } = error;
      if (message === "Network Error") {
        message = "后端接口连接异常";
      }
      else if (message.includes("timeout")) {
        message = "系统接口请求超时";
      }
      else if (message.includes("Request failed with status code")) {
        message = "系统接口" + message.substring(message.length - 3) + "异常";
      }
      window.vant.Notify({
        message: message,
        type: 'warning',
        duration: 5 * 1000
      })
      return Promise.reject(error)
    }
  )
  win.$axios = service
})(window);
