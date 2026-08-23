# Reggie 全量代码审查结果收集

> 审查日期：2026-08-21，审查人：Claude（主会话 + 子 agent 混合）

---

## 批次 2：配置(config) + 过滤器(filter) + 工具类(utils) — ✅ 子 agent 完成
（详见对话中的完整报告，关键问题摘要）

### 高严重度
- `RedisConfig.java:71` — ObjectMapper 可见性 ALL+ANY，配合 DefaultTyping 反序列化攻击面扩大
- `RedisConfig.java:77` — BasicPolymorphicTypeValidator 用 Object.class 作为 base type，白名单形同虚设
- `CsrfFilter.java:37` — @Order 对 @WebFilter 不生效，过滤器顺序不可控
- `LoginCheckFilter.java:140-141,169-170` — Session 取值强转 Long/String 无 instanceof 校验，可能 ClassCastException
- `TestImageGenerator.java:192` — 启动主线程做 20+ 次外网下载，阻塞启动数十秒

### 中严重度
- `LoginCheckFilter.java:144` — tenantId 回退 BaseContext 无效
- `TestImageGenerator.java` — 死代码 countValidImages/cleanInvalidImages；isValidImage 仅按大小判断
- `AsyncConfig.java:44-63` — 两个线程池 TaskDecorator 逻辑重复
- `SpringUtils.java:128` — matchPath 每次新建 AntPathMatcher
- `QRCodeUtil.java:130` — 域名硬编码占位符
- `CollectionUtils.java:26` — 强制类型转换消除泛型警告，运行期才暴露
- `CorsConfig.java:33-40` — 开发域名硬编码
- `LoginCheckFilter.java:39` — PATH_MATCHER 访问权限过宽

### 低严重度
- `WebSocketConfig.java:12` — 空壳配置类
- `WebMvcConfig.java:52` — 每次 new JacksonObjectMapper
- `CsrfFilter.java:118` — token 比较用 equals 而非 MessageDigest.isEqual
- `LoginCheckFilter.java:200` — 方法名 check 表意不清

---

## 批次 5：支付(payment) + 财务(finance) — ✅ 子 agent 完成
（详见对话中的完整报告，关键问题摘要）

### 高严重度
- `WechatPayChannel.java:129-152` — HMAC-SHA256 验签错误：apiKey 既拼接到待签串又作 HMAC 密钥
- `PaymentController.java:195-264` — 退款并发竞态：渠道退款调用在事务外，两个并发请求可能重复退款
- `PaymentController.java:240-256` — orderService.updateById 用全实体覆盖，可能误改其他字段
- `FinanceController.java:67` — 提现创建未用 @Validated，DTO 直接用实体暴露内部字段
- `FinanceServiceImpl.java:497-499` — 提现流水号仅用毫秒时间戳，碰撞风险
- `FinanceServiceImpl.java:241-244` — 金额差额用 double 比较，精度丢失
- `FinanceServiceImpl.java:364-395` — 利润趋势逐日查库 N+1（一年 365 次查询）

### 中严重度
- `PaymentOrderServiceImpl.java:91-108` — ThreadLocal 租户上下文未清理
- `PaymentOrderMapper.java:46-58` — CAS 更新缺 notify_time
- `RefundRecordServiceImpl.java:80-82` — 退款流水号 UUID 仅取前 8 位
- `FinanceServiceImpl.java:241` — getSystemAmount()/getPlatformAmount() 可能 NPE
- `FinanceController.java:79-82` — 审核状态参数缺枚举校验
- `FinanceServiceImpl.java:93-107` — 审核流程缺状态机校验
- `FinanceController.java:84,151` — 操作人姓名硬编码 "Admin"
- `FinanceServiceImpl.java:485` — getStatus() 可空作 Map key

### 低严重度
- `PaymentConfigProperties.java:39-45` — 密钥默认空串而非 null
- 全模块字段注入 @Autowired
- `FinanceServiceImpl.java:212` — 对账单号碰撞

---

## 批次 3：会员(member) + 营销(marketing) — ✅ 子 agent 完成

### member 模块 — 高严重度
- `MemberServiceImpl.java:104-127` — incrementPointsById 后 getById 判定升级有 TOCTOU 竞态
- `CouponTemplateServiceImpl.java:48` — setSql 字符串拼接违反 SQL 注入规范
- `CouponTemplateServiceImpl.java:37-76` — 领券后库存回滚失败被吞，库存永久丢失
- `MemberServiceImpl.java:82-90` — deductBalance 等 @Update 缺租户隔离条件，依赖拦截器

### member 模块 — 中严重度
- `PointsRecordController.java:169-223` — 积分类型过滤硬编码三套别名，earn/consume 永不匹配（死代码）
- `CouponUserServiceImpl.java:81-87` — 过期券先用后判非原子操作
- `MemberServiceImpl.java:93-129` — MemberRewardService 注入实现类致代理失效，@Transactional 不生效
- `MemberLevelServiceImpl.java:22-36` — getDefaultLevel/findLevelByPoints 缺租户隔离
- `RechargeRecordServiceImpl.java:35-54` — addBalance 缺 AND status=1 条件

### member 模块 — 低严重度
- `MemberController.java:275-319` — myInfo 缺 coupon 归属二次校验
- `MemberLevelController.java:96-112` — save/update 缺 @Valid，update 用全实体覆盖
- `MemberTagServiceImpl.java:217,225` — 标签颜色硬编码 #409EFF（蓝）不符合品牌金
- `RechargeRecordController/PointsRecordController:146-149` — 重复注释块
- `MemberController.java:86-97` — 按 id 升序深度分页

