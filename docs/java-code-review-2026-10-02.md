# Reggie 外卖系统 — Java 代码审查报告（冗余/重复代码 + 阿里巴巴规范）

- 日期：2026-10-02
- 范围：`src/main/java` 全部 869 个 Java 文件（31+ 模块 + 顶层 common/config/dto/enums/filter/utils）+ `src/test/java` 128 个测试文件
- 方式：10 个并行审查分组全量通读 + 全仓 Grep 交叉验证（死代码、重复模式均以 grep 命中数为证据）
- 依据：《阿里巴巴 Java 开发手册》+ 项目工程约定（AGENTS.md：JDK 1.8 硬约束、PageUtils 分页封顶、租户插件、鉴权注解规则）

## 一、总览

| 分组 | 覆盖模块 | 发现数 | Critical |
|---|---|---|---|
| AI | ai (106 文件) | 23 | 0 |
| 配送组 | delivery/shopping/region/user/urgency | 28 | 5 |
| 库存团餐组 | inventory/dining | 31 | 6 |
| 会员营销组 | member/marketing/groupbuy | 37 | 4 |
| 资金组 | payment/order/withdraw/cashier/invoice | 20 | 4 |
| 平台系统组 | platform/sys/auth/tenant/customer | 33 | 7 |
| 菜品报表组 | dish/store/setmeal/category/favorite/address/recommend/report | 30 | 9 |
| 通知运营组 | notification/schedule/attendance/kds/printer/finance/cost/dashboard/export | 27 | 7 |
| 公共层+全仓 | common/config/dto/enums/filter/utils + 跨模块分析 | 24 | 3 |
| 测试 | src/test (128 文件) | 15 | 1 |
| **合计** | | **≈268** | **46** |

总体判断：分层结构、`#{}` 参数化 SQL、日志纪律（全仓未发现 `System.out`/`e.printStackTrace`/`${}` 注入）基本合格；**最大的三类系统性问题是：① 复制粘贴代码族（约 2000+ 行可收敛）与死代码；② 鉴权/租户校验/并发防重的"该做的没做全"；③ 魔法值与枚举双轨导致的状态口径分裂**。另有 4 个功能性必现缺陷（报表接口必抛 NPE、缓存永不命中等）说明部分链路从未被端到端验证过。

## 二、P0：必须立即处理（安全越权 + 资金/并发资损 + 功能已坏）

### 2.1 安全越权
1. **过滤器注册机制性失效**：`filter/CsrfFilter.java:39-41` 等 4 个过滤器均用 `@WebFilter` 注册，`@Order` 与 `@Profile("!dev")` 全部无效——CSRF/安全头/登录校验执行顺序未定义、dev 禁 CSRF 从未生效。改 `FilterRegistrationBean` 显式注册。
2. **硬编码默认密钥并静默降级**（3 处同一模式）：`module/platform/util/PlatformCredentialEncryptor.java:32-37,55-56`、`config/JasyptConfig.java:48`、`module/payment/util/PaymentCredentialEncryptor.java:33,76-80`——环境变量缺失时用开源仓库里可见的默认密钥加密生产凭据≈明文。应 fail-fast。
3. **鉴权注解缺失**：`module/platform/controller/DishPlatformMappingController.java:29-32`（顾客会话可增删改平台商品映射）、`module/dish/controller/DishFlavorController.java:49-117`（C 端可改菜品口味）。
4. **跨租户越权写**：`module/sys/controller/RoleController.java:200-216` + `RoleServiceImpl.java:71-89,114-137`——改角色权限/分配员工不校验归属，`permission`/`role_permission` 无 tenant_id 兜底，可清空他租户权限；`DeliveryTrackingServiceImpl.java:533-536`、`DiningTableController.java:383-384,417`、`TableAreaController.java:165`、`OrderDetailController.java:38-66`（IDOR）同类。
5. **令牌泄露**：`platform/adapter/impl/MeituanAdapter.java:74-75,269`（Eleme/Jd/Douyin 同）——解密后的 accessToken 明文拼 URL query 且含 token 的 url 打进 log.info。
6. **双重鉴权口径矛盾**：`auth/controller/EmployeeController.java` 6 个端点同时挂 `@RequiresAdmin` 又内联按旧 `role` 数字字段的 `isAdmin()`（:566 等 6 处），注解与内联判定标准不同，存在放行/拒绝错位。

