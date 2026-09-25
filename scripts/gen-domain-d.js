// 临时生成器：D域（订单履约支付域）真实数据 SQL
// 金额全用整数分计算，避免浮点误差。run: node gen-domain-d.js > ../src/main/resources/db/seed/domain-d-order.sql
'use strict';
const fs = require('fs');

// ---------- 目录 ----------
const DISH = { // id -> {name, price(cents), cat, img, spicy}
  1:['红烧肉',3800,1,'images/dishes/hongshaorou.jpg',0],2:['宫保鸡丁',2600,1,'images/dishes/gongbaojiding.jpg',1],
  3:['鱼香肉丝',2400,1,'images/dishes/yuxiangrousi.jpg',1],4:['辣子鸡',3200,1,'images/dishes/laziji.jpg',1],
  5:['糖醋里脊',3400,1,'images/dishes/tangculiji.jpg',0],6:['水煮牛肉',4200,1,'public/common/dishes/shuizhuniurou.jpg',1],
  7:['回锅肉',2800,1,'public/common/dishes/huiguorou.jpg',1],8:['毛血旺',3800,1,'public/common/dishes/maoxuewang.jpg',1],
  9:['剁椒鱼头',4800,1,'public/common/dishes/duojiaoyutou.jpg',1],10:['农家小炒肉',2600,1,'public/common/dishes/nongjiaxiaochorou.jpg',1],
  11:['糖醋排骨',3600,1,'public/common/dishes/tangcupaigu.jpg',0],12:['可乐鸡翅',2800,1,'public/common/dishes/kelejichi.jpg',0],
  13:['小炒黄牛肉',4200,1,'public/common/dishes/xiaochaohuangniurou.jpg',1],14:['香辣炸鸡翅',2600,1,'public/common/dishes/xianglazhajichi.jpg',1],
  15:['水煮鱼',4800,1,'public/common/dishes/shuizhuyu.jpg',1],16:['红烧茄子',1800,3,'public/common/dishes/hongshaoqiezi.jpg',0],
  17:['干扁豆角',1800,3,'public/common/dishes/ganbiandoujiao.jpg',1],18:['清炒时蔬',1600,3,'public/common/dishes/qingchaoshishu.jpg',0],
  19:['蒜蓉西兰花',2000,3,'images/dishes/suarongxilanhua.jpg',0],20:['地三鲜',2200,3,'public/common/dishes/disanxian.jpg',0],
  21:['木须肉',2200,3,'public/common/dishes/muxurou.jpg',0],22:['酸辣土豆丝',1400,3,'public/common/dishes/suanlatudousi.jpg',1],
  23:['香菇青菜',1600,3,'public/common/dishes/xiangguqingcai.jpg',0],24:['干煸四季豆',1800,3,'public/common/dishes/ganbiansijidou.jpg',1],
  25:['手撕包菜',1600,3,'public/common/dishes/shousibaocai.jpg',1],26:['口水鸡',2800,2,'images/dishes/koushuiji.jpg',1],
  27:['夫妻肺片',3000,2,'images/dishes/fuqifeipian.jpg',1],28:['老醋花生',1600,2,'images/dishes/laocuhuasheng.jpg',0],
  29:['凉拌黄瓜',1200,2,'images/dishes/liangbanghuanggua.jpg',0],30:['泡椒黄瓜',1200,2,'images/dishes/paohuanggua.jpg',1],
  31:['拍黄瓜',1200,2,'public/common/dishes/paihuanggua.jpg',0],32:['凉拌木耳',1600,2,'public/common/dishes/liangbanmuer.jpg',0],
  33:['皮蛋豆腐',1400,2,'public/common/dishes/pidandoufu.jpg',0],34:['凉拌牛肉',2800,2,'public/common/dishes/liangbanniurou.jpg',1],
  35:['番茄鸡蛋汤',1200,4,'images/dishes/fanqiejidantang.jpg',0],36:['西红柿蛋汤',1200,4,'images/dishes/xihongshidantang.jpg',0],
  37:['西湖牛肉羹',2200,4,'images/dishes/xihuniurougeng.jpg',0],38:['紫菜蛋花汤',1200,4,'public/common/dishes/zicaidanhuatang.jpg',0],
  39:['酸辣汤',1600,4,'public/common/dishes/suanlatang.jpg',1],40:['玉米排骨汤',2800,4,'public/common/dishes/yumipaitang.jpg',0],
  41:['蛋炒饭',1400,5,'images/dishes/danchaofan.jpg',0],42:['扬州炒饭',1800,5,'images/dishes/yangzhouchaofan.jpg',0],
  43:['牛肉面',2200,5,'images/dishes/niuroumian.jpg',0],44:['阳春面',1400,5,'public/common/dishes/yangchunmian.jpg',0],
  45:['米饭',300,5,'public/common/dishes/mifan.jpg',0],46:['小笼包',1600,5,'images/dishes/xiaolongbao.jpg',0],
  47:['猪肉水饺',1800,5,'public/common/dishes/zhuroushuijiao.jpg',0],48:['葱油饼',1000,5,'public/common/dishes/congyoubing.jpg',0],
  49:['柠檬水',800,6,'images/dishes/ningmengshui.jpg',0],50:['珍珠奶茶',1200,6,'public/common/dishes/zhenzhunaicha.jpg',0],
  51:['鲜榨橙汁',1200,6,'public/common/dishes/xianzhachengzhi.jpg',0],52:['可乐',600,6,'public/common/dishes/kele.jpg',0],
  53:['豆浆',600,6,'public/common/dishes/doujiang.jpg',0],54:['酸梅汤',800,6,'public/common/dishes/suanmeitang.jpg',0],
  55:['薯条',1000,7,'images/dishes/shutiao.jpg',0],56:['鸡米花',1400,7,'images/dishes/jimihua.jpg',0],
  57:['春卷',1200,7,'public/common/dishes/chunjuan.jpg',0],58:['南瓜饼',1200,7,'public/common/dishes/nanguabing.jpg',0],
  59:['烤羊肉串',800,7,'images/dishes/kaoyangrouchuan.jpg',0],60:['杨枝甘露',1800,7,'images/dishes/yangzhiganlu.jpg',0],
};
const SET = { // id -> [name, price(cents), img]
  1:['工作餐A套餐',2500,'images/dishes/setmeal-gongzuo.jpg'],2:['工作餐B套餐',2500,'images/dishes/setmeal-gongzuo.jpg'],
  3:['商务尊享套餐',5800,'images/dishes/setmeal-haohua.jpg'],4:['商务精选套餐',4800,'images/dishes/setmeal-haohua.jpg'],
  5:['闺蜜双人餐',6800,'images/dishes/setmeal-guimi.jpg'],6:['情侣双人餐',7800,'images/dishes/setmeal-guimi.jpg'],
  7:['亲子家庭餐',8800,'images/dishes/setmeal-qinzi.jpg'],8:['欢聚3-4人餐',12800,'images/dishes/setmeal-qinzi.jpg'],
  9:['上午茶套餐',3200,'images/dishes/setmeal-shangwu.jpg'],10:['下午茶套餐',3200,'images/dishes/setmeal-shangwu.jpg'],
  11:['单人盖浇饭套餐',2200,'images/dishes/setmeal-gongzuo.jpg'],12:['营养炖汤套餐',3800,'images/dishes/setmeal-haohua.jpg'],
};
const USER = {1:['林晓','13800010001'],2:['黄志强','13800010002'],3:['苏婉婷','13800010003'],4:['高建军','13800010004'],
  5:['钱多多','13800010005'],6:['周子涵','13800010006'],7:['吴彩霞','13800010007'],8:['郑国豪','13800010008'],
  9:['王美琳','13800010009'],10:['冯磊','13800010010'],11:['褚静怡','13800010011'],12:['卫青','13800010012'],
  13:['蒋欣怡','13800010013'],14:['沈浩然','13800010014'],15:['韩雪梅','13800010015'],16:['杨光','13800010016'],
  17:['朱思远','13800010017'],18:['秦雨萱','13800010018'],19:['许文博','13800010019'],20:['何丽君','13800010020'],
  21:['吕鹏','13800010021'],22:['施佳琪','13800010022'],23:['张沐晨','13800010023'],24:['孔雅静','13800010024'],
  25:['曹睿','13800010025']};