### marketing 模块 — 高严重度
- `MarketingToolServiceImpl.java:163-205` — 买赠计算缺租户隔离和使用次数校验
- `MarketingToolServiceImpl.java:252-301` — 秒杀 calculateFlashSalePrice read-check-act 严重超卖
- `MarketingServiceImpl.java:141-229` — 使用次数 getUserUsageCount 只查不落账，perUserLimit 永远不生效

### marketing 模块 — 中严重度
- `MarketingServiceImpl.java:141-184` — 满减/折扣未校验活动有效期时间窗口
- `MarketingToolServiceImpl.java:79-128` — 新客优惠无使用次数限制，可重复用
- `MarketingController.java:152-173` — 接口混用 @RequestParam + @RequestBody
- `MarketingController.java:83-137` — 批量保存循环逐条插入，缺 @Valid
- `MarketingServiceImpl.java:435-471` — getMarketingTrend 按天循环 N+1
- `MarketingServiceImpl.java:473-507` — getTopActivities 全量拉取内存排序
- `MarketingServiceImpl.java:186-229` — SCOPE_DISH 折扣错算到整单

### marketing 模块 — 低严重度
- `MarketingController.java:76-124` — 删除规则未校验关联生效中订单
- `MarketingToolController.java:72-168` — @Parameter "I d" 拼写错误

---

## 批次 1：common 公共组件 + annotation + aspect + event — ✅ 主会话审查完成

### 高严重度
- `RateLimitAspect.java:115-117` — 限流过期时间硬编码 1 秒（虽符合 perSecond 语义，但与 @RateLimit 注解的 timeWindow 参数不关联，注解上的 timeWindow 实际无效），限流窗口无法通过注解配置，灵活性丧失
- `BruteForceProtectionFilter.java:142` — doFilter 中 `chain.doFilter` 在 recordLoginFailure **之前**，登录失败依赖 Controller 主动调用 recordLoginFailure；Controller 忘记调用则防护完全失效，属"信任调用方"设计缺陷
- `BruteForceProtectionFilter.java:189-191` — recordFailedAttempt 的 catch 块仅 log.error 后静默返回，锁定失败时外部无感知，无法触发降级或告警
- `BruteForceProtectionFilter.java:228-229` — getFailedAttemptCount 用 Integer.parseInt(count.toString()) 解析 Redis 值，若 Redis 存储为非数字字符串（脚本异常写入），抛 NumberFormatException 被吞
- `PermissionAspect.java:85,91` — request.getAttribute 强转 (Long)/(String) 无 instanceof 校验，LoginCheckFilter 若存入非预期类型则 ClassCastException
- `PermissionAspect.java:159-163` — Redis 反序列化的 Set/List 直接强转为 Set<String>，泛型擦除下运行时类型不匹配会导致 ClassCastException
- `BaseContext.java:47-52` — setCurrentTenantId 允许写入 null（仅 warn），后续查询的租户过滤条件带 null，可能导致数据泄露或查询异常
- `RedisCacheUtil.java:18` — ScheduledExecutorService 用 volatile 修饰但非 atomic，init() 和 destroy() 在多线程下存在竞态
- `CsrfTokenUtil.java:32` — generateToken 中 `Long.toString(timestamp).getBytes()` 用默认字符集，extractTimestamp(73行)用 UTF-8 解码；在默认编码非 UTF-8 的系统（如 Windows GBK）下 timestamp 编码不一致，Long.parseLong 失败
- `LogMaskUtils.java:103-127` — 手写 JSON 字符串解析器的转义状态机不完整，仅检测 `\\`，无法正确处理 `\\\"` 或 `\\u0022`，脱敏边界错位
- `LogMaskUtils.java:83-85` — maskSensitiveInfo 用全局正则 `(\d{3})\d{4}(\d{4})` 替换，可能误伤订单号、门牌号等非敏感数字字段

### 中严重度
- `CsrfTokenUtil.java:26` — 每次 generateToken 新建 SecureRandom，建议提取为 static final 实例
- `CsrfTokenUtil.java:88-95` — isTokenNotExpired 未校验 token == null，直接传入 extractTimestamp 会导致 NPE
- `GlobalExceptionHandler.java:44-48` — SQL 异常仅依赖 MySQL `Duplicate entry` 消息格式的正则，切换数据库版本后匹配失败掩盖真实冲突；建议增加 `ex.getErrorCode()` 1062 判断
- `CsrfTokenUtil.java:66-79` — extractTimestamp 用明文字符串拼接时间戳，可被伪造；建议用 hash/签名
- `RateLimitAspect.java:152-153` — getClientIp 依赖 SpringUtils.getClientIp，代理头透传错误会导致限流失效
- `RedisCacheUtil.java:50` — 二删延时 600ms 硬编码，无法按场景调整
- `PermissionAspect.java:165,178,214,236,276` — 多处 catch(Exception) 静默返回空集合/降级，掩盖真实权限问题，安全语义弱化
- `GlobalExceptionHandler.java:45` — SQLIntegrityConstraintViolationException 仅识别 Duplicate entry，其他约束异常（外键、check）返回笼统"未知错误"

### 低严重度
- `RateLimitAspect.java` / `RedisCacheUtil.java` / `BruteForceProtectionFilter.java` 均字段注入 @Autowired
- `RateLimitAspect.java:181` — getClientIp 返回 "unknown"，多个未知 IP 共用一个限流 key，限流失效