### 2.2 资金与并发资损
7. **拼团参团无上限**：`module/groupbuy/service/impl/GroupBuyServiceImpl.java:176-199`——从不校验 maxMembers、不查重、无原子计数，并发可无限超员。
8. **营销参与人数读-改-写覆盖**：`marketing/service/impl/MarketingCampaignServiceImpl.java:244-246,591-593` + 拆箱 NPE（:226,653）。
9. **取号锁早于事务释放**：`dining/service/impl/QueueServiceImpl.java:35,72-141`——类级 @Transactional 下 finally 先释放 Redis 锁再提交，并发可生成重复排队号。
10. **退款编排双实现且已漂移**：`payment/controller/PaymentController.java:606-1034` vs `RefundServiceImpl.java:96-438`，两份 `updateOrderOnFullRefund` 状态白名单不一致；`RefundServiceImpl.java:95,133-160` 注释称"渠道调用在事务外"实际整个方法被 REQUIRES_NEW 包住并在事务内做外部 HTTP——长事务+资损定性错误。
11. **提现零校验**：`withdraw/service/impl/WithdrawalServiceImpl.java:47-57`——amount 无 null/≤0/上限校验，负额申请可在 approve 时反向加钱。
12. **收银按桌合并结账无幂等锁**：`cashier/service/impl/CashierServiceImpl.java:329-353`——并发双击可双收款；:128,945 用 JVM 内 ConcurrentHashMap 锁防重复日结，集群下失效（FinanceServiceImpl.java:57-62 同模式且锁 Map 永不清理=内存泄漏）。
13. **供应商结算无 CAS 可超额付款**：`inventory/service/impl/SupplierSettlementServiceImpl.java:82-105`。
14. **预订状态迁移无 CAS**：`dining/service/impl/ReservationServiceImpl.java:137-227`——双击"到店"重复建占位订单。
15. **7 个定时任务复制同一套 Redis 锁且降级策略矛盾**：PointsExpireTask.java:103 `return true` 无锁裸跑（多实例重复扣积分），OrderTimeoutTask.java:438 等 fail-closed——锁实现应抽 `RedisLockUtil.executeWithLock()`，降级语义统一。

### 2.3 功能性必现缺陷（说明链路未被验证）
16. `report/service/impl/ReportServiceImpl.java:796 vs :854`——getDishTrend 只 `select(Orders::getId)` 却按 `getOrderTime()` 建映射，**菜品趋势图永远全 0**；:1103 vs :1126 `toMap` value 为 null **复购率接口必抛 NPE**。
17. `store/service/impl/BusinessHoursServiceImpl.java:38`——`selectById(tenantId)` 错键查门店（应 findByTenantId），营业时间校验对多数门店查错行。
18. `recommend/service/impl/RecommendServiceImpl.java:249-257`——把菜品 ID 当 categoryId 查套餐，推荐语义错误。
19. `inventory/service/impl/ReplenishServiceImpl.java:474`——对 JSON 数组用 `readValue(..., Map.class)` 必抛 MismatchedInputException 又被宽 catch 吞掉，**补货缓存永不命中**，每次请求全量重算 + N+1（:229 死查询、:288 循环查流水）。
20. `sys/controller/SystemConfigController.java:72-113`——`@RequestBody Object` 下 `instanceof SystemConfig` 永假，该分支静默不更新任何配置；`sys/service/impl/SystemConfigServiceImpl.java:42-64,95-98`——@Cacheable 自调用失效 + `.eq(tenantId, null)` 生成永不命中的条件，每次插入重复全局配置行。
21. `dish/service/impl/DishServiceImpl.java:289-302`——`autoToggleSoldOut` 把商户"手动停售"的菜品在库存>0 时静默改回起售。
22. 测试库语义回退未同步：工作区未提交改动将测试库从 reggie_test 回退为共用 reggie 单库，与 AGENTS.md、测试 javadoc、硬编码主键策略互斥——存在误删演示数据与 PK 冲突双重风险（TestDatabaseCleaner.java:46 / application-test.yml:12）。
    → 决策（2026-10-02）：跟随单库方向，文档与注释已对账（AGENTS.md 更新 + 撞库风险清单入 §八）。

