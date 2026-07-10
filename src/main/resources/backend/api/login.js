function loginApi(data) {
  return $axios({
    'url': '/employee/login',
    'method': 'post',
    data
  })
}

function logoutApi(){
  return $axios({
    'url': '/employee/logout',
    'method': 'post',
  })
}

// 忘记密码
function forgotPasswordApi(data) {
  return $axios({
    'url': '/employee/forgot-password',
    'method': 'post',
    data
  })
}