---

## 批次 4：库存(inventory) + 订单(order) — ✅ 主会话审查完成

### inventory 模块 — 正面设计（值得保留）
- `StockRecordServiceImpl.java:52-79` — 库存出入库使用 SQL 层原子操作 `addStock`/`deductStock`（`WHERE stock_qty >= #{qty}`），彻底消除 read-check-write 超卖竞态
- `StockCheckServiceImpl.java:106-145` — 盘点 completeCheck 用原子 `adjustStockTo` + CAS 状态更新，异常时整个事务回滚含库存调整和明细插入
- `PurchaseOrderServiceImpl.java:121-150` — 采购收货用原子 CAS `receiveFully`（`received_qty < qty` 才更新）+ CAS 状态置 RECEIVED，防并发重复收货

### inventory 模块 — 高严重度
- `StockCheckServiceImpl.java:84-93` — completeCheck 循环内先 `materialService.getById` 读 `bookQty`，再 `adjustStockTo` 写——但 `bookQty` 仅用于明细记录，若盘点过程中库存被其他交易改变，明细记录的 bookQty 与实际盘点时刻不一致（业务可接受，但应在明细中同时记录操作时刻）
- `StockCheckServiceImpl.java:104` — 盈亏金额计算 `diff.multiply(material.getUnitPrice())`，`unitPrice` 可能为 null（虽用三元兜底 ZERO），但盈亏口径应统一（建议使用入库加权均价而非当前单价）

### inventory 模块 — 中严重度
- `PurchaseOrderServiceImpl.java:195-207` — approveOrder/cancelOrder 用 `updateById` 全实体覆盖，可能误改其他字段
- `PurchaseOrderServiceImpl.java:125-126` — 采购收货时 `alreadyReceived` 从内存快照取（`detail.getReceivedQty()`），并发下 CAS 保证只入库一次，但快照值可能已过时导致 `toReceive` 计算不准（实际入库量由 CAS 保证正确，此处仅影响 stockIn 记录量）

### order 模块 — 高严重度
- `OrderServiceImpl.java:265-361` — submitEatInOrder（堂食下单）**无幂等令牌保护**，与外卖下单 submit 的 Redis SETNX 不对称，并发重复提交风险
- `OrderServiceImpl.java:710-721` — confirmOrder 用 `updateById` 全实体覆盖，可能误改其他字段
- `OrderServiceImpl.java:729-756` — rejectOrder 先 `updateById` 变更状态为 CANCELLED，再回退库存；库存回退失败时订单状态已变，仅靠"补偿任务"兜底，补偿任务未在代码中确认存在
- `OrderServiceImpl.java:787-823` — cancelOrder 同 rejectOrder，先改状态再回退库存，时序相反，失败态不一致

### order 模块 — 中严重度
- `OrderServiceImpl.java:497-510` — orderPage 时间范围用字符串比较 `queryWrapper.ge/le(Orders::getOrderTime, beginTime)`，传入格式不规范则静默忽略
- `OrderServiceImpl.java:548-553` — userPage 用 BeanUtils.copyProperties 再设 OrderDetails，N+1 查询已在 543 行优化为分组，但 detailsMap.getOrDefault 对不在 details 中的订单返回空列表（正确）
- `OrderServiceImpl.java:596-598` — again 每次调用查询全量购物车，购物车大时性能差，应只查询订单中出现的商品 ID
- `OrderServiceImpl.java:603-606` — again 合并购物车时 key 未考虑 `dishFlavor`，不同口味的同一菜品会被错误合并

### order 模块 — 低严重度
- `OrderServiceImpl.java:248-253,354-359` — 打印失败仅 log.warn，无重试机制，打印任务可能永久丢失
- `OrderServiceImpl.java:902-904` — 幂等令牌含 System.currentTimeMillis()，同一毫秒内两次提交生成不同 key（虽 UUID 后缀保证唯一，但毫秒时间戳无实际作用）

---

## 批次 6：配送(delivery) + 打印(printer) — ✅ 主会话审查完成

### delivery 模块 — 高严重度
- `AbstractDeliveryPlatform.java:130-143` — 签名算法使用 MD5（`MD5 后转大写`），已被 NIST 正式废弃，存在碰撞攻击风险；生产接入外卖平台时各平台文档通常要求 HMAC-SHA256 或 RSA，当前通用 MD5 约定与实际平台要求不符，接入时必然要覆写
- `AbstractDeliveryPlatform.java:96-121` — postPlatform 调用 `HttpUtil.post(url, params)` **无超时配置、无重试逻辑、无熔断**；网络抖动时 hang 住主线程，影响接单响应时间；无重试意味着平台瞬时故障即视为失败，fail-closed 放大失败面
- `DeliveryEnhancedServiceImpl.java:371-397` — calculateStepFee 调用 `getFeeSteps(ruleId, null)` 传 null 作为 tenantId，阶梯费配置缺租户隔离，多租户下可能读到其他租户的费率导致配送费计算错误
- `DeliveryTrackingServiceImpl.java:100-127` — updateRiderLocation 先 `selectById` 再 `updateById` 全实体覆盖骑手坐标，并发上报时后写入覆盖先写入的最后位置时间，丢失中间轨迹；且未使用 CAS 条件更新

