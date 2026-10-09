/**
 * 骑手端接口封装。所有方法返回 Promise，resolve 值为后端统一响应体（取业务数据用 .data）。
 */
(function (win) {
  var RiderApi = {
    // ---- 鉴权 ----
    login: function (phone, password, rememberMe) {
      return $axios.post('/api/rider/login', {
        phone: phone, password: password, rememberMe: !!rememberMe
      });
    },
    logout: function () {
      return $axios.post('/api/rider/logout');
    },
    // 发送短信验证码（复用 C 端统一发码接口，验证码存 HttpSession）
    sendSmsCode: function (phone) {
      return $axios.post('/user/sendMsg', { phone: phone });
    },
    // 自助重置密码
    forgotPassword: function (phone, code, newPassword) {
      return $axios.post('/api/rider/forgot-password', {
        phone: phone, code: code, newPassword: newPassword
      });
    },
    me: function () {
      return $axios.get('/api/rider/me');
    },
    setOnline: function (online) {
      return $axios.post(online ? '/api/rider/online' : '/api/rider/offline');
    },

    // ---- 任务 ----
    myTasks: function (scope) {
      return $axios.get('/api/rider/tasks/mine', { params: { scope: scope } });
    },
    hall: function () {
      return $axios.get('/api/rider/tasks/hall');
    },
    detail: function (id) {
      return $axios.get('/api/rider/tasks/' + id);
    },
    grab: function (id) {
      return $axios.post('/api/rider/tasks/' + id + '/grab');
    },
    accept: function (id) {
      return $axios.post('/api/rider/tasks/' + id + '/accept');
    },
    // 修改点(P0-6)：取餐需核销门店取餐码
    pickup: function (id, pickupCode) {
      return $axios.post('/api/rider/tasks/' + id + '/pickup', {}, { params: { pickupCode: pickupCode } });
    },
    deliver: function (id) {
      return $axios.post('/api/rider/tasks/' + id + '/deliver');
    },

    // ---- 我的收入（修改点 P0-1）----
    incomeSummary: function () {
      return $axios.get('/api/rider/income/summary');
    },
    incomeRecords: function (range, page, size) {
      return $axios.get('/api/rider/income/records', {
        params: { range: range, page: page, size: size }
      });
    },

    // ---- 骑手结算与提现（修改点 P0-1 增量2）----
    incomeBalance: function () {
      return $axios.get('/api/rider/settlement/balance');
    },
    incomeWithdrawals: function () {
      return $axios.get('/api/rider/settlement/withdrawals');
    },
    applyWithdraw: function (amount) {
      return $axios.post('/api/rider/settlement/withdraw', {}, { params: { amount: amount } });
    },

    // ---- 骑手评价（修改点 P0-2）----
    // 收到的评价（当前登录骑手）
    riderEvaluationReceived: function (page, size) {
      return $axios.get('/api/rider-evaluation/received', { params: { page: page, pageSize: size } });
    },
    // 骑手评分统计（需骑手ID）
    riderEvaluationStats: function (riderId) {
      return $axios.get('/api/rider-evaluation/rider/' + riderId + '/stats');
    },

    // ---- 骑手异常与转单（修改点 P0-3）----
    // 上报配送异常
    submitException: function (orderId, exceptionType, description) {
      return $axios.post('/api/rider-exception', {
        orderId: orderId, exceptionType: exceptionType, description: description
      });
    },
    // 我的异常工单
    myExceptions: function (page, size) {
      return $axios.get('/api/rider-exception/mine', { params: { page: page, size: size } });
    },
    // 可转单的在线骑手
    availableRiders: function () {
      return $axios.get('/api/rider-exception/available-riders');
    },
    // 转单给其他骑手
    transfer: function (id, newRiderId) {
      return $axios.post('/api/rider/tasks/' + id + '/transfer', {}, { params: { newRiderId: newRiderId } });
    },

    // ---- 骑手消息中心（修改点 P0-4）----
    // 我的消息
    myMessages: function (page, size) {
      return $axios.get('/api/rider-message/mine', { params: { page: page, size: size } });
    },
    // 未读消息数
    unreadCount: function () {
      return $axios.get('/api/rider-message/unread-count');
    },
    // 标记单条已读
    markRead: function (id) {
      return $axios.put('/api/rider-message/' + id + '/read');
    },
    // 全部标记已读
    markAllRead: function () {
      return $axios.put('/api/rider-message/read-all');
    }
  };
  win.RiderApi = RiderApi;
})(window);
