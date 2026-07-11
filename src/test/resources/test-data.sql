-- =============================================
-- 瑞吉外卖系统测试数据
-- 生成时间: 2026-07-11
-- 说明: 使用INSERT IGNORE避免主键重复
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 0. tenant (租户) - 防止主键重复
-- ----------------------------
INSERT IGNORE INTO `tenant` VALUES (1, '测试餐厅总部', '13800138001', '北京市朝阳区建国路88号', 'MD5', 1, '2026-07-07 17:58:37', '2026-07-07 17:58:37', 1, 1);
INSERT IGNORE INTO `tenant` VALUES (2, '美味餐厅分店', '13800138002', '上海市浦东新区陆家嘴', 'MD5', 1, '2026-07-07 17:58:37', '2026-07-07 17:58:37', 1, 1);
INSERT IGNORE INTO `tenant` VALUES (3, '香满楼餐厅', '13800138003', '广州市天河区珠江新城', 'MD5', 1, '2026-07-07 17:58:37', '2026-07-07 17:58:37', 1, 1);

-- ----------------------------
-- 1. category (菜品分类) - 12条
-- ----------------------------
INSERT IGNORE INTO `category` VALUES (1, 1, '热菜', 1, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (2, 1, '凉菜', 2, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (3, 1, '汤类', 3, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (4, 1, '主食', 4, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (5, 1, '小吃', 5, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (6, 1, '饮品', 6, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (7, 2, '单人套餐', 1, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (8, 2, '双人套餐', 2, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (9, 2, '多人套餐', 3, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (10, 2, '商务套餐', 4, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (11, 2, '儿童套餐', 5, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `category` VALUES (12, 2, '聚会套餐', 6, 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);

-- ----------------------------
-- 2. dish (菜品) - 30条
-- ----------------------------
-- 热菜 (category_id=1)
INSERT IGNORE INTO `dish` VALUES (1, '红烧肉', 1, 38.00, 'DISH001', './images/dishes/hongshaorou.jpg', '经典家常菜，肥而不腻', 1, 1, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 100.00, 10.00);
INSERT IGNORE INTO `dish` VALUES (2, '宫保鸡丁', 1, 32.00, 'DISH002', './images/dishes/gongbaojiding.jpg', '川菜经典，麻辣鲜香', 1, 2, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 80.00, 10.00);
INSERT IGNORE INTO `dish` VALUES (3, '鱼香肉丝', 1, 28.00, 'DISH003', './images/dishes/yuxiangrousi.jpg', '酸甜可口，下饭神器', 1, 3, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 120.00, 15.00);
INSERT IGNORE INTO `dish` VALUES (4, '糖醋里脊', 1, 35.00, 'DISH004', './images/dishes/tangculiji.jpg', '外酥里嫩，酸甜适口', 1, 4, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 90.00, 10.00);
INSERT IGNORE INTO `dish` VALUES (5, '水煮牛肉', 1, 42.00, 'DISH005', './images/dishes/shuizhuniurou.jpg', '麻辣鲜香，正宗川味', 1, 5, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 60.00, 8.00);
INSERT IGNORE INTO `dish` VALUES (6, '麻婆豆腐', 1, 18.00, 'DISH006', './images/dishes/mapodoufu.jpg', '麻辣鲜香，口感嫩滑', 1, 6, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 150.00, 20.00);
INSERT IGNORE INTO `dish` VALUES (7, '回锅肉', 1, 30.00, 'DISH007', './images/dishes/huiguorou.jpg', '肉片薄嫩，蒜苗清香', 1, 7, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 70.00, 10.00);
INSERT IGNORE INTO `dish` VALUES (8, '蒜蓉西兰花', 1, 16.00, 'DISH008', './images/dishes/suanrongxilanhua.jpg', '清爽健康，蒜香四溢', 1, 8, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 200.00, 30.00);

-- 凉菜 (category_id=2)
INSERT IGNORE INTO `dish` VALUES (9, '凉拌黄瓜', 2, 12.00, 'DISH009', './images/dishes/liangbanhuanggua.jpg', '清脆爽口，夏日必备', 1, 1, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 180.00, 25.00);
INSERT IGNORE INTO `dish` VALUES (10, '皮蛋豆腐', 2, 15.00, 'DISH010', './images/dishes/pidan doufu.jpg', '经典凉菜，滑嫩鲜香', 1, 2, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 160.00, 20.00);
INSERT IGNORE INTO `dish` VALUES (11, '口水鸡', 2, 22.00, 'DISH011', './images/dishes/koushuiji.jpg', '麻辣鲜香，肉质细嫩', 1, 3, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 100.00, 15.00);
INSERT IGNORE INTO `dish` VALUES (12, '拍黄瓜', 2, 10.00, 'DISH012', './images/dishes/paihuanggua.jpg', '开胃爽口，解腻佳品', 1, 4, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 200.00, 30.00);

-- 汤类 (category_id=3)
INSERT IGNORE INTO `dish` VALUES (13, '酸辣汤', 3, 15.00, 'DISH013', './images/dishes/suanlatang.jpg', '酸辣开胃，暖身好汤', 1, 1, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 300.00, 50.00);
INSERT IGNORE INTO `dish` VALUES (14, '紫菜蛋花汤', 3, 12.00, 'DISH014', './images/dishes/zicidanhuatang.jpg', '清淡鲜美，营养滋补', 1, 2, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 350.00, 50.00);
INSERT IGNORE INTO `dish` VALUES (15, '番茄蛋汤', 3, 10.00, 'DISH015', './images/dishes/fanqiedantang.jpg', '酸甜可口，老少皆宜', 1, 3, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 400.00, 50.00);

-- 主食 (category_id=4)
INSERT IGNORE INTO `dish` VALUES (16, '米饭', 4, 3.00, 'DISH016', './images/dishes/mifan.jpg', '东北大米，颗粒饱满', 1, 1, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 500.00, 100.00);
INSERT IGNORE INTO `dish` VALUES (17, '扬州炒饭', 4, 18.00, 'DISH017', './images/dishes/yangzhouchaofan.jpg', '蛋香浓郁，粒粒分明', 1, 2, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 200.00, 30.00);
INSERT IGNORE INTO `dish` VALUES (18, '牛肉面', 4, 22.00, 'DISH018', './images/dishes/niuroumian.jpg', '汤鲜面滑，牛肉软烂', 1, 3, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 150.00, 25.00);

-- 小吃 (category_id=5)
INSERT IGNORE INTO `dish` VALUES (19, '春卷', 5, 12.00, 'DISH019', './images/dishes/chunjuan.jpg', '外皮酥脆，内馅鲜美', 1, 1, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 100.00, 15.00);
INSERT IGNORE INTO `dish` VALUES (20, '炸鸡翅', 5, 15.00, 'DISH020', './images/dishes/zhajichi.jpg', '外酥里嫩，香辣可口', 1, 2, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 120.00, 20.00);
INSERT IGNORE INTO `dish` VALUES (21, '薯条', 5, 10.00, 'DISH021', './images/dishes/shutiao.jpg', '金黄酥脆，蘸酱更香', 1, 3, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 150.00, 25.00);

-- 饮品 (category_id=6)
INSERT IGNORE INTO `dish` VALUES (22, '珍珠奶茶', 6, 12.00, 'DISH022', './images/dishes/zhenzhunicha.jpg', '香浓奶茶，Q弹珍珠', 1, 1, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 200.00, 30.00);
INSERT IGNORE INTO `dish` VALUES (23, '柠檬水', 6, 8.00, 'DISH023', './images/dishes/ningmengshui.jpg', '清爽解渴，维C丰富', 1, 2, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 300.00, 50.00);
INSERT IGNORE INTO `dish` VALUES (24, '酸梅汤', 6, 10.00, 'DISH024', './images/dishes/suanmeitang.jpg', '冰镇酸梅，消暑解腻', 1, 3, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 250.00, 40.00);
INSERT IGNORE INTO `dish` VALUES (25, '橙汁', 6, 10.00, 'DISH025', './images/dishes/chengzhi.jpg', '鲜榨橙汁，维C满满', 1, 4, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 200.00, 30.00);
INSERT IGNORE INTO `dish` VALUES (26, '豆浆', 6, 6.00, 'DISH026', './images/dishes/doujiang.jpg', '现磨豆浆，营养健康', 1, 5, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 300.00, 50.00);
INSERT IGNORE INTO `dish` VALUES (27, '可乐', 6, 5.00, 'DISH027', './images/dishes/kele.jpg', '冰镇可乐，畅爽痛快', 1, 6, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 500.00, 100.00);
INSERT IGNORE INTO `dish` VALUES (28, '雪碧', 6, 5.00, 'DISH028', './images/dishes/xuebi.jpg', '清爽雪碧，解渴必备', 1, 7, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 500.00, 100.00);
INSERT IGNORE INTO `dish` VALUES (29, '凉茶', 6, 8.00, 'DISH029', './images/dishes/liangcha.jpg', '清热降火，传统配方', 1, 8, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 200.00, 30.00);
INSERT IGNORE INTO `dish` VALUES (30, '王老吉', 6, 6.00, 'DISH030', './images/dishes/wanglaoji.jpg', '怕上火喝王老吉', 1, 9, 1, '2026-07-07 17:59:44', '2026-07-07 17:59:44', 1, 1, 0, 300.00, 50.00);

-- ----------------------------
-- 3. dish_flavor (菜品口味) - 25条
-- ----------------------------
INSERT IGNORE INTO `dish_flavor` VALUES (1, 1, '辣度', '["不辣","微辣","中辣","特辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (2, 2, '辣度', '["不辣","微辣","中辣","特辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (3, 2, '甜度', '["不加糖","少糖","正常","多糖"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (4, 3, '辣度', '["不辣","微辣","中辣","特辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (5, 4, '口味', '["酸甜","糖醋"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (6, 5, '辣度', '["微辣","中辣","特辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (7, 6, '辣度', '["不辣","微辣","中辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (8, 7, '辣度', '["不辣","微辣","中辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (9, 8, '口味', '["蒜香","少盐"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (10, 9, '口味', '["不辣","微辣","中辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (11, 10, '口味', '["原味","微辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (12, 11, '辣度', '["微辣","中辣","特辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (13, 13, '口味', '["正常","加辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (14, 15, '口味', '["正常","少盐"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (15, 16, '规格', '["小碗","大碗"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (16, 18, '辣度', '["不辣","微辣","中辣","特辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (17, 18, '加料', '["加蛋","加肉","加蔬菜"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (18, 19, '口味', '["原味","辣味"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (19, 20, '辣度', '["不辣","微辣","中辣","特辣"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (20, 22, '甜度', '["无糖","少糖","正常甜","多糖"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (21, 22, '温度', '["常温","热饮","冰饮"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (22, 23, '温度', '["常温","冰镇"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (23, 24, '温度', '["常温","冰镇"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (24, 25, '温度', '["常温","冰镇"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);
INSERT IGNORE INTO `dish_flavor` VALUES (25, 17, '口味', '["蛋炒饭","扬州炒饭","酱油炒饭"]', 1, '2026-07-07 17:59:12', '2026-07-07 17:59:12', 1, 1, 0);

-- ----------------------------
-- 4. setmeal (套餐) - 12条
-- ----------------------------
INSERT IGNORE INTO `setmeal` VALUES (1, 7, '单人工作餐', 28.00, 1, 'SET001', '一荤一素一汤，营养搭配', './images/setmeal/danren.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (2, 7, '单人豪华餐', 38.00, 1, 'SET002', '两荤一素一汤，丰盛美味', './images/setmeal/danrenhaohua.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (3, 8, '双人甜蜜套餐', 68.00, 1, 'SET003', '三荤两素一汤，甜蜜共享', './images/setmeal/shuangrentianmi.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (4, 8, '双人浪漫套餐', 88.00, 1, 'SET004', '四荤两素两汤，浪漫晚餐', './images/setmeal/shuangrenlangman.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (5, 9, '三人家庭套餐', 98.00, 1, 'SET005', '四荤三素三汤，家庭聚餐', './images/setmeal/sanrenjiating.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (6, 9, '四人欢乐套餐', 128.00, 1, 'SET006', '五荤四素四汤，欢乐时光', './images/setmeal/sirenhuanle.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (7, 9, '六人聚会套餐', 198.00, 1, 'SET007', '六荤五素五汤，朋友聚会', './images/setmeal/liurenjuhui.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (8, 10, '商务简餐', 58.00, 1, 'SET008', '两荤两素一汤，商务首选', './images/setmeal/shangwujiancan.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (9, 10, '商务宴请套餐', 168.00, 1, 'SET009', '六荤四素两汤，宴请佳选', './images/setmeal/shangwuyanqing.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (10, 11, '儿童营养套餐', 25.00, 1, 'SET010', '一荤一素一汤，营养均衡', './images/setmeal/ertongyingyang.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (11, 11, '儿童欢乐套餐', 32.00, 1, 'SET011', '两荤一素一汤，孩子爱吃', './images/setmeal/ertonghuanle.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);
INSERT IGNORE INTO `setmeal` VALUES (12, 12, '八人盛宴套餐', 298.00, 1, 'SET012', '八荤六素六汤，盛宴狂欢', './images/setmeal/barenshengyan.jpg', 1, '2026-07-07 17:59:50', '2026-07-07 17:59:50', 1, 1, 0);

-- ----------------------------
-- 5. setmeal_dish (套餐菜品关系) - 40条
-- ----------------------------
-- 单人工作餐 (setmeal_id=1)
INSERT IGNORE INTO `setmeal_dish` VALUES (1, '1', '1', '红烧肉', 38.00, 1, 1, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (2, '1', '8', '蒜蓉西兰花', 16.00, 1, 2, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (3, '1', '14', '紫菜蛋花汤', 12.00, 1, 3, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);

-- 单人豪华餐 (setmeal_id=2)
INSERT IGNORE INTO `setmeal_dish` VALUES (4, '2', '1', '红烧肉', 38.00, 1, 1, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (5, '2', '2', '宫保鸡丁', 32.00, 1, 2, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (6, '2', '8', '蒜蓉西兰花', 16.00, 1, 3, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (7, '2', '13', '酸辣汤', 15.00, 1, 4, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);

-- 双人甜蜜套餐 (setmeal_id=3)
INSERT IGNORE INTO `setmeal_dish` VALUES (8, '3', '2', '宫保鸡丁', 32.00, 1, 1, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (9, '3', '4', '糖醋里脊', 35.00, 1, 2, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (10, '3', '8', '蒜蓉西兰花', 16.00, 1, 3, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (11, '3', '9', '凉拌黄瓜', 12.00, 1, 4, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (12, '3', '14', '紫菜蛋花汤', 12.00, 1, 5, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);

-- 双人浪漫套餐 (setmeal_id=4)
INSERT IGNORE INTO `setmeal_dish` VALUES (13, '4', '5', '水煮牛肉', 42.00, 1, 1, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (14, '4', '3', '鱼香肉丝', 28.00, 1, 2, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (15, '4', '10', '皮蛋豆腐', 15.00, 1, 3, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (16, '4', '9', '凉拌黄瓜', 12.00, 1, 4, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (17, '4', '13', '酸辣汤', 15.00, 2, 5, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);

-- 三人家庭套餐 (setmeal_id=5)
INSERT IGNORE INTO `setmeal_dish` VALUES (18, '5', '1', '红烧肉', 38.00, 1, 1, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (19, '5', '2', '宫保鸡丁', 32.00, 1, 2, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (20, '5', '7', '回锅肉', 30.00, 1, 3, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (21, '5', '8', '蒜蓉西兰花', 16.00, 1, 4, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (22, '5', '9', '凉拌黄瓜', 12.00, 1, 5, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (23, '5', '13', '酸辣汤', 15.00, 1, 6, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (24, '5', '14', '紫菜蛋花汤', 12.00, 1, 7, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);

-- 四人欢乐套餐 (setmeal_id=6)
INSERT IGNORE INTO `setmeal_dish` VALUES (25, '6', '1', '红烧肉', 38.00, 1, 1, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (26, '6', '5', '水煮牛肉', 42.00, 1, 2, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (27, '6', '2', '宫保鸡丁', 32.00, 1, 3, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (28, '6', '4', '糖醋里脊', 35.00, 1, 4, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (29, '6', '8', '蒜蓉西兰花', 16.00, 1, 5, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (30, '6', '11', '口水鸡', 22.00, 1, 6, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (31, '6', '9', '凉拌黄瓜', 12.00, 1, 7, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (32, '6', '10', '皮蛋豆腐', 15.00, 1, 8, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (33, '6', '13', '酸辣汤', 15.00, 2, 9, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (34, '6', '14', '紫菜蛋花汤', 12.00, 2, 10, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);

-- 六人聚会套餐 (setmeal_id=7)
INSERT IGNORE INTO `setmeal_dish` VALUES (35, '7', '1', '红烧肉', 38.00, 1, 1, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (36, '7', '5', '水煮牛肉', 42.00, 1, 2, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (37, '7', '2', '宫保鸡丁', 32.00, 1, 3, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (38, '7', '4', '糖醋里脊', 35.00, 1, 4, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (39, '7', '3', '鱼香肉丝', 28.00, 1, 5, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);
INSERT IGNORE INTO `setmeal_dish` VALUES (40, '7', '7', '回锅肉', 30.00, 1, 6, 1, '2026-07-07 18:09:17', '2026-07-07 18:09:17', 1, 1, 0);

-- ----------------------------
-- 6. employee (员工) - 10条
-- ----------------------------
INSERT IGNORE INTO `employee` VALUES (1, '系统管理员', 'admin', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13800138001', '男', '110101199001011234', 1, 1, 1, '2026-07-07 17:49:51', '2026-07-07 17:51:35', 1, 1);
INSERT IGNORE INTO `employee` VALUES (2, '李经理', 'limanager', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139010', '男', '320102198503152017', 1, 2, 1, '2026-07-01 09:00:00', '2026-07-01 09:00:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (3, '王大厨', 'wangchef', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139011', '男', '440103198806201234', 1, 2, 1, '2026-07-02 10:00:00', '2026-07-02 10:00:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (4, '赵服务员', 'zhaowaiter', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139012', '女', '510104199204151026', 1, 2, 1, '2026-07-02 14:00:00', '2026-07-02 14:00:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (5, '陈收银', 'chencashier', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139013', '女', '350203199507081020', 1, 2, 1, '2026-07-03 08:30:00', '2026-07-03 08:30:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (6, '刘配送', 'liudelivery', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139014', '男', '420106199309121035', 1, 2, 1, '2026-07-03 09:00:00', '2026-07-03 09:00:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (7, '孙采购', 'sunbuyer', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139015', '男', '610102198803250039', 1, 2, 1, '2026-07-04 10:00:00', '2026-07-04 10:00:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (8, '周店长', 'zhoumanager', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139016', '男', '330106198511120018', 1, 2, 1, '2026-07-04 14:00:00', '2026-07-04 14:00:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (9, '吴厨师', 'wucook', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139017', '男', '500112199006080032', 1, 2, 1, '2026-07-05 08:00:00', '2026-07-05 08:00:00', 1, 1);
INSERT IGNORE INTO `employee` VALUES (10, '郑财务', 'zhengfinance', '$2a$10$KpveBkD6hQYtVBysue2Q7.3QMu4hRFi1itwQS9Qu0KbJUL.ciBDb.', 'BCRYPT', '13900139018', '女', '370103199107220028', 1, 2, 1, '2026-07-05 09:00:00', '2026-07-05 09:00:00', 1, 1);

-- ----------------------------
-- 7. address_book (地址簿) - 12条
-- ----------------------------
INSERT IGNORE INTO `address_book` VALUES (1, 1, '张小明', 1, '13900139001', '110000', '北京市', '110100', '北京市', '110101', '东城区', '北京市东城区王府井大街1号', '家', 1, '2026-07-07 18:07:15', '2026-07-07 18:07:15', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (2, 1, '张小明', 1, '13900139001', '110000', '北京市', '110100', '北京市', '110102', '西城区', '北京市西城区金融街8号', '公司', 0, '2026-07-07 18:07:15', '2026-07-07 18:07:15', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (3, 2, '李晓红', 0, '13900139002', '310000', '上海市', '310100', '上海市', '310104', '徐汇区', '上海市徐汇区南京路100号', '家', 1, '2026-07-07 18:07:15', '2026-07-07 18:07:15', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (4, 3, '王大军', 1, '13900139003', '440000', '广东省', '440100', '广州市', '440106', '天河区', '广州市天河区天河路385号', '公司', 1, '2026-07-03 10:00:00', '2026-07-03 10:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (5, 4, '赵伟', 1, '13900139004', '320000', '江苏省', '320100', '南京市', '320102', '玄武区', '南京市玄武区中山路1号', '家', 1, '2026-07-04 11:00:00', '2026-07-04 11:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (6, 5, '钱芳', 0, '13900139005', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '杭州市西湖区文三路100号', '家', 1, '2026-07-05 08:00:00', '2026-07-05 08:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (7, 6, '孙磊', 1, '13900139006', '500000', '重庆市', '500100', '重庆市', '500112', '渝北区', '重庆市渝北区新南路8号', '家', 1, '2026-07-06 09:00:00', '2026-07-06 09:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (8, 7, '李娜', 0, '13900139007', '370000', '山东省', '370100', '济南市', '370103', '市中区', '济南市市中区经十路100号', '家', 1, '2026-07-07 10:00:00', '2026-07-07 10:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (9, 8, '周杰', 1, '13900139008', '350000', '福建省', '350100', '福州市', '350203', '思明区', '厦门市思明区厦禾路200号', '公司', 1, '2026-07-08 14:00:00', '2026-07-08 14:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (10, 9, '吴敏', 0, '13900139009', '420000', '湖北省', '420100', '武汉市', '420106', '武昌区', '武汉市武昌区珞喻路500号', '学校', 1, '2026-07-09 09:00:00', '2026-07-09 09:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (11, 10, '郑浩', 1, '13900139020', '610000', '陕西省', '610100', '西安市', '610113', '雁塔区', '西安市雁塔区高新路8号', '家', 1, '2026-07-10 11:00:00', '2026-07-10 11:00:00', 1, 1, 0, 1);
INSERT IGNORE INTO `address_book` VALUES (12, 11, '王静', 0, '13900139021', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '杭州市西湖区求是路5号', '公司', 1, '2026-07-10 15:00:00', '2026-07-10 15:00:00', 1, 1, 0, 1);

-- ----------------------------
-- 8. member_level (会员等级) - 5条
-- ----------------------------
INSERT IGNORE INTO `member_level` VALUES (1, 1, '普通会员', 0, 1.00, 1, '2026-07-07 17:59:57');
INSERT IGNORE INTO `member_level` VALUES (2, 1, '银卡会员', 1000, 0.95, 2, '2026-07-07 17:59:57');
INSERT IGNORE INTO `member_level` VALUES (3, 1, '金卡会员', 5000, 0.90, 3, '2026-07-07 17:59:57');
INSERT IGNORE INTO `member_level` VALUES (4, 1, '钻石会员', 10000, 0.85, 4, '2026-07-07 17:59:57');
INSERT IGNORE INTO `member_level` VALUES (5, 1, '至尊会员', 50000, 0.80, 5, '2026-07-07 17:59:57');

-- ----------------------------
-- 9. member (会员) - 12条
-- ----------------------------
INSERT IGNORE INTO `member` VALUES (1, 1, 1, 3, '张小明', '13900139001', 6500, 1580.50, 3680.00, 1, '2026-07-07 18:06:54', '2026-07-07 18:06:54');
INSERT IGNORE INTO `member` VALUES (2, 1, 2, 2, '李晓红', '13900139002', 2800, 680.00, 1520.00, 1, '2026-07-07 18:06:54', '2026-07-07 18:06:54');
INSERT IGNORE INTO `member` VALUES (3, 1, 3, 4, '王大军', '13900139003', 12500, 3580.00, 8960.00, 1, '2026-07-07 18:06:54', '2026-07-07 18:06:54');
INSERT IGNORE INTO `member` VALUES (4, 1, 4, 2, '赵伟', '13900139004', 1500, 320.00, 1200.00, 1, '2026-06-15 12:30:00', '2026-07-09 18:00:00');
INSERT IGNORE INTO `member` VALUES (5, 1, 5, 1, '钱芳', '13900139005', 350, 80.00, 350.00, 1, '2026-06-20 14:20:00', '2026-07-05 12:00:00');
INSERT IGNORE INTO `member` VALUES (6, 1, 6, 4, '孙磊', '13900139006', 10500, 2800.00, 7200.00, 1, '2026-07-01 09:15:00', '2026-07-09 20:00:00');
INSERT IGNORE INTO `member` VALUES (7, 1, 7, 2, '李娜', '13900139007', 1200, 450.00, 1800.00, 1, '2026-07-02 10:30:00', '2026-07-08 16:00:00');
INSERT IGNORE INTO `member` VALUES (8, 1, 8, 3, '周杰', '13900139008', 5800, 1200.00, 4200.00, 1, '2026-07-03 11:45:00', '2026-07-09 15:30:00');
INSERT IGNORE INTO `member` VALUES (9, 1, 9, 1, '吴敏', '13900139009', 200, 50.00, 200.00, 1, '2026-07-05 08:20:00', '2026-07-07 10:00:00');
INSERT IGNORE INTO `member` VALUES (10, 1, 10, 2, '郑浩', '13900139020', 1800, 600.00, 2200.00, 1, '2026-07-07 15:10:00', '2026-07-09 19:00:00');
INSERT IGNORE INTO `member` VALUES (11, 1, 11, 1, '王静', '13900139021', 100, 20.00, 100.00, 1, '2026-07-08 16:30:00', '2026-07-10 12:00:00');
INSERT IGNORE INTO `member` VALUES (12, 1, 12, 1, '冯雷', '13900139022', 50, 0.00, 50.00, 1, '2026-07-09 09:00:00', '2026-07-10 18:00:00');

-- ----------------------------
-- 10. dining_area (区域) - 8条
-- ----------------------------
INSERT IGNORE INTO `dining_area` VALUES (1, 1, '大厅A区', 1, '2026-07-07 17:59:20', '2026-07-07 17:59:20');
INSERT IGNORE INTO `dining_area` VALUES (2, 1, '大厅B区', 2, '2026-07-07 17:59:20', '2026-07-07 17:59:20');
INSERT IGNORE INTO `dining_area` VALUES (3, 1, '包间区', 3, '2026-07-07 17:59:20', '2026-07-07 17:59:20');
INSERT IGNORE INTO `dining_area` VALUES (4, 1, '户外露台', 4, '2026-07-07 17:59:20', '2026-07-07 17:59:20');
INSERT IGNORE INTO `dining_area` VALUES (5, 1, 'VIP包间', 5, '2026-07-07 17:59:20', '2026-07-07 17:59:20');
INSERT IGNORE INTO `dining_area` VALUES (6, 1, '儿童区', 6, '2026-07-07 17:59:20', '2026-07-07 17:59:20');
INSERT IGNORE INTO `dining_area` VALUES (7, 1, '吸烟区', 7, '2026-07-07 17:59:20', '2026-07-07 17:59:20');
INSERT IGNORE INTO `dining_area` VALUES (8, 1, '无烟区', 8, '2026-07-07 17:59:20', '2026-07-07 17:59:20');

-- ----------------------------
-- 11. dining_table (桌台) - 15条
-- ----------------------------
INSERT IGNORE INTO `dining_table` VALUES (1, 1, 1, 'A01', 4, 'FREE', NULL, NULL, 1, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (2, 1, 1, 'A02', 4, 'OCCUPIED', 100.00, NULL, 2, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (3, 1, 1, 'A03', 6, 'FREE', NULL, NULL, 3, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (4, 1, 1, 'A04', 8, 'FREE', NULL, NULL, 4, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (5, 1, 2, 'B01', 4, 'RESERVED', NULL, NULL, 5, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (6, 1, 2, 'B02', 4, 'FREE', NULL, NULL, 6, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (7, 1, 2, 'B03', 6, 'OCCUPIED', NULL, NULL, 7, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (8, 1, 3, 'V01', 8, 'FREE', 500.00, NULL, 8, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (9, 1, 3, 'V02', 12, 'FREE', 800.00, NULL, 9, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (10, 1, 3, 'V03', 10, 'CLEANING', 600.00, NULL, 10, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (11, 1, 4, 'T01', 4, 'FREE', NULL, NULL, 11, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (12, 1, 4, 'T02', 6, 'FREE', NULL, NULL, 12, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (13, 1, 5, 'VV01', 6, 'FREE', 1000.00, NULL, 13, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (14, 1, 5, 'VV02', 8, 'FREE', 1500.00, NULL, 14, '2026-07-07 17:59:27', '2026-07-07 17:59:27');
INSERT IGNORE INTO `dining_table` VALUES (15, 1, 6, 'K01', 4, 'FREE', NULL, NULL, 15, '2026-07-07 17:59:27', '2026-07-07 17:59:27');

-- ----------------------------
-- 12. user (用户) - 12条
-- ----------------------------
INSERT IGNORE INTO `user` VALUES (1, '系统管理员', '13800138001', '男', '110101199001011234', NULL, 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (2, '张小明', '13900139001', '男', '110101199001011001', './images/avatars/user1.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (3, '李晓红', '13900139002', '女', '110101199002021002', './images/avatars/user2.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (4, '王大军', '13900139003', '男', '440103198806201234', './images/avatars/user3.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (5, '赵伟', '13900139004', '男', '320102198503152017', './images/avatars/user4.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (6, '钱芳', '13900139005', '女', '320102199105210023', './images/avatars/user5.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (7, '孙磊', '13900139006', '男', '440103198807150014', './images/avatars/user6.jpg', 0, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (8, '李娜', '13900139007', '女', '510104199309080025', './images/avatars/user7.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (9, '周杰', '13900139008', '男', '350203199412110016', './images/avatars/user8.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (10, '吴敏', '13900139009', '女', '420106198910050027', './images/avatars/user9.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (11, '郑浩', '13900139020', '男', '610102198601200018', './images/avatars/user10.jpg', 1, 1, '2026-07-07 17:59:05');
INSERT IGNORE INTO `user` VALUES (12, '王静', '13900139021', '女', '330106199208250029', './images/avatars/user11.jpg', 1, 1, '2026-07-07 17:59:05');

-- ----------------------------
-- 13. orders (订单) - 12条
-- ----------------------------
INSERT IGNORE INTO `orders` VALUES (1, 'ORD20260701001', 4, 1, 1, '2026-07-01 12:00:00', '2026-07-01 12:45:00', 1, 82.00, NULL, NULL, '13900139001', '北京市东城区王府井大街1号', '张小明', '张小明', NULL, 'DELIVERY', 'IK20260701001', 1, '2026-07-01 12:00:00', '2026-07-01 12:45:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (2, 'ORD20260702001', 4, 2, 3, '2026-07-02 18:00:00', '2026-07-02 18:50:00', 2, 63.00, NULL, NULL, '13900139002', '上海市徐汇区南京路100号', '李晓红', '李晓红', NULL, 'DELIVERY', 'IK20260702001', 1, '2026-07-02 18:00:00', '2026-07-02 18:50:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (3, 'ORD20260703001', 4, 3, 4, '2026-07-03 12:00:00', '2026-07-03 12:30:00', 1, 86.00, NULL, NULL, '13900139003', '广州市天河区天河路385号', '王大军', '王大军', NULL, 'DELIVERY', 'IK20260703001', 1, '2026-07-03 12:00:00', '2026-07-03 12:30:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (4, 'ORD20260704001', 4, 4, 5, '2026-07-04 13:00:00', '2026-07-04 13:45:00', 1, 128.00, NULL, NULL, '13900139004', '南京市玄武区中山路1号', '赵伟', '赵伟', NULL, 'DELIVERY', 'IK20260704001', 1, '2026-07-04 13:00:00', '2026-07-04 13:45:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (5, 'ORD20260705001', 3, 5, 6, '2026-07-05 18:00:00', '2026-07-05 18:00:00', 1, 156.00, NULL, NULL, '13900139005', '杭州市西湖区文三路100号', '钱芳', '钱芳', 2, 'DINE_IN', 'IK20260705001', 1, '2026-07-05 18:00:00', '2026-07-05 18:00:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (6, 'ORD20260706001', 4, 6, 7, '2026-07-06 12:00:00', '2026-07-06 12:40:00', 2, 230.00, NULL, NULL, '13900139006', '重庆市渝北区新南路8号', '孙磊', '孙磊', NULL, 'DELIVERY', 'IK20260706001', 1, '2026-07-06 12:00:00', '2026-07-06 12:40:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (7, 'ORD20260707001', 4, 7, 8, '2026-07-07 18:00:00', '2026-07-07 18:30:00', 1, 98.00, NULL, NULL, '13900139007', '济南市市中区经十路100号', '李娜', '李娜', NULL, 'DELIVERY', 'IK20260707001', 1, '2026-07-07 18:00:00', '2026-07-07 18:30:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (8, 'ORD20260708001', 4, 8, 9, '2026-07-08 12:00:00', '2026-07-08 13:00:00', 1, 175.00, NULL, NULL, '13900139008', '厦门市思明区厦禾路200号', '周杰', '周杰', NULL, 'DELIVERY', 'IK20260708001', 1, '2026-07-08 12:00:00', '2026-07-08 13:00:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (9, 'ORD20260709001', 5, 9, 10, '2026-07-09 18:00:00', '2026-07-09 18:00:00', 1, 220.00, '超时未接单，系统自动取消', NULL, '13900139009', '武汉市武昌区珞喻路500号', '吴敏', '吴敏', 1, 'DINE_IN', 'IK20260709001', 1, '2026-07-09 18:00:00', '2026-07-10 13:16:49', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (10, 'ORD20260710001', 1, 10, 11, '2026-07-10 08:30:00', '2026-07-10 08:30:00', 1, 68.00, NULL, NULL, '13900139020', '西安市雁塔区高新路8号', '郑浩', '郑浩', NULL, 'DELIVERY', 'IK20260710001', 1, '2026-07-10 08:30:00', '2026-07-10 08:30:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (11, 'ORD20260711001', 4, 11, 12, '2026-07-11 12:00:00', '2026-07-11 12:45:00', 1, 56.00, NULL, NULL, '13900139021', '杭州市西湖区求是路5号', '王静', '王静', NULL, 'DELIVERY', 'IK20260711001', 1, '2026-07-11 12:00:00', '2026-07-11 12:45:00', 1, 1, 0);
INSERT IGNORE INTO `orders` VALUES (12, 'ORD20260711002', 2, 1, 1, '2026-07-11 18:00:00', '2026-07-11 18:00:00', 1, 120.00, '尽快送达', '19:00', '13900139001', '北京市东城区王府井大街1号', '张小明', '张小明', NULL, 'DELIVERY', 'IK20260711002', 1, '2026-07-11 18:00:00', '2026-07-11 18:00:00', 1, 1, 0);

-- ----------------------------
-- 14. order_detail (订单明细) - 35条
-- ----------------------------
-- 订单1: 红烧肉+宫保鸡丁+米饭+紫菜蛋花汤
INSERT IGNORE INTO `order_detail` VALUES (1, '红烧肉', './images/dishes/hongshaorou.jpg', 1, 1, NULL, '微辣', 1, 38.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (2, '宫保鸡丁', './images/dishes/gongbaojiding.jpg', 1, 2, NULL, '中辣', 1, 32.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (3, '米饭', './images/dishes/mifan.jpg', 1, 16, NULL, NULL, 2, 6.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (4, '紫菜蛋花汤', './images/dishes/zicidanhuatang.jpg', 1, 14, NULL, NULL, 1, 12.00, 1);

-- 订单2: 鱼香肉丝+麻婆豆腐+米饭
INSERT IGNORE INTO `order_detail` VALUES (5, '鱼香肉丝', './images/dishes/yuxiangrousi.jpg', 2, 3, NULL, '微辣', 1, 28.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (6, '麻婆豆腐', './images/dishes/mapodoufu.jpg', 2, 6, NULL, '中辣', 1, 18.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (7, '米饭', './images/dishes/mifan.jpg', 2, 16, NULL, NULL, 1, 3.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (8, '酸辣汤', './images/dishes/suanlatang.jpg', 2, 13, NULL, NULL, 1, 15.00, 1);

-- 订单3: 单人工作餐套餐
INSERT IGNORE INTO `order_detail` VALUES (9, '单人工作餐', './images/setmeal/danren.jpg', 3, NULL, 1, NULL, 1, 28.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (10, '单人豪华餐', './images/setmeal/danrenhaohua.jpg', 3, NULL, 2, NULL, 1, 38.00, 1);

-- 订单4: 双人浪漫套餐
INSERT IGNORE INTO `order_detail` VALUES (11, '双人浪漫套餐', './images/setmeal/shuangrenlangman.jpg', 4, NULL, 4, NULL, 1, 88.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (12, '米饭', './images/dishes/mifan.jpg', 4, 16, NULL, NULL, 2, 6.00, 1);

-- 订单5: 水煮牛肉+宫保鸡丁+米饭+酸辣汤
INSERT IGNORE INTO `order_detail` VALUES (13, '水煮牛肉', './images/dishes/shuizhuniurou.jpg', 5, 5, NULL, '特辣', 1, 42.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (14, '宫保鸡丁', './images/dishes/gongbaojiding.jpg', 5, 2, NULL, '微辣', 1, 32.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (15, '米饭', './images/dishes/mifan.jpg', 5, 16, NULL, NULL, 2, 6.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (16, '酸辣汤', './images/dishes/suanlatang.jpg', 5, 13, NULL, NULL, 1, 15.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (17, '珍珠奶茶', './images/dishes/zhenzhunicha.jpg', 5, 22, NULL, '少糖', 2, 24.00, 1);

-- 订单6: 四人欢乐套餐+饮料
INSERT IGNORE INTO `order_detail` VALUES (18, '四人欢乐套餐', './images/setmeal/sirenhuanle.jpg', 6, NULL, 6, NULL, 1, 128.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (19, '红烧肉', './images/dishes/hongshaorou.jpg', 6, 1, NULL, '中辣', 2, 76.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (20, '酸梅汤', './images/dishes/suanmeitang.jpg', 6, 24, NULL, NULL, 2, 20.00, 1);

-- 订单7: 糖醋里脊+回锅肉+米饭
INSERT IGNORE INTO `order_detail` VALUES (21, '糖醋里脊', './images/dishes/tangculiji.jpg', 7, 4, NULL, '酸甜', 1, 35.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (22, '回锅肉', './images/dishes/huiguorou.jpg', 7, 7, NULL, '微辣', 1, 30.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (23, '米饭', './images/dishes/mifan.jpg', 7, 16, NULL, NULL, 2, 6.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (24, '紫菜蛋花汤', './images/dishes/zicidanhuatang.jpg', 7, 14, NULL, NULL, 1, 12.00, 1);

-- 订单8: 商务简餐+额外菜品
INSERT IGNORE INTO `order_detail` VALUES (25, '商务简餐', './images/setmeal/shangwujiancan.jpg', 8, NULL, 8, NULL, 1, 58.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (26, '口水鸡', './images/dishes/koushuiji.jpg', 8, 11, NULL, '中辣', 1, 22.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (27, '拍黄瓜', './images/dishes/paihuanggua.jpg', 8, 12, NULL, NULL, 1, 10.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (28, '扬州炒饭', './images/dishes/yangzhouchaofan.jpg', 8, 17, NULL, NULL, 2, 36.00, 1);

-- 订单9: 商务宴请套餐
INSERT IGNORE INTO `order_detail` VALUES (29, '商务宴请套餐', './images/setmeal/shangwuyanqing.jpg', 9, NULL, 9, NULL, 1, 168.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (30, '米饭', './images/dishes/mifan.jpg', 9, 16, NULL, NULL, 3, 9.00, 1);

-- 订单10: 儿童营养套餐+小吃
INSERT IGNORE INTO `order_detail` VALUES (31, '儿童营养套餐', './images/setmeal/ertongyingyang.jpg', 10, NULL, 10, NULL, 1, 25.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (32, '薯条', './images/dishes/shutiao.jpg', 10, 21, NULL, NULL, 1, 10.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (33, '炸鸡翅', './images/dishes/zhajichi.jpg', 10, 20, NULL, '微辣', 1, 15.00, 1);
INSERT IGNORE INTO `order_detail` VALUES (34, '可乐', './images/dishes/kele.jpg', 10, 27, NULL, NULL, 1, 5.00, 1);

-- 订单11: 蒜蓉西兰花+麻婆豆腐+米饭
INSERT IGNORE INTO `order_detail` VALUES (35, '蒜蓉西兰花', './images/dishes/suanrongxilanhua.jpg', 11, 8, NULL, '蒜香', 1, 16.00, 1);

-- ----------------------------
-- 15. shopping_cart (购物车) - 10条
-- ----------------------------
INSERT IGNORE INTO `shopping_cart` VALUES (1, '红烧肉', './images/dishes/hongshaorou.jpg', 1, 1, NULL, '微辣', 1, 38.00, '2026-07-11 18:00:00', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (2, '宫保鸡丁', './images/dishes/gongbaojiding.jpg', 1, 2, NULL, '中辣', 1, 32.00, '2026-07-11 18:01:00', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (3, '米饭', './images/dishes/mifan.jpg', 1, 16, NULL, NULL, 2, 6.00, '2026-07-11 18:01:30', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (4, '双人浪漫套餐', './images/setmeal/shuangrenlangman.jpg', 2, NULL, 4, NULL, 1, 88.00, '2026-07-11 18:02:00', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (5, '酸辣汤', './images/dishes/suanlatang.jpg', 3, 13, NULL, NULL, 1, 15.00, '2026-07-11 18:03:00', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (6, '鱼香肉丝', './images/dishes/yuxiangrousi.jpg', 3, 3, NULL, '微辣', 1, 28.00, '2026-07-11 18:03:30', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (7, '珍珠奶茶', './images/dishes/zhenzhunicha.jpg', 4, 22, NULL, '少糖', 2, 24.00, '2026-07-11 18:04:00', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (8, '商务简餐', './images/setmeal/shangwujiancan.jpg', 5, NULL, 8, NULL, 1, 58.00, '2026-07-11 18:05:00', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (9, '水煮牛肉', './images/dishes/shuizhuniurou.jpg', 6, 5, NULL, '特辣', 1, 42.00, '2026-07-11 18:06:00', 1);
INSERT IGNORE INTO `shopping_cart` VALUES (10, '四人欢乐套餐', './images/setmeal/sirenhuanle.jpg', 7, NULL, 6, NULL, 1, 128.00, '2026-07-11 18:07:00', 1);

SET FOREIGN_KEY_CHECKS = 1;