### delivery 模块 — 中严重度
- `MeituanAdapter.java:136-143` — verifyCallback 中 TreeMap 用 Object 作为 value 类型（`TreeMap<String, Object>`），buildSignContent 拼接时直接 `append(e.getValue())`，若回调参数含复合对象则 toString 语义不确定
- `MeituanAdapter.java:143` — 签名比对用 `computed.equals(sign.trim().toUpperCase())`，非时序安全比较，理论上可通过 timing attack 逐字节猜解（虽实际难度高，但回调验签是安全边界，建议用 MessageDigest.isEqual）
- `MeituanAdapter.java:39-53,56-69,73-88` — acceptOrder/syncMenu/updateStatus 均以 `body != null` 判断成功，平台返回 `{"code":500,"msg":"error"}` 这类非空错误体时误判为成功
- `DeliveryEnhancedServiceImpl.java:125-142` — isInRange 对 TYPE_CIRCLE 调用 calculateDistance 用 Haversine 公式，对 TYPE_POLYGON 用射线法；但 isInRange 内部又 selectById(ruleId) 重复查询，findMatchingRule 已先查过一遍（N+1）
- `DeliveryEnhancedServiceImpl.java:326-366` — isPointInPolygon 用字符串 replace 解析 JSON 坐标（`polygonPointsJson.replace("],", ...)`），非标准 JSON 解析，嵌套数组或含空格时解析失败且静默返回 false
- `DeliveryTrackingServiceImpl.java:286-298,339-346` — 配送时间统计和骑手统计用 Java 内存循环遍历全量记录，大租户数据量时全表扫描 + 内存计算，应改为聚合 SQL
- `DeliveryTrackingServiceImpl.java:65-76` — saveOrUpdateRider 更新走 `updateById` 全实体覆盖，可能误改其他字段

### delivery 模块 — 低严重度
- `DeliveryEnhancedServiceImpl.java:170-186` — calculateDeliveryFee 中 feeType 用魔法数字 1/2/3 判断，DeliveryRangeRule 中 TYPE_CIRCLE/TYPE_POLYGON 已用常量，feeType 同样应定义常量
- `DeliveryTrackingServiceImpl.java:367-423` — getDeliveryOverview 在同方法内对 Rider 和 DeliveryTimeRecord 各建一个 LambdaQueryWrapper 全量查库，可合并为聚合查询
- `DeliveryTrackingServiceImpl.java:427-438` — getStatusText 用 switch 硬编码 6 种状态，与枚举不符，应直接用枚举的 description

### printer 模块 — 高严重度
- `WindowsSystemPrinterAdapter.java:52-83` — print 方法创建 `DocPrintJob` 后调用 `printJob.print(doc, attributes)`，该方法**同步阻塞**直到打印完成或超时；打印队列卡住时主线程 hang 住，下单流程被打印阻塞。虽 PrinterServiceImpl 已加 try-catch 防止拖垮事务，但同步阻塞本身降低吞吐量
- `XprinterAdapter.java:30-36, GprinterAdapter.java:30-36` — Xprinter 和 Gprinter 适配器**完全未实现真实打印逻辑**，仅 log.info 后 return true，恒返回成功；实际部署时打印任务静默丢失，前端/日志均无失败信号，是最隐蔽的"假成功"bug
- `PrinterDeviceManager.java:49-57` — dispatch 获取适配器后直接调用 `adapter.print(job, config)`，无异常捕获、无重试、无超时，适配器抛未捕获异常时直接向上冒泡

### printer 模块 — 中严重度
- `PrinterServiceImpl.java:73-76` — 查询启用打印机用 `CONCAT(',', print_types, ',') LIKE` 做包含判断，正确但全表扫描 + 字符串函数，大量打印机时索引失效，性能差
- `PrinterServiceImpl.java:86-100` — 对每个打印机逐台 `dispatch` + `save` 日志，循环内事务行为不明确（ PrinterServiceImpl 类级无 @Transactional），单台失败不影响其他，但未记录失败原因细分（仅统一"打印失败"）
- `PrinterAdapterFactory.java:35-51` — getAdapter 的 switch 对 null 和未知 brand 均 fallback 到 windowsSystemPrinterAdapter，品牌拼写错误（如 "XPRTINTER"）会静默降级到 Windows 打印，无告警
- `WindowsSystemPrinterAdapter.java:204-235` — buildPrintData 中 `ByteArrayOutputStream` 未设置初始容量，频繁扩容；且 `baos.write("\n".getBytes(StandardCharsets.UTF_8))` 逐行 new byte[]，浪费内存

### printer 模块 — 低严重度
- `WindowsSystemPrinterAdapter.java:191-201` — findPrintService 用 `service.getName().contains(printerName.trim())` 模糊匹配，打印机名含子串（如 "HP-1020" 含 "1020"）时误匹配
- `WindowsSystemPrinterAdapter.java:237-259` — formatTextLine 中 `text.length()` 计算字符数而非字节数，中文占 2 字节会导致对齐错位
- `PrinterDeviceManager.java` / `PrinterAdapterFactory.java` 均字段注入 @Autowired

### printer 模块 — 正面设计（值得保留）
- `PrinterServiceImpl.java:31-35` — 类注释明确禁止类级 @Transactional，避免打印故障拖垮下单事务，语义正确
- `PrinterDeviceManager.java:39-45` — findPrinters 用 LambdaQueryWrapper，规范

---

## 批次 7：菜品(dish) + 套餐(setmeal) + 购物车(shopping) + 地址(address) — ✅ 主会话审查完成