const ADDR = {1:'望京街道阜通东大街6号院3号楼2单元501',2:'中关村大街甲12号4号楼601',3:'三里屯工体北路21号3单元802',
  4:'学院路30号院家属区7号楼502',5:'朝阳公园南路8号院2号楼1101',6:'建国路88号院SOHO现代城B座1908',
  7:'中关村软件园二期和颐家园5号楼302',8:'望京SOHO塔2A座1606',9:'北四环西路58号理想大厦1203',10:'光华路SOHO2期A座2105',
  11:'西二旗北路领秀新硅谷6号楼901',12:'回龙观东大街龙腾苑五区3号楼704',13:'天通苑中苑12号楼2单元1002',14:'上地信息路上地佳园9号楼805',
  15:'亚运村安慧里三区8号楼503',16:'蓝色港湾国际商区亮马名居4号楼1201',17:'大屯路东金泉家园2号楼602',18:'常营天街天阶公馆1号楼1505',
  19:'双井富力城南区5号楼1902',20:'劲松中街劲松嘉园7号楼903',21:'潘家园南里东里2号楼404',22:'十里堡北里晨光家园6号楼1106',
  23:'百子湾家园中区8号楼1302',24:'甘露园中里朝阳园3号楼705',25:'定福庄北街福怡苑1号楼1001'};
const TABLE = {21:['C01',8],22:['C02',8],23:['C03',8],24:['C04',8],25:['C05',8],26:['D01',6],27:['D02',6],28:['D03',6],29:['E01',2],30:['E02',2]};
const RIDER = {1:['张伟','13810000001'],2:['李磊','13810000002'],3:['王强','13810000003'],4:['刘洋','13810000004'],
  5:['陈刚','13810000005'],6:['杨军','13810000006'],7:['赵鹏','13810000007'],8:['黄勇','13810000008'],
  9:['周涛','13810000009'],10:['吴斌','13810000010']};
const CASHIER = {7:['孙丽华'],8:['周敏'],14:['徐文静']};
const MD5 = 'e10adc3949ba59abbe56e057f20f883e';
const STORE_LNG = 116.461000, STORE_LAT = 39.917000;

function money(c){ return (c/100).toFixed(2); }
function esc(s){ return s == null ? 'NULL' : '\'' + String(s).replace(/'/g,'\'\'') + '\''; }
function n(v){ return v == null ? 'NULL' : (typeof v === 'number' ? v : v); }
function fmt(v, dec){ return v == null ? 'NULL' : Number(v).toFixed(dec==null?2:dec); }
function addMin(dt, m){ // dt 'YYYY-MM-DD HH:MM:SS'
  let [d,t]=dt.split(' '); let [y,mo,da]=d.split('-').map(Number); let [h,mi,s]=t.split(':').map(Number);
  let date = new Date(y, mo-1, da, h, mi + m, s);
  function p(x){return String(x).padStart(2,'0');}
  return date.getFullYear()+'-'+p(date.getMonth()+1)+'-'+p(date.getDate())+' '+p(date.getHours())+':'+p(date.getMinutes())+':'+p(date.getSeconds());
}

