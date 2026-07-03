-- ========================================
-- 餐厅管理系统 - 测试数据
-- 生成时间: 2026-07-03
-- 说明: 所有模块至少10条测试数据
-- ========================================

-- 设置时区
SET time_zone = '+08:00';

-- ========================================
-- 1. 租户表 (tenant) - 10条
-- ========================================
INSERT INTO tenant (id, name, phone, address, password_type, status, create_time, update_time) VALUES
(1, '测试餐厅总部', '13800138001', '北京市朝阳区建国路88号', 'MD5', 1, NOW(), NOW()),
(2, '美味餐厅分店', '13800138002', '上海市浦东新区陆家嘴', 'MD5', 1, NOW(), NOW()),
(3, '香满楼餐厅', '13800138003', '广州市天河区珠江新城', 'MD5', 1, NOW(), NOW()),
(4, '蜀香园', '13800138004', '成都市锦江区春熙路', 'MD5', 1, NOW(), NOW()),
(5, '海鲜大酒楼', '13800138005', '深圳市南山区海岸城', 'MD5', 1, NOW(), NOW()),
(6, '湘菜馆', '13800138006', '长沙市岳麓区麓山南路', 'MD5', 1, NOW(), NOW()),
(7, '粤式茶餐厅', '13800138007', '香港九龙旺角', 'MD5', 1, NOW(), NOW()),
(8, '日式料理店', '13800138008', '杭州市西湖区龙井路', 'MD5', 1, NOW(), NOW()),
(9, '韩式烤肉', '13800138009', '武汉市武昌区光谷', 'MD5', 1, NOW(), NOW()),
(10, '西餐厅', '13800138010', '南京市鼓楼区新街口', 'MD5', 1, NOW(), NOW());

