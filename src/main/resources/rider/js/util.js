// 三端共享 imgPath（单一真源，幂等注入）：必须先于本文件所有 Vue 组件定义
document.write('<script src="/shared/js/img-path.js?v=20260924"><\/script>');
/**
 * 骑手端通用工具：时间/金额格式化、高德导航 URI、状态文案。
 */
(function (win) {
  function pad(n) { return (n < 10 ? '0' : '') + n; }

  /**
   * 兼容两种 LocalDateTime 序列化结果：
   * ISO 字符串 "2026-09-23T12:00:00" 或 Jackson 数组 [2026,9,23,12,0]。
   */
  function formatTime(t) {
    if (t == null || t === '') return '';
    if (typeof t === 'string') {
      return t.replace('T', ' ').substring(0, 16);
    }
    if (Array.isArray(t)) {
      // [年,月,日,时,分[,秒]]
      var a = t;
      var s = a[0] + '-' + pad(a[1]) + '-' + pad(a[2]) + ' ' + pad(a[3] || 0) + ':' + pad(a[4] || 0);
      return s;
    }
    return String(t);
  }

  function money(n) {
    var v = Number(n);
    return (isNaN(v) ? 0 : v).toFixed(2);
  }

  /** 高德导航 URI（坐标 GCJ-02，与本系统一致）；坐标缺失时返回空串。 */
  function navUrl(lng, lat, name) {
    if (lng == null || lat == null) return '';
    var n = encodeURIComponent(name || '目的地');
    return 'https://uri.amap.com/navigation?to=' + lng + ',' + lat + ',' + n +
      '&mode=car&src=reggie&coordinate=gaode&callnative=1';
  }

  function statusText(s) {
    switch (s) {
      case 2: return '待接单';
      case 3: return '配送中';
      case 4: return '已完成';
      case 5: return '已取消';
      case 6: return '已退款';
      default: return '未知';
    }
  }

  win.RiderUtil = {
    formatTime: formatTime,
    money: money,
    navUrl: navUrl,
    statusText: statusText
  };
})(window);