// ---------- 订单规格 ----------
// o:[id, dine(0/1), status, userId, 'YYYY-MM-DD HH:MM', items[[kind,id,qty]], fullRed(cents), newRed(cents), dist(m)|0, remark|null, tableId|null, payMethod|null]
// payMethod: 1现金 2微信 3支付宝; null=未支付(待付款/取消)
const OD = [
[1,0,4,1,'2026-08-25 11:42:00',[['d',1,1],['d',22,1],['d',45,2]],0,0,1800,'微辣，米饭软一点',null,2],
[2,0,4,2,'2026-08-25 18:15:00',[['d',2,1],['d',5,1],['d',38,1],['d',45,1]],0,0,2400,null,null,3],
[3,0,4,3,'2026-08-26 12:05:00',[['d',3,1],['d',16,1],['d',45,1]],0,0,1500,'少放辣',null,2],
[4,0,4,4,'2026-08-27 11:38:00',[['d',11,1],['d',18,1],['d',45,2],['d',54,1]],0,0,3200,null,null,2],
[5,0,4,5,'2026-08-28 19:22:00',[['d',15,1],['d',45,2],['d',52,1]],0,0,2800,'尽快送达',null,3],
[6,0,4,6,'2026-09-01 11:50:00',[['s',2,1],['d',49,1]],0,0,1200,null,null,2],
[7,0,4,7,'2026-09-02 18:33:00',[['d',7,1],['d',24,1],['d',45,1],['d',38,1]],0,0,4100,'多放餐具',null,2],
[8,0,4,8,'2026-09-03 12:20:00',[['d',8,1],['d',45,1],['d',53,1]],0,0,3600,null,null,3],
[9,0,4,9,'2026-09-04 11:45:00',[['d',10,1],['d',17,1],['d',45,1]],0,0,1900,'不要香菜',null,2],
[10,0,4,10,'2026-09-05 18:08:00',[['d',13,1],['d',20,1],['d',45,1],['d',50,1]],1000,0,5200,null,null,2],
[11,0,4,11,'2026-09-05 12:12:00',[['d',4,1],['d',45,2],['d',54,1]],0,0,1600,null,null,3],
[12,0,4,12,'2026-09-06 19:40:00',[['d',9,1],['d',19,1],['d',45,2]],0,0,4600,'剁椒少放一点',null,2],
[13,0,4,13,'2026-09-08 11:55:00',[['s',1,1]],0,0,1300,null,null,2],
[14,0,4,14,'2026-09-08 18:26:00',[['d',6,1],['d',23,1],['d',45,2]],0,0,5900,null,null,3],
[15,0,4,15,'2026-09-09 12:30:00',[['d',12,1],['d',22,1],['d',45,1],['d',51,1]],0,0,2100,null,null,2],
[16,0,4,16,'2026-09-09 19:05:00',[['d',5,1],['d',16,1],['d',35,1],['d',45,1]],500,0,3400,'口味偏淡',null,3],
[17,0,4,17,'2026-09-10 11:32:00',[['d',43,1],['d',33,1]],0,0,1500,null,null,2],
[18,0,4,18,'2026-09-10 18:50:00',[['d',8,1],['d',31,1],['d',45,1],['d',52,1]],0,0,4400,null,null,2],
[19,0,4,19,'2026-09-11 12:08:00',[['s',3,1]],0,0,2500,null,null,3],
[20,0,4,20,'2026-09-11 13:05:00',[['d',11,1],['d',21,1],['d',45,2],['d',38,1]],1000,0,6200,'公司开票，不要辣',null,2],
[21,0,4,21,'2026-09-11 18:22:00',[['d',2,1],['d',28,1],['d',45,1]],0,800,2200,null,null,2],
[22,0,4,22,'2026-09-12 11:40:00',[['d',4,1],['d',18,1],['d',45,1],['d',54,1]],0,500,1700,null,null,3],
[23,0,4,23,'2026-09-12 18:36:00',[['d',15,1],['d',25,1],['d',45,2]],1500,0,6800,'主要少刺',null,2],
[24,0,4,24,'2026-09-12 12:25:00',[['s',4,1],['d',49,1]],0,0,1900,null,null,3],
[25,0,4,25,'2026-09-13 11:52:00',[['d',2,1],['d',3,1],['d',45,2],['d',54,1]],0,0,2600,null,null,2],
[26,0,4,1,'2026-09-13 19:18:00',[['s',5,1]],0,0,3000,'两人份多加一份筷',null,3],
[27,0,4,2,'2026-09-13 12:44:00',[['d',41,1],['d',56,1],['d',52,1]],0,0,1400,null,null,2],
[28,1,4,3,'2026-09-14 12:05:00',[['d',1,1],['d',19,1],['d',45,2],['d',38,1]],0,0,0,null,21,1],
[29,1,4,4,'2026-09-14 18:45:00',[['d',9,1],['d',22,1],['d',26,1],['d',45,2]],0,0,0,null,22,2],
[30,1,4,5,'2026-09-14 19:30:00',[['s',6,1],['d',51,2]],0,0,0,'靠窗位置',29,3],
[31,1,4,6,'2026-09-15 12:20:00',[['d',5,1],['d',16,1],['d',45,1]],0,0,0,null,23,2],
[32,1,4,7,'2026-09-15 18:10:00',[['d',11,1],['d',24,1],['d',35,1],['d',45,2]],0,0,0,'排骨少糖',24,2],
[33,1,4,8,'2026-09-16 12:35:00',[['s',11,1]],0,0,0,null,21,3],
[34,1,4,9,'2026-09-16 18:55:00',[['s',8,1],['d',52,2]],0,0,0,'家庭聚餐',25,2],
[35,1,4,10,'2026-09-17 12:15:00',[['d',7,1],['d',18,1],['d',45,1],['d',53,1]],0,0,0,null,22,1],
[36,1,4,11,'2026-09-17 19:22:00',[['d',13,1],['d',29,1],['d',45,1],['d',54,1]],0,0,0,null,23,3],
[37,1,4,12,'2026-09-18 12:40:00',[['d',2,1],['d',21,1],['d',45,2]],0,0,0,null,24,2],
[38,1,4,13,'2026-09-18 18:18:00',[['s',3,1],['d',33,1]],0,0,0,null,21,3],
[39,1,4,14,'2026-09-19 12:50:00',[['d',10,1],['d',32,1],['d',45,1],['d',39,1]],0,0,0,null,22,1],
[40,1,4,15,'2026-09-20 12:08:00',[['d',6,1],['d',23,1],['d',45,2],['d',38,1]],1500,0,0,null,25,2],
[41,0,3,16,'2026-09-21 11:35:00',[['d',4,1],['d',17,1],['d',45,1],['d',52,1]],0,0,2300,'送到放前台',null,2],
[42,0,3,17,'2026-09-22 12:02:00',[['d',5,1],['d',19,1],['d',45,1]],0,0,3100,null,null,3],
[43,0,3,18,'2026-09-23 18:40:00',[['d',8,1],['d',18,1],['d',45,1],['d',50,1]],0,0,3900,'不要葱花',null,2],
[44,0,2,19,'2026-09-24 11:28:00',[['d',2,1],['d',22,1],['d',45,2]],0,0,1600,null,null,2],
[45,0,2,20,'2026-09-24 18:12:00',[['d',12,1],['d',20,1],['d',45,1]],0,0,4200,'尽量快点',null,3],
[46,0,2,21,'2026-09-25 11:06:00',[['s',2,1]],0,500,1200,null,null,2],
[47,0,1,22,'2026-09-25 11:45:00',[['d',3,1],['d',16,1],['d',45,1]],0,0,1800,null,null,null],
[48,0,1,23,'2026-09-25 12:32:00',[['d',41,1],['d',56,1],['d',55,1]],0,500,1400,null,null,null],
[49,1,1,6,'2026-09-24 18:55:00',[['d',11,1],['d',19,1],['d',45,1]],0,0,0,null,21,null],
[50,1,1,24,'2026-09-25 12:10:00',[['d',6,1],['d',45,1]],0,0,0,null,22,null],
[51,0,5,25,'2026-09-23 11:50:00',[['d',43,1],['d',29,1]],0,0,1500,'临时有事',null,null],
[52,0,5,1,'2026-09-24 18:30:00',[['d',2,1],['d',45,2]],0,0,2000,null,null,null],
[53,0,5,2,'2026-09-25 11:15:00',[['d',15,1],['d',45,1]],0,0,2600,'点错了',null,null],
[54,1,5,3,'2026-09-22 18:05:00',[['d',5,1],['d',18,1],['d',45,1]],0,0,0,'改外卖了',23,null],
[55,1,5,4,'2026-09-25 12:40:00',[['d',7,1],['d',45,1]],0,0,0,null,21,null],
[56,0,6,5,'2026-09-15 12:30:00',[['d',6,1],['d',45,1],['d',52,1]],0,0,2800,'牛肉太老退货',null,2],
[57,0,6,6,'2026-09-18 18:45:00',[['d',9,1],['d',45,2]],0,0,4700,'鱼头不新鲜',null,3],
[58,0,6,7,'2026-09-20 12:20:00',[['s',1,1]],0,0,1500,'送错套餐',null,2],
[59,0,6,8,'2026-09-22 11:50:00',[['d',4,1],['d',22,1],['d',45,1]],0,0,1900,'辣子鸡不新鲜退款中',null,2],
[60,1,6,9,'2026-09-16 18:30:00',[['d',13,1],['d',23,1],['d',45,1]],0,0,0,'牛肉有异味',24,3],
];