## 三、系统性重复/冗余代码族（"多余和重复的代码"主清单）

按可收敛行数排序：

1. **平台适配器双胞胎体系**（≈1500 行 → ~300 行）
   - `module/delivery/platform/{Meituan,Eleme,Douyin,Dada,Fengniao,Shunfeng}Adapter.java`：6 个类 ~750 行几乎逐字节复制（Dada↔Fengniao 仅 5 行差异），已抽 AbstractDeliveryPlatform 但子类仍复制全部方法体。
   - `module/platform/adapter/impl/{Meituan,Eleme,Jd,Douyin}Adapter.java`：4 个 ~315 行类两两 diff 仅 66 行；与配送组**同名类**分属两模块，FQN 混淆。pullOrders/callPost/toDecimal/parseItems 全套复制。
   - 建议：统一 platform 适配 SPI，子类只留 endpoint+字段映射差异；两处同名类至少改名。
2. **Redis 分布式锁 tryLock/unlock + Lua：11 份逐字拷贝**——7 个定时任务（见 §2.2-15）+ `payment/controller/PaymentController.java:1041-1086`、`PaymentOrderServiceImpl.java:239-276`、`RefundServiceImpl.java:445-490`、`RefundReconcileTask.java:133-165`。抽 `RedisLockUtil.executeWithLock(key, ttl, body)`。（注：P0 已收敛任务侧 14 个锁作用点，payment 内 4 处留 P1）
3. **租户归属校验手工化：grep "不属于当前租户" = 34 处/16 文件**；`tenantId==null→throw "租户上下文缺失"` 模板在 dining 服务层 9 份 + 控制器 12 份、delivery 两服务 8 份。且与 MybatisPlusConfig 租户插件自动注入职责重叠。建议：抽 `TenantGuard.require()/assertOwned(...)`，能下推插件的删手写 eq。
4. **updateStatus 方法族：全仓 21 个实现**（Employee/Delivery/Dish/DishEvaluation/Setmeal/Store/Tenant/User…各写一套 DTO+状态校验+落库），R<Page> 分页模板 58 处。建议：泛型 `StatusUpdatable` + 状态枚举驱动统一处理。
5. **退款/订单状态联动**：PaymentController 与 RefundServiceImpl 两套编排（§2.2-10，P0 已收敛）；`"[对账待办]"` 魔法串 5 文件 8 处、resolveClientIp 逐字 2 份。
6. **库存回退三份拷贝**：`OrderStatusFlowServiceImpl.java:883-986`、`PaymentOrderServiceImpl.java:497-571`、`OrderStockRefundServiceImpl.java:56-151`，注释自认"搬自"且签名已漂移（2 参 vs 3 参）。收敛到 OrderStockRefundService 单一入口。
7. **守卫切面 4 胞胎 + 线程池租户透传装饰器 4 份**：`common/aspect/{Admin,Employee,Rider,User}GuardAspect` 同骨架（且都 return R.error 破坏 HTTP 语义，见 §5.3）；`config/AsyncConfig.java:44-68,100-116,144-160` 三连抄 + `EventListenerThreadPoolConfig.java:42-62`，行为已漂移（异步日志断 traceId）。抽公共 `BaseContextTaskDecorator`。
8. **`import Transactional;` 成倍重复（坏合并/脚本事故，12 个文件）**：inventory 3 文件（ReplenishServiceImpl 28 次、InventoryStatsServiceImpl 25 次、PriceHistoryServiceImpl 10 次）、platform 4 文件（15/26/22/16 次）、setmeal/recommend（7/18 次）、notification/printer（12/11 次）。全部清理至 1 条；建议加 import 检查防回归。
9. **单号生成逐行同构**：`PurchaseOrderServiceImpl.java:71-116` vs `StockCheckServiceImpl.java:70-115`（前缀+likeRight max+substring+DuplicateKey 重试 5 次+MAX_VALUE 兜底）。抽 `BizNoGenerator.next(prefix)`。
10. **营销/会员 CRUD 与装配样板**：marketing 5 实体 getXxxRules/saveOrUpdate/delete 五连复制（MarketingServiceImpl.java:60-90 等）；PointsRecordController vs RechargeRecordController ~80 行结构复制 + stats() 全表载入内存（应 SQL 聚合）；CouponTemplateServiceImpl expiring/expired 65 行复制；MemberServiceImpl 注册初始化两段复制；NotificationServiceImpl 发送循环 6 处复制 + 模板服务"普通版/WithTenant 版"双轨 CRUD 被两组端点消费；ExportController 8 个导出端点同构；StoreSyncServiceImpl syncDishes/Categories/Setmeals 三胞胎；Dining QueueServiceImpl cancel/recall/reactivate/seat 四态同构；WorkSchedule→Map 转换 3 份。
11. **工具方法重复造轮子**：truncate×3、LIKE 转义×2、parseIds×4（已有 ControllerUtils 仍各自手写）、maskPhone×2（已有 LogMaskUtils）、Haversine×2、金额计算"应付=(订单-券)×折扣"×3（CashierServiceImpl）、满减计算 3 份且防御深度不一致（MarketingServiceImpl:199/310/413——**试算与核价可能给出不同金额**）、新客立减双实现（@Deprecated 版仍被 Controller 调用）、`new ObjectMapper()` 全仓仍有 9 处（Holder 目标未达成，ObjectMapper 来源共 4 套）。
12. **常量/枚举双轨**：券状态 `"unused"/"used"/"expired"` 字符串 12+ 处 vs 已有 `CouponStatus` 枚举；订单状态 `com.reggie.enums.OrderStatus` 与 `Orders.STATUS_*` 两套并用；PASSWORD_TYPE_MD5/BCRYPT 双定义（PasswordUtils vs SecurityConstants）；角色键 `"SUPER_ADMIN"`/`roleKey`/`EmployeeRole(1)` 三套口径；Session key `"employee"/"user"/"tenantId"` 字面量 20+ 处无常量；`GroupBuyCampaignMapper.countParticipants` 与 ParticipationMapper SQL 逐字重复。
13. **测试基建重复**：68 个测试类四件套样板注解；5 个控制器测试私有复制 `withCsrfToken`+toJson（基类 BaseControllerTest 已有）；Rider 系列 6 份复制粘贴 seed/cleanup 块；schema.sql 与 schema-mysql.sql 约 90 表双重定义、orders/order_detail 在 6 份 schema 中重复 CREATE（改了不生效的死定义）。