### dish 模块 — 高严重度
- `DishServiceImpl.java:143` — `updateWithFlavor` 用 `this.updateById(dishDto)` 全实体覆盖，可能误改未显式提交的字段（如 `stockQty` 未改时仍写入 0）；建议用 `LambdaUpdateWrapper` 仅更新白名单字段
- `DishController.java:149` 等所有写操作 — 统一"先 getById 再校验 tenantId"模式，功能正确但非 fail-closed；getById 无租户过滤存在 IDOR 风险（虽在 ServiceImpl 层有补偿校验）

### dish 模块 — 中严重度
- `DishServiceImpl.java:289-301` — `autoToggleSoldOut` 先 getById 再 updateById，read-then-update TOCTOU 竞态（低风险，状态机非严格）
- `DishServiceImpl.java:310-333` — `getStats` 6 次 count 查询，可合并为 1 次聚合 SQL
- `DishSpecServiceImpl.java` — 所有方法 `if (tenantId != null)` 条件过滤，非 fail-closed
- `DishSpecServiceImpl.java:218-239` — `calculateSpecPrice` 对每个 option 单独 selectById（N+1，受 optionIds 数量限制影响小）
- `DishSpecServiceImpl.java` — `saveOrUpdateSpecGroup`/`saveOrUpdateSpecOption` 用 `updateById` 全实体覆盖

### dish 模块 — 低严重度
- `DishController.java:359-374` — `options` 下拉接口 `if (tenantId != null)` 条件过滤
- `DishSpecServiceImpl.java:255-289` — `getSpecStatistics` 分三次 count + 一次全量查 relation 做唯一 dish 数，可优化为一次聚合

### setmeal 模块 — 正面设计（值得保留）
- `SetmealServiceImpl.java` 全部写操作 — 双删缓存策略（写前删 + 事务提交后二删），设计良好
- `SetmealServiceImpl.java:184-217` — `deleteWithFlavorCheck` 先查套餐引用再删，设计合理

### setmeal 模块 — 中严重度
- `SetmealServiceImpl.java:134` — `updateWithDish` 用 `this.updateById(setmealDto)` 全实体覆盖
- `SetmealServiceImpl.java:237` — `getStats` 用 `this.count()` 无显式 tenantId（依赖 TenantLineInnerInterceptor，需确认 setmeal 表不在 IGNORE_TABLES 中）
- `SetmealController.java` — page/list/options 依赖 TenantLineInnerInterceptor 自动处理租户过滤，无补偿校验

### setmeal 模块 — 低严重度
- `SetmealServiceImpl.java:63` — `saveWithDish` 一删用 `doubleDeleteAllEntries("setmeal")` 清空全部套餐缓存，对大租户开销大；建议改为删除分类维度的缓存键

### shopping 模块 — 高严重度
- `ShoppingCartServiceImpl.java:29-66` — `sub()` 混合了"先读后原子"和"读后删除"两条路径：先 getOne 读数量，判断 >1 后 subQuantityAtomically 原子减；若原子减失败再 getOne + removeById，第二次 getOne 到 removeById 之间仍可能被另一线程 addQuantity。购物车以 userId 为作用域，低并发实际风险低

### shopping 模块 — 低严重度
- `ShoppingCartServiceImpl.java` — 类级 @Transactional，单个购物车操作不应有事务开销

### address 模块 — 中严重度
- `AddressBookController.java:133-153` — `setDefault` 先全量 `is_default = 0`（含 userId + tenantId 条件）再 `updateById` 设置当前地址，updateById 走的是未校验租户的 entity（先校验了 existing，OK 但非最优）
- `AddressBookController.java:186-198,210-222` — getDefault/list 仅按 userId 过滤，无 tenantId（address_book 表无 tenant_id 列，依赖 userId 隔离，可接受）

### 修正说明
- DishMapper.deductStock 实际上是正确的（`AND stock_qty >= #{qty}` 条件存在），前置摘要中曾误报"超卖风险"，此处更正为正确实现

---

## 批次 10：12 模块（franchise/cost/dining/cashier/report/recommend/auth/user/region/export/customer/common）— ✅ 子 agent 完成

### 高严重度
- `FranchiseeController.java:60,70,77` — update/removeByIds/getById 均无租户过滤，IDOR 越权
- `FranchiseContractController.java:63,73` — 修改/删除合同无租户归属校验
- `FranchiseSettlementServiceImpl.java:50,129-138` — 生成分账/确认/结算用 getById + updateById，无租户校验，可跨租户篡改资金数据
- `CostServiceImpl.java:409` — `qw.last("LIMIT " + limit)` 字符串拼接 SQL 注入
- `CashierServiceImpl.java:270-275` — `executeDailySettlement()` TOCTOU 日结竞态
- `CashierServiceImpl.java:203-213` — `cashPayment()` 无幂等检查，重试重复扣款
- `CashierServiceImpl.java:236-238` — `deleteCashierRecord()` 删除不回滚订单状态
- `TableAreaController.java:59-143` — 大面积 getById/removeById/updateById 无租户过滤
- `ReservationServiceImpl.java:48-84` — confirm/cancel/arrive 均无租户过滤
- `QueueServiceImpl.java:63-174` — takeNumber/callNext/cancelQueue 无租户过滤，序号污染
- `EmployeeController.java:124-138` — 密码升级在状态检查之前，已禁用账号仍可触发副作用
- `EmployeeController.java:190-249` — forgotPassword Mock 模式可能误入生产
- `EmployeeController.java:101-103` — 登录查询无 tenantId 过滤
- `UserServiceImpl.java:23-35` — register() check-then-act 竞态，并发注册同手机号重复
- `CustomerServiceServiceImpl.java:74-141` — getSessionById/assignAgent/closeSession/getSessionMessages 无租户过滤