// ---------- 计算 ----------
const FLAVOR_POOL = ['微辣','微辣','中辣','中辣','少辣','免辣'];
const SPICY = new Set([2,3,4,6,7,8,9,10,13,15,17,22,24,25,26,27,34,39]);
function flavorFor(dishId, seedIdx){
  if(!SPICY.has(dishId)) return null;
  return FLAVOR_POOL[(dishId*31 + seedIdx) % FLAVOR_POOL.length];
}
function feeFor(dist){ if(dist<=1500)return 300; if(dist<=2500)return 400; if(dist<=3500)return 500; if(dist<=4500)return 600; if(dist<=5500)return 700; if(dist<=6500)return 800; return 900; }
function stationFor(cat){ if(cat===2)return 'COLD'; if(cat===6)return 'DRINK'; if(cat===7)return 'DESSERT'; return 'HOT'; }

// 明细生成
let detailId = 1;
const details = []; // {id, orderId, name, img, dishId, setmealId, flavor, qty, amountCents}
const orderRows = [];
const paidOrders = []; // for payment
const deliveryOrders = []; // delivery orders (non pending)
const kitchenOrders = []; // status 2,3,4,6
let orderIdx = 0;

for(const spec of OD){
  const [id, dine, status, userId, dt, items, fullRed, newRed, dist, remark, tableId, payMethod] = spec;
  let subtotal = 0;
  let summaryParts = [];
  let di = 0;
  for(const it of items){
    const [kind, itemId, qty] = it;
    if(kind === 'd'){
      const d = DISH[itemId];
      const fl = flavorFor(itemId, detailId);
      subtotal += d[1]*qty;
      if(fl) summaryParts.push(d[0]+'x'+qty);
      else summaryParts.push(d[0]+'x'+qty);
      details.push({id:detailId, orderId:id, name:d[0], img:d[2], dishId:itemId, setmealId:null, flavor:fl, qty, amountCents:d[1]*qty});
    } else {
      const s = SET[itemId];
      subtotal += s[1]*qty;
      summaryParts.push(s[0]+'x'+qty);
      details.push({id:detailId, orderId:id, name:s[0], img:s[2], dishId:null, setmealId:itemId, flavor:null, qty, amountCents:s[1]*qty});
    }
    detailId++;
  }
  const deliveryFee = dine ? 0 : feeFor(dist);
  const amount = subtotal + deliveryFee - fullRed - newRed;
  if(amount <= 0) throw new Error('order '+id+' 金额非正: '+amount);
  if(subtotal + deliveryFee < fullRed + newRed) throw new Error('order '+id+' 优惠超总额');

  const uname = USER[userId][0], uphone = USER[userId][1];
  const addressText = dine ? null : '北京市朝阳区'+ADDR[userId];
  const tableInfo = dine ? TABLE[tableId] : null; // [name, seats]
  const checkoutTime = (status===1 || status===5) ? null : addMin(dt, 4 + (id%5)); // 已支付才有结账时间
  const riderId = (status===3) ? ((id-41)%6+1) : ((status===2 || (status===4 && !dine)) ? ((id%10)+1) : (status===6 ? ((id%10)+1) : null));
  // 取消单：部分已接单后退单（rider null 即可，简化）取消均 null
  if(status===5) { /* rider null */ }
  orderRows.push({
    id, number: (dine?'IN':'ON') + dt.replace(/[-: ]/g,'').slice(0,12) + String(1000+id%1000).padStart(4,'0'),
    status, userId, addressBookId: 100+userId,
    orderTime: dt, checkoutTime,
    payMethod, amountCents: amount, deliveryFeeCents: dine?0:deliveryFee, fullRed, newRed,
    remark, expect: dine?null:(dist<=3000?'预计30分钟送达':'预计45分钟送达'),
    userName: uname, phone: uphone, address: addressText, consignee: uname,
    dineType: dine?'EAT_IN':'OUTSIDE', tableId: dine?tableId:null, tableName: dine?tableInfo[0]:null,
    riderId: status===5?null:riderId, dispatchTime: (status===3||status===2||status===4&&!dine&&status===4)?addMin(dt,1+(id%4)):null,
    dishSummary: summaryParts.join('；'), items, dist, tableSeats: dine?tableInfo[1]:null, createUser: userId,
  });
  if(status===2||status===3||status===4||status===6){
    paidOrders.push({orderId:id, amountCents:amount, payMethod, orderTime:dt, checkedOut:checkoutTime, refunded:(status===6), status});
    kitchenOrders.push(orderRows[orderRows.length-1]);
  }
  if(!dine && status!==1) deliveryOrders.push(orderRows[orderRows.length-1]);
  orderIdx++;
}

// ---------- 输出 SQL ----------
let out = [];
out.push('SET NAMES utf8mb4;');
out.push('-- 域D：订单履约支付域真实数据（orders 1-60 / order_detail 1-300 等）');
out.push('-- 金额口径：order_detail.amount=单价x数量(行金额)；orders.amount=明细合计+配送费-满减-新客立减');
out.push('-- 依赖：域A(dish/setmeal)、域B(user/address_book)、域C(dining_table)；address_book_id 约定 = 100+user_id');