## 四、死代码清单（均经全仓 Grep 验证零调用方）

**整类/整文件**：`utils/optimization/CollectionUtils.java`、`common/aspect/RequiresTenantIsolation.java`（死注解）、`config/WebSocketConfig.java`（空类）、`enums/EmployeeRole.java`（僵尸枚举）、`dto/AcceptOrderDTO.java.tmp`（残留文件）、`src/test/resources/cleanup.sql` + `printer-agent-test-data.sql`（孤儿资源，误执行清演示数据）。

**方法/分支**（主要）：
- AI：无 actorType 旧会话 API 整条重载链 + formatDishesForPrompt/splitIntoChunks/isErrorResponse（≈300 行）；selectByConversationId、CircuitBreaker.getStatus/reset、supportsVision/getAdapterCount/getRegisteredAdapters、MSG_STATUS_FAILED、parseModelListResponse 格式4 死分支。
- 资金：`PaymentOrderServiceImpl.handlePaymentFail`（67 行）、RefundRecordServiceImpl.listUserRefunds、4 渠道的 queryOrder；P0 收敛后新增候选 `RefundService.refundOfflineByPaymentOrderId`。
- 平台/sys：EmployeeServiceImpl.login/getByUsername（两套登录漂移的旧侧）、PermissionServiceImpl.getPermissionKeys（含缓存）/getMenuTree、PermissionMapper 2 方法、RoleService.getByRoleKey/getRoleOptions/deleteRoleByCascade、TenantServiceImpl.saveVerifyCode、SystemConfigServiceImpl.setGlobalConfig、各适配器 buildSign/md5Upper×8。
- 配送/其他：ShoppingCartServiceImpl.sub（**且是修复前的有缺陷旧版**）、UserServiceImpl.register(phone)（Controller 内联了同逻辑）、UrgencyService.getUrgencyRecords/getUrgencyStats、GroupBuyService.autoCloseExpiredCampaigns（自调用别名）、MarketingCampaignService.batchDeleteCampaigns（Controller 手工重做了它的活）、GroupBuyCampaignMapper 2 方法、MaterialStockService 2 参重载、DiningTableService 2 参 pageWithArea、SupplierServiceImpl 2 方法（注释自承死代码）、WebSocketMessageService 4 方法、NotificationServiceImpl.sendAppPush、DashboardMapper.getMemberOverview、Attendance 3 个死 VO、ExportUtil.exportExcel/exportPdf、RecommendController.DEFAULT_LIMIT、StoreServiceImpl 7 个、BrowseHistoryServiceImpl.getTopCategories（名不副实）、AddressBookServiceImpl.listByCurrentUser/getByIdCurrent/setDefault(Long)、AiPromptDefaults 若干、CategoryServiceImpl 清理 `"categories"` 缓存（全仓无写入方）。
- 测试：ConcurrentLoadTest 空 @BeforeAll、假 token；AiToolExecutorTest 恒真断言用例。