-- ========================================
-- 2. 员工表 (employee) - 15条
-- ========================================
INSERT INTO employee (id, username, name, password, password_type, phone, sex, id_number, status, tenant_id, create_time, update_time) VALUES
(1, 'admin', '系统管理员', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138001', '男', '110101199001011234', 1, 1, NOW(), NOW()),
(2, 'zhangsan', '张三', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138002', '男', '110101199002021234', 1, 1, NOW(), NOW()),
(3, 'lisi', '李四', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138003', '女', '110101199003031234', 1, 1, NOW(), NOW()),
(4, 'wangwu', '王五', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138004', '男', '110101199004041234', 1, 1, NOW(), NOW()),
(5, 'zhaoliu', '赵六', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138005', '女', '110101199005051234', 1, 1, NOW(), NOW()),
(6, 'sunqi', '孙七', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138006', '男', '110101199006061234', 1, 1, NOW(), NOW()),
(7, 'zhouba', '周八', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138007', '女', '110101199007071234', 1, 1, NOW(), NOW()),
(8, 'wujiu', '吴九', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138008', '男', '110101199008081234', 1, 1, NOW(), NOW()),
(9, 'zhengshi', '郑十', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138009', '女', '110101199009091234', 1, 1, NOW(), NOW()),
(10, 'qianyi', '钱十一', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138010', '男', '110101199010101234', 1, 1, NOW(), NOW()),
(11, 'linger', '林十二', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138011', '女', '110101199011111234', 1, 1, NOW(), NOW()),
(12, 'huangsi', '黄十三', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138012', '男', '110101199012121234', 1, 2, NOW(), NOW()),
(13, 'chenyi', '陈十四', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138013', '女', '110101199101131234', 1, 2, NOW(), NOW()),
(14, 'aiwu', '艾武', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138014', '男', '110101199102141234', 1, 3, NOW(), NOW()),
(15, 'zhongliu', '钟流', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138015', '女', '110101199103151234', 1, 3, NOW(), NOW());

-- ========================================
-- 3. 分类表 (category) - 菜品分类10条 + 套餐分类10条
-- ========================================
INSERT INTO category (id, tenant_id, type, name, sort, create_time, update_time, is_deleted) VALUES
-- 菜品分类 (type=1)
(1, 1, 1, '热菜', 1, NOW(), NOW(), 0),
(2, 1, 1, '凉菜', 2, NOW(), NOW(), 0),
(3, 1, 1, '汤类', 3, NOW(), NOW(), 0),
(4, 1, 1, '主食', 4, NOW(), NOW(), 0),
(5, 1, 1, '饮品', 5, NOW(), NOW(), 0),
(6, 1, 1, '小吃', 6, NOW(), NOW(), 0),
(7, 1, 1, '海鲜', 7, NOW(), NOW(), 0),
(8, 1, 1, '素菜', 8, NOW(), NOW(), 0),
(9, 1, 1, '荤菜', 9, NOW(), NOW(), 0),
(10, 1, 1, '点心', 10, NOW(), NOW(), 0),
-- 套餐分类 (type=2)
(11, 1, 2, '单人套餐', 1, NOW(), NOW(), 0),
(12, 1, 2, '双人套餐', 2, NOW(), NOW(), 0),
(13, 1, 2, '家庭套餐', 3, NOW(), NOW(), 0),
(14, 1, 2, '商务套餐', 4, NOW(), NOW(), 0),
(15, 1, 2, '儿童套餐', 5, NOW(), NOW(), 0),
(16, 1, 2, '素食套餐', 6, NOW(), NOW(), 0),
(17, 1, 2, '海鲜套餐', 7, NOW(), NOW(), 0),
(18, 1, 2, '火锅套餐', 8, NOW(), NOW(), 0),
(19, 1, 2, '烧烤套餐', 9, NOW(), NOW(), 0),
(20, 1, 2, '定制套餐', 10, NOW(), NOW(), 0);

-- ========================================
-- 4. 菜品表 (dish) - 20条
-- ========================================
INSERT INTO dish (id, tenant_id, name, category_id, price, code, image, description, status, sort, create_time, update_time, is_deleted) VALUES
(1, 1, '红烧肉', 1, 5800, 'DISH001', 'images/dishes/hongshaorou.jpg', '经典家常菜，肥而不腻', 1, 1, NOW(), NOW(), 0),
(2, 1, '宫保鸡丁', 1, 4800, 'DISH002', 'images/dishes/gongbaojiding.jpg', '川菜经典，麻辣鲜香', 1, 2, NOW(), NOW(), 0),
(3, 1, '鱼香肉丝', 1, 4600, 'DISH003', 'images/dishes/yuxiangrous.jpg', '酸甜可口，下饭神器', 1, 3, NOW(), NOW(), 0),
(4, 1, '麻婆豆腐', 1, 3800, 'DISH004', 'images/dishes/mapotoufu.jpg', '麻辣鲜烫，回味无穷', 1, 4, NOW(), NOW(), 0),
(5, 1, '糖醋里脊', 1, 5200, 'DISH005', 'images/dishes/tangculiji.jpg', '外酥里嫩，酸甜适中', 1, 5, NOW(), NOW(), 0),
(6, 1, '凉拌黄瓜', 2, 1800, 'DISH006', 'images/dishes/liangbanghuanggua.jpg', '清爽解腻，开胃小菜', 1, 6, NOW(), NOW(), 0),
(7, 1, '拍黄瓜', 2, 1600, 'DISH007', 'images/dishes/paohuanggua.jpg', '蒜香浓郁，爽脆可口', 1, 7, NOW(), NOW(), 0),
(8, 1, '老醋花生', 2, 2200, 'DISH008', 'images/dishes/laocuhuasheng.jpg', '酸爽开胃，下酒好菜', 1, 8, NOW(), NOW(), 0),
(9, 1, '西湖牛肉羹', 3, 2800, 'DISH009', 'images/dishes/xihuniurougeng.jpg', '鲜美滑嫩，营养丰富', 1, 9, NOW(), NOW(), 0),
(10, 1, '番茄鸡蛋汤', 3, 2000, 'DISH010', 'images/dishes/fanqijidantang.jpg', '家常汤品，酸甜适口', 1, 10, NOW(), NOW(), 0),
(11, 1, '扬州炒饭', 4, 3200, 'DISH011', 'images/dishes/yangzhouchaofan.jpg', '粒粒分明，配料丰富', 1, 11, NOW(), NOW(), 0),
(12, 1, '牛肉面', 4, 3600, 'DISH012', 'images/dishes/niuroumian.jpg', '汤浓肉烂，面条劲道', 1, 12, NOW(), NOW(), 0),
(13, 1, '珍珠奶茶', 5, 1800, 'DISH013', 'images/drinks/zhenzhunaicha.jpg', '香浓丝滑，Q弹珍珠', 1, 13, NOW(), NOW(), 0),
(14, 1, '鲜榨果汁', 5, 2200, 'DISH014', 'images/drinks/xianzhaguozhi.jpg', '新鲜水果，营养健康', 1, 14, NOW(), NOW(), 0),
(15, 1, '薯条', 6, 1500, 'DISH015', 'images/dishes/shutiao.jpg', '外脆内软，咸香可口', 1, 15, NOW(), NOW(), 0),
(16, 1, '鸡米花', 6, 1800, 'DISH016', 'images/dishes/jimihua.jpg', '香脆可口，适合分享', 1, 16, NOW(), NOW(), 0),
(17, 1, '清蒸鲈鱼', 7, 8800, 'DISH017', 'images/dishes/qingzhengluyu.jpg', '肉质鲜嫩，原汁原味', 1, 17, NOW(), NOW(), 0),
(18, 1, '蒜蓉西兰花', 8, 2600, 'DISH018', 'images/dishes/suorongxilanhua.jpg', '清淡爽口，营养丰富', 1, 18, NOW(), NOW(), 0),
(19, 1, '辣子鸡', 9, 6800, 'DISH019', 'images/dishes/laziji.jpg', '麻辣鲜香，越吃越上瘾', 1, 19, NOW(), NOW(), 0),
(20, 1, '小笼包', 10, 2400, 'DISH020', 'images/dishes/xiaolongbao.jpg', '皮薄馅多，汤汁鲜美', 1, 20, NOW(), NOW(), 0);

-- ========================================
-- 5. 菜品口味表 (dish_flavor) - 30条
-- ========================================
INSERT INTO dish_flavor (id, tenant_id, dish_id, name, value, create_time, update_time, is_deleted) VALUES
(1, 1, 1, '辣度', '["不辣","微辣","中辣","重辣"]', NOW(), NOW(), 0),
(2, 1, 2, '辣度', '["不辣","微辣","中辣","重辣"]', NOW(), NOW(), 0),
(3, 1, 2, '忌口', '["不要葱","不要蒜","不要香菜"]', NOW(), NOW(), 0),
(4, 1, 3, '辣度', '["微辣","中辣","重辣"]', NOW(), NOW(), 0),
(5, 1, 4, '辣度', '["不辣","微辣","中辣","重辣"]', NOW(), NOW(), 0),
(6, 1, 5, '口味', '["酸甜","咸甜"]', NOW(), NOW(), 0),
(7, 1, 9, '温度', '["热饮","常温","去冰","少冰","多冰"]', NOW(), NOW(), 0),
(8, 1, 10, '温度', '["热饮","常温"]', NOW(), NOW(), 0),
(9, 1, 11, '口味', '["原味","加蛋","加肉"]', NOW(), NOW(), 0),
(10, 1, 12, '辣度', '["不辣","微辣","中辣"]', NOW(), NOW(), 0),
(11, 1, 13, '甜度', '["无糖","少糖","半糖","多糖","全糖"]', NOW(), NOW(), 0),
(12, 1, 13, '温度', '["热饮","常温","去冰","少冰","多冰"]', NOW(), NOW(), 0),
(13, 1, 14, '甜度', '["无糖","少糖","半糖","多糖"]', NOW(), NOW(), 0),
(14, 1, 14, '冰度', '["常温","少冰","多冰"]', NOW(), NOW(), 0),
(15, 1, 17, '口味', '["清蒸","红烧","糖醋"]', NOW(), NOW(), 0),
(16, 1, 19, '辣度', '["微辣","中辣","重辣","特辣"]', NOW(), NOW(), 0),
(17, 1, 19, '忌口', '["不要葱","不要蒜","不要香菜","不要辣"]', NOW(), NOW(), 0),
(18, 2, 1, '辣度', '["不辣","微辣","中辣"]', NOW(), NOW(), 0),
(19, 2, 2, '辣度', '["不辣","微辣","中辣","重辣"]', NOW(), NOW(), 0),
(20, 2, 4, '辣度', '["微辣","中辣","重辣"]', NOW(), NOW(), 0),
(21, 1, 6, '口味', '["蒜香","麻辣","五香"]', NOW(), NOW(), 0),
(22, 1, 7, '口味', '["蒜香","麻辣","酸辣"]', NOW(), NOW(), 0),
(23, 1, 8, '口味', '["原味","加醋"]', NOW(), NOW(), 0),
(24, 1, 15, '口味', '["原味","番茄","芝士"]', NOW(), NOW(), 0),
(25, 1, 16, '口味', '["原味","辣味","番茄味"]', NOW(), NOW(), 0),
(26, 1, 18, '口味', '["清炒","蒜蓉","白灼"]', NOW(), NOW(), 0),
(27, 1, 20, '口味', '["鲜肉","蟹黄","虾仁"]', NOW(), NOW(), 0),
(28, 1, 11, '加料', '["加蛋","加肉","加蔬菜"]', NOW(), NOW(), 0),
(29, 1, 5, '口味', '["酸甜","咸甜","糖醋"]', NOW(), NOW(), 0),
(30, 1, 3, '辣度', '["不辣","微辣","中辣"]', NOW(), NOW(), 0);

-- ========================================
-- 6. 套餐表 (setmeal) - 15条
-- ========================================
INSERT INTO setmeal (id, tenant_id, category_id, name, price, status, code, description, image, create_time, update_time, is_deleted) VALUES
(1, 1, 11, '单人工作餐', 2800, 1, 'SET001', '适合一人用餐，包含主食+菜品+饮品', 'images/setmeal/single.jpg', NOW(), NOW(), 0),
(2, 1, 12, '双人浪漫套餐', 8800, 1, 'SET002', '适合情侣或朋友，包含2主菜+1汤+2饮品', 'images/setmeal/couple.jpg', NOW(), NOW(), 0),
(3, 1, 13, '家庭欢聚套餐', 16800, 1, 'SET003', '适合3-4人家庭聚餐', 'images/setmeal/family.jpg', NOW(), NOW(), 0),
(4, 1, 14, '商务洽谈套餐', 12800, 1, 'SET004', '适合商务宴请，菜品精致', 'images/setmeal/business.jpg', NOW(), NOW(), 0),
(5, 1, 15, '儿童营养套餐', 2600, 1, 'SET005', '专为儿童设计，营养均衡', 'images/setmeal/kids.jpg', NOW(), NOW(), 0),
(6, 1, 16, '素食健康套餐', 3200, 1, 'SET006', '全素菜品，健康美味', 'images/setmeal/vegetarian.jpg', NOW(), NOW(), 0),
(7, 1, 17, '海鲜盛宴套餐', 28800, 1, 'SET007', '精选海鲜，奢华享受', 'images/setmeal/seafood.jpg', NOW(), NOW(), 0),
(8, 1, 18, '火锅狂欢套餐', 19800, 1, 'SET008', '适合6-8人火锅聚餐', 'images/setmeal/hotpot.jpg', NOW(), NOW(), 0),
(9, 1, 19, '烧烤派对套餐', 15800, 1, 'SET009', '烧烤串+啤酒，聚会首选', 'images/setmeal/bbq.jpg', NOW(), NOW(), 0),
(10, 1, 20, '定制生日套餐', 21800, 1, 'SET010', '生日宴定制，含蛋糕', 'images/setmeal/birthday.jpg', NOW(), NOW(), 0),
(11, 1, 11, '经济单人餐', 1980, 1, 'SET011', '经济实惠，饱腹之选', 'images/setmeal/economy.jpg', NOW(), NOW(), 0),
(12, 1, 12, '朋友聚会套餐', 11800, 1, 'SET012', '3-4人聚餐首选', 'images/setmeal/friends.jpg', NOW(), NOW(), 0),
(13, 1, 13, '三代同堂套餐', 25800, 1, 'SET013', '适合5-6人家庭聚餐', 'images/setmeal/family2.jpg', NOW(), NOW(), 0),
(14, 1, 14, '高管商务套餐', 18800, 1, 'SET014', '高端商务宴请', 'images/setmeal/vip.jpg', NOW(), NOW(), 0),
(15, 1, 15, '宝宝套餐', 2200, 1, 'SET015', '软糯易消化，适合宝宝', 'images/setmeal/baby.jpg', NOW(), NOW(), 0);

-- ========================================
-- 7. 套餐菜品关联表 (setmeal_dish) - 50条
-- ========================================
INSERT INTO setmeal_dish (id, tenant_id, setmeal_id, dish_id, name, price, copies, sort, create_time, update_time, is_deleted) VALUES
(1, 1, '1', '11', '扬州炒饭', 3200, 1, 1, NOW(), NOW(), 0),
(2, 1, '1', '1', '红烧肉', 5800, 1, 2, NOW(), NOW(), 0),
(3, 1, '1', '13', '珍珠奶茶', 1800, 1, 3, NOW(), NOW(), 0),

(4, 1, '2', '2', '宫保鸡丁', 4800, 1, 1, NOW(), NOW(), 0),
(5, 1, '2', '5', '糖醋里脊', 5200, 1, 2, NOW(), NOW(), 0),
(6, 1, '2', '9', '西湖牛肉羹', 2800, 1, 3, NOW(), NOW(), 0),
(7, 1, '2', '13', '珍珠奶茶', 1800, 2, 4, NOW(), NOW(), 0),

(8, 1, '3', '1', '红烧肉', 5800, 1, 1, NOW(), NOW(), 0),
(9, 1, '3', '2', '宫保鸡丁', 4800, 1, 2, NOW(), NOW(), 0),
(10, 1, '3', '17', '清蒸鲈鱼', 8800, 1, 3, NOW(), NOW(), 0),
(11, 1, '3', '10', '番茄鸡蛋汤', 2000, 1, 4, NOW(), NOW(), 0),
(12, 1, '3', '11', '扬州炒饭', 3200, 4, 5, NOW(), NOW(), 0),
(13, 1, '3', '14', '鲜榨果汁', 2200, 4, 6, NOW(), NOW(), 0),

(14, 1, '4', '3', '鱼香肉丝', 4600, 1, 1, NOW(), NOW(), 0),
(15, 1, '4', '4', '麻婆豆腐', 3800, 1, 2, NOW(), NOW(), 0),
(16, 1, '4', '5', '糖醋里脊', 5200, 1, 3, NOW(), NOW(), 0),
(17, 1, '4', '9', '西湖牛肉羹', 2800, 1, 4, NOW(), NOW(), 0),
(18, 1, '4', '12', '牛肉面', 3600, 4, 5, NOW(), NOW(), 0),

(19, 1, '5', '15', '薯条', 1500, 1, 1, NOW(), NOW(), 0),
(20, 1, '5', '16', '鸡米花', 1800, 1, 2, NOW(), NOW(), 0),
(21, 1, '5', '20', '小笼包', 2400, 1, 3, NOW(), NOW(), 0),
(22, 1, '5', '13', '珍珠奶茶', 1800, 1, 4, NOW(), NOW(), 0),

(23, 1, '6', '18', '蒜蓉西兰花', 2600, 1, 1, NOW(), NOW(), 0),
(24, 1, '6', '6', '凉拌黄瓜', 1800, 1, 2, NOW(), NOW(), 0),
(25, 1, '6', '10', '番茄鸡蛋汤', 2000, 1, 3, NOW(), NOW(), 0),
(26, 1, '6', '11', '扬州炒饭', 3200, 1, 4, NOW(), NOW(), 0),

(27, 1, '7', '17', '清蒸鲈鱼', 8800, 1, 1, NOW(), NOW(), 0),
(28, 1, '7', '8', '老醋花生', 2200, 1, 2, NOW(), NOW(), 0),
(29, 1, '7', '6', '凉拌黄瓜', 1800, 1, 3, NOW(), NOW(), 0),
(30, 1, '7', '14', '鲜榨果汁', 2200, 4, 4, NOW(), NOW(), 0),

(31, 1, '8', '1', '红烧肉', 5800, 2, 1, NOW(), NOW(), 0),
(32, 1, '8', '19', '辣子鸡', 6800, 1, 2, NOW(), NOW(), 0),
(33, 1, '8', '18', '蒜蓉西兰花', 2600, 1, 3, NOW(), NOW(), 0),
(34, 1, '8', '10', '番茄鸡蛋汤', 2000, 4, 4, NOW(), NOW(), 0),
(35, 1, '8', '12', '牛肉面', 3600, 4, 5, NOW(), NOW(), 0),

(36, 1, '9', '16', '鸡米花', 1800, 2, 1, NOW(), NOW(), 0),
(37, 1, '9', '15', '薯条', 1500, 2, 2, NOW(), NOW(), 0),
(38, 1, '9', '19', '辣子鸡', 6800, 1, 3, NOW(), NOW(), 0),
(39, 1, '9', '14', '鲜榨果汁', 2200, 4, 4, NOW(), NOW(), 0),

(40, 1, '10', '1', '红烧肉', 5800, 1, 1, NOW(), NOW(), 0),
(41, 1, '10', '5', '糖醋里脊', 5200, 1, 2, NOW(), NOW(), 0),
(42, 1, '10', '17', '清蒸鲈鱼', 8800, 1, 3, NOW(), NOW(), 0),
(43, 1, '10', '20', '小笼包', 2400, 1, 4, NOW(), NOW(), 0),
(44, 1, '10', '13', '珍珠奶茶', 1800, 4, 5, NOW(), NOW(), 0),

(45, 1, '11', '11', '扬州炒饭', 3200, 1, 1, NOW(), NOW(), 0),
(46, 1, '11', '7', '拍黄瓜', 1600, 1, 2, NOW(), NOW(), 0),

(47, 1, '12', '2', '宫保鸡丁', 4800, 1, 1, NOW(), NOW(), 0),
(48, 1, '12', '3', '鱼香肉丝', 4600, 1, 2, NOW(), NOW(), 0),
(49, 1, '12', '11', '扬州炒饭', 3200, 2, 3, NOW(), NOW(), 0),
(50, 1, '12', '14', '鲜榨果汁', 2200, 2, 4, NOW(), NOW(), 0);

-- ========================================
-- 8. 用户表 (user) - 20条
-- ========================================
INSERT INTO user (id, tenant_id, name, phone, sex, id_number, avatar, status) VALUES
(1, 1, '张小明', '13900139001', '男', '110101199001011001', 'images/avatars/user1.jpg', 1),
(2, 1, '李晓红', '13900139002', '女', '110101199002021002', 'images/avatars/user2.jpg', 1),
(3, 1, '王大军', '13900139003', '男', '110101199003031003', 'images/avatars/user3.jpg', 1),
(4, 1, '赵小美', '13900139004', '女', '110101199004041004', 'images/avatars/user4.jpg', 1),
(5, 1, '孙悟空', '13900139005', '男', '110101199005051005', 'images/avatars/user5.jpg', 1),
(6, 1, '猪八戒', '13900139006', '男', '110101199006061006', 'images/avatars/user6.jpg', 1),
(7, 1, '沙和尚', '13900139007', '男', '110101199007071007', 'images/avatars/user7.jpg', 1),
(8, 1, '白骨精', '13900139008', '女', '110101199008081008', 'images/avatars/user8.jpg', 1),
(9, 1, '唐三藏', '13900139009', '男', '110101199009091009', 'images/avatars/user9.jpg', 1),
(10, 1, '白素贞', '13900139010', '女', '110101199010101010', 'images/avatars/user10.jpg', 1),
(11, 1, '小青', '13900139011', '女', '110101199011111011', 'images/avatars/user11.jpg', 1),
(12, 1, '许仙', '13900139012', '男', '110101199012121012', 'images/avatars/user12.jpg', 1),
(13, 1, '法海', '13900139013', '男', '110101199101131013', 'images/avatars/user13.jpg', 1),
(14, 1, '林黛玉', '13900139014', '女', '110101199102141014', 'images/avatars/user14.jpg', 1),
(15, 1, '贾宝玉', '13900139015', '男', '110101199103151015', 'images/avatars/user15.jpg', 1),
(16, 1, '薛宝钗', '13900139016', '女', '110101199104161016', 'images/avatars/user16.jpg', 1),
(17, 1, '王熙凤', '13900139017', '女', '110101199105171017', 'images/avatars/user17.jpg', 1),
(18, 1, '刘姥姥', '13900139018', '女', '110101199106181018', 'images/avatars/user18.jpg', 1),
(19, 1, '宋江', '13900139019', '男', '110101199107191019', 'images/avatars/user19.jpg', 1),
(20, 1, '武松', '13900139020', '男', '110101199108201020', 'images/avatars/user20.jpg', 1);

-- ========================================
-- 9. 地址簿表 (address_book) - 15条
-- ========================================
INSERT INTO address_book (id, user_id, consignee, sex, phone, province_code, province_name, city_code, city_name, district_code, district_name, detail, label, is_default, create_time, update_time, is_deleted) VALUES
(1, 1, '张小明', '男', '13900139001', '110000', '北京市', '110100', '北京市', '110101', '东城区', '北京市东城区王府井大街1号', '家', 1, NOW(), NOW(), 0),
(2, 1, '张小明', '男', '13900139001', '110000', '北京市', '110100', '北京市', '110102', '西城区', '北京市西城区金融街8号', '公司', 0, NOW(), NOW(), 0),
(3, 2, '李晓红', '女', '13900139002', '310000', '上海市', '310100', '上海市', '310104', '徐汇区', '上海市徐汇区南京路100号', '家', 1, NOW(), NOW(), 0),
(4, 3, '王大军', '男', '13900139003', '440000', '广东省', '440100', '广州市', '440103', '荔湾区', '广州市荔湾区中山路50号', '家', 1, NOW(), NOW(), 0),
(5, 4, '赵小美', '女', '13900139004', '510000', '四川省', '510100', '成都市', '510104', '锦江区', '成都市锦江区春熙路88号', '家', 1, NOW(), NOW(), 0),
(6, 5, '孙悟空', '男', '13900139005', '440000', '广东省', '440300', '深圳市', '440304', '南山区', '深圳市南山区科技园', '公司', 1, NOW(), NOW(), 0),
(7, 6, '猪八戒', '男', '13900139006', '330000', '浙江省', '330100', '杭州市', '330102', '上城区', '杭州市上城区西湖路1号', '家', 1, NOW(), NOW(), 0),
(8, 7, '沙和尚', '男', '13900139007', '420000', '湖北省', '420100', '武汉市', '420106', '武昌区', '武汉市武昌区珞珈山', '家', 1, NOW(), NOW(), 0),
(9, 8, '白骨精', '女', '13900139008', '320000', '江苏省', '320100', '南京市', '320102', '玄武区', '南京市玄武区中山陵', '家', 1, NOW(), NOW(), 0),
(10, 9, '唐三藏', '男', '13900139009', '610000', '陕西省', '610100', '西安市', '610103', '碑林区', '西安市碑林区大雁塔', '公司', 1, NOW(), NOW(), 0),
(11, 10, '白素贞', '女', '13900139010', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '杭州市西湖区雷峰塔', '家', 1, NOW(), NOW(), 0),
(12, 11, '小青', '女', '13900139011', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '杭州市西湖区断桥', '家', 1, NOW(), NOW(), 0),
(13, 12, '许仙', '男', '13900139012', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '杭州市西湖区保俶塔', '公司', 1, NOW(), NOW(), 0),
(14, 13, '法海', '男', '13900139013', '320000', '江苏省', '320100', '南京市', '320111', '浦口区', '南京市浦口区金山寺', '家', 1, NOW(), NOW(), 0),
(15, 14, '林黛玉', '女', '13900139014', '210000', '辽宁省', '210100', '沈阳市', '210102', '和平区', '沈阳市和平区北陵', '家', 1, NOW(), NOW(), 0);

-- ========================================
-- 10. 会员等级表 (member_level) - 5条
-- ========================================
INSERT INTO member_level (id, tenant_id, name, min_points, discount, created_time) VALUES
(1, 1, '普通会员', 0, 1.00, NOW()),
(2, 1, '银卡会员', 1000, 0.95, NOW()),
(3, 1, '金卡会员', 5000, 0.90, NOW()),
(4, 1, '钻石会员', 10000, 0.85, NOW()),
(5, 1, '至尊会员', 50000, 0.80, NOW());

-- ========================================
-- 11. 会员表 (member) - 15条
-- ========================================
INSERT INTO member (id, tenant_id, user_id, level_id, name, phone, points, balance, total_consumption, status, created_time, updated_time) VALUES
(1, 1, 1, 3, '张小明', '13900139001', 6500, 1580.50, 3680.00, 1, NOW(), NOW()),
(2, 1, 2, 2, '李晓红', '13900139002', 2800, 680.00, 1520.00, 1, NOW(), NOW()),
(3, 1, 3, 4, '王大军', '13900139003', 12500, 3580.00, 8960.00, 1, NOW(), NOW()),
(4, 1, 4, 1, '赵小美', '13900139004', 500, 120.00, 360.00, 1, NOW(), NOW()),
(5, 1, 5, 5, '孙悟空', '13900139005', 58000, 8888.88, 25800.00, 1, NOW(), NOW()),
(6, 1, 6, 3, '猪八戒', '13900139006', 7200, 2100.00, 4680.00, 1, NOW(), NOW()),
(7, 1, 7, 2, '沙和尚', '13900139007', 3500, 850.00, 1850.00, 1, NOW(), NOW()),
(8, 1, 8, 1, '白骨精', '13900139008', 300, 50.00, 180.00, 1, NOW(), NOW()),
(9, 1, 9, 4, '唐三藏', '13900139009', 11000, 5200.00, 12500.00, 1, NOW(), NOW()),
(10, 1, 10, 3, '白素贞', '13900139010', 6800, 1680.00, 3580.00, 1, NOW(), NOW()),
(11, 1, 11, 2, '小青', '13900139011', 2400, 580.00, 1280.00, 1, NOW(), NOW()),
(12, 1, 12, 1, '许仙', '13900139012', 800, 200.00, 560.00, 1, NOW(), NOW()),
(13, 1, 13, 3, '法海', '13900139013', 5600, 1380.00, 2980.00, 1, NOW(), NOW()),
(14, 1, 14, 2, '林黛玉', '13900139014', 3200, 780.00, 1680.00, 1, NOW(), NOW()),
(15, 1, 15, 4, '贾宝玉', '13900139015', 9800, 2800.00, 6800.00, 1, NOW(), NOW());

-- ========================================
-- 12. 积分记录表 (points_record) - 20条
-- ========================================
INSERT INTO points_record (id, member_id, type, points, biz_type, biz_id, remark, created_time) VALUES
(1, 1, 'add', 500, '消费积分', 1, '消费满100元赠送500积分', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 2, 'add', 300, '消费积分', 2, '消费满60元赠送300积分', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(3, 3, 'add', 800, '消费积分', 3, '消费满200元赠送800积分', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(4, 5, 'add', 2000, '消费积分', 5, '消费满500元赠送2000积分', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 1, 'reduce', 100, '积分兑换', 1, '兑换50元代金券', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 3, 'add', 1200, '消费积分', 6, '消费满300元赠送1200积分', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 6, 'add', 600, '消费积分', 7, '消费满150元赠送600积分', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(8, 9, 'add', 1500, '消费积分', 9, '消费满400元赠送1500积分', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(9, 10, 'add', 700, '消费积分', 10, '消费满180元赠送700积分', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10, 15, 'add', 1000, '消费积分', 15, '消费满250元赠送1000积分', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 2, 'reduce', 50, '积分兑换', 2, '兑换小食一份', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(12, 4, 'add', 200, '消费积分', 4, '消费满50元赠送200积分', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(13, 7, 'add', 400, '消费积分', 7, '消费满100元赠送400积分', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(14, 13, 'add', 900, '消费积分', 13, '消费满230元赠送900积分', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(15, 14, 'add', 350, '消费积分', 14, '消费满90元赠送350积分', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(16, 1, 'add', 150, '签到积分', 1, '连续签到7天奖励', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17, 3, 'reduce', 500, '积分兑换', 3, '兑换菜品一份', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(18, 5, 'add', 3000, '活动奖励', 5, '会员日双倍积分', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(19, 6, 'reduce', 200, '积分兑换', 6, '兑换饮品一杯', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(20, 10, 'add', 600, '推荐奖励', 10, '推荐好友注册奖励', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ========================================
-- 13. 充值记录表 (recharge_record) - 10条
-- ========================================
INSERT INTO recharge_record (id, member_id, amount, gift_amount, payment_method, created_time) VALUES
(1, 1, 500.00, 50.00, '微信支付', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2, 3, 1000.00, 150.00, '支付宝', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(3, 5, 2000.00, 300.00, '微信支付', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(4, 9, 800.00, 80.00, '银行卡', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(5, 2, 300.00, 30.00, '微信支付', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, 6, 600.00, 60.00, '支付宝', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(7, 10, 400.00, 40.00, '微信支付', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(8, 15, 1200.00, 180.00, '银行卡', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(9, 13, 500.00, 50.00, '支付宝', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10, 7, 350.00, 35.00, '微信支付', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ========================================
-- 14. 优惠券模板表 (coupon_template) - 10条
-- ========================================
INSERT INTO coupon_template (id, tenant_id, name, type, condition_amount, discount_amount, discount_rate, total_count, remain_count, valid_days, status, created_time, updated_time) VALUES
(1, 1, '满100减20', 'DISCOUNT', 100.00, 20.00, NULL, 1000, 856, 30, 1, NOW(), NOW()),
(2, 1, '满200减50', 'DISCOUNT', 200.00, 50.00, NULL, 800, 623, 30, 1, NOW(), NOW()),
(3, 1, '全场8折', 'RATE', NULL, NULL, 0.80, 500, 412, 15, 1, NOW(), NOW()),
(4, 1, '满500减120', 'DISCOUNT', 500.00, 120.00, NULL, 600, 458, 30, 1, NOW(), NOW()),
(5, 1, '新人专享9折', 'RATE', NULL, NULL, 0.90, 2000, 1456, 7, 1, NOW(), NOW()),
(6, 1, '满300减80', 'DISCOUNT', 300.00, 80.00, NULL, 700, 589, 30, 1, NOW(), NOW()),
(7, 1, '会员日特惠', 'RATE', NULL, NULL, 0.75, 300, 221, 3, 1, NOW(), NOW()),
(8, 1, '满1000减300', 'DISCOUNT', 1000.00, 300.00, NULL, 400, 367, 30, 1, NOW(), NOW()),
(9, 1, '周末特惠8.5折', 'RATE', NULL, NULL, 0.85, 600, 523, 2, 1, NOW(), NOW()),
(10, 1, '生日礼券50元', 'CASH', NULL, 50.00, NULL, 1000, 945, 30, 1, NOW(), NOW());

-- ========================================
-- 15. 用户优惠券表 (coupon_user) - 15条
-- ========================================
INSERT INTO coupon_user (id, member_id, template_id, code, status, used_time, order_id, expire_time, created_time) VALUES
(1, 1, 1, 'CP202601010001', 'USED', DATE_SUB(NOW(), INTERVAL 15 DAY), 1, DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY)),
(2, 1, 3, 'CP202601010002', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 2, 5, 'CP202601020001', 'USED', DATE_SUB(NOW(), INTERVAL 10 DAY), 2, DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
(4, 3, 2, 'CP202601030001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 5, 8, 'CP202601040001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 6, 1, 'CP202601050001', 'USED', DATE_SUB(NOW(), INTERVAL 20 DAY), 3, DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 50 DAY)),
(7, 9, 4, 'CP202601060001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, 10, 3, 'CP202601070001', 'USED', DATE_SUB(NOW(), INTERVAL 8 DAY), 4, DATE_ADD(DATE_SUB(NOW(), INTERVAL 8 DAY), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY)),
(9, 15, 10, 'CP202601080001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 29 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10, 2, 6, 'CP202601090001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(11, 4, 5, 'CP202601100001', 'EXPIRED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(12, 7, 7, 'CP202601110001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(13, 13, 9, 'CP202601120001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(14, 14, 3, 'CP202601130001', 'USED', DATE_SUB(NOW(), INTERVAL 25 DAY), 5, DATE_ADD(DATE_SUB(NOW(), INTERVAL 25 DAY), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY)),
(15, 11, 6, 'CP202601140001', 'UNUSED', NULL, NULL, DATE_ADD(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ========================================
-- 16. 就餐区域表 (dining_area) - 8条
-- ========================================
INSERT INTO dining_area (id, tenant_id, name, sort, created_time, updated_time) VALUES
(1, 1, '大厅A区', 1, NOW(), NOW()),
(2, 1, '大厅B区', 2, NOW(), NOW()),
(3, 1, '包间区', 3, NOW(), NOW()),
(4, 1, 'VIP包间', 4, NOW(), NOW()),
(5, 1, '露天花园', 5, NOW(), NOW()),
(6, 1, '二楼宴会厅', 6, NOW(), NOW()),
(7, 1, '吧台区', 7, NOW(), NOW()),
(8, 1, '儿童游乐区', 8, NOW(), NOW());

-- ========================================
-- 17. 餐桌表 (dining_table) - 20条
-- ========================================
INSERT INTO dining_table (id, tenant_id, area_id, name, seat_count, status, min_amount, qr_code_url, sort, created_time, updated_time) VALUES
(1, 1, 1, 'A01', 4, 'FREE', NULL, 'http://localhost:8080/qr/table/A01', 1, NOW(), NOW()),
(2, 1, 1, 'A02', 4, 'OCCUPIED', 200.00, 'http://localhost:8080/qr/table/A02', 2, NOW(), NOW()),
(3, 1, 1, 'A03', 6, 'FREE', NULL, 'http://localhost:8080/qr/table/A03', 3, NOW(), NOW()),
(4, 1, 1, 'A04', 4, 'FREE', NULL, 'http://localhost:8080/qr/table/A04', 4, NOW(), NOW()),
(5, 1, 1, 'A05', 8, 'OCCUPIED', 500.00, 'http://localhost:8080/qr/table/A05', 5, NOW(), NOW()),
(6, 1, 2, 'B01', 4, 'FREE', NULL, 'http://localhost:8080/qr/table/B01', 6, NOW(), NOW()),
(7, 1, 2, 'B02', 6, 'FREE', NULL, 'http://localhost:8080/qr/table/B02', 7, NOW(), NOW()),
(8, 1, 2, 'B03', 4, 'RESERVED', 200.00, 'http://localhost:8080/qr/table/B03', 8, NOW(), NOW()),
(9, 1, 2, 'B04', 8, 'FREE', NULL, 'http://localhost:8080/qr/table/B04', 9, NOW(), NOW()),
(10, 1, 3, 'C01', 8, 'FREE', 800.00, 'http://localhost:8080/qr/table/C01', 10, NOW(), NOW()),
(11, 1, 3, 'C02', 10, 'FREE', 1000.00, 'http://localhost:8080/qr/table/C02', 11, NOW(), NOW()),
(12, 1, 3, 'C03', 12, 'OCCUPIED', 1500.00, 'http://localhost:8080/qr/table/C03', 12, NOW(), NOW()),
(13, 1, 4, 'D01', 12, 'FREE', 2000.00, 'http://localhost:8080/qr/table/D01', 13, NOW(), NOW()),
(14, 1, 4, 'D02', 16, 'FREE', 3000.00, 'http://localhost:8080/qr/table/D02', 14, NOW(), NOW()),
(15, 1, 5, 'E01', 4, 'FREE', NULL, 'http://localhost:8080/qr/table/E01', 15, NOW(), NOW()),
(16, 1, 5, 'E02', 6, 'FREE', NULL, 'http://localhost:8080/qr/table/E02', 16, NOW(), NOW()),
(17, 1, 6, 'F01', 20, 'FREE', 5000.00, 'http://localhost:8080/qr/table/F01', 17, NOW(), NOW()),
(18, 1, 6, 'F02', 30, 'FREE', 8000.00, 'http://localhost:8080/qr/table/F02', 18, NOW(), NOW()),
(19, 1, 7, 'G01', 2, 'OCCUPIED', NULL, 'http://localhost:8080/qr/table/G01', 19, NOW(), NOW()),
(20, 1, 7, 'G02', 2, 'FREE', NULL, 'http://localhost:8080/qr/table/G02', 20, NOW(), NOW());

-- ========================================
-- 18. 排队表 (dining_queue) - 10条
-- ========================================
INSERT INTO dining_queue (id, tenant_id, queue_no, phone, seat_count, status, created_time, updated_time) VALUES
(1, 1, 'Q001', '13900139021', 2, 'WAITING', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(2, 1, 'Q002', '13900139022', 4, 'WAITING', DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(3, 1, 'Q003', '13900139023', 3, 'CALLED', DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(4, 1, 'Q004', '13900139024', 2, 'WAITING', DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(5, 1, 'Q005', '13900139025', 6, 'WAITING', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(6, 1, 'Q006', '13900139026', 4, 'SEATED', DATE_SUB(NOW(), INTERVAL 8 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE)),
(7, 1, 'Q007', '13900139027', 2, 'WAITING', DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(8, 1, 'Q008', '13900139028', 8, 'WAITING', DATE_SUB(NOW(), INTERVAL 3 MINUTE), DATE_SUB(NOW(), INTERVAL 3 MINUTE)),
(9, 1, 'Q009', '13900139029', 4, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 2 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE)),
(10, 1, 'Q010', '13900139030', 2, 'WAITING', DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE));

-- ========================================
-- 19. 预订表 (dining_reservation) - 12条
-- ========================================
INSERT INTO dining_reservation (id, tenant_id, table_id, customer_name, phone, reserved_time, seat_count, status, remark, created_time, updated_time) VALUES
(1, 1, 10, '张大伟', '13900139031', DATE_ADD(NOW(), INTERVAL 2 HOUR), 6, 'CONFIRMED', '庆祝生日', NOW(), NOW()),
(2, 1, 11, '李小红', '13900139032', DATE_ADD(NOW(), INTERVAL 3 HOUR), 8, 'CONFIRMED', '商务宴请', NOW(), NOW()),
(3, 1, 13, '王大明', '13900139033', DATE_ADD(NOW(), INTERVAL 4 HOUR), 10, 'PENDING', '家庭聚餐', NOW(), NOW()),
(4, 1, 14, '赵美丽', '13900139034', DATE_ADD(NOW(), INTERVAL 5 HOUR), 12, 'PENDING', '公司年会', NOW(), NOW()),
(5, 1, 3, '孙悟空', '13900139005', DATE_ADD(NOW(), INTERVAL 1 HOUR), 4, 'CONFIRMED', '朋友聚会', NOW(), NOW()),
(6, 1, 8, '猪八戒', '13900139006', DATE_ADD(NOW(), INTERVAL 6 HOUR), 4, 'PENDING', NULL, NOW(), NOW()),
(7, 1, 12, '沙和尚', '13900139007', DATE_ADD(NOW(), INTERVAL 7 HOUR), 8, 'CONFIRMED', '团队聚餐', NOW(), NOW()),
(8, 1, 17, '唐三藏', '13900139009', DATE_ADD(NOW(), INTERVAL 1 DAY), 20, 'PENDING', '大型宴会', NOW(), NOW()),
(9, 1, 10, '白素贞', '13900139010', DATE_ADD(NOW(), INTERVAL 8 HOUR), 6, 'CANCELLED', '取消预订', NOW(), NOW()),
(10, 1, 11, '许仙', '13900139012', DATE_ADD(NOW(), INTERVAL 9 HOUR), 8, 'CONFIRMED', '纪念日', NOW(), NOW()),
(11, 1, 13, '法海', '13900139013', DATE_ADD(NOW(), INTERVAL 2 DAY), 10, 'PENDING', '朋友聚会', NOW(), NOW()),
(12, 1, 14, '林黛玉', '13900139014', DATE_ADD(NOW(), INTERVAL 10 HOUR), 12, 'CONFIRMED', '诗社活动', NOW(), NOW());

-- ========================================
-- 20. 供应商表 (supplier) - 12条
-- ========================================
INSERT INTO supplier (id, tenant_id, name, contact, phone, address, status, created_time, updated_time) VALUES
(1, 1, '新鲜蔬菜供应商', '菜老板', '13800139001', '北京市新发地批发市场', 1, NOW(), NOW()),
(2, 1, '优质肉类供应商', '肉老板', '13800139002', '北京市顺义区肉类批发市场', 1, NOW(), NOW()),
(3, 1, '海鲜水产供应商', '鱼老板', '13800139003', '天津市塘沽区渔港', 1, NOW(), NOW()),
(4, 1, '粮油调味供应商', '米老板', '13800139004', '山东省临沂市粮油市场', 1, NOW(), NOW()),
(5, 1, '水果批发商', '果老板', '13800139005', '广东省深圳市水果市场', 1, NOW(), NOW()),
(6, 1, '酒水饮料供应商', '酒老板', '13800139006', '北京市朝阳区酒水批发市场', 1, NOW(), NOW()),
(7, 1, '冻品供应商', '冻老板', '13800139007', '上海市浦东新区冻品市场', 1, NOW(), NOW()),
(8, 1, '蛋类供应商', '蛋老板', '13800139008', '河北省张家口市蛋类养殖基地', 1, NOW(), NOW()),
(9, 1, '豆制品供应商', '豆老板', '13800139009', '黑龙江省哈尔滨市豆制品厂', 1, NOW(), NOW()),
(10, 1, '干货调料供应商', '调老板', '13800139010', '四川省成都市调料市场', 1, NOW(), NOW()),
(11, 1, '一次性用品供应商', '用老板', '13800139011', '浙江省义乌市小商品市场', 1, NOW(), NOW()),
(12, 1, '清洁用品供应商', '清老板', '13800139012', '广东省广州市清洁用品批发市场', 1, NOW(), NOW());

-- ========================================
-- 21. 物料分类表 (material_category) - 10条
-- ========================================
INSERT INTO material_category (id, tenant_id, name, sort, created_time, updated_time) VALUES
(1, 1, '蔬菜类', 1, NOW(), NOW()),
(2, 1, '肉类', 2, NOW(), NOW()),
(3, 1, '海鲜类', 3, NOW(), NOW()),
(4, 1, '粮油类', 4, NOW(), NOW()),
(5, 1, '调味品类', 5, NOW(), NOW()),
(6, 1, '水果类', 6, NOW(), NOW()),
(7, 1, '酒水饮料', 7, NOW(), NOW()),
(8, 1, '冻品类', 8, NOW(), NOW()),
(9, 1, '蛋豆制品', 9, NOW(), NOW()),
(10, 1, '日用品', 10, NOW(), NOW());

-- ========================================
-- 22. 物料表 (material) - 25条
-- ========================================
INSERT INTO material (id, tenant_id, category_id, name, unit, stock_qty, min_stock, unit_price, supplier_id, barcode, status, created_time, updated_time) VALUES
(1, 1, 1, '大白菜', '斤', 150.00, 50.00, 2.50, 1, '690000010001', 1, NOW(), NOW()),
(2, 1, 1, '西红柿', '斤', 80.00, 30.00, 4.50, 1, '690000010002', 1, NOW(), NOW()),
(3, 1, 1, '黄瓜', '斤', 120.00, 40.00, 3.00, 1, '690000010003', 1, NOW(), NOW()),
(4, 1, 1, '土豆', '斤', 200.00, 80.00, 2.00, 1, '690000010004', 1, NOW(), NOW()),
(5, 1, 1, '青椒', '斤', 60.00, 20.00, 5.50, 1, '690000010005', 1, NOW(), NOW()),
(6, 1, 2, '猪肉', '斤', 100.00, 30.00, 28.00, 2, '690000020001', 1, NOW(), NOW()),
(7, 1, 2, '牛肉', '斤', 50.00, 15.00, 58.00, 2, '690000020002', 1, NOW(), NOW()),
(8, 1, 2, '鸡肉', '斤', 80.00, 25.00, 18.00, 2, '690000020003', 1, NOW(), NOW()),
(9, 1, 2, '羊肉', '斤', 45.00, 15.00, 45.00, 2, '690000020004', 1, NOW(), NOW()),
(10, 1, 3, '活虾', '斤', 30.00, 10.00, 68.00, 3, '690000030001', 1, NOW(), NOW()),
(11, 1, 3, '鲈鱼', '条', 25.00, 8.00, 38.00, 3, '690000030002', 1, NOW(), NOW()),
(12, 1, 3, '扇贝', '斤', 40.00, 15.00, 48.00, 3, '690000030003', 1, NOW(), NOW()),
(13, 1, 4, '大米', '袋', 80.00, 20.00, 65.00, 4, '690000040001', 1, NOW(), NOW()),
(14, 1, 4, '面粉', '袋', 60.00, 15.00, 45.00, 4, '690000040002', 1, NOW(), NOW()),
(15, 1, 5, '食用油', '桶', 45.00, 10.00, 78.00, 4, '690000050001', 1, NOW(), NOW()),
(16, 1, 5, '盐', '袋', 200.00, 50.00, 3.00, 10, '690000050002', 1, NOW(), NOW()),
(17, 1, 5, '酱油', '瓶', 150.00, 40.00, 12.00, 10, '690000050003', 1, NOW(), NOW()),
(18, 1, 6, '苹果', '斤', 100.00, 30.00, 8.50, 5, '690000060001', 1, NOW(), NOW()),
(19, 1, 6, '香蕉', '斤', 80.00, 25.00, 4.50, 5, '690000060002', 1, NOW(), NOW()),
(20, 1, 7, '可乐', '瓶', 300.00, 100.00, 3.50, 6, '690000070001', 1, NOW(), NOW()),
(21, 1, 7, '啤酒', '瓶', 200.00, 50.00, 5.00, 6, '690000070002', 1, NOW(), NOW()),
(22, 1, 8, '速冻水饺', '袋', 120.00, 40.00, 15.00, 7, '690000080001', 1, NOW(), NOW()),
(23, 1, 9, '鸡蛋', '斤', 250.00, 80.00, 6.50, 8, '690000090001', 1, NOW(), NOW()),
(24, 1, 9, '豆腐', '块', 100.00, 30.00, 3.50, 9, '690000090002', 1, NOW(), NOW()),
(25, 1, 10, '餐巾纸', '包', 500.00, 100.00, 2.00, 11, '690000100001', 1, NOW(), NOW());

-- ========================================
-- 23. 库存记录表 (stock_record) - 20条
-- ========================================
INSERT INTO stock_record (id, tenant_id, material_id, type, qty, unit_price, total_amount, biz_id, remark, operator, created_time) VALUES
(1, 1, 1, 'IN', 200.00, 2.50, 500.00, 1, '采购入库', '张三', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 1, 6, 'IN', 100.00, 28.00, 2800.00, 2, '采购入库', '张三', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 1, 13, 'IN', 50.00, 65.00, 3250.00, 3, '采购入库', '李四', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 1, 10, 'IN', 30.00, 68.00, 2040.00, 4, '采购入库', '张三', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 1, 1, 'OUT', 50.00, 2.50, 125.00, 1, '菜品消耗', '王五', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 1, 6, 'OUT', 30.00, 28.00, 840.00, 1, '菜品消耗', '王五', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 1, 3, 'OUT', 40.00, 3.00, 120.00, 2, '菜品消耗', '李四', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, 1, 15, 'IN', 20.00, 78.00, 1560.00, 5, '采购入库', '张三', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9, 1, 20, 'IN', 100.00, 3.50, 350.00, 6, '采购入库', '李四', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10, 1, 8, 'OUT', 25.00, 18.00, 450.00, 1, '菜品消耗', '王五', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(11, 1, 2, 'IN', 100.00, 4.50, 450.00, 7, '采购入库', '张三', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12, 1, 5, 'OUT', 30.00, 5.50, 165.00, 2, '菜品消耗', '李四', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(13, 1, 11, 'OUT', 10.00, 38.00, 380.00, 1, '菜品消耗', '王五', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(14, 1, 17, 'IN', 50.00, 12.00, 600.00, 8, '采购入库', '张三', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(15, 1, 23, 'OUT', 80.00, 6.50, 520.00, 1, '菜品消耗', '李四', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(16, 1, 19, 'IN', 50.00, 8.50, 425.00, 9, '采购入库', '王五', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(17, 1, 14, 'OUT', 20.00, 45.00, 900.00, 1, '菜品消耗', '张三', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(18, 1, 24, 'IN', 50.00, 3.50, 175.00, 10, '采购入库', '李四', NOW()),
(19, 1, 25, 'IN', 100.00, 2.00, 200.00, 11, '采购入库', '王五', NOW()),
(20, 1, 16, 'OUT', 30.00, 3.00, 90.00, 1, '菜品消耗', '张三', NOW());

-- ========================================
-- 24. 库存盘点表 (stock_check) - 10条
-- ========================================
INSERT INTO stock_check (id, tenant_id, check_no, status, total_diff_amount, operator, remark, created_time, updated_time) VALUES
(1, 1, 'PC20260101001', 'COMPLETED', 125.50, '张三', '月度盘点', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(2, 1, 'PC20260102001', 'COMPLETED', 85.00, '李四', '周度盘点', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(3, 1, 'PC20260103001', 'COMPLETED', 210.80, '王五', '月度盘点', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(4, 1, 'PC20260104001', 'IN_PROGRESS', NULL, '张三', '周度盘点', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 1, 'PC20260105001', 'DRAFT', NULL, '李四', '新建盘点', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 1, 'PC20260201001', 'COMPLETED', 95.30, '王五', '月度盘点', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
(7, 1, 'PC20260202001', 'COMPLETED', 156.00, '张三', '周度盘点', DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY)),
(8, 1, 'PC20260203001', 'COMPLETED', 68.90, '李四', '月度盘点', DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)),
(9, 1, 'PC20260204001', 'COMPLETED', 178.60, '王五', '周度盘点', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10, 1, 'PC20260205001', 'COMPLETED', 112.40, '张三', '月度盘点', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ========================================
-- 25. 库存盘点明细表 (stock_check_detail) - 30条
-- ========================================
INSERT INTO stock_check_detail (id, tenant_id, check_id, material_id, book_qty, actual_qty, diff_qty, remark) VALUES
(1, 1, 1, 1, 150.00, 148.50, -1.50, '损耗'),
(2, 1, 1, 6, 100.00, 102.00, 2.00, '盘点误差'),
(3, 1, 1, 13, 80.00, 79.50, -0.50, '损耗'),
(4, 1, 2, 1, 200.00, 201.00, 1.00, '盘点误差'),
(5, 1, 2, 3, 120.00, 118.00, -2.00, '损耗'),
(6, 1, 2, 10, 30.00, 31.50, 1.50, '新增入库未记账'),
(7, 1, 3, 6, 50.00, 48.00, -2.00, '损耗'),
(8, 1, 3, 8, 80.00, 82.00, 2.00, '盘点误差'),
(9, 1, 3, 15, 45.00, 44.00, -1.00, '损耗'),
(10, 1, 3, 20, 300.00, 298.50, -1.50, '损耗'),
(11, 1, 4, 1, 150.00, 150.00, 0, '一致'),
(12, 1, 4, 2, 80.00, 78.50, -1.50, '损耗'),
(13, 1, 4, 7, 50.00, 50.00, 0, '一致'),
(14, 1, 6, 3, 120.00, 121.50, 1.50, '新增入库未记账'),
(15, 1, 6, 5, 60.00, 58.00, -2.00, '损耗'),
(16, 1, 6, 14, 60.00, 59.50, -0.50, '损耗'),
(17, 1, 7, 11, 25.00, 24.00, -1.00, '损耗'),
(18, 1, 7, 12, 40.00, 41.50, 1.50, '盘点误差'),
(19, 1, 7, 17, 150.00, 149.00, -1.00, '损耗'),
(20, 1, 8, 9, 45.00, 45.00, 0, '一致'),
(21, 1, 8, 18, 100.00, 98.50, -1.50, '损耗'),
(22, 1, 8, 19, 80.00, 82.00, 2.00, '盘点误差'),
(23, 1, 9, 4, 200.00, 198.00, -2.00, '损耗'),
(24, 1, 9, 21, 200.00, 200.00, 0, '一致'),
(25, 1, 9, 23, 250.00, 248.50, -1.50, '损耗'),
(26, 1, 10, 2, 80.00, 81.00, 1.00, '盘点误差'),
(27, 1, 10, 16, 200.00, 199.00, -1.00, '损耗'),
(28, 1, 10, 22, 120.00, 121.50, 1.50, '新增入库未记账'),
(29, 1, 4, 5, 60.00, 61.00, 1.00, '盘点误差'),
(30, 1, 4, 24, 100.00, 100.00, 0, '一致');

-- ========================================
-- 26. 采购单表 (purchase_order) - 10条
-- ========================================
INSERT INTO purchase_order (id, tenant_id, order_no, supplier_id, total_amount, status, operator, remark, created_time, updated_time) VALUES
(1, 1, 'PO20260101001', 1, 1250.00, 'COMPLETED', '张三', '蔬菜采购', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2, 1, 'PO20260102001', 2, 5600.00, 'COMPLETED', '李四', '肉类采购', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(3, 1, 'PO20260103001', 3, 4080.00, 'COMPLETED', '王五', '海鲜采购', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 1, 'PO20260104001', 4, 6500.00, 'COMPLETED', '张三', '粮油采购', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 1, 'PO20260105001', 5, 1700.00, 'COMPLETED', '李四', '水果采购', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(6, 1, 'PO20260106001', 6, 1850.00, 'IN_PROGRESS', '王五', '酒水采购', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(7, 1, 'PO20260107001', 7, 3000.00, 'PENDING', '张三', '冻品采购', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 1, 'PO20260108001', 10, 480.00, 'COMPLETED', '李四', '调料采购', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 1, 'PO20260109001', 8, 1625.00, 'PENDING', '王五', '蛋类采购', NOW(), NOW()),
(10, 1, 'PO20260110001', 11, 1000.00, 'DRAFT', '张三', '日用品采购', NOW(), NOW());

-- ========================================
-- 27. 采购单明细表 (purchase_order_detail) - 40条
-- ========================================
INSERT INTO purchase_order_detail (id, purchase_order_id, material_id, qty, unit_price, amount, received_qty, remark) VALUES
-- PO20260101001
(1, 1, 1, 100.00, 2.50, 250.00, 100.00, '大白菜'),
(2, 1, 2, 50.00, 4.50, 225.00, 50.00, '西红柿'),
(3, 1, 3, 60.00, 3.00, 180.00, 60.00, '黄瓜'),
(4, 1, 4, 80.00, 2.00, 160.00, 80.00, '土豆'),
(5, 1, 5, 40.00, 5.50, 220.00, 40.00, '青椒'),
-- PO20260102001
(6, 2, 6, 100.00, 28.00, 2800.00, 100.00, '猪肉'),
(7, 2, 7, 30.00, 58.00, 1740.00, 30.00, '牛肉'),
(8, 2, 8, 50.00, 18.00, 900.00, 50.00, '鸡肉'),
(9, 2, 9, 20.00, 45.00, 900.00, 20.00, '羊肉'),
-- PO20260103001
(10, 3, 10, 30.00, 68.00, 2040.00, 30.00, '活虾'),
(11, 3, 11, 25.00, 38.00, 950.00, 25.00, '鲈鱼'),
(12, 3, 12, 40.00, 48.00, 1920.00, 40.00, '扇贝'),
-- PO20260104001
(13, 4, 13, 50.00, 65.00, 3250.00, 50.00, '大米'),
(14, 4, 14, 40.00, 45.00, 1800.00, 40.00, '面粉'),
(15, 4, 15, 20.00, 78.00, 1560.00, 20.00, '食用油'),
-- PO20260105001
(16, 5, 18, 100.00, 8.50, 850.00, 100.00, '苹果'),
(17, 5, 19, 80.00, 4.50, 360.00, 80.00, '香蕉'),
-- PO20260106001
(18, 6, 20, 200.00, 3.50, 700.00, 150.00, '可乐'),
(19, 6, 21, 150.00, 5.00, 750.00, 100.00, '啤酒'),
-- PO20260107001
(20, 7, 22, 100.00, 30.00, 3000.00, 0, '速冻水饺'),
-- PO20260108001
(21, 8, 16, 50.00, 3.00, 150.00, 50.00, '盐'),
(22, 8, 17, 50.00, 12.00, 600.00, 50.00, '酱油'),
(23, 8, 10, 30.00, 68.00, 2040.00, 30.00, '活虾'),
-- PO20260109001
(24, 9, 23, 100.00, 6.50, 650.00, 0, '鸡蛋'),
(25, 9, 24, 50.00, 3.50, 175.00, 0, '豆腐'),
(26, 9, 8, 40.00, 18.00, 720.00, 0, '鸡肉'),
-- PO20260110001
(27, 10, 25, 200.00, 2.00, 400.00, 0, '餐巾纸'),
(28, 10, 5, 60.00, 5.50, 330.00, 0, '青椒'),
(29, 10, 4, 100.00, 2.00, 200.00, 0, '土豆'),
(30, 1, 6, 50.00, 28.00, 1400.00, 50.00, '猪肉(补充)'),
(31, 2, 7, 20.00, 58.00, 1160.00, 20.00, '牛肉(补充)'),
(32, 3, 12, 20.00, 48.00, 960.00, 20.00, '扇贝(补充)'),
(33, 4, 14, 30.00, 45.00, 1350.00, 30.00, '面粉(补充)'),
(34, 5, 18, 50.00, 8.50, 425.00, 50.00, '苹果(补充)'),
(35, 6, 21, 50.00, 5.00, 250.00, 30.00, '啤酒(部分到货)'),
(36, 8, 17, 30.00, 12.00, 360.00, 30.00, '酱油(补充)'),
(37, 3, 11, 10.00, 38.00, 380.00, 10.00, '鲈鱼(补充)'),
(38, 5, 19, 40.00, 4.50, 180.00, 40.00, '香蕉(补充)'),
(39, 7, 22, 50.00, 30.00, 1500.00, 0, '速冻水饺(采购)'),
(40, 6, 20, 100.00, 3.50, 350.00, 50.00, '可乐(部分到货)');

-- ========================================
-- 28. 订单表 (orders) - 15条
-- ========================================
INSERT INTO orders (id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, amount, remark, phone, address, user_name, consignee, table_id, dining_type) VALUES
(1, 'ORD20260101001', 4, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 1 HOUR, 1, 328.00, '少辣', '13900139001', '北京市东城区王府井大街1号', '张小明', '张小明', NULL, 'DELIVERY'),
(2, 'ORD20260102001', 4, 2, 3, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 1 HOUR, 2, 156.00, NULL, '13900139002', '上海市徐汇区南京路100号', '李晓红', '李晓红', NULL, 'DELIVERY'),
(3, 'ORD20260103001', 4, 3, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 2 HOUR, 1, 858.00, '商务宴请', '13900139003', NULL, '王大军', '王大军', 12, 'DINING'),
(4, 'ORD20260104001', 4, 4, 4, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR, 3, 89.00, '快一点', '13900139004', '成都市锦江区春熙路88号', '赵小美', '赵小美', NULL, 'DELIVERY'),
(5, 'ORD20260105001', 4, 5, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 3 HOUR, 1, 2580.00, '豪华配置', '13900139005', NULL, '孙悟空', '孙悟空', 14, 'DINING'),
(6, 'ORD20260106001', 1, 5, 6, NOW(), NULL, 1, 168.00, NULL, '13900139006', '深圳市南山区科技园', '猪八戒', '猪八戒', NULL, 'DELIVERY'),
(7, 'ORD20260107001', 4, 7, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR) + INTERVAL 2 HOUR, 2, 398.00, '朋友聚餐', '13900139007', NULL, '沙和尚', '沙和尚', 3, 'DINING'),
(8, 'ORD20260108001', 5, 8, 7, DATE_SUB(NOW(), INTERVAL 10 HOUR), NULL, NULL, 258.00, '取消订单', '13900139008', '杭州市上城区西湖路1号', '白骨精', '白骨精', NULL, 'DELIVERY'),
(9, 'ORD20260109001', 4, 9, NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR) + INTERVAL 2 HOUR, 1, 1880.00, '生日宴', '13900139009', NULL, '唐三藏', '唐三藏', 17, 'DINING'),
(10, 'ORD20260110001', 4, 10, 11, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR) + INTERVAL 1 HOUR, 2, 580.00, NULL, '13900139010', '杭州市西湖区雷峰塔', '白素贞', '白素贞', NULL, 'DELIVERY'),
(11, 'ORD20260111001', 4, 11, 12, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR) + INTERVAL 1 HOUR, 1, 328.00, NULL, '13900139011', '杭州市西湖区断桥', '小青', '小青', NULL, 'TAKEOUT'),
(12, 'ORD20260112001', 4, 12, 13, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR) + INTERVAL 1 HOUR, 3, 168.00, '少冰', '13900139012', '杭州市西湖区保俶塔', '许仙', '许仙', NULL, 'TAKEOUT'),
(13, 'ORD20260113001', 1, 13, NULL, NOW(), NULL, 1, 1280.00, '公司聚餐', '13900139013', NULL, '法海', '法海', 11, 'DINING'),
(14, 'ORD20260114001', 4, 14, 15, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR) + INTERVAL 1 HOUR, 2, 458.00, NULL, '13900139014', '沈阳市和平区北陵', '林黛玉', '林黛玉', NULL, 'DELIVERY'),
(15, 'ORD20260115001', 4, 15, NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE) + INTERVAL 1 HOUR, 1, 688.00, '朋友聚会', '13900139015', NULL, '贾宝玉', '贾宝玉', 2, 'DINING');

-- ========================================
-- 29. 订单明细表 (order_detail) - 45条
-- ========================================
INSERT INTO order_detail (id, name, image, order_id, dish_id, setmeal_id, dish_flavor, number, amount) VALUES
(1, '扬州炒饭', 'images/dishes/yangzhouchaofan.jpg', 1, 11, NULL, NULL, 1, 32.00),
(2, '红烧肉', 'images/dishes/hongshaorou.jpg', 1, 1, NULL, '["微辣"]', 1, 58.00),
(3, '珍珠奶茶', 'images/drinks/zhenzhunaicha.jpg', 1, 13, NULL, '["少糖","少冰"]', 1, 18.00),
(4, '宫保鸡丁', 'images/dishes/gongbaojiding.jpg', 1, 2, NULL, '["中辣"]', 1, 48.00),
(5, '鱼香肉丝', 'images/dishes/yuxiangrous.jpg', 2, 3, NULL, '["微辣"]', 1, 46.00),
(6, '老醋花生', 'images/dishes/laocuhuasheng.jpg', 2, 8, NULL, NULL, 1, 22.00),
(7, '珍珠奶茶', 'images/drinks/zhenzhunaicha.jpg', 2, 13, NULL, '["半糖","去冰"]', 2, 36.00),
(8, '红烧肉', 'images/dishes/hongshaorou.jpg', 3, 1, NULL, '["不辣"]', 2, 116.00),
(9, '宫保鸡丁', 'images/dishes/gongbaojiding.jpg', 3, 2, NULL, '["微辣"]', 2, 96.00),
(10, '清蒸鲈鱼', 'images/dishes/qingzhengluyu.jpg', 3, 17, NULL, NULL, 1, 88.00),
(11, '番茄鸡蛋汤', 'images/dishes/fanqijidantang.jpg', 3, 10, NULL, NULL, 1, 20.00),
(12, '鲜榨果汁', 'images/drinks/xianzhaguozhi.jpg', 3, 14, NULL, NULL, 4, 88.00),
(13, '麻婆豆腐', 'images/dishes/mapotoufu.jpg', 4, 4, NULL, '["特辣"]', 1, 38.00),
(14, '糖醋里脊', 'images/dishes/tangculiji.jpg', 4, 5, NULL, NULL, 1, 52.00),
(15, '单人工作餐', 'images/setmeal/single.jpg', 5, NULL, 1, NULL, 1, 28.00),
(16, '红烧肉', 'images/dishes/hongshaorou.jpg', 5, 1, NULL, '["中辣"]', 1, 58.00),
(17, '珍珠奶茶', 'images/drinks/zhenzhunaicha.jpg', 5, 13, NULL, '["全糖","多冰"]', 3, 54.00),
(18, '宫保鸡丁', 'images/dishes/gongbaojiding.jpg', 6, 2, NULL, '["不辣"]', 1, 48.00),
(19, '西湖牛肉羹', 'images/dishes/xihuniurougeng.jpg', 6, 9, NULL, NULL, 1, 28.00),
(20, '鲜榨果汁', 'images/drinks/xianzhaguozhi.jpg', 6, 14, NULL, '["无糖"]', 2, 44.00),
(21, '双人浪漫套餐', 'images/setmeal/couple.jpg', 7, NULL, 2, NULL, 1, 88.00),
(22, '鱼香肉丝', 'images/dishes/yuxiangrous.jpg', 8, 3, NULL, '["中辣"]', 1, 46.00),
(23, '凉拌黄瓜', 'images/dishes/liangbanghuanggua.jpg', 8, 6, NULL, NULL, 1, 18.00),
(24, '鲜榨果汁', 'images/drinks/xianzhaguozhi.jpg', 8, 14, NULL, '["少糖"]', 1, 22.00),
(25, '家庭欢聚套餐', 'images/setmeal/family.jpg', 9, NULL, 3, NULL, 1, 168.00),
(26, '单人工作餐', 'images/setmeal/single.jpg', 10, NULL, 1, NULL, 1, 28.00),
(27, '清蒸鲈鱼', 'images/dishes/qingzhengluyu.jpg', 10, 17, NULL, NULL, 1, 88.00),
(28, '扬州炒饭', 'images/dishes/yangzhouchaofan.jpg', 10, 11, NULL, NULL, 2, 64.00),
(29, '珍珠奶茶', 'images/drinks/zhenzhunaicha.jpg', 10, 13, NULL, '["半糖","常温"]', 2, 36.00),
(30, '薯条', 'images/dishes/shutiao.jpg', 11, 15, NULL, NULL, 2, 30.00),
(31, '鸡米花', 'images/dishes/jimihua.jpg', 11, 16, NULL, NULL, 2, 36.00),
(32, '珍珠奶茶', 'images/drinks/zhenzhunaicha.jpg', 11, 13, NULL, '["无糖","去冰"]', 1, 18.00),
(33, '商务洽谈套餐', 'images/setmeal/business.jpg', 13, NULL, 4, NULL, 1, 128.00),
(34, '儿童营养套餐', 'images/setmeal/kids.jpg', 14, NULL, 5, NULL, 1, 26.00),
(35, '蒜蓉西兰花', 'images/dishes/suorongxilanhua.jpg', 14, 18, NULL, NULL, 1, 26.00),
(36, '清蒸鲈鱼', 'images/dishes/qingzhengluyu.jpg', 14, 17, NULL, NULL, 1, 88.00),
(37, '西湖牛肉羹', 'images/dishes/xihuniurougeng.jpg', 14, 9, NULL, NULL, 1, 28.00),
(38, '鲜榨果汁', 'images/drinks/xianzhaguozhi.jpg', 14, 14, NULL, '["无糖"]', 1, 22.00),
(39, '红烧肉', 'images/dishes/hongshaorou.jpg', 15, 1, NULL, '["微辣"]', 1, 58.00),
(40, '糖醋里脊', 'images/dishes/tangculiji.jpg', 15, 5, NULL, NULL, 1, 52.00),
(41, '清蒸鲈鱼', 'images/dishes/qingzhengluyu.jpg', 15, 17, NULL, NULL, 1, 88.00),
(42, '小笼包', 'images/dishes/xiaolongbao.jpg', 15, 20, NULL, NULL, 1, 24.00),
(43, '珍珠奶茶', 'images/drinks/zhenzhunaicha.jpg', 15, 13, NULL, '["半糖","少冰"]', 3, 54.00),
(44, '番茄鸡蛋汤', 'images/dishes/fanqijidantang.jpg', 3, 10, NULL, NULL, 1, 20.00),
(45, '麻婆豆腐', 'images/dishes/mapotoufu.jpg', 1, 4, NULL, '["中辣"]', 1, 38.00);

-- ========================================
-- 30. 购物车表 (shopping_cart) - 10条
-- ========================================
INSERT INTO shopping_cart (id, name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) VALUES
(1, '红烧肉', 1, 1, NULL, '["微辣"]', 2, 116.00, 'images/dishes/hongshaorou.jpg', NOW()),
(2, '珍珠奶茶', 1, 13, NULL, '["少糖","少冰"]', 1, 18.00, 'images/drinks/zhenzhunaicha.jpg', NOW()),
(3, '宫保鸡丁', 2, 2, NULL, '["中辣"]', 1, 48.00, 'images/dishes/gongbaojiding.jpg', NOW()),
(4, '单人工作餐', 3, NULL, 1, NULL, 2, 56.00, 'images/setmeal/single.jpg', NOW()),
(5, '清蒸鲈鱼', 5, 17, NULL, NULL, 1, 88.00, 'images/dishes/qingzhengluyu.jpg', NOW()),
(6, '鲜榨果汁', 5, 14, NULL, '["无糖"]', 2, 44.00, 'images/drinks/xianzhaguozhi.jpg', NOW()),
(7, '鱼香肉丝', 7, 3, NULL, '["微辣"]', 1, 46.00, 'images/dishes/yuxiangrous.jpg', NOW()),
(8, '薯条', 8, 15, NULL, NULL, 3, 45.00, 'images/dishes/shutiao.jpg', NOW()),
(9, '双人浪漫套餐', 10, NULL, 2, NULL, 1, 88.00, 'images/setmeal/couple.jpg', NOW()),
(10, '鸡米花', 15, 16, NULL, NULL, 2, 36.00, 'images/dishes/jimihua.jpg', NOW());

-- ========================================

-- ========================================
-- 打印机配置表结构 (printer_config)
-- ========================================
DROP TABLE IF EXISTS printer_config;
CREATE TABLE printer_config (
    id bigint NOT NULL AUTO_INCREMENT,
    tenant_id bigint NOT NULL,
    store_id bigint DEFAULT NULL,
    name varchar(50) NOT NULL,
    type varchar(20) NOT NULL,
    brand varchar(20) DEFAULT NULL,
    device_id varchar(100) DEFAULT NULL,
    ip_address varchar(15) DEFAULT NULL,
    port int DEFAULT NULL,
    paper_size varchar(10) DEFAULT '58mm',
    print_type varchar(20) NOT NULL,
    status int DEFAULT '1',
    sort int DEFAULT '0',
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    system_printer_name varchar(200) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id)
);

-- ========================================
-- 打印日志表结构 (printer_log)
-- ========================================
DROP TABLE IF EXISTS printer_log;
CREATE TABLE printer_log (
    id bigint NOT NULL AUTO_INCREMENT,
    order_id bigint DEFAULT NULL,
    print_type varchar(20) NOT NULL,
    printer_id bigint DEFAULT NULL,
    content text,
    status int DEFAULT '0',
    error_msg varchar(255) DEFAULT NULL,
    created_time datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_printer (printer_id)
);

-- 31. 打印机配置表 (printer_config) - 10条
-- ========================================
INSERT INTO printer_config (id, tenant_id, store_id, name, type, brand, device_id, ip_address, port, paper_size, print_type, status, sort, created_time, updated_time, system_printer_name) VALUES
(1, 1, 1, '前台打印机', 'BLUETOOTH', 'HP', 'DEV001', '192.168.1.101', 9100, '58mm', 'KITCHEN', 1, 1, NOW(), NOW(), 'HP-Deskjet-2700'),
(2, 1, 1, '后厨打印机', 'WIFI', 'Canon', 'DEV002', '192.168.1.102', 9100, '80mm', 'KITCHEN', 1, 2, NOW(), NOW(), 'Canon-G3010'),
(3, 1, 1, '收银打印机', 'USB', 'EPSON', 'DEV003', '192.168.1.103', 9100, '58mm', 'RECEIPT', 1, 3, NOW(), NOW(), 'EPSON-LQ-1600'),
(4, 1, 1, '外卖打印机', 'BLUETOOTH', 'HP', 'DEV004', '192.168.1.104', 9100, '80mm', 'DELIVERY', 1, 4, NOW(), NOW(), 'HP-Deskjet-3600'),
(5, 1, 1, '备餐打印机', 'WIFI', 'Xprinter', 'DEV005', '192.168.1.105', 9100, '80mm', 'KITCHEN', 1, 5, NOW(), NOW(), 'Xprinter-470B'),
(6, 2, 2, '分店前台打印机', 'BLUETOOTH', 'HP', 'DEV006', '192.168.2.101', 9100, '58mm', 'RECEIPT', 1, 1, NOW(), NOW(), 'HP-Deskjet-2700-Branch'),
(7, 2, 2, '分店后厨打印机', 'WIFI', 'Canon', 'DEV007', '192.168.2.102', 9100, '80mm', 'KITCHEN', 1, 2, NOW(), NOW(), 'Canon-G3010-Branch'),
(8, 1, 1, '酒水吧台打印机', 'USB', 'EPSON', 'DEV008', '192.168.1.106', 9100, '58mm', 'BAR', 1, 6, NOW(), NOW(), 'EPSON-TM-U220'),
(9, 1, 1, '甜品台打印机', 'BLUETOOTH', 'Xprinter', 'DEV009', '192.168.1.107', 9100, '58mm', 'KITCHEN', 1, 7, NOW(), NOW(), 'Xprinter-370B'),
(10, 1, 1, '测试打印机', 'WIFI', 'Test', 'DEV010', '192.168.1.108', 9100, '80mm', 'TEST', 0, 8, NOW(), NOW(), 'Test-Printer-001');

-- ========================================
-- 32. 打印日志表 (printer_log) - 15条
-- ========================================
INSERT INTO printer_log (id, order_id, print_type, printer_id, content, status, error_msg, created_time) VALUES
(1, 1, 'KITCHEN', 2, '订单号:ORD20260101001\n菜品: 红烧肉x1, 宫保鸡丁x1, 鱼香肉丝x1\n备注: 少辣', 1, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 1, 'RECEIPT', 3, '订单号:ORD20260101001\n金额: 328.00\n支付方式: 微信支付', 1, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 2, 'KITCHEN', 2, '订单号:ORD20260102001\n菜品: 鱼香肉丝x1, 老醋花生x1', 1, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 2, 'RECEIPT', 3, '订单号:ORD20260102001\n金额: 156.00\n支付方式: 支付宝', 1, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 3, 'KITCHEN', 2, '订单号:ORD20260103001\n菜品: 红烧肉x2, 宫保鸡丁x2, 清蒸鲈鱼x1, 番茄鸡蛋汤x1, 鲜榨果汁x4\n备注: 商务宴请', 1, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 3, 'RECEIPT', 3, '订单号:ORD20260103001\n金额: 858.00\n支付方式: 微信支付\n桌号: 12', 1, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, 5, 'KITCHEN', 2, '订单号:ORD20260105001\n套餐: 家庭欢聚套餐x1\n备注: 豪华配置', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 5, 'RECEIPT', 3, '订单号:ORD20260105001\n金额: 2580.00\n支付方式: 微信支付\n桌号: 14', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 7, 'KITCHEN', 2, '订单号:ORD20260107001\n套餐: 双人浪漫套餐x1\n备注: 朋友聚餐', 1, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(10, 7, 'RECEIPT', 3, '订单号:ORD20260107001\n金额: 398.00\n支付方式: 支付宝\n桌号: 3', 1, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(11, 9, 'KITCHEN', 2, '订单号:ORD20260109001\n套餐: 家庭欢聚套餐x1\n备注: 生日宴', 1, NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(12, 9, 'RECEIPT', 3, '订单号:ORD20260109001\n金额: 1880.00\n支付方式: 微信支付\n桌号: 17', 1, NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(13, 10, 'DELIVERY', 4, '订单号:ORD20260110001\n菜品: 单人工作餐x1, 清蒸鲈鱼x1, 扬州炒饭x2, 珍珠奶茶x2\n配送地址: 杭州市西湖区雷峰塔', 1, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(14, 11, 'RECEIPT', 3, '订单号:ORD20260111001\n金额: 328.00\n支付方式: 微信支付\n自提', 1, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(15, 12, 'RECEIPT', 3, '订单号:ORD20260112001\n金额: 168.00\n支付方式: 银行卡\n自提', 1, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ========================================
-- 33. 支付订单表 (payment_order) - 15条
-- ========================================
INSERT INTO payment_order (id, order_id, tenant_id, trade_no, channel_trade_no, channel, amount, status, paid_time, notify_time, created_time, updated_time) VALUES
(1, 1, 1, 'WX202601010001', 'wx_batch_001', 'WECHAT', 328.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 2, 1, 'ALI202601020001', 'alipay_batch_001', 'ALIPAY', 156.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 3, 1, 'WX202601030001', 'wx_batch_002', 'WECHAT', 858.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 4, 1, 'WX202601040001', 'wx_batch_003', 'WECHAT', 89.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 5, 1, 'ALI202601050001', 'alipay_batch_002', 'ALIPAY', 2580.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 6, 1, 'WX202601060001', NULL, 'WECHAT', 168.00, 'PENDING', NULL, NULL, NOW(), NOW()),
(7, 7, 1, 'ALI202601070001', 'alipay_batch_003', 'ALIPAY', 398.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(8, 8, 1, 'WX202601080001', NULL, 'WECHAT', 258.00, 'CANCELLED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(9, 9, 1, 'WX202601090001', 'wx_batch_004', 'WECHAT', 1880.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(10, 10, 1, 'ALI202601100001', 'alipay_batch_004', 'ALIPAY', 580.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(11, 11, 1, 'WX202601110001', 'wx_batch_005', 'WECHAT', 328.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(12, 12, 1, 'BANK202601120001', 'bank_batch_001', 'BANK_CARD', 168.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(13, 13, 1, 'WX202601130001', NULL, 'WECHAT', 1280.00, 'PENDING', NULL, NULL, NOW(), NOW()),
(14, 14, 1, 'ALI202601140001', 'alipay_batch_005', 'ALIPAY', 458.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(15, 15, 1, 'WX202601150001', 'wx_batch_006', 'WECHAT', 688.00, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- ========================================
-- 34. 退款记录表 (refund_record) - 5条
-- ========================================
INSERT INTO refund_record (id, payment_order_id, refund_no, amount, reason, status, created_time) VALUES
(1, 8, 'RF20260101001', 258.00, '用户取消订单', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(2, 1, 'RF20260102001', 58.00, '菜品质量问题', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 3, 'RF20260103001', 88.00, '上菜超时', 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 5, 'RF20260104001', 0.00, '部分退款-优惠券已使用', 'FAILED', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(5, 2, 'RF20260105001', 22.00, '菜品少送', 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ========================================
-- 35. 配送订单表 (delivery_order) - 10条
-- ========================================
INSERT INTO delivery_order (id, tenant_id, platform_order_id, platform, dish_summary, amount, user_name, phone, address, status, order_time, created_time, updated_time) VALUES
(1, 1, 'MT202601010001', 'MEITUAN', '红烧肉x1,宫保鸡丁x1,珍珠奶茶x1', 328.00, '张小明', '13900139001', '北京市东城区王府井大街1号', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 1, 'MT202601020001', 'MEITUAN', '鱼香肉丝x1,老醋花生x1,珍珠奶茶x2', 156.00, '李晓红', '13900139002', '上海市徐汇区南京路100号', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 1, 'ELE202601030001', 'ELEME', '麻婆豆腐x1,糖醋里脊x1', 89.00, '赵小美', '13900139004', '成都市锦江区春熙路88号', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 1, 'MT202601040001', 'MEITUAN', '宫保鸡丁x1,西湖牛肉羹x1,鲜榨果汁x2', 168.00, '猪八戒', '13900139006', '深圳市南山区科技园', 'DELIVERING', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(5, 1, 'ELE202601050001', 'ELEME', '鱼香肉丝x1,凉拌黄瓜x1,鲜榨果汁x1', 88.00, '白骨精', '13900139008', '杭州市上城区西湖路1号', 'PENDING', NOW(), NOW(), NOW()),
(6, 1, 'MT202601060001', 'MEITUAN', '单人工作餐x1,清蒸鲈鱼x1,扬州炒饭x2,珍珠奶茶x2', 580.00, '白素贞', '13900139010', '杭州市西湖区雷峰塔', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(7, 1, 'ELE202601070001', 'ELEME', '薯条x2,鸡米花x2,珍珠奶茶x1', 81.00, '孙小小', '13900139021', '北京市朝阳区望京', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(8, 1, 'MT202601080001', 'MEITUAN', '红烧肉x1,糖醋里脊x1,西湖牛肉羹x1', 168.00, '李小胖', '13900139022', '上海市浦东新区张江', 'CANCELLED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9, 1, 'ELE202601090001', 'ELEME', '清蒸鲈鱼x1,蒜蓉西兰花x1,鲜榨果汁x2', 138.00, '王大锤', '13900139023', '广州市天河区天河城', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10, 1, 'MT202601100001', 'MEITUAN', '辣子鸡x1,番茄鸡蛋汤x1,扬州炒饭x1', 102.00, '赵铁柱', '13900139024', '深圳市福田区华强北', 'PENDING', NOW(), NOW(), NOW());

-- ========================================
-- 完成提示
-- ========================================
SELECT '测试数据插入完成！' AS message;
SELECT
    '员工表' AS table_name, COUNT(*) AS count FROM employee
UNION ALL
SELECT '分类表', COUNT(*) FROM category
UNION ALL
SELECT '菜品表', COUNT(*) FROM dish
UNION ALL
SELECT '菜品口味表', COUNT(*) FROM dish_flavor
UNION ALL
SELECT '套餐表', COUNT(*) FROM setmeal
UNION ALL
SELECT '套餐菜品关联表', COUNT(*) FROM setmeal_dish
UNION ALL
SELECT '用户表', COUNT(*) FROM user
UNION ALL
SELECT '地址簿表', COUNT(*) FROM address_book
UNION ALL
SELECT '会员等级表', COUNT(*) FROM member_level
UNION ALL
SELECT '会员表', COUNT(*) FROM member
UNION ALL
SELECT '积分记录表', COUNT(*) FROM points_record
UNION ALL
SELECT '充值记录表', COUNT(*) FROM recharge_record
UNION ALL
SELECT '优惠券模板表', COUNT(*) FROM coupon_template
UNION ALL
SELECT '用户优惠券表', COUNT(*) FROM coupon_user
UNION ALL
SELECT '就餐区域表', COUNT(*) FROM dining_area
UNION ALL
SELECT '餐桌表', COUNT(*) FROM dining_table
UNION ALL
SELECT '排队表', COUNT(*) FROM dining_queue
UNION ALL
SELECT '预订表', COUNT(*) FROM dining_reservation
UNION ALL
SELECT '供应商表', COUNT(*) FROM supplier
UNION ALL
SELECT '物料分类表', COUNT(*) FROM material_category
UNION ALL
SELECT '物料表', COUNT(*) FROM material
UNION ALL
SELECT '库存记录表', COUNT(*) FROM stock_record
UNION ALL
SELECT '库存盘点表', COUNT(*) FROM stock_check
UNION ALL
SELECT '库存盘点明细表', COUNT(*) FROM stock_check_detail
UNION ALL
SELECT '采购单表', COUNT(*) FROM purchase_order
UNION ALL
SELECT '采购单明细表', COUNT(*) FROM purchase_order_detail
UNION ALL
SELECT '订单表', COUNT(*) FROM orders
UNION ALL
SELECT '订单明细表', COUNT(*) FROM order_detail
UNION ALL
SELECT '购物车表', COUNT(*) FROM shopping_cart
UNION ALL
SELECT '打印机配置表', COUNT(*) FROM printer_config
UNION ALL
SELECT '打印日志表', COUNT(*) FROM printer_log
UNION ALL
SELECT '支付订单表', COUNT(*) FROM payment_order
UNION ALL
SELECT '退款记录表', COUNT(*) FROM refund_record
UNION ALL
SELECT '配送订单表', COUNT(*) FROM delivery_order;
