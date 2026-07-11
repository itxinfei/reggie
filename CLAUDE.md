# 瑞吉外卖 (Reggie Takeout)

Spring Boot 2.4.5 + MyBatis-Plus 3.4.2 + Redis + MySQL 的外卖管理系统。

## 快速命令

```bash
mvn compile               # 编译
mvn test -Dtest=XxxTest   # 运行单个测试
mvn test                  # 运行全部测试（注意：233个测试中204个因类加载问题预存失败）
mvn package -DskipTests   # 打包
java -jar target/reggie_take_out.jar  # 启动（需 MySQL + Redis）
```

## 模块结构

```
src/main/java/com/reggie/
├── common/          # 通用组件：R(响应封装)、GlobalExceptionHandler、BaseContext(ThreadLocal)
│                    # PasswordUtils、LogMaskUtils、RateLimitAspect、BruteForceProtectionFilter
├── config/          # 配置：WebMvcConfig、MybatisPlusConfig(多租户+分页)、RedisConfig
├── controller/      # 核心API：Employee、Category、Dish、Setmeal、Order、ShoppingCart、AddressBook
├── filter/          # LoginCheckFilter（登录拦截）
├── entity/          # 实体（对应数据库表）
├── dto/             # 数据传输对象（DishDto、SetmealDto、OrderDto）
├── mapper/          # MyBatis-Plus Mapper接口
├── service/         # 业务接口 + impl 实现
├── enums/           # 枚举（UserStatus、DishStatus、OrderStatus等）
├── utils/           # 工具（SMSUtils、ValidateCodeUtils、QRCodeUtil）
├── module/          # 扩展模块
│   ├── dining/      # 堂食：桌台、区域、预订、排队
│   ├── inventory/   # 进销存：原料、供应商、采购、盘点
│   ├── member/      # 会员：等级、积分、优惠券、充值
│   ├── payment/     # 支付：支付单、退款
│   ├── delivery/    # 外卖配送
│   ├── printer/     # 小票打印（多品牌适配）
│   └── report/      # 经营报表
└── util/            # 二维码生成、测试图片生成器

src/main/resources/
├── backend/         # 管理后台（Element UI，50+ 页面）
│   └── index.html   # 主框架，CDN 依赖：Vue 2.6、Element UI 2.x、Axios、Remix Icon 4.6
└── front/           # 移动端（Vant UI，12 个页面）
```

## 前端图标规范（2026-07-11 更新）

- **图标库**：Remix Icon 4.6（CDN：`cdn.jsdelivr.net/npm/remixicon@4.6.0/fonts/remixicon.css`）
- **许可证**：Apache 2.0，免费商用，2700+ 图标
- **使用方式**：`<i class="ri-xxx-line"></i>`，所有图标以 `ri-` 开头，建议使用 `line` 风格
- **菜单配置**：`backend/index.html` 的 `menuList` 中 `icon` 值格式为 `ri-{name}-line`
- **禁止**：不得使用旧 iconfont 样式类（`icon-category`、`icon-member` 等），旧 iconfont 文件已全部删除
- **搜索**：https://remixicon.com/ 在线搜索所需图标名称

## 代码规范（阿里巴巴 Java 开发规范）

### 命名
- 类名 UpperCamelCase，方法名 lowerCamelCase
- 常量全大写 + 下划线，Long 赋值用大写 L
- 抽象类以 `Abstract`/`Base` 开头，异常以 `Exception` 结尾
- 实现接口方法必须标注 `@Override`

### 代码结构
- 方法不超过 80 行，超过拆分为多个小方法
- switch 必须有 default 分支
- 删除注释代码和调试代码，不留 TODO（除非有明确计划）
- 集合初始化指定容量

### OOP
- 包装类比较用 `equals()`，禁止用 `==`
- `@SuppressWarnings` 必须注释理由
- 资源操作使用 try-with-resources 自动关闭

### 日志
- 使用 SLF4J + 占位符，禁止 `System.out`
- 日志禁止使用 emoji

### 异常
- catch 异常必须打日志
- 不要吞掉异常（禁止空 catch 块）

## 数据库
- 主脚本：`reggie.sql`（25 张表）
- 测试脚本：`src/test/resources/schema*.sql`
- 多租户：`tenant_id` 字段 + MybatisPlus TenantLineInnerInterceptor
- 自动填充：`MyMetaObjecthandler`（createTime/updateTime/createUser/updateUser/tenantId）

## 测试
- 框架：JUnit 5 + SpringBootTest + MockMvc + H2
- 已知问题：233 个测试中 204 个因 `NoClassDefFoundError: LEmployeeService` 预存失败（类加载问题，不影响编译）

<!-- rtk-instructions v2 -->
# RTK (Rust Token Killer) - Token-Optimized Commands

## Golden Rule

**Always prefix commands with `rtk`**. If RTK has a dedicated filter, it uses it. If not, it passes through unchanged. This means RTK is always safe to use.

**Important**: Even in command chains with `&&`, use `rtk`:
```bash
# ❌ Wrong
git add . && git commit -m "msg" && git push

# ✅ Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## RTK Commands by Workflow

### Build & Compile (80-90% savings)
```bash
rtk mvn compile          # Maven compile output
rtk mvn test             # Maven test output
```

### Test (60-99% savings)
```bash
rtk mvn test             # Maven test failures only
```

### Git (59-80% savings)
```bash
rtk git status           # Compact status
rtk git log              # Compact log (works with all git flags)
rtk git diff             # Compact diff (80%)
rtk git show             # Compact show (80%)
rtk git add              # Ultra-compact confirmations (59%)
rtk git commit           # Ultra-compact confirmations (59%)
rtk git push             # Ultra-compact confirmations
rtk git pull             # Ultra-compact confirmations
rtk git branch           # Compact branch list
rtk git fetch            # Compact fetch
rtk git stash            # Compact stash
rtk git worktree         # Compact worktree
```

Note: Git passthrough works for ALL subcommands, even those not explicitly listed.

### GitHub (26-87% savings)
```bash
rtk gh pr view <num>     # Compact PR view (87%)
rtk gh pr checks         # Compact PR checks (79%)
rtk gh run list          # Compact workflow runs (82%)
rtk gh issue list        # Compact issue list (80%)
rtk gh api               # Compact API responses (26%)
```

### Files & Search (60-75% savings)
```bash
rtk ls <path>            # Tree format, compact (65%)
rtk read <file>          # Code reading with filtering (60%)
rtk grep <pattern>       # Search grouped by file (75%)
rtk find <pattern>       # Find grouped by directory (70%)
```

### Meta Commands
```bash
rtk gain                 # View token savings statistics
rtk proxy <cmd>          # Run command without filtering (for debugging)
```

Overall average: **60-90% token reduction** on common development operations.
<!-- /rtk-instructions -->