合计约 **90+ 处方法级、7 个整类**。建议一次性批量删除（删后 `mvn test` 回归），并在 review 流程中把"无调用方即删"写入规范。

## 五、阿里巴巴规范问题（按规则族归并，各模块典型位置）

### 5.1 并发处理
- JVM 内锁做多实例防重：FinanceServiceImpl.java:57-62、RefundRecordServiceImpl.java:64,114-148、CashierServiceImpl.java:128（P0 已改 Redis 锁；PlatformReconcileTaskServiceImpl.java:58,77 同型残留 P1）。
- 读-改-写无 CAS：MarketingCampaignServiceImpl、ReservationServiceImpl、SupplierSettlementServiceImpl（P0 已修）；PlatformOrderPersistServiceImpl.java:91-96 幂等靠先查再插 + `nanoTime%100000` 订单号必撞；PlatformPullTask.java:53 唯一未加分布式锁的拉单任务。
- @Transactional 事务内做外部 HTTP/sleep：NotificationServiceImpl.java:116,194,984、PlatformSyncServiceImpl.java:84-112（sleep 退避最长 7s 持连接）、RefundServiceImpl（P0 已修）；GroupBuyServiceImpl.java:121-134 事务内 catch 吞退款异常→UnexpectedRollbackException 隐患。
- Thread.sleep 抢锁占调度线程：CouponExpiration/GroupBuyExpire/PointsExpire（30 池仅 3 线程，SchedulingConfig.java:40）；AiProviderManager.java:403-415 伪流式 sleep 占死业务池。

