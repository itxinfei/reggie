/**
 * 骑手位置定时上报。
 * 骑手在线的页面（index/hall/detail）引入并 RiderLocation.start() 后，
 * 每 12 秒读取一次 GPS 并 POST 到 /api/rider/location，供顾客端配送追踪使用。
 *
 * 坐标口径：浏览器 geolocation 返回 WGS-84，全系统地图使用高德 GCJ-02，
 * 上报前在此完成转换，否则顾客端看到的骑手位置会有几十~几百米偏移。
 *
 * 降级：非安全上下文（内网 HTTP 且非 localhost）下浏览器不提供 navigator.geolocation，
 * 此时静默不启动，不报错、不影响接单主流程；待站点升级 HTTPS 后自动生效。
 */
(function (win) {
  var INTERVAL_MS = 12000;

  // ---- WGS-84 → GCJ-02（国测局火星坐标，标准转换算法）----
  var PI = 3.1415926535897932384626;
  var A = 6378245.0;
  var EE = 0.00669342162296594323;

  function outOfChina(lng, lat) {
    return (lng < 72.004 || lng > 137.8347) || (lat < 0.8293 || lat > 55.8271);
  }

  function transformLat(lng, lat) {
    var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat +
      0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
    ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
    ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
    return ret;
  }

  function transformLng(lng, lat) {
    var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng +
      0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
    ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
    ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
    return ret;
  }

  function wgs84ToGcj02(lng, lat) {
    // 国境外坐标不转换
    if (outOfChina(lng, lat)) { return [lng, lat]; }
    var dLat = transformLat(lng - 105.0, lat - 35.0);
    var dLng = transformLng(lng - 105.0, lat - 35.0);
    var radLat = lat / 180.0 * PI;
    var magic = Math.sin(radLat);
    magic = 1 - EE * magic * magic;
    var sqrtMagic = Math.sqrt(magic);
    dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
    dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
    return [lng + dLng, lat + dLat];
  }

  // ---- 在线状态标志（由工作台上下线开关维护，hall/detail 页读取）----
  function isOnline() {
    // 无标志时默认在线，保证登录后各页行为与历史一致
    try { return sessionStorage.getItem('riderOnline') !== '0'; } catch (e) { return true; }
  }

  function setOnlineFlag(v) {
    try { sessionStorage.setItem('riderOnline', v ? '1' : '0'); } catch (e) { /* 忽略 */ }
  }

  var RiderLocation = {
    _timer: null,

    start: function () {
      // 浏览器不支持定位（如内网HTTP）→ 静默降级
      if (!navigator.geolocation) { return; }
      // 已知离线（工作台下线后进入 hall/detail）：不启动定时器
      if (!isOnline()) { return; }
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

    // 供工作台开关调用：下线时停止上报，省电且避免无效请求
    setEnabled: function (online) {
      setOnlineFlag(online);
      if (online) { this.start(); } else { this.stop(); }
    },

    _reportOnce: function () {
      // 骑手离线：跳过本轮，不耗电上报
      if (!isOnline()) { return; }
      navigator.geolocation.getCurrentPosition(function (position) {
        // 等待定位期间骑手可能已下线
        if (!isOnline()) { return; }
        var coords = position.coords;
        var gcj = wgs84ToGcj02(coords.longitude, coords.latitude);
        // 上报失败静默（下次定时自动重试），避免 Toast 打扰骑手正常作业
        $axios.post('/api/rider/location', {
          longitude: gcj[0],
          latitude: gcj[1],
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
