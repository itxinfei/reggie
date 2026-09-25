// Generates domain-a-dish.sql (菜品基础域 seed) — run: node scripts/gen-domain-a-seed.js
const fs = require('fs');
const path = require('path');

const T = "'2026-09-01 09:00:00'"; // base time for 基础资料

function esc(s) { return s.replace(/'/g, "''"); }

// ---------- 1. category ----------
const categories = [
  [1, 1, '热菜', 1], [2, 1, '凉菜', 2], [3, 1, '素菜', 3], [4, 1, '汤羹', 4],
  [5, 1, '主食', 5], [6, 1, '饮品', 6], [7, 1, '甜品小吃', 7],
  [8, 2, '招牌套餐', 1], [9, 2, '商务套餐', 2], [10, 2, '家庭套餐', 3],
];

// ---------- 2. dish ----------
// [id, name, category_id, price, image, description, status]
const dishes = [
  [1,  '红烧肉',      1, '38.00', 'images/dishes/hongshaorou.jpg',          '肥而不腻，入口即化，佐饭佳品', 1],
  [2,  '宫保鸡丁',    1, '26.00', 'images/dishes/gongbaojiding.jpg',        '鸡丁配花生米干辣椒爆炒，酸甜微辣', 1],
  [3,  '鱼香肉丝',    1, '24.00', 'images/dishes/yuxiangrousi.jpg',         '猪肉丝配木耳笋丝，鱼香味浓，经典川菜', 1],
  [4,  '辣子鸡',      1, '32.00', 'images/dishes/laziji.jpg',               '鸡丁与大量干红辣椒爆炒，酥香麻辣', 1],
  [5,  '糖醋里脊',    1, '34.00', 'images/dishes/tangculiji.jpg',           '里脊裹糖醋汁，外酥里嫩', 1],
  [6,  '水煮牛肉',    1, '42.00', 'public/common/dishes/shuizhuniurou.jpg', '嫩滑牛肉片，麻辣红油，香气十足', 1],
  [7,  '回锅肉',      1, '28.00', 'public/common/dishes/huiguorou.jpg',     '五花肉配青蒜豆瓣酱，川菜之魂', 1],
  [8,  '毛血旺',      1, '38.00', 'public/common/dishes/maoxuewang.jpg',    '鸭血毛肚午餐肉，麻辣鲜香一大盆', 1],
  [9,  '剁椒鱼头',    1, '48.00', 'public/common/dishes/duojiaoyutou.jpg',  '鳙鱼头铺剁椒蒸制，鲜辣开胃', 1],
  [10, '农家小炒肉',  1, '26.00', 'public/common/dishes/nongjiaxiaochorou.jpg', '五花肉配青红椒，家常湘味', 1],
  [11, '糖醋排骨',    1, '36.00', 'public/common/dishes/tangcupaigu.jpg',   '小排烧糖醋，酸甜入味，下饭一绝', 1],
  [12, '可乐鸡翅',    1, '28.00', 'public/common/dishes/kelejichi.jpg',     '鸡翅与可乐同烧，色泽红亮甜香', 1],
  [13, '小炒黄牛肉',  1, '42.00', 'public/common/dishes/xiaochaohuangniurou.jpg', '黄牛肉配小米辣快炒，鲜嫩下饭', 1],
  [14, '香辣炸鸡翅',  1, '26.00', 'public/common/dishes/xianglazhajichi.jpg', '鸡翅炸至金黄，香辣多汁', 0],
  [15, '水煮鱼',      1, '48.00', 'public/common/dishes/shuizhuyu.jpg',     '草鱼片麻辣水煮，鲜嫩爽滑', 1],
  [16, '红烧茄子',    3, '18.00', 'public/common/dishes/hongshaoqiezi.jpg', '茄子过油红烧，软糯咸香', 1],
  [17, '干扁豆角',    3, '18.00', 'public/common/dishes/ganbiandoujiao.jpg','豆角干煸至虎皮，配肉末香辣', 1],
  [18, '清炒时蔬',    3, '16.00', 'public/common/dishes/qingchaoshishu.jpg','当季绿叶菜清炒，清淡解腻', 1],
  [19, '蒜蓉西兰花',  3, '20.00', 'images/dishes/suarongxilanhua.jpg',      '西兰花配蒜蓉清炒，健康营养', 1],
  [20, '地三鲜',      3, '22.00', 'public/common/dishes/disanxian.jpg',     '土豆茄子青椒，东北经典家常素菜', 1],
  [21, '木须肉',      3, '22.00', 'public/common/dishes/muxurou.jpg',       '猪肉鸡蛋木耳黄瓜同炒，营养丰富', 1],
  [22, '酸辣土豆丝',  3, '14.00', 'public/common/dishes/suanlatudousi.jpg', '土豆丝爽脆，酸辣开胃', 1],
  [23, '香菇青菜',    3, '16.00', 'public/common/dishes/xiangguqingcai.jpg','小青菜配鲜香菇，清淡爽口', 1],
  [24, '干煸四季豆',  3, '18.00', 'public/common/dishes/ganbiansijidou.jpg','四季豆煸至起皱，咸香下饭', 1],
  [25, '手撕包菜',    3, '16.00', 'public/common/dishes/shousibaocai.jpg',  '包菜手撕爆炒，爽脆清甜', 1],
  [26, '口水鸡',      2, '28.00', 'images/dishes/koushuiji.jpg',            '红油芝麻凉拌鸡，麻辣鲜香', 1],
  [27, '夫妻肺片',    2, '30.00', 'images/dishes/fuqifeipian.jpg',          '牛肉牛杂麻辣凉拌，成都名菜', 1],
  [28, '老醋花生',    2, '16.00', 'images/dishes/laocuhuasheng.jpg',        '花生米老醋浸泡，酸甜开胃', 1],
  [29, '凉拌黄瓜',    2, '12.00', 'images/dishes/liangbanghuanggua.jpg',    '黄瓜凉拌，蒜香爽脆', 1],
  [30, '泡椒黄瓜',    2, '12.00', 'images/dishes/paohuanggua.jpg',          '黄瓜泡椒腌制，酸辣脆爽', 1],
  [31, '拍黄瓜',      2, '12.00', 'public/common/dishes/paihuanggua.jpg',   '黄瓜拍碎凉拌，蒜香爽脆', 0],
  [32, '凉拌木耳',    2, '16.00', 'public/common/dishes/liangbanmuer.jpg',  '木耳凉拌，爽脆可口', 1],
  [33, '皮蛋豆腐',    2, '14.00', 'public/common/dishes/pidandoufu.jpg',    '内酯豆腐配皮蛋，浇汁清凉嫩滑', 1],
  [34, '凉拌牛肉',    2, '28.00', 'public/common/dishes/liangbanniurou.jpg','卤牛肉切片凉拌，鲜香有嚼劲', 1],
  [35, '番茄鸡蛋汤',  4, '12.00', 'images/dishes/fanqiejidantang.jpg',      '番茄鸡蛋，酸甜暖胃', 1],
  [36, '西红柿蛋汤',  4, '12.00', 'images/dishes/xihongshidantang.jpg',     '西红柿配蛋花，清淡鲜香', 1],
  [37, '西湖牛肉羹',  4, '22.00', 'images/dishes/xihuniurougeng.jpg',       '牛肉末豆腐香菇，鲜香滑嫩', 1],
  [38, '紫菜蛋花汤',  4, '12.00', 'public/common/dishes/zicaidanhuatang.jpg','紫菜蛋花，鲜香清淡', 1],
  [39, '酸辣汤',      4, '16.00', 'public/common/dishes/suanlatang.jpg',    '酸辣开胃，料足暖胃', 1],
  [40, '玉米排骨汤',  4, '28.00', 'public/common/dishes/yumipaitang.jpg',   '排骨玉米慢炖，清甜滋补', 1],
  [41, '蛋炒饭',      5, '14.00', 'images/dishes/danchaofan.jpg',           '鸡蛋米饭快炒，粒粒分明', 1],
  [42, '扬州炒饭',    5, '18.00', 'images/dishes/yangzhouchaofan.jpg',      '火腿虾仁青豆鸡蛋，配料丰富', 1],
  [43, '牛肉面',      5, '22.00', 'images/dishes/niuroumian.jpg',           '牛肉卤汁配手擀面，汤浓肉烂', 1],
  [44, '阳春面',      5, '14.00', 'public/common/dishes/yangchunmian.jpg',  '清汤光面，葱花猪油，简单鲜美', 1],
  [45, '米饭',        5, '3.00',  'public/common/dishes/mifan.jpg',         '东北大米蒸制，香糯可口', 1],
  [46, '小笼包',      5, '16.00', 'images/dishes/xiaolongbao.jpg',          '灌汤小笼包，一笼八个', 1],
  [47, '猪肉水饺',    5, '18.00', 'public/common/dishes/zhuroushuijiao.jpg','现包水饺，皮薄馅大', 1],
  [48, '葱油饼',      5, '10.00', 'public/common/dishes/congyoubing.jpg',   '葱油手抓饼，层层酥脆', 1],
  [49, '柠檬水',      6, '8.00',  'images/dishes/ningmengshui.jpg',         '鲜柠檬冰镇，酸甜解渴', 1],
  [50, '珍珠奶茶',    6, '12.00', 'public/common/dishes/zhenzhunaicha.jpg', '香浓奶茶配黑糖珍珠', 1],
  [51, '鲜榨橙汁',    6, '12.00', 'public/common/dishes/xianzhachengzhi.jpg','橙子鲜榨，维C满满', 1],
  [52, '可乐',        6, '6.00',  'public/common/dishes/kele.jpg',          '冰镇可乐，气泡畅爽', 1],
  [53, '豆浆',        6, '6.00',  'public/common/dishes/doujiang.jpg',      '现磨豆浆，香浓醇厚', 1],
  [54, '酸梅汤',      6, '8.00',  'public/common/dishes/suanmeitang.jpg',   '古法熬制酸梅汤，生津解渴', 1],
  [55, '薯条',        7, '10.00', 'images/dishes/shutiao.jpg',              '金黄炸薯条，配番茄酱', 1],
  [56, '鸡米花',      7, '14.00', 'images/dishes/jimihua.jpg',              '炸鸡米花，外酥里嫩', 1],
  [57, '春卷',        7, '12.00', 'public/common/dishes/chunjuan.jpg',      '炸春卷，金黄酥脆', 1],
  [58, '南瓜饼',      7, '12.00', 'public/common/dishes/nanguabing.jpg',    '糯米南瓜饼，外酥里糯甜香', 0],
  [59, '烤羊肉串',    7, '8.00',  'images/dishes/kaoyangrouchuan.jpg',      '羊肉串炭火烤制，孜然飘香', 1],
  [60, '杨枝甘露',    7, '18.00', 'images/dishes/yangzhiganlu.jpg',         '芒果柚子西米露，清甜港式甜品', 1],
];

const dishById = {};
dishes.forEach(d => { dishById[d[0]] = { name: d[1], price: d[3] }; });

// ---------- 3. setmeal ----------
// [id, name, category_id, price, image, code, description]
const setmeals = [
  [1,  '工作餐A套餐',     8, '25.00',  'images/dishes/setmeal-gongzuo.jpg', 'SM001', '一荤一素一汤一饭，午餐实惠之选'],
  [2,  '工作餐B套餐',     8, '25.00',  'images/dishes/setmeal-gongzuo.jpg', 'SM002', '宫保鸡丁配米饭和汤，吃饱吃好'],
  [3,  '商务尊享套餐',    9, '58.00',  'images/dishes/setmeal-haohua.jpg',  'SM003', '红烧肉加素菜汤饭，商务待客有面子'],
  [4,  '商务精选套餐',    9, '48.00',  'images/dishes/setmeal-haohua.jpg',  'SM004', '两荤一素配汤饭，商务简餐精选'],
  [5,  '闺蜜双人餐',      8, '68.00',  'images/dishes/setmeal-guimi.jpg',   'SM005', '双主菜配小食饮品，闺蜜分享刚好'],
  [6,  '情侣双人餐',      8, '78.00',  'images/dishes/setmeal-guimi.jpg',   'SM006', '鱼头双人分享，配凉菜米饭果汁'],
  [7,  '亲子家庭餐',      10, '88.00', 'images/dishes/setmeal-qinzi.jpg',   'SM007', '两荤两小吃配饮品，一家三口爱吃'],
  [8,  '欢聚3-4人餐',     10, '128.00','images/dishes/setmeal-qinzi.jpg',   'SM008', '三荤两素配汤和米饭，全家欢聚共享'],
  [9,  '上午茶套餐',      9, '32.00',  'images/dishes/setmeal-shangwu.jpg', 'SM009', '小笼包配豆浆葱油饼，早午茶点心'],
  [10, '下午茶套餐',      9, '32.00',  'images/dishes/setmeal-shangwu.jpg', 'SM010', '甜品小吃配奶茶，下午茶好时光'],
  [11, '单人盖浇饭套餐',  8, '22.00',  'images/dishes/setmeal-gongzuo.jpg', 'SM011', '一份盖浇饭配酸梅汤，单人管饱'],
  [12, '营养炖汤套餐',    10, '38.00', 'images/dishes/setmeal-haohua.jpg',  'SM012', '炖汤配时蔬米饭，养生暖胃'],
];

// setmeal_dish: [setmeal_id, dish_id, copies]
const setmealDish = [
  [1, 10, 1], [1, 45, 1], [1, 35, 1],
  [2, 2, 1], [2, 45, 1], [2, 38, 1],
  [3, 1, 1], [3, 19, 1], [3, 45, 1], [3, 38, 1],
  [4, 5, 1], [4, 21, 1], [4, 45, 1], [4, 35, 1],
  [5, 3, 1], [5, 13, 1], [5, 50, 2], [5, 55, 1],
  [6, 9, 1], [6, 26, 1], [6, 45, 2], [6, 51, 2],
  [7, 12, 1], [7, 5, 1], [7, 55, 1], [7, 56, 1], [7, 49, 2], [7, 45, 2],
  [8, 1, 1], [8, 6, 1], [8, 4, 1], [8, 20, 1], [8, 39, 1], [8, 45, 4],
  [9, 46, 1], [9, 53, 1], [9, 48, 1],
  [10, 60, 1], [10, 55, 1], [10, 50, 1],
  [11, 3, 1], [11, 45, 1], [11, 54, 1],
  [12, 40, 1], [12, 45, 1], [12, 18, 1],
];

// ---------- 4. dish_flavor: [dish_id, name, [options...]] ----------
const flavors = [
  [1, '份量', ['小份', '大份']],
  [2, '辣度', ['不辣', '微辣', '中辣']],
  [2, '加料', ['加花生', '免花生']],
  [3, '辣度', ['不辣', '微辣', '中辣']],
  [4, '辣度', ['不辣', '微辣', '中辣', '特辣']],
  [5, '份量', ['小份', '大份']],
  [6, '辣度', ['微辣', '中辣', '特辣']],
  [7, '辣度', ['不辣', '微辣', '中辣']],
  [8, '辣度', ['中辣', '特辣']],
  [9, '辣度', ['微辣', '中辣', '特辣']],
  [10, '辣度', ['不辣', '微辣', '中辣', '特辣']],
  [11, '份量', ['小份', '大份']],
  [12, '份量', ['小份', '大份']],
  [13, '辣度', ['微辣', '中辣', '特辣']],
  [13, '做法', ['免香菜', '正常', '加辣']],
  [14, '辣度', ['微辣', '中辣', '特辣']],
  [15, '辣度', ['微辣', '中辣', '特辣']],
  [17, '辣度', ['不辣', '微辣', '中辣']],
  [18, '做法', ['少油', '正常']],
  [20, '做法', ['免辣椒', '正常']],
  [21, '做法', ['少油', '正常']],
  [22, '辣度', ['不辣', '微辣', '中辣', '特辣']],
  [26, '辣度', ['微辣', '中辣', '特辣']],
  [27, '辣度', ['微辣', '中辣', '特辣']],
  [28, '份量', ['小份', '大份']],
  [30, '份量', ['小份', '大份']],
  [33, '做法', ['常温', '冰镇']],
  [34, '辣度', ['微辣', '中辣']],
  [39, '加料', ['加豆腐', '加蛋花']],
  [40, '份量', ['小份', '大份']],
  [41, '加料', ['加蛋', '加火腿', '加虾仁']],
  [42, '加料', ['加虾仁', '加叉烧']],
  [43, '份量', ['小份', '大份']],
  [43, '加料', ['加牛肉', '加蛋']],
  [44, '做法', ['汤面', '干拌']],
  [45, '份量', ['一碗', '两碗']],
  [46, '份量', ['一笼(8个)', '两笼(16个)']],
  [47, '份量', ['12个', '18个', '24个']],
  [49, '甜度', ['少糖', '半糖', '标准糖']],
  [49, '温度', ['常温', '冰镇']],
  [49, '冰量', ['去冰', '少冰', '多冰']],
  [50, '甜度', ['三分糖', '五分糖', '七分糖', '全糖']],
  [50, '加料', ['珍珠', '椰果', '布丁']],
  [51, '甜度', ['不加糖', '微糖']],
  [52, '温度', ['常温', '冰镇']],
  [53, '温度', ['热', '常温', '冰']],
  [54, '甜度', ['少糖', '标准糖']],
  [55, '加料', ['加芝士', '正常']],
  [56, '份量', ['小份', '大份']],
  [57, '份量', ['4个', '6个']],
  [59, '辣度', ['不辣', '微辣', '中辣', '特辣']],
  [60, '甜度', ['少糖', '半糖', '标准糖']],
  [60, '加料', ['加西柚', '加芒果']],
];

// ---------- 5. dish_spec_group ----------
// [id, name, type, required, max_select, sort_order, status, remark]
const specGroups = [
  [1,  '份量',     1, 1, 1, 1, 1, '大份小份可选，默认小份'],
  [2,  '辣度',     1, 0, 1, 2, 1, '按口味选择辣度'],
  [3,  '麻度',     1, 0, 1, 3, 1, '麻度可选'],
  [4,  '甜度',     1, 0, 1, 4, 1, '饮品甜度可选'],
  [5,  '温度',     1, 0, 1, 5, 1, '冷热可选'],
  [6,  '冰量',     1, 0, 1, 6, 1, '加冰量可选'],
  [7,  '主食加料', 2, 0, 2, 7, 1, '炒饭面食可加配料'],
  [8,  '做法',     1, 0, 1, 8, 1, '少油等做法可选'],
  [9,  '免忌',     2, 0, 3, 9, 1, '忌口配料可选'],
  [10, '打包方式', 1, 0, 1, 10, 1, '堂食或打包'],
  [11, '餐具份数', 1, 0, 1, 11, 1, '按就餐人数配餐具'],
  [12, '米饭份量', 1, 0, 1, 12, 1, '米饭可选份量'],
  [13, '面量',     1, 0, 1, 13, 1, '面条可选份量'],
  [14, '饮品加料', 2, 0, 2, 14, 1, '奶茶等饮品可加配料'],
];

// ---------- 6. dish_spec_option: [group_id, name, price_adjust, sort_order] ----------
const specOptions = [
  [1, '小份', '0.00', 1], [1, '中份', '2.00', 2], [1, '大份', '4.00', 3],
  [2, '不辣', '0.00', 1], [2, '微辣', '0.00', 2], [2, '中辣', '0.00', 3], [2, '特辣', '0.00', 4],
  [3, '微麻', '0.00', 1], [3, '中麻', '0.00', 2], [3, '特麻', '0.00', 3],
  [4, '无糖', '0.00', 1], [4, '少糖', '0.00', 2], [4, '半糖', '0.00', 3], [4, '标准糖', '0.00', 4],
  [5, '常温', '0.00', 1], [5, '冰镇', '0.00', 2], [5, '热饮', '0.00', 3],
  [6, '去冰', '0.00', 1], [6, '少冰', '0.00', 2], [6, '多冰', '0.00', 3],
  [7, '加蛋', '2.00', 1], [7, '加火腿', '3.00', 2], [7, '加虾仁', '8.00', 3], [7, '加牛肉', '6.00', 4],
  [8, '少油', '0.00', 1], [8, '正常', '0.00', 2], [8, '免辣', '0.00', 3],
  [9, '免葱', '0.00', 1], [9, '免蒜', '0.00', 2], [9, '免香菜', '0.00', 3],
  [10, '堂食装', '0.00', 1], [10, '打包装', '0.00', 2],
  [11, '1份', '0.00', 1], [11, '2份', '0.00', 2], [11, '3份', '0.00', 3],
  [12, '一碗', '0.00', 1], [12, '两碗', '2.00', 2],
  [13, '小份', '0.00', 1], [13, '大份', '2.00', 2],
  [14, '珍珠', '3.00', 1], [14, '椰果', '3.00', 2], [14, '布丁', '3.00', 3],
];

// ---------- 7. dish_spec_relation: [dish_id, group_id] ----------
const specRelations = [
  [1, 1], [1, 9], [2, 1], [2, 2], [3, 1], [3, 2], [4, 2], [4, 3], [5, 1],
  [6, 2], [6, 3], [7, 2], [8, 2], [8, 3], [9, 2], [10, 1], [10, 2], [11, 1],
  [12, 1], [13, 1], [13, 2], [13, 8], [14, 2], [15, 2], [15, 3],
  [16, 1], [16, 8], [17, 2], [18, 8], [19, 1], [20, 1], [20, 9], [21, 1],
  [22, 2], [22, 8], [23, 1], [24, 1], [24, 2], [25, 1], [25, 8],
  [26, 2], [26, 3], [27, 2], [27, 3], [27, 9], [28, 1], [29, 1], [29, 9],
  [30, 1], [31, 1], [31, 9], [32, 1], [32, 9], [33, 5], [34, 1], [34, 2],
  [35, 1], [36, 1], [37, 1], [38, 1], [39, 2], [39, 7], [40, 1],
  [41, 1], [41, 7], [42, 1], [42, 7], [43, 13], [43, 7], [44, 13], [44, 8],
  [45, 12], [46, 1], [47, 1], [48, 1],
  [49, 4], [49, 5], [49, 6], [50, 4], [50, 6], [50, 14], [51, 4], [51, 6],
  [52, 5], [52, 6], [53, 5], [54, 4], [54, 5], [54, 6],
  [55, 1], [56, 1], [57, 1], [58, 1], [59, 2], [59, 9], [60, 4], [60, 6],
];

// ---------- 8. dish_material: [dish_id, material_id, usage_qty, sort] ----------
const dishMaterials = [
  [1, 1, 0.300, 1], [1, 32, 0.020, 2], [1, 33, 0.030, 3],
  [2, 6, 0.250, 1], [2, 12, 0.050, 2], [2, 16, 0.010, 3],
  [3, 2, 0.250, 1], [3, 20, 0.050, 2], [3, 16, 0.010, 3],
  [4, 5, 0.300, 1], [4, 16, 0.060, 2],
  [5, 2, 0.300, 1], [5, 32, 0.030, 2], [5, 34, 0.020, 3],
  [6, 4, 0.300, 1], [6, 16, 0.050, 2],
  [7, 1, 0.300, 1], [7, 30, 0.020, 2], [7, 15, 0.050, 3],
  [9, 10, 0.600, 1], [9, 16, 0.080, 2],
  [13, 4, 0.250, 1], [13, 16, 0.020, 2],
  [15, 9, 0.600, 1], [15, 16, 0.060, 2],
  [16, 14, 0.400, 1], [16, 30, 0.030, 2],
  [19, 18, 0.350, 1], [19, 30, 0.050, 2],
  [20, 13, 0.200, 1], [20, 14, 0.200, 2], [20, 15, 0.100, 3],
  [22, 13, 0.350, 1], [22, 34, 0.020, 2], [22, 16, 0.010, 3],
  [21, 6, 0.150, 1], [21, 8, 2.000, 2], [21, 20, 0.050, 3], [21, 19, 0.100, 4],
  [26, 5, 0.350, 1], [26, 12, 0.030, 2],
  [40, 3, 0.300, 1], [40, 22, 0.300, 2],
  [41, 8, 2.000, 1], [41, 23, 0.250, 2],
  [45, 23, 0.250, 1],
  [50, 38, 0.300, 1], [50, 39, 0.050, 2],
];

// ---------- build SQL ----------
const lines = [];
lines.push('SET NAMES utf8mb4;');

// category
lines.push('DELETE FROM `category`;');
lines.push('INSERT INTO `category` (`id`,`type`,`name`,`sort`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`) VALUES');
const catVals = categories.map(c => `(${c[0]},${c[1]},'${esc(c[2])}',${c[3]},1,${T},${T},1,1,0)`);
lines.push(catVals.join(',\n') + ';');

// dish
lines.push('DELETE FROM `dish`;');
lines.push('INSERT INTO `dish` (`id`,`name`,`category_id`,`price`,`code`,`image`,`description`,`status`,`sort`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`,`stock_qty`,`min_stock`) VALUES');
const dishVals = dishes.map(d => {
  const id = d[0], price = d[3], status = d[6];
  let stock = '200.00', min = '30.00';
  if (id === 45) { stock = '1000.00'; min = '200.00'; }
  else if (id >= 49 && id <= 54) { stock = '150.00'; min = '20.00'; }
  else if (id >= 35 && id <= 40) { stock = '120.00'; min = '20.00'; }
  const code = 'D' + String(id).padStart(3, '0');
  return `(${id},'${esc(d[1])}',${d[2]},${price},'${code}','${esc(d[4])}','${esc(d[5])}',${status},${id},1,${T},${T},1,1,0,${stock},${min})`;
});
lines.push(dishVals.join(',\n') + ';');

// dish_flavor
lines.push('DELETE FROM `dish_flavor`;');
lines.push('INSERT INTO `dish_flavor` (`id`,`dish_id`,`name`,`value`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`) VALUES');
let fid = 1;
const flavorVals = flavors.map(f => {
  const j = JSON.stringify(f[2]);
  return `(${fid++},${f[0]},'${esc(f[1])}','${j}',1,${T},${T},1,1,0)`;
});
lines.push(flavorVals.join(',\n') + ';');

// setmeal
lines.push('DELETE FROM `setmeal`;');
lines.push('INSERT INTO `setmeal` (`id`,`category_id`,`name`,`price`,`status`,`code`,`description`,`image`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`) VALUES');
const smVals = setmeals.map(s => `(${s[0]},${s[2]},'${esc(s[1])}',${s[3]},1,'${s[5]}','${esc(s[6])}','${esc(s[4])}',1,${T},${T},1,1,0)`);
lines.push(smVals.join(',\n') + ';');

// setmeal_dish
lines.push('DELETE FROM `setmeal_dish`;');
lines.push('INSERT INTO `setmeal_dish` (`id`,`setmeal_id`,`dish_id`,`name`,`price`,`copies`,`sort`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`) VALUES');
let sdid = 1;
const sdVals = setmealDish.map(sd => {
  const d = dishById[sd[1]];
  return `(${sdid++},${sd[0]},${sd[1]},'${esc(d.name)}',${d.price},${sd[2]},${sd[2]},1,${T},${T},1,1,0)`;
});
lines.push(sdVals.join(',\n') + ';');

// dish_cost
lines.push('DELETE FROM `dish_cost`;');
lines.push('INSERT INTO `dish_cost` (`id`,`dish_id`,`dish_name`,`material_cost`,`labor_cost`,`other_cost`,`total_cost`,`sale_price`,`profit_rate`,`remark`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES');
let dcid = 1;
const dcVals = dishes.map(d => {
  const price = parseFloat(d[3]);
  const m = +(price * 0.40).toFixed(2);
  const l = +(price * 0.13).toFixed(2);
  const o = +(price * 0.07).toFixed(2);
  const total = +(m + l + o).toFixed(2);
  const pr = +((price - total) / price * 100).toFixed(2);
  return `(${dcid++},${d[0]},'${esc(d[1])}',${m.toFixed(2)},${l.toFixed(2)},${o.toFixed(2)},${total.toFixed(2)},${price.toFixed(2)},${pr.toFixed(2)},'按标准份量核算',1,${T},${T},1,1)`;
});
lines.push(dcVals.join(',\n') + ';');

// dish_spec_group
lines.push('DELETE FROM `dish_spec_group`;');
lines.push('INSERT INTO `dish_spec_group` (`id`,`name`,`type`,`required`,`max_select`,`sort_order`,`status`,`remark`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES');
const sgVals = specGroups.map(g => `(${g[0]},'${esc(g[1])}',${g[2]},${g[3]},${g[4]},${g[5]},${g[6]},'${esc(g[7])}',1,${T},${T},1,1)`);
lines.push(sgVals.join(',\n') + ';');

// dish_spec_option
lines.push('DELETE FROM `dish_spec_option`;');
lines.push('INSERT INTO `dish_spec_option` (`id`,`group_id`,`name`,`price_adjust`,`sort_order`,`status`,`remark`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES');
let oid = 1;
const soVals = specOptions.map(o => `(${oid++},${o[0]},'${esc(o[1])}',${o[2]},${o[3]},1,NULL,1,${T},${T},1,1)`);
lines.push(soVals.join(',\n') + ';');

// dish_spec_relation
lines.push('DELETE FROM `dish_spec_relation`;');
lines.push('INSERT INTO `dish_spec_relation` (`id`,`dish_id`,`group_id`,`sort_order`,`tenant_id`,`create_time`,`create_user`) VALUES');
let rid = 1;
let relSort = {};
const srVals = specRelations.map(r => {
  relSort[r[0]] = (relSort[r[0]] || 0) + 1;
  return `(${rid++},${r[0]},${r[1]},${relSort[r[0]]},1,${T},1)`;
});
lines.push(srVals.join(',\n') + ';');

// dish_material
lines.push('DELETE FROM `dish_material`;');
lines.push('INSERT INTO `dish_material` (`id`,`tenant_id`,`dish_id`,`material_id`,`usage_qty`,`sort`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`) VALUES');
let mid = 1;
const dmVals = dishMaterials.map(m => `(${mid++},1,${m[0]},${m[1]},${m[2].toFixed(3)},${m[3]},${T},${T},1,1,0)`);
lines.push(dmVals.join(',\n') + ';');

lines.push('');
const out = lines.join('\n');
const outPath = path.join(__dirname, '..', 'src', 'main', 'resources', 'db', 'seed', 'domain-a-dish.sql');
fs.writeFileSync(outPath, out, 'utf8');

console.log('wrote', outPath);
console.log('rows:', {
  category: categories.length,
  dish: dishes.length,
  dish_flavor: flavors.length,
  setmeal: setmeals.length,
  setmeal_dish: setmealDish.length,
  dish_cost: dishes.length,
  spec_group: specGroups.length,
  spec_option: specOptions.length,
  spec_relation: specRelations.length,
  dish_material: dishMaterials.length,
});