### 5.2 控制语句/OOP——拆箱 NPE 族（手册【强制】）
`Integer 与 int ==/</>= 直接比较`：DeliveryTrackingServiceImpl.java:652-786（5 处+）、DeliveryEnhancedServiceImpl.java:281-352、CashierServiceImpl.java:1094,1154、AttendanceServiceImpl/FinanceServiceImpl/CostServiceImpl/KitchenTicketServiceImpl 约 15 处、MarketingCampaignServiceImpl.java:226,653（P0 已修两处）、MarketingServiceImpl.java:220-284、CouponTemplateServiceImpl.java:74,227、PermissionServiceImpl.java:220、ReplenishServiceImpl.java:529。同文件内"一处判空一处不判"的口径分裂尤为普遍。**建议：状态一律走枚举 + Objects.equals 或判空，加静态检查拦截。**
- `new BigDecimal(double)`：DeliveryEnhancedServiceImpl.java:504；doubleValue→Math.round 走 double 算折扣均值：MemberLevelController.java:246-250；Number→doubleValue→BigDecimal.valueOf：CustomerMemberController.java:137。
- 金额转分用 int 有溢出隐患：ReportServiceImpl.java:177-184。

### 5.3 异常与日志
- 吞异常丢堆栈：AiProviderConfigServiceImpl.java:232-358 三处、CouponUserServiceImpl.java:107-114（log.warn 不带 e）、MemberRewardServiceImpl.java:157,178、OrderTimeoutTask.java:154,240,312（同文件其他 catch 都带 e，口径不一）、OrderServiceImpl.java:1171-1175（券失效按 0 折扣静默放行，多收钱无告警）。
- Controller 逐处 `catch (CustomException)→R.error(e.getMessage())` 绕开 GlobalExceptionHandler：ReservationController/DiningTableController 共 10 处；Service 返回 `R<Void>`（AttendanceServiceImpl.java:310,378）与 Map 伪协议（NotificationTemplateServiceImpl:177-304）是 Web 模型下沉业务层的分层违规。
- 鉴权失败返回 HTTP 200 + code=0：4 个 GuardAspect（common/aspect），与 GlobalExceptionHandler 声明的 401/403 契约矛盾。
- 幂等返回成功掩盖数据缺失：ReservationController.java:176-220；QueueServiceImpl 假成功（P0 已修）。

### 5.4 常量与命名（魔法值族，全仓最普遍违规）
状态/类型/阈值裸写：配送时效状态 0-5、催单 "SENT"、提现/结算 "PENDING/APPROVED"、营销 ruleType 1-5、payType/payMethod 1-6 双处定义、通知 status/channel、考勤状态、"BILL/KITCHEN/DELIVERY"、紧急度阈值字符串 "3"/"2"/"1"、AI 场景串 "business_analysis" 等 20+ 处（已有 AiPromptDefaults.SCENES 不用）、RAG 状态串、fixedDelay/超时 15000ms×6、"LIMIT 100000"×5。枚举/常量已存在却不引用的"双轨"至少 8 组（见 §3-12）。命名：AI/Ai 前缀混用、`createTime/createdTime` ↔ `created_time/create_time` 同库两套列名（Material vs StockRecord）、机翻 Javadoc（"处理 sue invoice"遍布全仓）、`ALGO_COMPARE_DAYS` 常量挪用。

### 5.5 集合与性能（N+1 族，20+ 处）
RiderTaskQueryServiceImpl.java:108-181（注释称批量实为每单 5 查≈1000 SQL）、PreferenceAnalysisServiceImpl、StoreServiceImpl:492、SetmealDishController:187（filter 内逐条 getById）、RecommendServiceImpl:331、DiningTable/Queue stats（5 次 count 应 GROUP BY；ReservationMapper.countByStatus 已有正确范式）、PointsRecord/RechargeRecord/CustomerPortal/MarketingCampaign stats（list() 全表载入内存过滤）、Finance/Cost trend（循环逐日查）、MaterialServiceImpl 手写冒泡×4、RegionServiceImpl O(n²) 树构建、InventoryStats 30 次重复 stream 过滤。返回 null 集合与"伪重写"缺 @Override：MaterialServiceImpl.java:110 等 3 处。

