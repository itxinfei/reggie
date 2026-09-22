(function (doc, win) {
    var docEl = doc.documentElement,
        resizeEvt = 'orientationchange' in win ? 'orientationchange' : 'resize';

    // 核心：html fontSize = 参照宽度 / 375，375 视口下 1rem = 1px。
    function recalc() {
        var clientWidth = docEl.clientWidth;
        var clientHeight = win.innerHeight || docEl.clientHeight;
        if (!clientWidth) return;
        // 横屏（宽 > 高）按短边（高）参照，避免手机旋转后宽边参与把字体整屏放大；
        // 竖屏按宽度。桌面宽屏短边通常仍大于 750，封顶行为与原方案一致。
        var refSize = clientHeight && clientWidth > clientHeight ? clientHeight : clientWidth;
        // 桌面端把“手机视口”宽度封顶到 750px，避免整页按比例爆炸；
        // 配合 index.css 中容器的 max-width + margin:auto，呈现为居中的“手机”预览。
        var designWidth = Math.min(refSize, 750);
        docEl.style.fontSize = (designWidth / 375) + 'px';
    }

    // rAF 节流：resize / 旋转期间每帧最多重算一次
    var ticking = false;
    function requestRecalc() {
        if (ticking) return;
        ticking = true;
        if (win.requestAnimationFrame) {
            win.requestAnimationFrame(function () { ticking = false; recalc(); });
        } else {
            ticking = false;
            recalc();
        }
    }

    if (!doc.addEventListener) return;
    // 脚本在 <head> 同步加载：解析到此处立即设置根字号（此时 clientWidth 即可取视口宽），
    // 不必等到 DOMContentLoaded，消除 CSS 先按默认字号渲染导致的首屏布局跳动。
    recalc();
    win.addEventListener(resizeEvt, requestRecalc, false);
    win.addEventListener('resize', requestRecalc, false);
    doc.addEventListener('DOMContentLoaded', recalc, false); // 兜底
})(document, window);
