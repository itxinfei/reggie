/**
 * RemixIcon 字体加载兜底（2026-09-07）
 *
 * 背景（必须保留此说明，防回退）：
 *   styles/remixicon-fallback.css 用 emoji Unicode 作为 ::before 内容做兜底。
 *   若它与真字体 remixicon.css 同时加载且在其后，会**覆盖**真字体的 ::before，
 *   导致全站 ri-* 图标渲染为彩色 emoji（违反"C端禁止 emoji 作图标"规范）。
 *
 * 现行策略：
 *   1. 页面只加载真字体 remixicon.css；fallback 不再静态引入；
 *   2. 本脚本检测 remixicon 字体是否真的加载成功；
 *   3. 仅当确认加载失败时，才动态注入 fallback 样式（此时 emoji 兜底是可接受的降级）。
 *
 * 无全局污染：整体 IIFE，不定义任何全局变量。
 */
(function () {
    /** 兜底样式路径：page/*.html → ../styles/ ；根目录 index.html → styles/ */
    function fallbackCssPath() {
        return /\/page\/[^/]*\.html/.test(window.location.pathname)
            ? '../styles/remixicon-fallback.css'
            : 'styles/remixicon-fallback.css';
    }

    /** 动态注入兜底样式（幂等） */
    function loadFallback() {
        if (document.getElementById('remixiconFallbackCss')) return;
        var link = document.createElement('link');
        link.id = 'remixiconFallbackCss';
        link.rel = 'stylesheet';
        link.href = fallbackCssPath();
        document.head.appendChild(link);
    }

    function detect() {
        // 不支持 FontFace API：无法判定，保持真字体（宁可少兜底，也不要误注入 emoji）
        if (!document.fonts || !document.fonts.load) return;
        try {
            // 使用 RemixIcon 私有区字符触发真实加载，避免 check() 因字体未使用而恒 false
            document.fonts.load('1em remixicon', '\uea01').then(function () {
                var ok = true;
                try {
                    ok = document.fonts.check('1em remixicon');
                } catch (e) {
                    ok = true;
                }
                if (!ok) loadFallback();
            }, function () {
                loadFallback();
            });
        } catch (e) {
            loadFallback();
        }
    }

    detect();
})();