### 5.6 工程/分层/其余
- Controller 直连 Mapper：RiderAuthController.java:47-48、ReservationController.java:66,311、MaterialController.java:114-146（两次写库无事务）。
- 跨模块直用他人 Mapper（member→order OrderMapper、marketing→recommend BrowseHistoryMapper、coupon→MemberMapper）。
- SELECT * 注解 SQL：RoleMapper/PermissionMapper/SystemConfigMapper（手册【强制】禁止）。
- 手写 getter/setter 违逆 Lombok 约定：UpdateEmployeeStatus*、TenantRegisterDTO、RecordBrowseDTO。
- GET /test/{id} 实为写操作（AiProviderController.java:234）；BaseModelAdapter.java:31-36 静态块 System.setProperty 全局副作用；同一 Bean 两个 @PostConstruct。
- 资源泄漏：ExportUtil.java:277-328 PDF document 异常路径不 close；RateLimitAspect.java:66 本地限流 Map 无界增长。
- 配置类：CorsConfig/OpenApiConfig 硬编码 localhost 清单应外置。

## 六、正面发现（避免误报，可作内部范例）
- CouponTemplateServiceImpl.claimCoupon：原子扣减+唯一索引兜底；FlashSaleMapper.deductStock CAS SQL；DiningTableServiceImpl/MaterialMapper/PurchaseOrderDetailMapper 库存与桌台变更全带期望旧值；RefundServiceImpl 回调 CAS 幂等；登录 changeSessionId 防固定；BaseContext ThreadLocal 在 Filter finally 集中清理；TenantTestExecutionListener 全局防租户串号；无 @Disabled 测试。
- 全仓未发现：`${}` SQL 注入、`System.out`/`e.printStackTrace`、`jakarta.*`、JDK9+ 语法（项目硬约束执行良好）。

## 七、修复优先级建议
1. **P0（本周）**：§2 全部 22 项——已于 2026-10-02 当日完成，见 §八。
2. **P1**：payment 内剩余 4 处锁副本并入 RedisLockUtil、TenantGuard/单号生成/BizNo 工具抽取、拆箱 NPE 族全量替换、吞异常补堆栈、删 12 文件重复 `Transactional` import。
3. **P2**：死代码批量清除（一次 PR）、状态枚举单轨化、营销计算收敛同源、N+1 批量化。
4. **P3**：updateStatus/导出/CRUD 模板泛型化、守卫切面合并、命名与机翻注释清理、@DirtiesContext 65 处按需回收、schema 重复定义合并、9 个 @Sql 脚本裸 DELETE 治理。
5. **防回归**：引入静态扫描门禁（P3C 阿里插件 + errorprone 拆箱/事务规则），否则复制粘贴模式会继续生长。

## 八、P0 修复记录（2026-10-02 当日完成）

§2 全部 22 项已实施，全量回归 **955 用例 0 失败 0 错误（mvn clean test，BUILD SUCCESS）**，较修复前基线（38 红）转全绿。要点：

