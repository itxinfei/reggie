/**
 * 骑手端接口封装。所有方法返回 Promise，resolve 值为后端统一响应体（取业务数据用 .data）。
 */
(function (win) {
  var RiderApi = {
    // ---- 鉴权 ----
    login: function (phone, password) {
      return $axios.post('/api/rider/login', { phone: phone, password: password });
    },
    logout: function () {
      return $axios.post('/api/rider/logout');
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
    pickup: function (id) {
      return $axios.post('/api/rider/tasks/' + id + '/pickup');
    },
    deliver: function (id) {
      return $axios.post('/api/rider/tasks/' + id + '/deliver');
    }
  };
  win.RiderApi = RiderApi;
})(window);
