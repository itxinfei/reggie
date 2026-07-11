-- ========================================
-- 瑞吉外卖前端测试数据
-- 每个菜单至少10条，不超过100条数据
-- ========================================

SET time_zone = '+08:00';

-- 使用INSERT IGNORE避免主键重复错误
SET sql_mode = '';

-- ========================================
-- 1. 菜品分类 (category) - 12条
-- ========================================
INSERT IGNORE INTO category (id, tenant_id, type, name, sort, create_time, update_time, is_deleted) VALUES
(1, 1, 1, '热销', 1, NOW(), NOW(), 0),
(2, 1, 1, '优惠', 2, NOW(), NOW(), 0),
(3, 1, 1, '热菜', 3, NOW(), NOW(), 0),
(4, 1, 1, '凉菜', 4, NOW(), NOW(), 0),
(5, 1, 1, '汤类', 5, NOW(), NOW(), 0),
(6, 1, 1, '主食', 6, NOW(), NOW(), 0),
(7, 1, 1, '饮品', 7, NOW(), NOW(), 0),
(8, 1, 1, '小吃', 8, NOW(), NOW(), 0),
(9, 1, 1, '海鲜', 9, NOW(), NOW(), 0),
(10, 1, 1, '素菜', 10, NOW(), NOW(), 0),
(11, 1, 2, '单人套餐', 11, NOW(), NOW(), 0),
(12, 1, 2, '双人套餐', 12, NOW(), NOW(), 0);

-- ========================================
-- 2. 菜品 (dish) - 30条
-- ========================================
INSERT IGNORE INTO dish (id, tenant_id, name, category_id, price, code, image, description, status, sort, create_time, update_time, is_deleted) VALUES
-- 热销菜品
(1, 1, '红烧肉', 1, 5800, 'DISH001', './images/dish1.jpg', '经典家常菜，肥而不腻，入口即化', 1, 1, NOW(), NOW(), 0),
(2, 1, '宫保鸡丁', 1, 4800, 'DISH002', './images/dish2.jpg', '川菜经典，麻辣鲜香，花生酥脆', 1, 2, NOW(), NOW(), 0),
(3, 1, '鱼香肉丝', 1, 4600, 'DISH003', './images/dish3.jpg', '酸甜可口，下饭神器，营养丰富', 1, 3, NOW(), NOW(), 0),
(4, 1, '麻婆豆腐', 1, 3800, 'DISH004', './images/dish4.jpg', '麻辣鲜烫，豆腐嫩滑，回味无穷', 1, 4, NOW(), NOW(), 0),
(5, 1, '糖醋里脊', 1, 5200, 'DISH005', './images/dish5.jpg', '外酥里嫩，酸甜适中，老少皆宜', 1, 5, NOW(), NOW(), 0),

-- 优惠菜品
(6, 1, '凉拌黄瓜', 2, 1800, 'DISH006', './images/dish6.jpg', '清爽解腻，开胃小菜，蒜香浓郁', 1, 6, NOW(), NOW(), 0),
(7, 1, '拍黄瓜', 2, 1600, 'DISH007', './images/dish7.jpg', '蒜香浓郁，爽脆可口，下酒好菜', 1, 7, NOW(), NOW(), 0),
(8, 1, '老醋花生', 2, 2200, 'DISH008', './images/dish8.jpg', '酸爽开胃，花生酥脆，经典凉菜', 1, 8, NOW(), NOW(), 0),

-- 热菜
(9, 1, '水煮肉片', 3, 5600, 'DISH009', './images/dish9.jpg', '麻辣鲜香，肉片嫩滑，川菜代表', 1, 9, NOW(), NOW(), 0),
(10, 1, '回锅肉', 3, 5200, 'DISH010', './images/dish10.jpg', '肉片香嫩，蒜苗翠绿，家常美味', 1, 10, NOW(), NOW(), 0),
(11, 1, '辣子鸡', 3, 6800, 'DISH011', './images/dish11.jpg', '麻辣鲜香，鸡肉酥脆，越吃越上瘾', 1, 11, NOW(), NOW(), 0),
(12, 1, '清蒸鲈鱼', 3, 8800, 'DISH012', './images/dish12.jpg', '肉质鲜嫩，原汁原味，营养丰富', 1, 12, NOW(), NOW(), 0),
(13, 1, '红烧排骨', 3, 6600, 'DISH013', './images/dish13.jpg', '色泽红亮，肉质酥烂，味道浓郁', 1, 13, NOW(), NOW(), 0),

