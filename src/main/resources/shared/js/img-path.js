// 三端共享图片路径工具（单一真源）。
// 权限语义：public/ 前缀免登录静态直出；private/ 与旧相对路径走 /common/download 鉴权。
// 三入口（backend/js/components.js、front/js/common.js、rider/js/util.js）文件头同步注入本文件。
(function (global) {
  if (typeof global.imgPath === 'function') return; // 幂等，防重复注入

  function imgPathPlaceholder() {
    var p = (typeof location !== 'undefined' && location.pathname) ? location.pathname : '';
    if (p.indexOf('/backend/') === 0) return '/backend/images/noImg.png';
    if (p.indexOf('/rider/') === 0) return '/front/images/noImg.png'; // rider 暂无自有占位图，复用 front
    return '/front/images/noImg.png';
  }

  function imgPath(path) {
    if (!path) return imgPathPlaceholder();
    if (/^https?:\/\//i.test(path)) return path;
    if (path.indexOf('/common/download') === 0) return path;
    if (path.charAt(0) === '/') return path;
    if (path.indexOf('public/') === 0) return '/uploads/' + path;
    return '/common/download?name=' + encodeURIComponent(path);
  }

  global.imgPath = imgPath;
  global.imgPathPlaceholder = imgPathPlaceholder;
})(typeof window !== 'undefined' ? window : this);
