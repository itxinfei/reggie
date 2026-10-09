// 登录API
function loginApi(data) {
  return $axios({
    url: '/employee/login',
    method: 'post',
    data: data
  })
}

// 退出API
function logoutApi() {
  return $axios({
    url: '/employee/logout',
    method: 'post'
  })
}

// 忘记密码API
// 修改点：前端统一使用 newPassword 字段名，后端兼容 password 和 newPassword
function forgotPasswordApi(data) {
  return $axios({
    url: '/employee/forgot-password',
    method: 'post',
    data: {
      username: data.username,
      phone: data.phone,
      newPassword: data.newPassword
    }
  })
}