-- 凉菜
(14, 1, '皮蛋豆腐', 4, 2200, 'DISH014', './images/dish14.jpg', '清凉爽口，开胃佳品，简单美味', 1, 14, NOW(), NOW(), 0),
(15, 1, '蒜泥白肉', 4, 3800, 'DISH015', './images/dish15.jpg', '蒜香浓郁，肉片薄嫩，经典川菜', 1, 15, NOW(), NOW(), 0),
(16, 1, '夫妻肺片', 4, 4200, 'DISH016', './images/dish16.jpg', '麻辣鲜香，口感丰富，四川名菜', 1, 16, NOW(), NOW(), 0),

-- 汤类
(17, 1, '番茄鸡蛋汤', 5, 2000, 'DISH017', './images/dish17.jpg', '家常汤品，酸甜适口，营养健康', 1, 17, NOW(), NOW(), 0),
(18, 1, '西湖牛肉羹', 5, 2800, 'DISH018', './images/dish18.jpg', '鲜美滑嫩，营养丰富，口感细腻', 1, 18, NOW(), NOW(), 0),
(19, 1, '紫菜蛋花汤', 5, 1800, 'DISH019', './images/dish19.jpg', '清淡鲜美，营养丰富，老少皆宜', 1, 19, NOW(), NOW(), 0),

-- 主食
(20, 1, '扬州炒饭', 6, 3200, 'DISH020', './images/dish20.jpg', '粒粒分明，配料丰富，经典主食', 1, 20, NOW(), NOW(), 0),
(21, 1, '牛肉面', 6, 3600, 'DISH021', './images/dish21.jpg', '汤浓肉烂，面条劲道，满足感强', 1, 21, NOW(), NOW(), 0),
(22, 1, '小笼包', 6, 2400, 'DISH022', './images/dish22.jpg', '皮薄馅多，汤汁鲜美，精致点心', 1, 22, NOW(), NOW(), 0),

-- 饮品
(23, 1, '珍珠奶茶', 7, 1800, 'DISH023', './images/dish23.jpg', '香浓丝滑，Q弹珍珠，人气饮品', 1, 23, NOW(), NOW(), 0),
(24, 1, '鲜榨果汁', 7, 2200, 'DISH024', './images/dish24.jpg', '新鲜水果，营养健康，维C满满', 1, 24, NOW(), NOW(), 0),
(25, 1, '柠檬水', 7, 1200, 'DISH025', './images/dish25.jpg', '清爽解渴，酸甜可口，夏日必备', 1, 25, NOW(), NOW(), 0),

-- 小吃
(26, 1, '薯条', 8, 1500, 'DISH026', './images/dish26.jpg', '外脆内软，咸香可口，休闲小吃', 1, 26, NOW(), NOW(), 0),
(27, 1, '鸡米花', 8, 1800, 'DISH027', './images/dish27.jpg', '香脆可口，适合分享，人气小吃', 1, 27, NOW(), NOW(), 0),
(28, 1, '炸鸡翅', 8, 2200, 'DISH028', './images/dish28.jpg', '外酥里嫩，多汁美味，经典炸鸡', 1, 28, NOW(), NOW(), 0),

-- 海鲜
(29, 1, '蒜蓉粉丝蒸扇贝', 9, 5800, 'DISH029', './images/dish29.jpg', '蒜香浓郁，扇贝鲜嫩，粉丝入味', 1, 29, NOW(), NOW(), 0),
(30, 1, '椒盐虾', 9, 6800, 'DISH030', './images/dish30.jpg', '外酥里嫩，椒盐香浓，海鲜美味', 1, 30, NOW(), NOW(), 0);