// orders
out.push('DELETE FROM `orders`;');
out.push('INSERT INTO `orders` (`id`,`number`,`status`,`user_id`,`address_book_id`,`order_time`,`checkout_time`,`pay_method`,`amount`,`delivery_fee`,`full_reduction_amount`,`new_customer_discount_amount`,`remark`,`internal_remark`,`expect_delivery_time`,`user_name`,`phone`,`address`,`consignee`,`dining_type`,`table_id`,`table_name`,`idempotency_key`,`stock_refunded`,`used_coupon_id`,`rider_id`,`dispatch_time`,`platform_type`,`platform_order_id`,`platform_shop_id`,`platform_raw`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`,`tenant_id`,`version`,`master_order_id`,`split_count`) VALUES');
const orderLines = orderRows.map(o=>{
  return '('+[
    o.id, esc(o.number), o.status, o.userId, o.addressBookId,
    esc(o.orderTime), esc(o.checkoutTime), o.payMethod==null?null:o.payMethod,
    money(o.amountCents), o.deliveryFeeCents===0&&o.dineType==='EAT_IN'?money(0):money(o.deliveryFeeCents),
    money(o.fullRed), money(o.newRed), esc(o.remark), null, esc(o.expect),
    esc(o.userName), esc(o.phone), esc(o.address), esc(o.consignee),
    esc(o.dineType), o.tableId, esc(o.tableName),
    null, (o.status===6?1:0), null, o.riderId, esc(o.dispatchTime),
    'SELF', null, null, null,
    esc(o.orderTime), esc(o.checkoutTime||o.orderTime), o.createUser, 1, 0, 1, 1, null, null
  ].join(',')+')';
});
out.push(orderLines.join(',\n')+';');

// order_detail
out.push('');
out.push('DELETE FROM `order_detail`;');
out.push('INSERT INTO `order_detail` (`id`,`name`,`order_id`,`dish_id`,`setmeal_id`,`dish_flavor`,`number`,`amount`,`remark`,`image`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`) VALUES');
const detailLines = details.map(d=>{
  const o = orderRows.find(x=>x.id===d.orderId);
  return '('+[d.id, esc(d.name), d.orderId, d.dishId, d.setmealId, esc(d.flavor), d.qty, money(d.amountCents),
    null, esc(d.img), 1, esc(o.orderTime), esc(o.orderTime), o.createUser, 1, 0].join(',')+')';
});
out.push(detailLines.join(',\n')+';');

// shopping_cart (12 用户当前购物车)
const cartData = [
  [1,'d',2,1],[1,'d',45,2],[2,'s',1,1],[3,'d',4,1],[3,'d',45,2],[4,'d',11,1],
  [5,'d',15,1],[5,'d',45,2],[6,'s',4,1],[7,'d',43,1],[8,'d',8,1],[9,'d',5,1],[9,'d',22,1],
  [10,'s',2,1],[11,'d',3,1],[11,'d',16,1],[12,'d',13,1],[13,'d',41,1],[14,'d',9,1],[15,'d',12,1],[15,'d',45,2],
];
let cartId = 1;
const cartLines = cartData.map((c)=>{
  const [uid, kind, itemId, qty] = c;
  const name = kind==='d'?DISH[itemId][0]:SET[itemId][0];
  const img = kind==='d'?DISH[itemId][2]:SET[itemId][2];
  const price = kind==='d'?DISH[itemId][1]:SET[itemId][1];
  const fl = kind==='d'?flavorFor(itemId, uid+cartId):null;
  const dt = '2026-09-25 '+('10:'+String(20+cartId).padStart(2,'0')+':00');
  const line = '('+[cartId, esc(name), uid, kind==='d'?itemId:null, kind==='d'?null:itemId, esc(fl), qty, money(price*qty), esc(img), 1, esc(dt)].join(',')+')';
  cartId++;
  return line;
});
out.push('');
out.push('DELETE FROM `shopping_cart`;');
out.push('INSERT INTO `shopping_cart` (`id`,`name`,`user_id`,`dish_id`,`setmeal_id`,`dish_flavor`,`number`,`amount`,`image`,`tenant_id`,`create_time`) VALUES');
out.push(cartLines.join(',\n')+';');

// payment_order（已支付订单，除现金堂食外各一条）
let payId = 1;
const payLines = [];
const paymentRef = {}; // orderId -> {payId, tradeNo, amountCents, status}
for(const p of paidOrders){
  const oo = orderRows.find(x=>x.id===p.orderId);
  if(oo.payMethod===1 && oo.dineType==='EAT_IN'){ paymentRef[p.orderId] = null; continue; } // 现金堂食无线上支付单
  const channel = oo.payMethod===3?'ALIPAY':'WECHAT';
  const tradeNo = 'TC'+oo.orderTime.replace(/[-: ]/g,'').slice(0,14)+String(payId).padStart(4,'0');
  const channelTradeNo = channel==='WECHAT' ? '420000'+('531'+String(Math.abs(oo.id*9761+1))).slice(-8)+('000'+payId).slice(-5)
    : '2026'+('0241'+String(Math.abs(oo.id*8713+7))).slice(-8)+('000'+payId).slice(-5);
  const status = p.refunded?'REFUND':'SUCCESS';
  const paidTime = addMin(p.orderTime, 3 + (payId%5));
  paymentRef[p.orderId] = {payId, tradeNo, amountCents:p.amountCents, status};
  payLines.push('('+[payId, p.orderId, 1, esc(tradeNo), esc(channelTradeNo), esc(channel), money(p.amountCents), esc(status),
    esc(paidTime), esc(addMin(paidTime,0)), esc(p.orderTime), esc(paidTime), 0, 1, oo.createUser, oo.createUser].join(',')+')');
  payId++;
}
out.push('');
out.push('DELETE FROM `payment_order`;');
out.push('INSERT INTO `payment_order` (`id`,`order_id`,`tenant_id`,`trade_no`,`channel_trade_no`,`channel`,`amount`,`status`,`paid_time`,`notify_time`,`created_time`,`update_time`,`is_deleted`,`version`,`create_user`,`update_user`) VALUES');
out.push(payLines.join(',\n')+';');