### 中严重度
- `CostServiceImpl.java` 多处 — `if (tenantId != null)` 条件过滤，非 fail-closed
- `CostServiceImpl.java:311-362` — `getCostTrend()` 按天循环查询 N+1
- `CostController.java` — 所有 @RequestBody 缺 @Valid
- `ReportServiceImpl.java:265-267,485-518,624-629` — 多处按天循环 N+1 查询
- `ReportController.java:265-296` — exportReport 全量加载内存，可能 OOM
- `MarketingCampaignController.java:106-122` — 批量删除用 Map 接收，强制类型转换可能 ClassCastException
- `RecommendServiceImpl.java:247-252` — `qw.last("LIMIT " + limit * 2)` 字符串拼接
- `RecommendServiceImpl.java:341-348` — `dishService.list(wrapper)` 无 limit，加载全部在售菜品
- `RecommendServiceImpl.java:290-297` — 协同过滤对每个相似用户循环查询 N+1
- `UserController.java:119-134` — 验证码存 HttpSession，集群下不一致
- `UserController.java:132-134` — 开发模式日志输出明文验证码
- `EmployeeController.java:147-161` — 登录成功后重新查询逻辑混用内存 emp 和 freshEmp
- `EmployeeController.java:545-588` — 批量修改状态 TOCTOU 竞态
- `RegionController.java:51-56` — tree() 无权限注解 + 无缓存
- `RegionController.java:162-177` — 删除地区 TOCTOU 竞态
- `ExportController.java:102-114,147-149` — 订单导出包含未脱敏手机号（PII 泄露）
- `ExportController.java:148-151` — 金额累加用 double
- `ExportController.java:298-319` — `LIMIT 100000` 硬编码上限，可能 OOM
- `CustomerServiceServiceImpl.java:47` — sessionNo 用毫秒时间戳，高并发重复
- `CustomerServiceServiceImpl.java:158-172` — markMessagesAsRead 逐条 updateById，N 次写入
- `EmployeeGuardAspect.java:51-58` — Session 兜底从 session.getAttribute 获取，存在篡改风险
- `PermissionAspect.java:96-99` — Redis 不可用时每次查库，无本地缓存降级
- `BruteForceProtectionFilter.java:339-354` — getIdentifier 用 getParameter 获取 username，员工登录用 @RequestBody JSON，getParameter 无法获取（过滤器维度失效）
- `RedisCacheUtil.java:127-132` — deleteByPattern 前缀重叠可能误删
- `ReservationServiceImpl.java:77-81` — arrive() 桌台状态更新失败被吞，数据不一致
- `DiningTableServiceImpl.java:67-104` — changeStatus() 新建桌台状态为 null 时跳过校验
- `QueueServiceImpl.java:96-119` — 分布式锁降级过于宽松，Redis 不可用时放弃锁保护

### 低严重度
- `CashierServiceImpl.java:478-512` — getCashierTrend() 按天循环查询
- `CostController.java:38` — 路径 /cost 未遵循 /api/ 前缀
- `DiningTableController.java:83-87` — save() 新增桌台未设默认状态
- `RecommendController.java:187-205` — recordBrowse() 用 Map 接收缺结构化校验
- `RateLimitAspect.java:115-117` — 限流过期时间硬编码 1s
- `PermissionAspect.java:104` — split(",") 含空格时产生空字符串

### 正面发现
- `DishMapper.java:26-28` — deductStock 含 `AND stock_qty >= #{qty}` 条件，正确防超卖
- `SetmealServiceImpl.java` — 双删缓存策略设计良好

---

## 批次 8：系统管理(sys) + 门店(store) + 多租户(tenant) + 分类(category) — ✅ 子 agent 完成

### 高严重度
- `SystemConfigController.java:127-129` — update 直接 updateById(config)，前端可提交任意 id/tenantId，跨租户篡改配置
- `SystemConfigController.java:156-162` — delete 直接 removeById(id)，可删除任意租户配置
- `SystemConfigController.java:113-119` — add 直接 save(config)，前端提交的 tenantId 可被篡改
- `SystemConfigController.java:62-77` — rootBatchUpdate 的 configKey 来自前端无白名单校验
- `SystemConfigServiceImpl.java:63` — updateById(existing) 绕过租户拦截器
- `RoleController.java:141-147` — update 直接 roleService.updateById(role)，前端可提交任意 tenantId
- `RoleController.java:127-134` — add 直接 roleService.save(role)，前端可提交任意 tenantId
- `RoleController.java:154-170` — delete 用 getById(id) + updateById(role)，绕过租户拦截器
- `RoleController.java:75-90` — 分页查询缺 tenantId 过滤
- `RoleController.java:112-120` — list() 缺 tenantId 过滤
- `StoreController.java:88-94` — getStoreDetail 接受路径 tenantId 无越权校验，分店管理员可获取其他分店详情
- `StoreController.java:148-156` — updateStore 无越权校验
- `StoreController.java:179-187` — updateStatus 可停用/启用任意门店
- `StoreController.java:194-207` — batchUpdateStatus 接收任意 tenantIds 列表，可批量停用其他门店
- `StoreController.java:113-140` — createStore 用 Map 接收，storeType 无枚举校验，可传非法值
- `StoreServiceImpl.java:187-231` — listAllStores 全表查询无租户过滤
- `TenantController.java:54-58` — Mass Assignment：@RequestBody Tenant 直接绑定实体，passwordType 等字段可被篡改
- `TenantServiceImpl.java:69` — employee.setRole(1) 硬编码角色ID为超级管理员，魔法数字
- `TenantServiceImpl.java:69` — employee.setPhone(tenant.getPhone()) 与 controller 传入 phone 参数不一致
- `CategoryController.java:159-168` — get 详情接口缺 @RequireEmployee，可被未登录用户访问
- `CategoryController.java:174-190` — list 接口缺 @RequireEmployee
- `CategoryController.java:196-213` — options 接口缺 @RequireEmployee
- `CategoryServiceImpl.java:50,72,140` — remove/getById/updateById 均绕过租户拦截器
- `CategoryController.java:139-154` — update 用 LambdaUpdateWrapper 但缺 tenantId 过滤

