(function (doc, win) {
    var docEl = doc.documentElement,
        resizeEvt = 'orientationchange' in window ? 'orientationchange' : 'resize',
        recalc = function () {
            var clientWidth = docEl.clientWidth;
            if (!clientWidth) return;
            // 桌面端：把“手机视口”宽度封顶到 750px，避免整页按比例爆炸；
            // 配合 index.css 中 #main 的 max-width + margin:auto，呈现为居中的“手机”预览。
            var designWidth = Math.min(clientWidth, 750);
            docEl.style.fontSize = (designWidth / 375) + 'px';
        };

    if (!doc.addEventListener) return;
    win.addEventListener(resizeEvt, recalc, false);
    doc.addEventListener('DOMContentLoaded', recalc, false);
})(document, window);