// 图片路径转换
function imgPath(path){
    return '/common/download?name=' + path
}

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
    '4': '已完成', '5': '已取消', '6': '已退款'
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