-- ========================================
-- 3. 菜品口味 (dish_flavor) - 25条
-- ========================================
INSERT IGNORE INTO dish_flavor (id, tenant_id, dish_id, name, value, create_time, update_time, is_deleted) VALUES
(1, 1, 1, '辣度', '["不辣","微辣","中辣","重辣"]', NOW(), NOW(), 0),
(2, 1, 2, '辣度', '["不辣","微辣","中辣","重辣"]', NOW(), NOW(), 0),
(3, 1, 2, '忌口', '["不要葱","不要蒜","不要香菜"]', NOW(), NOW(), 0),
(4, 1, 3, '辣度', '["微辣","中辣","重辣"]', NOW(), NOW(), 0),
(5, 1, 4, '辣度', '["不辣","微辣","中辣","重辣"]', NOW(), NOW(), 0),
(6, 1, 5, '口味', '["酸甜","咸甜"]', NOW(), NOW(), 0),
(7, 1, 9, '辣度', '["微辣","中辣","重辣","特辣"]', NOW(), NOW(), 0),
(8, 1, 11, '辣度', '["微辣","中辣","重辣","特辣"]', NOW(), NOW(), 0),
(9, 1, 12, '口味', '["清蒸","红烧","糖醋"]', NOW(), NOW(), 0),
(10, 1, 13, '口味', '["原味","蒜香","麻辣"]', NOW(), NOW(), 0),
(11, 1, 15, '辣度', '["不辣","微辣","中辣"]', NOW(), NOW(), 0),
(12, 1, 16, '辣度', '["微辣","中辣","重辣"]', NOW(), NOW(), 0),
(13, 1, 17, '温度', '["热饮","常温"]', NOW(), NOW(), 0),
(14, 1, 18, '温度', '["热饮","常温"]', NOW(), NOW(), 0),
(15, 1, 20, '口味', '["原味","加蛋","加肉"]', NOW(), NOW(), 0),
(16, 1, 21, '辣度', '["不辣","微辣","中辣"]', NOW(), NOW(), 0),
(17, 1, 22, '口味', '["鲜肉","蟹黄","虾仁"]', NOW(), NOW(), 0),
(18, 1, 23, '甜度', '["无糖","少糖","半糖","多糖","全糖"]', NOW(), NOW(), 0),
(19, 1, 23, '温度', '["热饮","常温","去冰","少冰","多冰"]', NOW(), NOW(), 0),
(20, 1, 24, '甜度', '["无糖","少糖","半糖","多糖"]', NOW(), NOW(), 0),
(21, 1, 24, '冰度', '["常温","少冰","多冰"]', NOW(), NOW(), 0),
(22, 1, 26, '口味', '["原味","番茄","芝士"]', NOW(), NOW(), 0),
(23, 1, 27, '口味', '["原味","辣味","番茄味"]', NOW(), NOW(), 0),
(24, 1, 29, '口味', '["蒜蓉","豆豉","麻辣"]', NOW(), NOW(), 0),
(25, 1, 30, '口味', '["椒盐","蒜蓉","香辣"]', NOW(), NOW(), 0);

-- ========================================
-- 4. 套餐 (setmeal) - 12条
-- ========================================
INSERT IGNORE INTO setmeal (id, tenant_id, category_id, name, price, status, code, description, image, create_time, update_time, is_deleted) VALUES
(1, 1, 11, '单人工作餐', 2800, 1, 'SET001', '适合一人用餐，包含主食+菜品+饮品', './images/setmeal1.jpg', NOW(), NOW(), 0),
(2, 1, 11, '经济单人餐', 1980, 1, 'SET002', '经济实惠，饱腹之选', './images/setmeal2.jpg', NOW(), NOW(), 0),
(3, 1, 11, '豪华单人餐', 3800, 1, 'SET003', '丰盛美味，满足感强', './images/setmeal3.jpg', NOW(), NOW(), 0),
(4, 1, 12, '双人浪漫套餐', 8800, 1, 'SET004', '适合情侣或朋友，包含2主菜+1汤+2饮品', './images/setmeal4.jpg', NOW(), NOW(), 0),
(5, 1, 12, '朋友聚会套餐', 11800, 1, 'SET005', '3-4人聚餐首选，菜品丰富', './images/setmeal5.jpg', NOW(), NOW(), 0),
(6, 1, 12, '家庭欢聚套餐', 16800, 1, 'SET006', '适合3-4人家庭聚餐', './images/setmeal6.jpg', NOW(), NOW(), 0),
(7, 1, 11, '商务洽谈套餐', 12800, 1, 'SET007', '适合商务宴请，菜品精致', './images/setmeal7.jpg', NOW(), NOW(), 0),
(8, 1, 11, '儿童营养套餐', 2600, 1, 'SET008', '专为儿童设计，营养均衡', './images/setmeal8.jpg', NOW(), NOW(), 0),
(9, 1, 11, '素食健康套餐', 3200, 1, 'SET009', '全素菜品，健康美味', './images/setmeal9.jpg', NOW(), NOW(), 0),
(10, 1, 12, '海鲜盛宴套餐', 28800, 1, 'SET010', '精选海鲜，奢华享受', './images/setmeal10.jpg', NOW(), NOW(), 0),
(11, 1, 12, '烧烤派对套餐', 15800, 1, 'SET011', '烧烤串+啤酒，聚会首选', './images/setmeal11.jpg', NOW(), NOW(), 0),
(12, 1, 11, '宝宝套餐', 2200, 1, 'SET012', '软糯易消化，适合宝宝', './images/setmeal12.jpg', NOW(), NOW(), 0);