- **新增公共组件**：`common/RedisLockUtil`（SET NX + Lua 值匹配删除，全环境 fail-closed）、`common/LocalDevFallback`（dev/test/local 密钥兜底判定）、`config/FilterRegistrationConfig`（4 过滤器显式注册，顺序 SecurityHeader→TraceId→Csrf→LoginCheck，dev 真实禁用 CSRF；`@ServletComponentScan` 已移除）。
- **安全**：3 处硬编码默认密钥改为 dev 兜底+显著告警 / 非 dev fail-fast（`require-env-key` 默认翻转为 true）；platform 4 适配器日志经 `LogMaskUtils.maskUrl` 全量脱敏；DishPlatformMapping/DishFlavor 写端点补鉴权注解；RoleController 权限/员工分配、DeliveryTracking 时效更新、DiningTable 二维码/海报、TableArea、OrderDetail 补租户归属/员工校验；EmployeeController 删除 6 处与 @RequiresAdmin 矛盾的内联旧 role 判定，updateStatus 租户缺失改 fail-closed。
- **资金/并发**：拼团参团 `SELECT FOR UPDATE`+上限/防重校验；营销参与人数 setSql 原子占额（批量推送滚动计数）；取号改锁内编程式事务（dining_queue 已有 uk_queue_tenant_no 兜底）；预订/供应商结算条件 CAS（超额付款被拒）；收银按桌结账复用幂等锁+payType 归一；日结/对账/利润/退款记录 4 处 JVM 锁改 Redis 锁+落库前查重双保险；提现 DTO+服务端双校验、申请人取会话用户。
- **退款收敛**：PaymentController 退化为参数校验+转发，编排唯一化到 RefundServiceImpl（白名单取并集含 CANCELLED、restoreForOrder 并入、渠道 HTTP 全部移出事务、0 元单双表更新入单事务）；锁副本未增（payment 内 4 处留 P1 并入 RedisLockUtil）。
- **定时任务**：7 个任务 + platform 3 个任务共 14 个锁作用点改调 RedisLockUtil，PointsExpireTask fail-open 裸跑分支消除，降级统一 fail-closed。
- **功能修复**：Report 三处 select 列缺失（趋势恒 0、复购 NPE）、BusinessHours 错键查询、Recommend 菜品→分类映射、Replenish 缓存 List 反序列化+key 维度+死查询改内存分组（消 N+1）、SystemConfig rootBatchUpdate convertValue+委托与 @Cacheable 自代理+isNull 修正（回归中暴露并补 `unless="#result==null"`：缓存真实生效后 null 写入 RedisCache 抛 IAE）、DishServiceImpl 编辑路径售罄只降不升。
- **测试跟随**：SupplierSettlementTest 超额用例改为断言拒绝+正常付清；QueueControllerTest 假成功断言改 422；WithdrawalServiceTest/RoleControllerTest/OrderDetailControllerTest/SystemConfigControllerTest/PaymentControllerTest 16b 按新校验适配；测试侧新增 `MockMvcFilterExclusionAutoConfiguration` 维持"MockMvc 不过 CSRF/登录过滤器"的历史约定（过滤器自身行为由独立单测覆盖）。

**待用户动作**：
1. 手工执行迁移 SQL：`db/20261002_groupbuy_unique_participation.sql`、`db/20261002_settlement_unique_index.sql`，及 finance 两表唯一索引（reconciliation_statement/profit_analysis）——reggie 单库执行一次即可。
2. `application-prod.yml` 删除 `require-env-key: false` 并配置 `REGGIE_PAYMENT_KEY`/`REGGIE_PLATFORM_KEY`/`JASYPT_ENCRYPTOR_PASSWORD` 环境变量（yml 不入库，需本地手改）。
3. 已知残留（归 P1/P2）：README.md 6 处双库旧表述、9 个 @Sql 脚本无条件全表 DELETE（已知撞库带：cashier_record 6000-6024、user 990001-990009、shopping_cart 990108-990133）、`schema-dining.sql` 缺 uk_queue_tenant_no、`refundOfflineByPaymentOrderId` 新死代码候选、RiderOrderFlowTest 依赖执行顺序的夹具污染倾向（本轮全绿但同 JVM 乱序下有隐患）。
4. 测试跑完开发库被清：用备份恢复 `mysql -uroot -p -h localhost reggie < db/backup-reggie-preP0test.sql`。
5. 教训记录：本轮两次"红色"均为 Maven 增量编译陈旧类假象（新增/修改测试类未重编），验证一律 `mvn clean test`。