### 跨模块关键问题
- `MybatisPlusConfig.java:71-73` — **ignoreTable 在 BaseContext.getCurrentTenantId() == null 时返回 true**（跳过所有租户过滤），与 getTenantId() 返回 -1L 的 fail-closed 兜底逻辑矛盾。租户上下文为空时隔离完全失效

### 中严重度
- `RoleServiceImpl.java:47-65` — assignPermissions 逐条 insert 而非批量插入
- `SysOperationLogController.java:96-103` — getByBizId 的 tableName 参数无白名单校验
- `SystemConfigController.java:82-97` — 分页查询缺 tenantId 过滤
- `StoreController.java:164-173` — switchStore 后 session.setAttribute 与 BaseContext.setCurrentTenantId 不一致
- `StoreServiceImpl.java:346-351` — getTodaySummary 全量加载订单求和，高订单量 OOM 风险
- `StoreController.java:198-207` — batchUpdateStatus 强制类型转换 ((List<Integer>) body.get("tenantIds"))，JSON 大数字抛 ClassCastException
- `TenantController.java:71-78` — catch (CustomException) 泄露内部错误信息给前端
- `TenantServiceImpl.java:72-73` — setSex("1") setIdNumber("") 硬编码默认值

### 低严重度
- `StoreController.java:230-231` — try-with-resources 多资源语法（JDK 9+ 才支持分号多资源），JDK 1.8 不兼容
- `CategoryController.java:84-85` — type.isEmpty() 无法过滤纯空格字符串
- `CategoryController.java:74` — @GetMapping 前异常缩进
- `SystemConfigController.java:159-160` — 重复的 @Parameter 注解

---

## 批次 9：AI(ai) + 消息通知(notification) + 定时任务(schedule) + 仪表盘(dashboard) — ✅ 子 agent 完成

### AI 模块 — 高严重度
- `AIChatServiceImpl.java:504-566` — buildMessages() 将用户消息直接拼接进对话上下文，无过滤/转义，Prompt 注入风险
- `AIChatServiceImpl.java:143-238` — CompletableFuture.runAsync 中 emitter.complete() 缺少统一异常保护，SSE 连接可能永久悬挂
- `AiProviderController.java:113,135,147` — getById/updateById/removeById 绕过租户拦截器（ai_provider_config 在 IGNORE_TABLES），可跨租户读取 API Key
- `AiProviderConfigServiceImpl.java:145-309` — 三处 HttpURLConnection 的 InputStream 未用 try-with-resources 关闭，资源泄露

### AI 模块 — 中严重度
- `AIChatController.java:137,188,204,276,309` — 多处 @RequestBody Map<String,Object> 配合强制类型转换，ClassCastException 风险
- `CircuitBreakerService.java:63-83` — 熔断器状态切换非原子，高并发下可能同时放行大量请求突破 PROBE_COUNT
- `AiProviderConfigServiceImpl.java:94-97` — activateProvider 用 updateById(p) 全字段更新，可能覆盖并发修改
- `AIChatServiceImpl.java:485-495` — recordFeedback 用 updateById(record) 全字段更新

### AI 模块 — 低严重度
- `AICacheService.java:62-89` — rebuildDishCache() 无租户隔离，全局缓存被所有租户共享
- `ConversationContextService.java:247-264` — compress() 用 synchronized 实例级锁，锁粒度过大

### notification 模块 — 高严重度
- `UserExperienceController.java:27-105` — 全部接口无 @RequireEmployee 或 @RequiresPermission，任意用户可触发系统通知和语音播报
- `NotificationServiceImpl.java:99-162` — @Transactional 方法内调用外部 HTTP（短信/推送），长事务持锁

### notification 模块 — 中严重度
- `NotificationController.java:222,280,333,487` — 多处 @RequestBody Map 配合强制类型转换，ClassCastException 风险
- `NotificationController.java:123,142` — 模板增删改接口无 @RequiresPermission 注解
- `NotificationServiceImpl.java:548-644` — resolveUserIds 按数字 ID 查用户无租户过滤，可跨租户投递通知
- `WebSocketMessageService.java:24-99` — 纯内存存储，进程重启后全部通知丢失

### notification 模块 — 低严重度
- `WebSocketMessageService.java:41-43,67-70,86-89` — tenantId == null 时回退硬编码 1L

### schedule 模块 — 高严重度
- `CouponExpirationTask.java:25` — 无分布式锁保护，多实例部署下必导致优惠券被重复处理

