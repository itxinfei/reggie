/**
 * 骑手位置定时上报。
 * 骑手在线的页面（index/hall/detail）引入并 RiderLocation.start() 后，
 * 每 12 秒读取一次 GPS 并 POST 到 /api/rider/location，供顾客端配送追踪使用。
 *
 * 降级：非安全上下文（内网 HTTP 且非 localhost）下浏览器不提供 navigator.geolocation，
 * 此时静默不启动，不报错、不影响接单主流程；待站点升级 HTTPS 后自动生效。
 */
(function (win) {
  var INTERVAL_MS = 12000;

  var RiderLocation = {
    _timer: null,

    start: function () {
      // 浏览器不支持定位（如内网HTTP）→ 静默降级
      if (!navigator.geolocation) { return; }
      if (this._timer) { return; }  // 防重复启动
      var self = this;
      var report = function () { self._reportOnce(); };
      report();  // 进页面立即上报一次，不必等首个间隔
      this._timer = setInterval(report, INTERVAL_MS);
    },

    stop: function () {
      if (this._timer) {
        clearInterval(this._timer);
        this._timer = null;
      }
    },

    _reportOnce: function () {
      navigator.geolocation.getCurrentPosition(function (position) {
        var coords = position.coords;
        // 上报失败静默（下次定时自动重试），避免 Toast 打扰骑手正常作业
        $axios.post('/api/rider/location', {
          longitude: coords.longitude,
          latitude: coords.latitude,
          speed: coords.speed,
          heading: coords.heading
        }).catch(function () { /* 忽略，下轮重试 */ });
      }, function () {
        // 定位失败（拒绝授权/信号弱）：静默等待下轮，不弹错误
      }, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 5000
      });
    }
  };

  win.RiderLocation = RiderLocation;
})(window);