// refund_record（5 全额 + 1 部分）
const refunds = [];
let rfId = 1;
function addRefund(orderId, refundType, amountCents, reason, status, applyUser){
  const pay = paymentRef[orderId];
  if(!pay){ throw new Error('退款单缺少支付单: '+orderId); }
  const oo = orderRows.find(x=>x.id===orderId);
  const applyTime = addMin(oo.checkoutTime, 30 + rfId*11);
  const auditTime = status==='SUCCESS'?addMin(applyTime, 120):null;
  const refundTime = status==='SUCCESS'?addMin(applyTime, 300):null;
  refunds.push({id:rfId, paymentOrderId:pay.payId, orderId, refundNo:'RF'+applyTime.replace(/[-: ]/g,'').slice(0,14)+String(rfId).padStart(3,'0'),
    amountCents, reason, status, refundType, applyUser, createdTime:applyTime, auditUserId:1, auditTime, refundTime, updateUser:1});
  rfId++;
}
// 56 57 58 全额成功；59 60 全额退款中；25 部分成功
addRefund(56,1, orderRows.find(x=>x.id===56).amountCents, '菜品质量不符，整单退款', 'SUCCESS', 5);
addRefund(57,1, orderRows.find(x=>x.id===57).amountCents, '鱼头不新鲜，整单退款', 'SUCCESS', 6);
addRefund(58,1, orderRows.find(x=>x.id===58).amountCents, '配送超时送错餐，整单退款', 'SUCCESS', 7);
addRefund(59,1, orderRows.find(x=>x.id===59).amountCents, '辣子鸡食材不新鲜，申请退款', 'PENDING', 8);
addRefund(60,1, orderRows.find(x=>x.id===60).amountCents, '牛肉有异味，申请退款', 'PENDING', 9);
const partDishCents = 2600; // 宫保鸡丁 26 元
addRefund(25,2, partDishCents, '宫保鸡丁口味不符，部分退款', 'SUCCESS', 25);
out.push('');
out.push('DELETE FROM `refund_record`;');
out.push('INSERT INTO `refund_record` (`id`,`payment_order_id`,`order_id`,`tenant_id`,`refund_no`,`amount`,`reason`,`status`,`refund_type`,`apply_user_id`,`created_time`,`is_deleted`,`version`,`create_user`,`update_time`,`update_user`,`audit_user_id`,`audit_time`,`reject_reason`,`refund_time`) VALUES');
out.push(refunds.map(r=>{
  const updTime = r.status==='SUCCESS'?r.refundTime:r.createdTime;
  return '('+[r.id, r.paymentOrderId, r.orderId, 1, esc(r.refundNo), money(r.amountCents), esc(r.reason), esc(r.status),
    r.refundType, r.applyUser, esc(r.createdTime), 0, 1, 1, esc(updTime), r.updateUser, r.auditUserId,
    esc(r.auditTime), r.rejectReason, esc(r.refundTime)].join(',')+')';
}).join(',\n')+';');

// delivery_order（外卖单，除待付款外）
const dlStatusMap = {2:'PENDING',3:'DELIVERING',4:'DELIVERED',5:'CANCELLED',6:'DELIVERED'};
out.push('');
out.push('DELETE FROM `delivery_order`;');
out.push('INSERT INTO `delivery_order` (`id`,`tenant_id`,`platform_order_id`,`platform`,`order_id`,`dish_summary`,`amount`,`user_name`,`phone`,`address`,`status`,`order_time`,`created_time`,`update_time`,`created_user`,`update_user`,`is_deleted`,`version`) VALUES');
const dlLines = deliveryOrders.map((o, i)=>{
  const platform = (i%2===0)?'MEITUAN':'ELEME';
  const platNo = (platform==='MEITUAN'?'MT':'EL')+o.orderTime.replace(/[-: ]/g,'').slice(0,12)+String(10000+o.id*17);
  const st = o.status===3 ? ['ACCEPTED','PICKING','DELIVERING'][i%3] : dlStatusMap[o.status];
  return '('+[i+1, 1, esc(platNo), esc(platform), o.id, esc(o.dishSummary), money(o.amountCents),
    esc(o.userName), esc(o.phone), esc(o.address), esc(st), esc(o.orderTime), esc(o.orderTime), esc(o.orderTime), o.createUser, o.createUser, 0, 1].join(',')+')';
});
out.push(dlLines.join(',\n')+';');

// delivery_range_rule (10)
const rangeRules = [
  [1,'门店1公里商圈',2000,1,1000,3.00,6.00,3.00,6.00,39.00,'核心商圈，满39免配送'],
  [2,'门店1.5公里范围',2800,1,1500,3.00,6.00,3.00,6.00,49.00,'1.5公里内配送'],
  [3,'2公里主城区',2600,1,2000,4.00,7.00,4.00,7.00,49.00,'主城区'],
  [4,'3公里扩展区',3000,1,3000,5.00,8.00,5.00,8.00,59.00,'扩展配送区'],
  [5,'4公里远城区',3600,1,4000,6.00,9.00,6.00,9.00,69.00,'远城区'],
  [6,'5公里边缘区',4200,1,5000,7.00,9.00,7.00,9.00,79.00,'边缘配送'],
  [7,'6公里延长区',4800,1,6000,8.00,9.00,8.00,9.00,89.00,'延长配送'],
  [8,'8公里特向区',6000,1,8000,9.00,9.00,9.00,9.00,99.00,'企业团餐特向'],
  [9,'雨天固定加价',2500,1,3500,5.00,7.00,5.00,7.00,0.00,'恶劣天气固定5元起送'],
  [10,'写字楼基础+距离',3500,3,2500,3.00,9.00,3.00,9.00,29.00,'写字楼配送'],
];
out.push('');
out.push('DELETE FROM `delivery_range_rule`;');
out.push('INSERT INTO `delivery_range_rule` (`id`,`rule_name`,`range_type`,`center_longitude`,`center_latitude`,`radius`,`polygon_points`,`fee_type`,`base_fee`,`fee_per_km`,`min_fee`,`max_fee`,`free_threshold`,`status`,`sort_order`,`remark`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`) VALUES');
out.push(rangeRules.map((r,i)=>{
  const [id,name,dist,ftype,radius,minFee,maxFee,baseFee,_,freeTh,remark] = r;
  return '('+[id, esc(name), 1, STORE_LNG, STORE_LAT, radius, null, ftype, baseFee, ftype===1?null:(dist/1000).toFixed(1),
    minFee, maxFee, freeTh===0?null:freeTh, 1, i+1, esc(remark), 1, '2026-09-01 09:00:00', '2026-09-01 09:00:00', 1, 1].join(',')+')';
}).join(',\n')+';');