### schedule 模块 — 中/低严重度
- `OperationLogAspect.java:60-63` — 异步请求无法记录操作日志（RequestContextHolder 为 null）
- `StockRefundCompensationTask.java:121-125` — 补偿任务一次性加载全量订单，大租户内存压力大

### dashboard 模块 — 中严重度
- `DashboardServiceImpl.java:46` — 类级 @Transactional 标注在无写操作服务上，无意义
- `DashboardServiceImpl.java:149,281,388,511` — tenantId=null 时租户拦截器行为不确定（未显式跳过）

### dashboard 模块 — 低严重度
- `DashboardServiceImpl.java:467-492` — Redis Pipeline RENAME 非原子，expire 失败时无过期时间数据堆积
- `DashboardController.java:143-173` — /all 接口异常返回结构与正常不一致

---

## 全量审查汇总

### 批次完成状态
| 批次 | 模块 | 状态 |
|------|------|------|
| 1 | common + annotation + aspect + event | ✅ |
| 2 | config + filter + utils | ✅ |
| 3 | member + marketing | ✅ |
| 4 | inventory + order | ✅ |
| 5 | payment + finance | ✅ |
| 6 | delivery + printer | ✅ |
| 7 | dish + setmeal + shopping + address | ✅ |
| 8 | sys + store + tenant + category | ✅ |
| 9 | ai + notification + schedule + dashboard | ✅ |
| 10 | franchise + cost + dining + cashier + report + recommend + 其他 | ✅ |

### 系统级横向问题（按严重度排序）

**P0 — 多租户隔离不彻底（最系统性、最严重）**
- franchise、dining(TableArea/Queue/Reservation)、cashier、customer、sys(SystemConfig/Role)、store、category、ai_provider_config 大面积使用 getById/removeById/updateById 主键操作绕过 TenantLineInnerInterceptor
- 多处 `if (tenantId != null)` 条件过滤（fail-open），tenantId 为 null 时跳过过滤
- MybatisPlusConfig.ignoreTable 在 BaseContext.getCurrentTenantId()==null 时返回 true，与 getTenantId() 返回 -1L 的 fail-closed 兜底逻辑矛盾，租户上下文为空时隔离完全失效
- CategoryController.get/list/options 缺 @RequireEmployee，可被匿名访问

**P0 — 资金安全**
- CashierServiceImpl.cashPayment() 无幂等检查，重试重复扣款
- CashierServiceImpl.executeDailySettlement() TOCTOU 日结竞态
- CashierServiceImpl.deleteCashierRecord() 删除不回滚订单状态
- PaymentController 退款并发竞态
- WechatPayChannel HMAC-SHA256 验签错误
- FinanceServiceImpl 金额计算用 double
- ExportController 金额累加用 double

**P1 — 认证/权限安全**
- EmployeeController 密码升级在状态检查之前
- EmployeeController.forgotPassword Mock 模式可能误入生产
- BruteForceProtectionFilter.getIdentifier 用 getParameter 获取 username，员工登录用 @RequestBody JSON，getParameter 无法获取（过滤器维度失效）
- UserExperienceController 全部接口无认证注解
- RedisConfig ObjectMapper 可见性 ALL+ANY + BasicPolymorphicTypeValidator 白名单形同虚设，反序列化攻击面

**P1 — SQL 注入**
- CostServiceImpl `qw.last("LIMIT " + limit)` 字符串拼接
- RecommendServiceImpl `qw.last("LIMIT " + limit * 2)` 字符串拼接
- CouponTemplateServiceImpl `setSql` 字符串拼接

**P1 — PII 泄露**
- ExportController 订单导出包含未脱敏手机号和地址

**P1 — 安全配置**
- CsrfFilter @Order 对 @WebFilter 不生效
- LoginCheckFilter Session 取值强转无 instanceof 校验
- CsrfTokenUtil 默认字符集不一致
- RateLimitAspect 限流窗口硬编码 1s

**P2 — 性能问题（N+1 查询）**
- ReportServiceImpl 多处按天循环查询
- CostServiceImpl.getCostTrend 按天循环
- MarketingServiceImpl 按天循环
- MarketingServiceImpl.getTopActivities 全量拉取内存排序
- RecommendServiceImpl 协同过滤循环查询
- FinanceServiceImpl 利润趋势逐日查库（365次/年）
- DeliveryTrackingServiceImpl 全量扫描内存计算
- StoreServiceImpl.getTodaySummary 全量加载订单求和

**P2 — 并发/幂等**
- OrderServiceImpl.submitEatInOrder 无幂等令牌
- MemberServiceImpl.incrementPointsById TOCTOU
- CartServiceImpl.sub() 混合路径 + TOCTOU
- CouponExpirationTask 无分布式锁
- CircuitBreakerService 状态切换非原子

**P2 — 设计缺陷**
- updateById 全实体覆盖普遍（DishServiceImpl、SetmealServiceImpl、OrderServiceImpl 等多处）
- AI Prompt 注入风险
- User 注册 check-then-act 竞态
- WebSocketMessageService 纯内存存储无持久化
- Xprinter/Gprinter 适配器空壳（假成功）

**P3 — 代码质量**
- 多处 @RequestBody Map 配合强制类型转换
- 多处字段注入 @Autowired
- 多处魔法数字/硬编码字符串
- StoreController try-with-resources 多资源语法 JDK 8 不兼容
- 操作人姓名硬编码

---

---