function loginApi(data) {
    return $axios({
      'url': '/user/login',
      'method': 'post',
      data
    })
}

function sendMsgApi(data) {
    return $axios({
        'url': '/user/sendMsg',
        'method': 'post',
        data
    })
}

function loginoutApi() {
  return $axios({
    'url': '/user/loginout',
    'method': 'post',
  })
}

// 修改点：新增获取用户信息API，用于个人中心页面
function getUserInfoApi() {
  return $axios({
    'url': '/user/info',
    'method': 'get',
  })
}
  