// delivery_fee_step (>=15)
function feeStepsFor(ruleId, base){
  const steps = base; // [ [start,end,fee] ] in meters
  return steps.map((s,j)=>[ruleId, s[0], s[1], s[2], null, null, j+1]);
}
const feeSteps = [].concat(
  feeStepsFor(1, [[0,1000,3.00],[1000,2000,4.00],[2000,3000,5.00],[3000,4000,6.00],[4000,5000,7.00],[5000,6000,8.00],[6000,999999,9.00]]),
  feeStepsFor(3, [[0,2000,4.00],[2000,3000,5.00],[3000,4000,6.00],[4000,5000,7.00],[5000,6000,8.00],[6000,999999,9.00]]),
  feeStepsFor(4, [[0,3000,5.00],[3000,4000,6.00],[4000,5000,7.00],[5000,6000,8.00],[6000,8000,9.00]]),
  feeStepsFor(10,[[0,1000,3.00],[1000,2000,4.00],[2000,3000,5.00],[3000,4000,6.00],[4000,5000,7.00],[5000,999999,8.00]]),
);
out.push('');
out.push('DELETE FROM `delivery_fee_step`;');
out.push('INSERT INTO `delivery_fee_step` (`id`,`rule_id`,`start_distance`,`end_distance`,`fee`,`increment_distance`,`increment_fee`,`sort_order`,`tenant_id`,`create_time`,`update_time`) VALUES');
let fsId = 1;
out.push(feeSteps.map(s=>{
  const line = '('+[fsId, s[0], s[1].toFixed(2), s[2]===999999?999999.00:s[2].toFixed(2), s[3].toFixed(2), null, null, s[4], 1, '2026-09-01 09:00:00', '2026-09-01 09:00:00'].join(',')+')';
  fsId++;
  return line;
}).join(',\n')+';');

// rider (10)
const riderRows = [
  [1,'张伟','13810000001',0,3,5120,4.9,'2026-09-25 12:20:00'],
  [2,'李磊','13810000002',1,2,3418,4.8,'2026-09-25 12:25:00'],
  [3,'王强','13810000003',1,1,2876,4.9,'2026-09-25 12:18:00'],
  [4,'刘洋','13810000004',1,3,6245,4.7,'2026-09-25 12:22:00'],
  [5,'陈刚','13810000005',2,0,1650,4.8,'2026-09-25 12:10:00'],
  [6,'杨军','13810000006',1,2,4521,4.9,'2026-09-25 12:26:00'],
  [7,'赵鹏','13810000007',0,1,7980,4.8,'2026-09-25 12:15:00'],
  [8,'黄勇','13810000008',1,0,2394,5.0,'2026-09-25 12:28:00'],
  [9,'周涛','13810000009',1,2,3177,4.7,'2026-09-25 12:21:00'],
  [10,'吴斌','13810000010',0,1,5236,4.9,'2026-09-25 12:19:00'],
];
out.push('');
out.push('DELETE FROM `rider`;');
out.push('INSERT INTO `rider` (`id`,`name`,`phone`,`password`,`avatar`,`current_longitude`,`current_latitude`,`status`,`current_order_count`,`total_order_count`,`rating`,`last_location_time`,`tenant_id`,`create_time`,`update_time`) VALUES');
out.push(riderRows.map(r=>{
  return '('+[r[0], esc(r[1]), esc(r[2]), esc(MD5), '', (STORE_LNG + (r[0]-5)*0.008).toFixed(6), (STORE_LAT + (r[0]-5)*0.006).toFixed(6), r[3], r[4], r[5], r[6].toFixed(1), esc(r[7]), 1, '2026-08-01 09:00:00', esc(r[7])].join(',')+')';
}).join(',\n')+';');

// rider_location_record (>=25)
let rlId = 1;
const rlLines = [];
for(let r=1;r<=10;r++){
  const cnt = r<=5?3:2;
  for(let k=0;k<cnt;k++){
    const t = addMin('2026-09-25 12:00:00', r*3 + k*5);
    const lon = (STORE_LNG + (r-5)*0.006 + (k-1)*0.002).toFixed(6);
    const lat = (STORE_LAT + (r-5)*0.004 + (k-1)*0.003).toFixed(6);
    rlLines.push('('+[rlId, r, null, lon, lat, (10+((r+k)%28)).toFixed(2), ((r*k*37)%360).toFixed(2), esc(t), 1, esc(t)].join(',')+')');
    rlId++;
  }
}
out.push('');
out.push('DELETE FROM `rider_location_record`;');
out.push('INSERT INTO `rider_location_record` (`id`,`rider_id`,`order_id`,`longitude`,`latitude`,`speed`,`direction`,`record_time`,`tenant_id`,`create_time`) VALUES');
out.push(rlLines.join(',\n')+';');

// delivery_time_record (外卖配送时效，>=20)
out.push('');
out.push('DELETE FROM `delivery_time_record`;');
out.push('INSERT INTO `delivery_time_record` (`id`,`order_id`,`order_number`,`rider_id`,`rider_name`,`order_time`,`accept_time`,`pickup_time`,`deliver_time`,`estimated_minutes`,`actual_minutes`,`distance`,`status`,`remark`,`tenant_id`,`create_time`,`update_time`) VALUES');
const dtLines = deliveryOrders.map((o,i)=>{
  const riderId = (o.riderId || ((o.id%10)+1));
  const riderName = RIDER[riderId][0];
  const acceptT = o.riderId? addMin(o.orderTime, 3 + (o.id%5)) : null;
  const pickupT = o.riderId? addMin(acceptT, 10 + (o.id%6)) : null;
  const deliverT = o.status===4||o.status===6 ? addMin(pickupT, 16 + (o.id%11)) : null;
  const estimated = 25 + (o.dist/1000 |0)*3;
  const actual = deliverT? Math.round((new Date(deliverT.replace(' ','T'))-new Date(o.orderTime.replace(' ','T')))/60000) : null;
  const status = o.status===5?5:(o.status===2?0:(o.status===3?3:4));
  return '('+[i+1, o.id, esc(o.number), riderId, esc(riderName), esc(o.orderTime), esc(acceptT), esc(pickupT), esc(deliverT),
    estimated, actual, o.dist, status, esc(o.status===5?'骑手已接单后退单':null), 1, esc(o.orderTime), esc(deliverT||o.orderTime)].join(',')+')';
});
out.push(dtLines.join(',\n')+';');

// kitchen_ticket（已支付订单后厨工单）
out.push('');
out.push('DELETE FROM `kitchen_ticket`;');
out.push('INSERT INTO `kitchen_ticket` (`id`,`tenant_id`,`order_id`,`order_no`,`order_type`,`table_name`,`customer_count`,`status`,`urgent`,`dish_summary`,`receive_time`,`cook_start_time`,`ready_time`,`finish_time`,`cancel_time`,`cook_duration_seconds`,`station_code`,`remark`,`create_time`,`update_time`,`create_user`,`update_user`,`is_deleted`) VALUES');
let ktId = 1;
const ktLines = kitchenOrders.map(o=>{
  const receiveT = addMin(o.orderTime, 1);
  const finished = (o.status===4||o.status===6);
  const cooking = (o.status===3);
  const cookStart = (finished||cooking)? addMin(receiveT, 2 + (o.id%4)) : null;
  const ready = (finished||cooking)? addMin(cookStart, 8 + (o.id%9)) : null;
  const finish = finished? addMin(ready, (o.dineType==='EAT_IN'?3:6)) : null;
  const cookDur = (finished||cooking)? (8 + (o.id%9))*60 + (o.id%60): null;
  const status = o.status===6?4:(o.status===2?1:(o.status===3?2:4));
  const firstCat = o.items.length? (o.items[0][0]==='d'? DISH[o.items[0][1]][2] : 1) : 1;
  const station = stationFor(firstCat);
  const customerCount = o.dineType==='EAT_IN'? o.tableSeats : null;
  const t = '('+[ktId, 1, o.id, esc(o.number), esc(o.dineType==='EAT_IN'?'EAT_IN':'OUTSIDE'), esc(o.tableName), customerCount,
    status, 0, esc(o.dishSummary), esc(receiveT), esc(cookStart), esc(ready), esc(finish), null, cookDur, esc(station),
    esc(o.remark), esc(o.orderTime), esc(finish||o.orderTime), o.createUser, 1, 0].join(',')+')';
  ktId++;
  return t;
});
out.push(ktLines.join(',\n')+';');

