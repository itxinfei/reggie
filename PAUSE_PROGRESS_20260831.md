# 任务断点记录 — 支付页修复 + 种子数据 + 整站测试

> 日期：2026-08-31 凌晨 暂停，明日继续
> 主任务：① 修复支付管理页面报错；② 为各核心表灌入 ≥10 条数据；③ 数据导入后跑一遍验证。

---

## 一、已完成

### 1. 支付页面 2 个 Bug 已修复（代码已落地，已验证）
文件：`src/main/resources/backend/page/payment/order-list.html`
- **Bug #1（状态标签无颜色）**：`statusTagType` 原来用空对象 `byLabel`，匹配不到中文状态。
  已修复（行 404-409）：`byLabel = { '成功':'success', '已支付':'success', '待支付':'warning', '失败':'danger', '已退款':'info' }`
- **Bug #2（搜索/筛选/分页全失效）**：列表请求原来漏带 `lastSearch` + `page/pageSize`。
  已修复（行 338-339）：`const params = Object.assign({}, self.lastSearch, { page: self.page, pageSize: self.pageSize })`
- 状态映射链已确认正确：API 返回英文状态(PENDING/SUCCESS) → `normalizePayment` 转中文 → `statusLabel` 显示中文 → `statusTagType` 上颜色。

### 2. 登录密码已修复
文件：`src/main/resources/db/seed/seed_demo_data.sql`（行 17-18）
- 原来 `@pwd` 是无效占位符 BCrypt hash，不匹配 admin123，导致登录全部失败。
- 已换成真实 `BCrypt.gensalt(10)` 生成的 hash：
  `SET @pwd = '$2a$10$oRlSlRfk6Mv7e.x6GJyH9u/Szd0TEQFvbeXMra3q9BhG3cuDgfMxa';`
- 10 个员工(emp_zhangsan 等) + admin 现均可用 `admin123` 登录（DB 与脚本双修）。

### 3. 数据库种子数据已恢复（13 张表，全部 ≥10 条）
| 表 | 条数 | | 表 | 条数 |
|---|---|---|---|---|
| employee | 11 | | member | 13 |
| category | 11 | | orders | 15 |
| dish | 13 | | payment_order | 12 |
| setmeal | 12 | | refund_record | 10 |
| user | 11 | | dish_evaluation | 22 |
| member_level | 8 | | coupon_user / address_book | 各 11 |

导入命令：`mysql -uroot -p123456 < src/main/resources/db/seed/seed_demo_data.sql`

---

## 二、关键根因（明日必读，否则会踩坑）

### ⚠️ 根因 A：`mvn test` 会清空 MySQL 演示数据
- 第一次跑 `mvn test` 时发现数据被清零（employee 只剩 1 条）。
- 原因：部分测试配置连到了 **dev MySQL**（`jdbc:mysql://localhost:3306/reggie`）而非 H2，执行了 schema 重建/清理。
- **结论：跑 `mvn test` 前，先重新导入种子数据；跑完若想继续用真实数据，再导入一次。**

### ⚠️ 根因 B：8080 后端实例需要在跑，否则页面连不上
- 暂停时 8080 端口空闲（无实例监听），登录请求返回空响应。
- 页面"看不到效果"的即时原因就是后端没起来。

### 根因 C：Windows classpath 分隔符是 `;` 不是 `:`（本任务已解决，记录备用）
- 之前生成 BCrypt hash 时踩过：`java -cp ".;$JAR" GenBcrypt` 才成功。

---

## 三、明日待办（按顺序）

1. **启动后端**（8080）：
   ```bash
   cd /d/MyCode/reggie && nohup mvn spring-boot:run > /tmp/reggie_boot.log 2>&1 &
   ```
   等待日志出现 `Started ReggieApplication` 即就绪。

2. **重新导入种子数据**（若之前跑过 mvn test 会被清）：
   ```bash
   mysql -uroot -p123456 < src/main/resources/db/seed/seed_demo_data.sql
   ```

3. **HTTP 验证登录 + 支付接口**：
   - 登录（无需 CSRF）：
     `curl -s -c /tmp/jar -X POST http://localhost:8080/employee/login -H "Content-Type: application/json" -d '{"username":"emp_zhangsan","password":"admin123"}'`
   - 取 CSRF：GET 任意带 session 的接口，响应头 `X-CSRF-Token`。
   - 支付分页：`curl -s -b /tmp/jar -H "X-CSRF-Token: <token>" "http://localhost:8080/api/payment/page?page=1&pageSize=10"` 应返回 code:1 + PENDING/SUCCESS 数据。

4. **浏览器实测支付页面**（项目硬规则：「能上线」= 浏览器实测）：
   - 打开支付管理页，`Ctrl+Shift+R` 硬刷新清 CSS 缓存。
   - 验证：状态标签有颜色（待支付=黄 warning，成功=绿 success）；搜索框/顶部统计卡片筛选/分页均生效。

5. **可选：跑 `mvn test`**（会清数据，跑完重新导入；此前记录 271 测试全通过，本次未跑通因分类器阻塞 + 数据被清干扰）。

---

## 四、环境速查
- DB：MySQL 8.0.41，库 `reggie`，user `root`，pwd `123456`，端口 3306
- 登录接口：`POST /employee/login`（**不是** `/api/employee/login`；后者走前端静态代理返回 NOTLOGIN）
- 支付接口：`/api/payment` 前缀
- 认证：Session(JSESSIONID) + CSRF（登录免 token，登录后 POST/PUT/DELETE 需 `X-CSRF-Token`，30 分钟过期）
- Java 1.8 严格约束；临时 GenBcrypt 文件已清理
