// 三端共享 imgPath（单一真源，幂等注入）：必须先于下方 mixin 装配
document.write('<script src="/shared/js/img-path.js?v=20260924"><\/script>');

// 读取 :root 上的 CSS 自定义属性值。JS 配置无法直接消费 CSS 变量的场景
// （如 Vant Dialog 的 confirmButtonColor）用它取令牌，保持单一事实来源
function cssVar(name){
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

// 跳登录页并携带回跳地址（当前路径含查询串），登录成功后由 login.html 跳回原页面，保留操作意图
function goLogin(){
    window.location.href = '/front/page/login.html?redirect='
        + encodeURIComponent(window.location.pathname + window.location.search);
}

// 全站加载的是开发版 vue.js：模板编译为 with(_renderProxy){…}，而 vm 的 has 拦截器对
// 未在实例上声明的标识符返回 true 且不再回退 window，导致模板裸调 window.imgPath/cssVar
// 时拿到 undefined（imgPath is not a function，整段 render 中断）。
// 通过全局 mixin 把这两个工具注入为所有实例的方法，模板即可直接调用；幂等防重复安装。
// 已评价商品集合（供订单列表/详情判断「去评价」入口是否还应显示）。
// key 规则与 my-evaluations.html 一致：orderId_dishId（菜品）、orderId_s_setmealId（套餐）。
// 一次拉取后在本页缓存；评价提交后其他页面重新加载时自然取到最新集合。
var reggieEvalKeysPromise = null;
function loadEvalKeys(force) {
    if (!force && reggieEvalKeysPromise) return reggieEvalKeysPromise;
    reggieEvalKeysPromise = new Promise(function (resolve) {
        if (typeof $axios !== 'function') { resolve(new Set()); return; }
        $axios({ url: '/api/dish-evaluation/user/my', method: 'get',
                 params: { page: 1, pageSize: 200 } })
            .then(function (res) {
                var set = new Set();
                if (res && res.code === 1 && res.data && res.data.records) {
                    res.data.records.forEach(function (r) {
                        if (r.orderId && r.dishId) {
                            set.add(r.orderId + '_' + r.dishId);
                        } else if (r.orderId && r.setmealId) {
                            set.add(r.orderId + '_s_' + r.setmealId);
                        }
                    });
                }
                resolve(set);
            })
            .catch(function () { resolve(new Set()); });
    });
    return reggieEvalKeysPromise;
}

// 图片加载失败统一兜底：dataset 标记保证只兜底一次，
// 避免兜底图本身也失败时 error 事件被无限触发（内联 $event.target.src=... 写法有此风险）
function imgFallback(e) {
    var el = e && e.target;
    if (!el || el.dataset.imgFallback) return;
    el.dataset.imgFallback = '1';
    el.src = '/front/images/noImg.png';
}

// 静默恢复用户会话：sessionStorage 按 tab 隔离，新标签页/复制链接打开时本地登录态会丢失，
// 但服务端 cookie 会话可能仍有效。以 /user/info?full=1 探测并回填本地缓存。
// 返回 Promise<boolean>：true=已具备有效登录态（原有或恢复成功）；false=真实未登录。
// 纯 GET 探测 + skipAuthRedirect，不会触发 NOTLOGIN 强制跳转，游客页面可安全调用。
function restoreUserSession() {
    var phone = '';
    try { phone = sessionStorage.getItem('userPhone') || ''; } catch (e) {}
    if (/^1\d{10}$/.test(phone)) { return Promise.resolve(true); }
    if (typeof $axios !== 'function') { return Promise.resolve(false); }
    return $axios({ url: '/user/info', method: 'get', params: { full: 1 },
                    skipAuthRedirect: true, silent: true })
        .then(function (r) {
            var u = r && r.data;
            // 仅接受完整手机号，脱敏/异常号不回填，避免污染本地登录态
            if (r.code === 1 && u && /^1\d{10}$/.test(u.phone)) {
                try {
                    sessionStorage.setItem('userPhone', u.phone);
                    if (u.id) { sessionStorage.setItem('userId', u.id); }
                    if (u.name) { sessionStorage.setItem('userName', u.name); }
                    if (u.avatar) { sessionStorage.setItem('userAvatar', u.avatar); }
                } catch (e) {}
                return true;
            }
            return false;
        })
        .catch(function () { return false; });
}

function installReggieVueHelpers(){
    if (!window.Vue || window.Vue.__reggieHelpersInstalled) return;
    window.Vue.mixin({ methods: {
        imgPath: function (p) { return imgPath(p); },
        imgFallback: imgFallback,
        cssVar: cssVar,
        // 订单是否还有未评价商品；依赖实例数据 evalKeys（页面 created 用 loadEvalKeys 装配）
        canEvaluateOrder: function (order) {
            if (!order || !order.orderDetails || !this.evalKeys) return false;
            var keys = this.evalKeys;
            return order.orderDetails.some(function (d) {
                if (!d || (!d.dishId && !d.setmealId)) return false;
                var key = d.dishId ? (order.id + '_' + d.dishId)
                                   : (order.id + '_s_' + d.setmealId);
                return !keys.has(key);
            });
        }
    } });
    window.Vue.__reggieHelpersInstalled = true;
}
// common.js 在 vue.js 之后加载的页面：到此 Vue 已就绪，立即安装
installReggieVueHelpers();

// 将url传参转换为对象（支持中文参数自动解码）
function parseUrl(url) {
    // 修改点：防御性处理，URL中无?时返回空对象
    var queryIndex = url.indexOf("?");
    if (queryIndex === -1) return {};
    var parse = url.substring(queryIndex + 1),
        params = parse.split("&"),
        len = params.length,
        item = [],
        param = {};

    for (var i = 0; i < len; i++) {
        item = params[i].split("=");
        if (item[0]) {
            // 修改点：使用decodeURIComponent解码中文参数
            param[item[0]] = item[1] ? decodeURIComponent(item[1]) : '';
        }
    }

    return param;
}

// 获取送达时间（当前时间+1小时）
function getFinishTime() {
    var now = new Date();
    // 修改点：修复23点后显示24:xx的溢出Bug，对24取模
    var hour = (now.getHours() + 1) % 24;
    var minute = now.getMinutes();
    if (hour < 10) {
        hour = '0' + hour;
    }
    if (minute < 10) {
        minute = '0' + minute;
    }
    return hour + ':' + minute;
}

// ============ 枚举字典（后端唯一真源） ============
// 背景：订单状态此前两端各写一套且互相矛盾——后台 order/list.html 用 0 基、
// 本文件用 1 基但缺"已退款"且 2/3 语义与后端相反、order.html 的 Tab 又与本文件相反。
// 现统一由后端 /api/meta/enums 下发；接口不可用时回退到下方兜底常量（值与后端一致）。
var ENUM_DICT = {};

// 订单状态兜底（严格对齐 com.reggie.enums.OrderStatus：1 基）
var ORDER_STATUS_FALLBACK = {
    '1': '待付款', '2': '待接单', '3': '配送中',
    '4': '已完成', '5': '已取消', '6': '已退款', '7': '已分账'
};

/**
 * 拉取后端枚举字典。各页面初始化时调用一次即可，失败不影响页面（走兜底）。
 */
function loadEnums(callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', '/api/meta/enums', true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.onreadystatechange = function () {
        if (xhr.readyState !== 4) return;
        try {
            if (xhr.status === 200) {
                var res = JSON.parse(xhr.responseText);
                if (res && String(res.code) === '1' && res.data) {
                    ENUM_DICT = toEnumMapDict(res.data);
                }
            }
        } catch (e) {
            console.warn('枚举字典加载失败，使用本地兜底', e);
        }
        if (typeof callback === 'function') callback();
    };
    xhr.send();
}

/** 将 {名: [{code,label}]} 转为 {名: {'1':'待付款', ...}}，code 统一转字符串便于比较 */
function toEnumMapDict(data) {
    var out = {};
    for (var key in data) {
        if (!Object.prototype.hasOwnProperty.call(data, key)) continue;
        var items = data[key], map = {};
        if (Object.prototype.toString.call(items) === '[object Array]') {
            for (var i = 0; i < items.length; i++) {
                var it = items[i];
                if (it && it.code !== undefined && it.code !== null) map[String(it.code)] = it.label;
            }
        }
        out[key] = map;
    }
    return out;
}

// 订单状态映射（优先取后端字典，回退兜底常量）
function getStatus(status) {
    var dict = ENUM_DICT.orderStatus || {};
    var val = dict[String(status)];
    if (val) return val;
    return ORDER_STATUS_FALLBACK[String(status)] || '';
}

// ============ 统一返回兜底（全局） ============
// 所有页面的 goBack 方法统一调用此函数，避免各页面自行处理 sessionStorage/referrer 判断不一致
function goBack() {
    if (history.length > 1) {
        history.go(-1);
    } else {
        window.location.href = '/front/index.html';
    }
}