// cashier_record (>=25)
out.push('');
out.push('DELETE FROM `cashier_record`;');
out.push('INSERT INTO `cashier_record` (`id`,`order_id`,`order_number`,`pay_type`,`amount`,`actual_amount`,`change_amount`,`cashier_time`,`cashier_id`,`cashier_name`,`remark`,`tenant_id`,`create_time`,`create_user`) VALUES');
// 堂食已支付单 + 若干外卖单
const cashierCandidates = paidOrders.map(p=>orderRows.find(x=>x.id===p.orderId)).filter(o=>o && o.checkoutTime);
const cashierPick = cashierCandidates.filter(o=>o.dineType==='EAT_IN').concat(cashierCandidates.filter(o=>o.dineType!=='EAT_IN').slice(0,11)).slice(0,26);
let caId = 1;
const caLines = cashierPick.map((o)=>{
  const cashierId = [7,8,14][o.id%3];
  const payType = o.payMethod || 2;
  return '('+[caId++, o.id, esc(o.number), payType, money(o.amountCents), money(o.amountCents), money(0),
    esc(o.checkoutTime), cashierId, esc(CASHIER[cashierId][0]), null, 1, esc(o.checkoutTime), cashierId].join(',')+')';
});
out.push(caLines.join(',\n')+';');

// daily_settlement（过去15天）
out.push('');
out.push('DELETE FROM `daily_settlement`;');
out.push('INSERT INTO `daily_settlement` (`id`,`settlement_date`,`total_revenue`,`cash_income`,`wechat_income`,`alipay_income`,`bankcard_income`,`other_income`,`order_count`,`refund_amount`,`refund_count`,`net_income`,`material_cost`,`labor_cost`,`other_cost`,`total_cost`,`gross_profit`,`profit_rate`,`status`,`settlement_time`,`settlement_user_id`,`settlement_user_name`,`remark`,`tenant_id`,`create_time`,`update_time`,`create_user`,`update_user`,`version`) VALUES');
// 统计每天已支付订单（status 2/3/4/6）与退款成功
const dayMap = {};
for(const o of orderRows){
  const day = o.orderTime.slice(0,10);
  if(!isNaN(Date.parse(day))) {
    if(o.status===2||o.status===3||o.status===4||o.status===6){
      dayMap[day] = dayMap[day] || {ord:0, cash:0, wechat:0, alipay:0};
      dayMap[day].ord++;
      if(o.payMethod===1) dayMap[day].cash += o.amountCents;
      else if(o.payMethod===2) dayMap[day].wechat += o.amountCents;
      else if(o.payMethod===3) dayMap[day].alipay += o.amountCents;
    }
  }
}
const refundDay = {};
for(const r of refunds){
  if(r.status==='SUCCESS'){
    const day = (r.refundTime).slice(0,10);
    refundDay[day] = refundDay[day] || {amt:0, cnt:0};
    refundDay[day].amt += r.amountCents; refundDay[day].cnt++;
  }
}
const settleDays = [];
for(let dd=11;dd<=25;dd++){
  const day = '2026-09-' + String(dd).padStart(2,'0');
  const m = dayMap[day] || {ord:0,cash:0,wechat:0,alipay:0};
  const rf = refundDay[day] || {amt:0,cnt:0};
  const total = m.cash + m.wechat + m.alipay;
  const net = total - rf.amt;
  const mat = Math.round(total*0.42), labor = Math.round(total*0.20), other = Math.round(total*0.08);
  const totalCost = mat + labor + other;
  const profit = total - totalCost;
  const rate = total? ((profit/total*100)) : 0;
  settleDays.push({day, total, cash:m.cash, wechat:m.wechat, alipay:m.alipay, ord:m.ord, rfAmt:rf.amt, rfCnt:rf.cnt, net, mat, labor, other, totalCost, profit, rate});
}
const settleLines = settleDays.map((s,i)=>{
  const stime = s.day+' 23:30:00';
  return '('+[i+1, esc(s.day), money(s.total), money(s.cash), money(s.wechat), money(s.alipay), money(0), money(0),
    s.ord, money(s.rfAmt), s.rfCnt, money(s.net), money(s.mat), money(s.labor), money(s.other), money(s.totalCost),
    money(s.profit), s.rate.toFixed(2), 1, esc(stime), 1, esc('王建国'), esc(s.ord<2?'客流较少，正常日结':'正常日结'),
    1, esc(stime), esc(stime), 1, 1, 1].join(',')+')';
});
out.push(settleLines.join(',\n')+';');

out.push('');

// ---------- 自检 ----------
for(const o of orderRows){
  const detSum = details.filter(d=>d.orderId===o.id).reduce((a,d)=>a+d.amountCents,0);
  const expect = detSum + (o.dineType==='EAT_IN'?0:feeFor(o.dist)) - o.fullRed - o.newRed;
  if(expect !== o.amountCents) throw new Error('订单'+o.id+'金额不一致: 期望'+expect+' 实存'+o.amountCents);
}
console.log('明细数:', details.length);
console.log('订单数:', orderRows.length);
console.log('支付单数:', payLines.length);
console.log('退款单数:', refunds.length);
console.log('配送单数:', dlLines.length);
console.log('配送时效数:', dtLines.length);
console.log('后厨工单数:', ktLines.length);
console.log('收银流水数:', caLines.length);
console.log('日结数:', settleLines.length);
console.log('骑手位置数:', rlLines.length);

const sql = out.join('\n');
const target = 'D:/MyCode/reggie/src/main/resources/db/seed/domain-d-order.sql';
fs.writeFileSync(target, sql, 'utf8');
console.log('written:', target, sql.length, 'bytes');