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

// 订单状态映射
function getStatus(status) {
    var statusMap = {
        1: '待付款',
        2: '正在派送',
        3: '已派送',
        4: '已完成',
        5: '已取消'
    };
    return statusMap[status] || '';
}