-- ========================================
-- 5. 套餐菜品关联 (setmeal_dish) - 40条
-- ========================================
INSERT IGNORE INTO setmeal_dish (id, tenant_id, setmeal_id, dish_id, name, price, copies, sort, create_time, update_time, is_deleted) VALUES
-- 单人工作餐
(1, 1, 1, 20, '扬州炒饭', 3200, 1, 1, NOW(), NOW(), 0),
(2, 1, 1, 1, '红烧肉', 5800, 1, 2, NOW(), NOW(), 0),
(3, 1, 1, 23, '珍珠奶茶', 1800, 1, 3, NOW(), NOW(), 0),

-- 经济单人餐
(4, 1, 2, 20, '扬州炒饭', 3200, 1, 1, NOW(), NOW(), 0),
(5, 1, 2, 6, '凉拌黄瓜', 1800, 1, 2, NOW(), NOW(), 0),

-- 豪华单人餐
(6, 1, 3, 12, '清蒸鲈鱼', 8800, 1, 1, NOW(), NOW(), 0),
(7, 1, 3, 20, '扬州炒饭', 3200, 1, 2, NOW(), NOW(), 0),
(8, 1, 3, 23, '珍珠奶茶', 1800, 1, 3, NOW(), NOW(), 0),

-- 双人浪漫套餐
(9, 1, 4, 2, '宫保鸡丁', 4800, 1, 1, NOW(), NOW(), 0),
(10, 1, 4, 5, '糖醋里脊', 5200, 1, 2, NOW(), NOW(), 0),
(11, 1, 4, 18, '西湖牛肉羹', 2800, 1, 3, NOW(), NOW(), 0),
(12, 1, 4, 23, '珍珠奶茶', 1800, 2, 4, NOW(), NOW(), 0),

-- 朋友聚会套餐
(13, 1, 5, 2, '宫保鸡丁', 4800, 1, 1, NOW(), NOW(), 0),
(14, 1, 5, 3, '鱼香肉丝', 4600, 1, 2, NOW(), NOW(), 0),
(15, 1, 5, 9, '水煮肉片', 5600, 1, 3, NOW(), NOW(), 0),
(16, 1, 5, 17, '番茄鸡蛋汤', 2000, 1, 4, NOW(), NOW(), 0),
(17, 1, 5, 20, '扬州炒饭', 3200, 2, 5, NOW(), NOW(), 0),

-- 家庭欢聚套餐
(18, 1, 6, 1, '红烧肉', 5800, 1, 1, NOW(), NOW(), 0),
(19, 1, 6, 2, '宫保鸡丁', 4800, 1, 2, NOW(), NOW(), 0),
(20, 1, 6, 12, '清蒸鲈鱼', 8800, 1, 3, NOW(), NOW(), 0),
(21, 1, 6, 17, '番茄鸡蛋汤', 2000, 1, 4, NOW(), NOW(), 0),
(22, 1, 6, 20, '扬州炒饭', 3200, 3, 5, NOW(), NOW(), 0),
(23, 1, 6, 24, '鲜榨果汁', 2200, 3, 6, NOW(), NOW(), 0),

-- 商务洽谈套餐
(24, 1, 7, 3, '鱼香肉丝', 4600, 1, 1, NOW(), NOW(), 0),
(25, 1, 7, 4, '麻婆豆腐', 3800, 1, 2, NOW(), NOW(), 0),
(26, 1, 7, 5, '糖醋里脊', 5200, 1, 3, NOW(), NOW(), 0),
(27, 1, 7, 18, '西湖牛肉羹', 2800, 1, 4, NOW(), NOW(), 0),

-- 儿童营养套餐
(28, 1, 8, 26, '薯条', 1500, 1, 1, NOW(), NOW(), 0),
(29, 1, 8, 27, '鸡米花', 1800, 1, 2, NOW(), NOW(), 0),
(30, 1, 8, 22, '小笼包', 2400, 1, 3, NOW(), NOW(), 0),
(31, 1, 8, 23, '珍珠奶茶', 1800, 1, 4, NOW(), NOW(), 0),

-- 素食健康套餐
(32, 1, 9, 10, '回锅肉', 5200, 1, 1, NOW(), NOW(), 0),
(33, 1, 9, 6, '凉拌黄瓜', 1800, 1, 2, NOW(), NOW(), 0),
(34, 1, 9, 17, '番茄鸡蛋汤', 2000, 1, 3, NOW(), NOW(), 0),

-- 海鲜盛宴套餐
(35, 1, 10, 29, '蒜蓉粉丝蒸扇贝', 5800, 1, 1, NOW(), NOW(), 0),
(36, 1, 10, 30, '椒盐虾', 6800, 1, 2, NOW(), NOW(), 0),
(37, 1, 10, 12, '清蒸鲈鱼', 8800, 1, 3, NOW(), NOW(), 0),

