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
    // 取本人完整手机号，修复新标签页 sessionStorage 丢失后无法恢复真实手机号
    params: { full: 1 }
  })
}

// 修改点(2026-09-16)：更新当前用户基本信息（昵称/性别/头像），个人中心编辑资料与更换头像复用
function updateUserInfoApi(data) {
  return $axios({
    'url': '/user/info',
    'method': 'put',
    data
  })
}
  