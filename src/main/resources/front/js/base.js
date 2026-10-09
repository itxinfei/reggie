(function (doc, win) {
    var docEl = doc.documentElement,
        resizeEvt = 'orientationchange' in win ? 'orientationchange' : 'resize';

    // 核心：html fontSize = 参照宽度 / 375，375 视口下 1rem = 1px。
    function recalc() {
        var clientWidth = docEl.clientWidth;
        var clientHeight = win.innerHeight || docEl.clientHeight;
        if (!clientWidth) return;
        // 桌面 1:1 口径 opt-in：页面在 <html> 标注 data-rem-flat 后，
        // 宽屏（>=751）时 1rem=1px。收口原页内 @media(min-width:751px)
        // font-size:1px !important 覆盖；未标注页面维持原有封顶口径，不受影响。
        if (docEl.hasAttribute('data-rem-flat') && clientWidth >= 751) {
            docEl.style.fontSize = '1px';
            return;
        }
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

/* ============================================================
 * 品牌动态化（R-21-A 配套 · 店铺品牌化，2026-09-30）
 * 本文件被全端页面（/front/index.html 与 /front/page/*.html）加载，
 * 在此一处把写死的"瑞吉外卖"替换为真实店铺名（/restaurant/info 匿名可访问）。
 * 策略：sessionStorage 缓存 60 秒 → 接口失败静默（保留写死文案兜底）。
 * 说明：TTL 曾为 10 分钟，商家在"店铺设置"改名后顾客端最长 10 分钟不更新，
 *       与"即时生效"冲突；缩短为 60 秒，兼顾接口请求量与改名时效。
 * ============================================================ */
(function (doc, win) {
    var BRAND_CACHE_KEY = 'reggieBrandInfo';
    var BRAND_CACHE_TTL = 60 * 1000;
    var DEFAULT_BRAND = '瑞吉外卖';

    function readCache() {
        try {
            var raw = win.sessionStorage.getItem(BRAND_CACHE_KEY);
            if (!raw) return null;
            var data = JSON.parse(raw);
            if (!data || !data.name || !data.ts || (Date.now() - data.ts > BRAND_CACHE_TTL)) {
                return null;
            }
            return data;
        } catch (e) { return null; }
    }

    function writeCache(data) {
        try { win.sessionStorage.setItem(BRAND_CACHE_KEY, JSON.stringify(data)); } catch (e) { /* 隐私模式等场景忽略 */ }
    }

    // 相对路径规范化为上下文绝对路径（/front/page/ 下的页面按相对路径引用图片会 404）
    function normalizeLogo(url) {
        if (!url) return null;
        if (/^https?:\/\//.test(url) || url.charAt(0) === '/') return url;
        return '/' + url;
    }

    function applyBrand(info) {
        if (!info || !info.name || info.name === DEFAULT_BRAND) return;
        // 1) 标题：各页 <title> 均含写死的"瑞吉外卖"，运行时替换，免逐页改文件
        if (doc.title && doc.title.indexOf(DEFAULT_BRAND) !== -1) {
            doc.title = doc.title.split(DEFAULT_BRAND).join(info.name);
        }
        // 2) 头部店名/Logo（元素存在才替换，防御式；首页主体由 Vue 读取 /restaurant/info 数据驱动）
        var nameEl = doc.querySelector('.rc-name');
        if (nameEl && nameEl.textContent.indexOf(DEFAULT_BRAND) !== -1) {
            nameEl.textContent = info.name;
        }
        var logoUrl = normalizeLogo(info.logo);
        if (logoUrl) {
            var logoEl = doc.querySelector('.rc-logo');
            if (logoEl && logoEl.tagName === 'IMG') {
                logoEl.setAttribute('src', logoUrl);
            }
        }
    }

    function loadBrand() {
        var cached = readCache();
        if (cached) { applyBrand(cached); return; }
        if (typeof win.fetch !== 'function') return;
        try {
            win.fetch('/restaurant/info', { credentials: 'same-origin' })
                .then(function (res) { return res.ok ? res.json() : null; })
                .then(function (json) {
                    if (!json || json.code !== 1 || !json.data) return;
                    var data = { name: json.data.name, logo: json.data.logo, ts: Date.now() };
                    writeCache(data);
                    applyBrand(data);
                })
                .catch(function () { /* 静默：网络异常保留写死文案兜底 */ });
        } catch (e) { /* 静默 */ }
    }

    if (doc.title && doc.title.indexOf(DEFAULT_BRAND) !== -1) {
        loadBrand();
        // Vue 渲染的首页店名可能晚于本次替换，DOM 就绪后再兜底一次
        if (doc.readyState === 'loading') {
            doc.addEventListener('DOMContentLoaded', function () {
                var cached = readCache();
                if (cached) { applyBrand(cached); }
            }, false);
        }
    }
})(document, window);