-- 烧烤派对套餐
(38, 1, 11, 28, '炸鸡翅', 2200, 2, 1, NOW(), NOW(), 0),
(39, 1, 11, 27, '鸡米花', 1800, 2, 2, NOW(), NOW(), 0),
(40, 1, 11, 26, '薯条', 1500, 2, 3, NOW(), NOW(), 0);

-- ========================================
-- 6. 员工 (employee) - 10条
-- ========================================
INSERT IGNORE INTO employee (id, username, name, password, password_type, phone, sex, id_number, status, tenant_id, create_time, update_time, create_user, update_user) VALUES
(1, 'admin', '系统管理员', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138001', '男', '110101199001011234', 1, 1, NOW(), NOW(), 1, 1),
(2, 'zhangsan', '张三', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138002', '男', '110101199002021234', 1, 1, NOW(), NOW(), 1, 1),
(3, 'lisi', '李四', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138003', '女', '110101199003031234', 1, 1, NOW(), NOW(), 1, 1),
(4, 'wangwu', '王五', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138004', '男', '110101199004041234', 1, 1, NOW(), NOW(), 1, 1),
(5, 'zhaoliu', '赵六', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138005', '女', '110101199005051234', 1, 1, NOW(), NOW(), 1, 1),
(6, 'sunqi', '孙七', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138006', '男', '110101199006061234', 1, 1, NOW(), NOW(), 1, 1),
(7, 'zhouba', '周八', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138007', '女', '110101199007071234', 1, 1, NOW(), NOW(), 1, 1),
(8, 'wujiu', '吴九', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138008', '男', '110101199008081234', 1, 1, NOW(), NOW(), 1, 1),
(9, 'zhengshi', '郑十', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138009', '女', '110101199009091234', 1, 1, NOW(), NOW(), 1, 1),
(10, 'qianyi', '钱十一', 'e10adc3949ba59abbe56e057f20f883e', 'MD5', '13800138010', '男', '110101199010101234', 1, 1, NOW(), NOW(), 1, 1);

-- ========================================
-- 7. 地址簿 (address_book) - 12条
-- ========================================
INSERT IGNORE INTO address_book (id, user_id, consignee, sex, phone, province_code, province_name, city_code, city_name, district_code, district_name, detail, label, is_default, create_time, update_time, is_deleted, tenant_id) VALUES
(1, 1, '张小明', '男', '13900139001', '110000', '北京市', '110100', '北京市', '110101', '东城区', '北京市东城区王府井大街1号', '家', 1, NOW(), NOW(), 0, 1),
(2, 1, '张小明', '男', '13900139001', '110000', '北京市', '110100', '北京市', '110102', '西城区', '北京市西城区金融街8号', '公司', 0, NOW(), NOW(), 0, 1),
(3, 2, '李晓红', '女', '13900139002', '310000', '上海市', '310100', '上海市', '310104', '徐汇区', '上海市徐汇区南京路100号', '家', 1, NOW(), NOW(), 0, 1),
(4, 3, '王大军', '男', '13900139003', '440000', '广东省', '440100', '广州市', '440103', '荔湾区', '广州市荔湾区中山路50号', '家', 1, NOW(), NOW(), 0, 1),
(5, 4, '赵小美', '女', '13900139004', '510000', '四川省', '510100', '成都市', '510104', '锦江区', '成都市锦江区春熙路88号', '家', 1, NOW(), NOW(), 0, 1),
(6, 5, '孙悟空', '男', '13900139005', '440000', '广东省', '440300', '深圳市', '440304', '南山区', '深圳市南山区科技园', '公司', 1, NOW(), NOW(), 0, 1),
(7, 6, '猪八戒', '男', '13900139006', '330000', '浙江省', '330100', '杭州市', '330102', '上城区', '杭州市上城区西湖路1号', '家', 1, NOW(), NOW(), 0, 1),
(8, 7, '沙和尚', '男', '13900139007', '420000', '湖北省', '420100', '武汉市', '420106', '武昌区', '武汉市武昌区珞珈山', '家', 1, NOW(), NOW(), 0, 1),
(9, 8, '白骨精', '女', '13900139008', '320000', '江苏省', '320100', '南京市', '320102', '玄武区', '南京市玄武区中山陵', '家', 1, NOW(), NOW(), 0, 1),
(10, 9, '唐三藏', '男', '13900139009', '610000', '陕西省', '610100', '西安市', '610103', '碑林区', '西安市碑林区大雁塔', '公司', 1, NOW(), NOW(), 0, 1),
(11, 10, '白素贞', '女', '13900139010', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '杭州市西湖区雷峰塔', '家', 1, NOW(), NOW(), 0, 1),
(12, 11, '小青', '女', '13900139011', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '杭州市西湖区断桥', '家', 1, NOW(), NOW(), 0, 1);

-- ========================================
-- 8. 会员等级 (member_level) - 5条
-- ========================================
INSERT IGNORE INTO member_level (id, tenant_id, name, min_points, discount, created_time) VALUES
(1, 1, '普通会员', 0, 1.00, NOW()),
(2, 1, '银卡会员', 1000, 0.95, NOW()),
(3, 1, '金卡会员', 5000, 0.90, NOW()),
(4, 1, '钻石会员', 10000, 0.85, NOW()),
(5, 1, '至尊会员', 50000, 0.80, NOW());

-- ========================================
-- 9. 会员 (member) - 12条
-- ========================================
INSERT IGNORE INTO member (id, tenant_id, user_id, level_id, name, phone, points, balance, total_consumption, status, created_time, updated_time) VALUES
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
(12, 1, 12, 1, '许仙', '13900139012', 800, 200.00, 560.00, 1, NOW(), NOW());

-- ========================================
-- 10. 桌台 (dining_table) - 15条
-- ========================================
INSERT IGNORE INTO dining_table (id, tenant_id, area_id, name, seat_count, status, min_amount, qr_code_url, sort, created_time, updated_time) VALUES
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
(15, 1, 5, 'E01', 4, 'FREE', NULL, 'http://localhost:8080/qr/table/E01', 15, NOW(), NOW());

-- ========================================
-- 11. 区域 (dining_area) - 8条
-- ========================================
INSERT IGNORE INTO dining_area (id, tenant_id, name, sort, created_time, updated_time) VALUES
(1, 1, '大厅A区', 1, NOW(), NOW()),
(2, 1, '大厅B区', 2, NOW(), NOW()),
(3, 1, '包间区', 3, NOW(), NOW()),
(4, 1, 'VIP包间', 4, NOW(), NOW()),
(5, 1, '露天花园', 5, NOW(), NOW()),
(6, 1, '二楼宴会厅', 6, NOW(), NOW()),
(7, 1, '吧台区', 7, NOW(), NOW()),
(8, 1, '儿童游乐区', 8, NOW(), NOW());

-- ========================================
-- 12. 用户 (user) - 12条
-- ========================================
INSERT IGNORE INTO user (id, tenant_id, name, phone, sex, id_number, avatar, status) VALUES
(1, 1, '张小明', '13900139001', '男', '110101199001011001', './images/avatar1.jpg', 1),
(2, 1, '李晓红', '13900139002', '女', '110101199002021002', './images/avatar2.jpg', 1),
(3, 1, '王大军', '13900139003', '男', '110101199003031003', './images/avatar3.jpg', 1),
(4, 1, '赵小美', '13900139004', '女', '110101199004041004', './images/avatar4.jpg', 1),
(5, 1, '孙悟空', '13900139005', '男', '110101199005051005', './images/avatar5.jpg', 1),
(6, 1, '猪八戒', '13900139006', '男', '110101199006061006', './images/avatar6.jpg', 1),
(7, 1, '沙和尚', '13900139007', '男', '110101199007071007', './images/avatar7.jpg', 1),
(8, 1, '白骨精', '13900139008', '女', '110101199008081008', './images/avatar8.jpg', 1),
(9, 1, '唐三藏', '13900139009', '男', '110101199009091009', './images/avatar9.jpg', 1),
(10, 1, '白素贞', '13900139010', '女', '110101199010101010', './images/avatar10.jpg', 1),
(11, 1, '小青', '13900139011', '女', '110101199011111011', './images/avatar11.jpg', 1),
(12, 1, '许仙', '13900139012', '男', '110101199012121012', './images/avatar12.jpg', 1);

-- ========================================
-- 13. 订单 (orders) - 12条
-- ========================================
INSERT IGNORE INTO orders (id, number, status, user_id, address_book_id, order_time, checkout_time, pay_method, amount, remark, phone, address, user_name, consignee, table_id, dining_type) VALUES
(1, 'ORD20260101001', 4, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 1 HOUR, 1, 328.00, '少辣', '13900139001', '北京市东城区王府井大街1号', '张小明', '张小明', NULL, 'DELIVERY'),
(2, 'ORD20260102001', 4, 2, 3, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 1 HOUR, 2, 156.00, NULL, '13900139002', '上海市徐汇区南京路100号', '李晓红', '李晓红', NULL, 'DELIVERY'),
(3, 'ORD20260103001', 4, 3, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 2 HOUR, 1, 858.00, '商务宴请', '13900139003', NULL, '王大军', '王大军', 12, 'DINING'),
(4, 'ORD20260104001', 4, 4, 4, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 HOUR, 3, 89.00, '快一点', '13900139004', '成都市锦江区春熙路88号', '赵小美', '赵小美', NULL, 'DELIVERY'),
(5, 'ORD20260105001', 4, 5, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 3 HOUR, 1, 2580.00, '豪华配置', '13900139005', NULL, '孙悟空', '孙悟空', 14, 'DINING'),
(6, 'ORD20260106001', 1, 6, 7, NOW(), NULL, 1, 168.00, NULL, '13900139006', '深圳市南山区科技园', '猪八戒', '猪八戒', NULL, 'DELIVERY'),
(7, 'ORD20260107001', 4, 7, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR) + INTERVAL 2 HOUR, 2, 398.00, '朋友聚餐', '13900139007', NULL, '沙和尚', '沙和尚', 3, 'DINING'),
(8, 'ORD20260108001', 5, 8, 9, DATE_SUB(NOW(), INTERVAL 10 HOUR), NULL, NULL, 258.00, '取消订单', '13900139008', '杭州市上城区西湖路1号', '白骨精', '白骨精', NULL, 'DELIVERY'),
(9, 'ORD20260109001', 4, 9, NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR) + INTERVAL 2 HOUR, 1, 1880.00, '生日宴', '13900139009', NULL, '唐三藏', '唐三藏', 17, 'DINING'),
(10, 'ORD20260110001', 4, 10, 11, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR) + INTERVAL 1 HOUR, 2, 580.00, NULL, '13900139010', '杭州市西湖区雷峰塔', '白素贞', '白素贞', NULL, 'DELIVERY'),
(11, 'ORD20260111001', 4, 11, 12, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR) + INTERVAL 1 HOUR, 1, 328.00, NULL, '13900139011', '杭州市西湖区断桥', '小青', '小青', NULL, 'TAKEOUT'),
(12, 'ORD20260112001', 1, 12, NULL, NOW(), NULL, 1, 1280.00, '公司聚餐', '13900139012', NULL, '许仙', '许仙', 11, 'DINING');

-- ========================================
-- 14. 订单明细 (order_detail) - 35条
-- ========================================
INSERT IGNORE INTO order_detail (id, name, image, order_id, dish_id, setmeal_id, dish_flavor, number, amount) VALUES
(1, '扬州炒饭', './images/dish20.jpg', 1, 20, NULL, NULL, 1, 32.00),
(2, '红烧肉', './images/dish1.jpg', 1, 1, NULL, '["微辣"]', 1, 58.00),
(3, '珍珠奶茶', './images/dish23.jpg', 1, 23, NULL, '["少糖","少冰"]', 1, 18.00),
(4, '宫保鸡丁', './images/dish2.jpg', 1, 2, NULL, '["中辣"]', 1, 48.00),
(5, '鱼香肉丝', './images/dish3.jpg', 2, 3, NULL, '["微辣"]', 1, 46.00),
(6, '老醋花生', './images/dish8.jpg', 2, 8, NULL, NULL, 1, 22.00),
(7, '珍珠奶茶', './images/dish23.jpg', 2, 23, NULL, '["半糖","去冰"]', 2, 36.00),
(8, '红烧肉', './images/dish1.jpg', 3, 1, NULL, '["不辣"]', 2, 116.00),
(9, '宫保鸡丁', './images/dish2.jpg', 3, 2, NULL, '["微辣"]', 2, 96.00),
(10, '清蒸鲈鱼', './images/dish12.jpg', 3, 12, NULL, NULL, 1, 88.00),
(11, '番茄鸡蛋汤', './images/dish17.jpg', 3, 17, NULL, NULL, 1, 20.00),
(12, '鲜榨果汁', './images/dish24.jpg', 3, 24, NULL, NULL, 4, 88.00),
(13, '麻婆豆腐', './images/dish4.jpg', 4, 4, NULL, '["特辣"]', 1, 38.00),
(14, '糖醋里脊', './images/dish5.jpg', 4, 5, NULL, NULL, 1, 52.00),
(15, '单人工作餐', './images/setmeal1.jpg', 5, NULL, 1, NULL, 1, 28.00),
(16, '红烧肉', './images/dish1.jpg', 5, 1, NULL, '["中辣"]', 1, 58.00),
(17, '珍珠奶茶', './images/dish23.jpg', 5, 23, NULL, '["全糖","多冰"]', 3, 54.00),
(18, '宫保鸡丁', './images/dish2.jpg', 6, 2, NULL, '["不辣"]', 1, 48.00),
(19, '西湖牛肉羹', './images/dish18.jpg', 6, 18, NULL, NULL, 1, 28.00),
(20, '鲜榨果汁', './images/dish24.jpg', 6, 24, NULL, '["无糖"]', 2, 44.00),
(21, '双人浪漫套餐', './images/setmeal4.jpg', 7, NULL, 4, NULL, 1, 88.00),
(22, '鱼香肉丝', './images/dish3.jpg', 8, 3, NULL, '["中辣"]', 1, 46.00),
(23, '凉拌黄瓜', './images/dish6.jpg', 8, 6, NULL, NULL, 1, 18.00),
(24, '鲜榨果汁', './images/dish24.jpg', 8, 24, NULL, '["少糖"]', 1, 22.00),
(25, '家庭欢聚套餐', './images/setmeal6.jpg', 9, NULL, 6, NULL, 1, 168.00),
(26, '单人工作餐', './images/setmeal1.jpg', 10, NULL, 1, NULL, 1, 28.00),
(27, '清蒸鲈鱼', './images/dish12.jpg', 10, 12, NULL, NULL, 1, 88.00),
(28, '扬州炒饭', './images/dish20.jpg', 10, 20, NULL, NULL, 2, 64.00),
(29, '珍珠奶茶', './images/dish23.jpg', 10, 23, NULL, '["半糖","常温"]', 2, 36.00),
(30, '薯条', './images/dish26.jpg', 11, 26, NULL, NULL, 2, 30.00),
(31, '鸡米花', './images/dish27.jpg', 11, 27, NULL, NULL, 2, 36.00),
(32, '珍珠奶茶', './images/dish23.jpg', 11, 23, NULL, '["无糖","去冰"]', 1, 18.00),
(33, '商务洽谈套餐', './images/setmeal7.jpg', 12, NULL, 7, NULL, 1, 128.00),
(34, '番茄鸡蛋汤', './images/dish17.jpg', 3, 17, NULL, NULL, 1, 20.00),
(35, '麻婆豆腐', './images/dish4.jpg', 1, 4, NULL, '["中辣"]', 1, 38.00);

-- ========================================
-- 15. 购物车 (shopping_cart) - 10条
-- ========================================
INSERT IGNORE INTO shopping_cart (id, name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) VALUES
(1, '红烧肉', 1, 1, NULL, '["微辣"]', 2, 116.00, './images/dish1.jpg', NOW()),
(2, '珍珠奶茶', 1, 23, NULL, '["少糖","少冰"]', 1, 18.00, './images/dish23.jpg', NOW()),
(3, '宫保鸡丁', 2, 2, NULL, '["中辣"]', 1, 48.00, './images/dish2.jpg', NOW()),
(4, '单人工作餐', 3, NULL, 1, NULL, 2, 56.00, './images/setmeal1.jpg', NOW()),
(5, '清蒸鲈鱼', 5, 12, NULL, NULL, 1, 88.00, './images/dish12.jpg', NOW()),
(6, '鲜榨果汁', 5, 24, NULL, '["无糖"]', 2, 44.00, './images/dish24.jpg', NOW()),
(7, '鱼香肉丝', 7, 3, NULL, '["微辣"]', 1, 46.00, './images/dish3.jpg', NOW()),
(8, '薯条', 8, 26, NULL, NULL, 3, 45.00, './images/dish26.jpg', NOW()),
(9, '双人浪漫套餐', 10, NULL, 4, NULL, 1, 88.00, './images/setmeal4.jpg', NOW()),
(10, '鸡米花', 12, 27, NULL, NULL, 2, 36.00, './images/dish27.jpg', NOW());

-- ========================================
-- 完成提示
-- ========================================
SELECT '前端测试数据插入完成！' AS message;
SELECT
    '菜品分类' AS table_name, COUNT(*) AS count FROM category
UNION ALL
SELECT '菜品', COUNT(*) FROM dish
UNION ALL
SELECT '菜品口味', COUNT(*) FROM dish_flavor
UNION ALL
SELECT '套餐', COUNT(*) FROM setmeal
UNION ALL
SELECT '套餐菜品关联', COUNT(*) FROM setmeal_dish
UNION ALL
SELECT '员工', COUNT(*) FROM employee
UNION ALL
SELECT '地址簿', COUNT(*) FROM address_book
UNION ALL
SELECT '会员等级', COUNT(*) FROM member_level
UNION ALL
SELECT '会员', COUNT(*) FROM member
UNION ALL
SELECT '桌台', COUNT(*) FROM dining_table
UNION ALL
SELECT '区域', COUNT(*) FROM dining_area
UNION ALL
SELECT '用户', COUNT(*) FROM user
UNION ALL
SELECT '订单', COUNT(*) FROM orders
UNION ALL
SELECT '订单明细', COUNT(*) FROM order_detail
UNION ALL
SELECT '购物车', COUNT(*) FROM shopping_cart;
