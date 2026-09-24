# 运行时图片存储与访问统一设计

- 日期：2026-09-24
- 状态：已确认（设计评审通过，待实施计划）
- 范围：系统运行中上传/下载的图片（`uploads/`）；页面 classpath 静态图不在范围内

## 1. 背景与目标

现状问题：

1. 前端图片路径拼接零散不一致：`front` 有统一 `imgPath()`，`backend` 15+ 处裸拼 `'/common/download?name=' + x` 且行为分裂（combo 判断外链、order 防重复前缀、evaluation 手写 `encodeURIComponent`、空值处理各异），`rider` 无处理。
2. 存储目录只按业务分（`images/{bizType}/`），不区分图片来源与权限；`/common/download` 一律要求登录，导致匿名浏览菜单场景可能裂图。
3. 海量运行时图片缺少面向可维护性、可扩容的目录设计。

目标：

- 三端统一同一套图片路径函数（共享单文件，全量收敛调用点）。
- `uploads/` 按「权限(ppublic/private) / 来源(system/admin/user) / 业务(bizType) / 月份(yyyyMM)」分层，参考大厂对象存储设计。
- 路径即权限：`public/` 免登录静态直出，`private/` 按来源段细分 session 要求。
- 磁盘扩容 = 迁移根目录 + 改 `reggie.path` 配置，零刷库、零改码。
- 存量文件与数据库相对路径全量迁移（文件先拷、库后刷，幂等可回滚窗口）。
- 缺失的运行所需预置图从互联网搜索下载补入。

## 2. 范围与非目标

### 范围

- 上传接口落盘规则、下载接口鉴权分流、静态映射、鉴权白名单。
- 三端（backend / front / rider）图片 URL 生成收敛到共享 `imgPath()`。
- `uploads/` 存量迁移工具（dry-run + apply）。
- 运行时预置图（`public/system/`）补齐。
- AI 附件、二维码、测试图生成器的落盘路径对齐。

### 非目标

- classpath 页面静态图（`backend/images/logo.png`、`noImg.png`、front 图标等）不动。
- 不引入对象存储/OSS，仍为本地磁盘 + Spring 静态映射。
- 不做按业务子目录独立分盘的细粒度映射（仅整根目录可迁移，已确认）。
- 不上 Flyway（项目约定迁移脚本仅参考、手动执行）。

## 3. 存储结构（§1）

```
uploads/                                    ← 整根可迁移（reggie.path）
├── public/                                 ← 首段即权限：免登录静态直出
│   ├── system/{category}/                  ← 运行所需预置图（defaults/、brand/ 等，缺图互联网下载补入）
│   ├── admin/{bizType}/{yyyyMM}/           ← 员工上传的公开图
│   └── user/{bizType}/{yyyyMM}/            ← 顾客上传的公开图
└── private/                                ← 需登录，走 download 鉴权
    ├── admin/{bizType}/{yyyyMM}/
    └── user/{bizType}/{yyyyMM}/
```

设计要点：

| 手段 | 解决什么 |
|---|---|
| `{yyyyMM}` 日期分片 | 单目录不堆积海量文件；按月整目录迁移/备份/清理 |
| 来源/业务两级 | 按 `admin/dishes`、`user/evaluation` 精确定位问题与热点 |
| `public/`/`private/` 首段 | 权限边界物理可见，审计按目录扫描 |
| 相对路径入库 + 根目录配置 | 磁盘扩容零刷库 |
| 文件名 UUID | 无重名冲突，合并/回滚安全 |

特例不分 yyyyMM：`system/{category}/`、`private/{admin|user}/ai/{tenantId}/`。

### 3.1 bizType → 落盘映射

上传接口服务端维护映射表，按「session 角色 + bizType」自动拼路径，前端不感知：

| bizType | 可见性 | 来源段 | 落盘示例 | 说明 |
|---|---|---|---|---|
| `dish` | public | admin | `public/admin/dishes/202609/uuid.jpg` | 菜品图 |
| `setmeal`（新增） | public | admin | `public/admin/setmeal/202609/…` | 套餐图；修复 combo 页未传 bizType 回退 dishes 的缺陷 |
| `evaluation`（新增） | public | user | `public/user/evaluation/202609/…` | 评价晒单 |
| `purchase`/`stockcheck`/`stockrecord`/`supplier`/`tenant` | private | admin | `private/admin/{biz}/202609/…` | 管理端业务图 |
| `avatar` | private | 按 session | employee→`private/admin/avatar/…`，user→`private/user/avatar/…` | 双端头像 |
| `chat`（新增） | private | user | `private/user/chat/202609/…` | 客服图片 |
| `ai` | private | 按 actorType | `private/{admin\|user}/ai/{tenantId}/…` | AI 附件 |
| 不传/未知 | 同 `dish` | admin | 同 `dish` | 旧调用兼容 |

权限判定只看路径首段（及 `private/` 下第二段），不引入数据库新字段。来源段由服务端从 session 判定，不信任前端传参。

## 4. 后端改造（§2）

### 4.1 上传 `POST /common/upload`（CommonController）

- 以「visibility + 目录段」映射表替代现有 `BIZ_DIR_MAP`。
- 来源段从 session（`employee`/`user`）判定；无 session 维持现有拒绝逻辑。
- 拼路径：`{visibility}/{source}/{dir}/{yyyyMM}/{uuid}{suffix}`，`yyyyMM` 用 Java 8 `DateTimeFormatter` 取当前月。
- 返回相对路径（含 `public/` 或 `private/` 前缀）；DB/前端契约格式不变，仅多一段前缀。
- 文件类型、大小、魔数校验逻辑全部保留。

### 4.2 下载 `GET /common/download`

按 `name` 首段分流：

1. `public/**` → 不要求登录，路径穿越校验后出流（静态映射的兜底）。
2. `private/admin/**` → 要求 employee session。
3. `private/user/**` → 要求 user session。
4. 旧路径（无 `public/`/`private/` 前缀，如 `images/**`、裸文件名）→ 兼容：employee 或 user 任一登录可读，迁移完成后移除。

- 路径穿越防护（canonical path + 分隔符后缀前缀校验）保持不变。
- 缺文件仍返回 SVG 占位图。

### 4.3 静态映射（WebMvcConfig）

```java
registry.addResourceHandler("/uploads/public/**")
        .addResourceLocations("file:" + basePath + "public/");
```

- `basePath` 启动时读 `reggie.path`（缺省 `{user.dir}/uploads/`，处理 target/classes 场景，与 CommonController.init 一致）。
- 只映射 `public/`；`private/` 永不进静态映射。

### 4.4 鉴权白名单（AuthConstants / LoginCheckFilter）

- 白名单 `/uploads/**` 收窄为 `/uploads/public/**`。
- `/images/**` 保留至迁移观察期结束，之后删除。
- `/common/download` **加入白名单**（否则 Filter 先拦、handler 无法对 public 分流）；该接口的登录校验全部下沉到 handler 内按路径前段执行：`public/**` 放行、`private/**` 与旧路径按 §4.2 规则校验。

### 4.5 其他落盘点

- `AiAttachmentServiceImpl`：`private/{actorType→admin|user}/ai/{tenantId}/`（保留 tenantId 分层，**不套 yyyyMM**，与 §3 特例一致）。
- `QRCodeUtil`：`public/admin/qr/{yyyyMM}/`（分享扫码免登录）。
- `TestImageGenerator`：`public/admin/dishes/`（仅测试数据）。

## 5. 前端统一（§3）

### 5.1 共享文件

路径：`src/main/resources/shared/js/img-path.js`（classpath 根下 `/shared/js/img-path.js`，静态映射 `classpath:/` 已覆盖）。

```js
function imgPath(path) {
  if (!path) return placeholder();
  if (/^https?:\/\//.test(path)) return path;
  if (path.indexOf('/common/download') === 0) return path;
  if (path.charAt(0) === '/') return path;
  if (path.indexOf('public/') === 0) return '/uploads/' + path;
  return '/common/download?name=' + encodeURIComponent(path);
}
function placeholder() {
  var p = location.pathname;
  if (p.indexOf('/backend/') === 0) return '/backend/images/noImg.png';
  if (p.indexOf('/rider/') === 0) return '/front/images/noImg.png';
  return '/front/images/noImg.png';
}
```

行为契约（全量兼容已确认）：

1. 空值 → 按端占位图（backend→`/backend/images/noImg.png`，front/rider→`/front/images/noImg.png`）。
2. `http(s)://` 外链原样返回。
3. 已带 `/common/download` 前缀不重复拼接。
4. `/` 开头站内绝对路径原样返回（classpath 静态图等）。
5. `public/` 前缀 → `/uploads/{path}` 静态直出。
6. 其余相对路径（含 `private/` 与旧 `images/**`）→ `encodeURIComponent` 后拼 `/common/download?name=`。

前端不判断权限，只按路径前缀分流；权限在服务端。

### 5.2 三端收敛（全量替换）

| 端 | 动作 |
|---|---|
| backend | `index.html` 及各页面裸拼全部改调 `imgPath(x)`；`components.js` 上传组件内 4 处、`api/food.js`、各页 `getImage`/`imageUrl` 包装函数内部改调（包装保留）；`evaluation-list.html` 手写 encode 移除 |
| front | 删 `common.js` 本地 `imgPath` 定义，改引共享文件；保留 `installReggieVueHelpers()` mixin 注入；`order.html` 局部防重复特判删除（共享版已含） |
| rider | 统一引共享文件预置（当前无图片引用，零行为变化） |

引入位置：三端公共入口页与 `common.js` 顶部；`imgPath` 需在业务脚本执行前就绪。

### 5.3 上传回显

`el-upload`/vant 成功回调存 `response.data`（相对路径），回显调 `imgPath`；public 图回显即静态 URL，获得缓存收益。

## 6. 存量迁移（§4）

依据：各表 `image`/`images`/`avatar`/`storage_path` 等字段中的相对路径，反查文件按 bizType + 上传者角色重定位。

### 6.1 规则表

| 旧路径样式 | 来源判定 | 新路径 |
|---|---|---|
| `images/dishes/*` | 默认 admin | `public/admin/dishes/{yyyyMM}/*` |
| `images/{purchase\|stockcheck\|stockrecord\|supplier\|tenant}/*` | 目录自带 bizType | `private/admin/{biz}/{yyyyMM}/*` |
| `images/avatar/*` | employee 表→admin，user 表→user | `private/{admin\|user}/avatar/{yyyyMM}/*` |
| `images/ai/*` | ai_attachment.actorType | `private/{admin\|user}/ai/{tenantId}/*` |
| 裸文件名（历史脏数据） | 所在表推断业务 | 按同表规则归位；文件不存在则不刷库，保留原值由占位图兜底 |

- 新路径 `{yyyyMM}` 取原文件 mtime；不可得用迁移执行月。
- 同一路径被多行引用：以第一次映射为准，不重复拷贝。

### 6.2 执行方式

一次性 Java 迁移工具（`utils/` 下独立 main 或 CommandLineRunner）：

1. **dry-run（默认）**：扫描 DB + 磁盘，输出旧→新映射清单与统计，不落盘。
2. **`--apply`**：先拷贝（不删旧文件）→ 校验字节数 → 再批量 UPDATE DB。
3. 幂等：已有 `public/`/`private/` 前缀的行跳过。
4. 失败处理：单文件失败记日志跳过，结束输出失败清单；文件拷贝成功才更新对应行。
5. DB 更新按表分批提交（JDBC batch，每批 500 行）。

### 6.3 回滚窗口

- 旧文件保留不删；download 旧路径兼容分支保障双路径可读。
- 观察期（约一周）后执行清理脚本删除旧目录；随后移除旧路径兼容分支与 `/images/**` 白名单。

## 7. 预置图补齐（§5）

- 清单：`public/system/defaults/`（默认头像、默认菜品图、占位大图）、`public/system/brand/`（登录宣传图等）；只补运行时引用的预置图。
- 来源：可商用图库（Unsplash/Pexels 等直链）下载；记录来源与授权说明（版权清晰）。
- 一次性脚本执行；产物在 `uploads/` 下（不进 git，遵循项目 ignore 约定）。
- 前端引用与普通 public 图一致：存 `public/system/…` 相对路径，`imgPath` 直出。

## 8. 测试与验收（§6）

| 层 | 内容 |
|---|---|
| 后端单测 | 上传落盘路径断言（各 bizType、avatar 双角色、未知回退）；download：public 免登录 200、private/admin 缺 employee 401、private/user 缺 user 401、旧路径兼容、路径穿越拦截；`@Sql` + schema 同步约定 |
| 迁移工具 | 测试目录 + 测试库跑 dry-run / apply 幂等用例 |
| 前端验收 | 人工清单：三端公开图直出 `/uploads/public/…`、私有图走 download、空值占位图按端正确、外链/防重复分支不回归 |
| 回归 | `mvn test` 全绿（414 用例）；`mvn verify` 过 animal-sniffer |
| 兼容 | 迁移窗口期新旧路径混存时页面图片均可显示 |

## 9. 风险与对策

| 风险 | 对策 |
|---|---|
| private 泄漏 | 静态映射仅 public；白名单同步收窄为 `/uploads/public/**` |
| 迁移中断导致半迁移 | 文件先拷后刷库、幂等重跑、旧文件保留 + 旧路径兼容 |
| `components.js` 收敛点多易回归 | 改后全页面人工过上传回显 |
| 双重编码 | 统一 encode 一次、后端 decode 一次（现状后端已 decode）；旧数据无 % 序列不受影响 |
| 越权落盘 | 来源段只信 session，不信前端参数 |

## 10. 决策记录

| 决策 | 结论 |
|---|---|
| 统一范围 | 三端统一同一套函数，全量收敛 |
| 函数行为 | 全量兼容（外链/防重复/绝对路径/encode） |
| 共享机制 | 共享单文件 `/shared/js/img-path.js` |
| 目录维度 | 公私 / 来源 / 业务 / yyyyMM 分层（方案 A） |
| 公开范围 | 预置图 + 菜品/套餐 + 评价晒单；其余私有 |
| 存量 | 文件 + 库全量迁移 |
| 扩容粒度 | 整根目录可迁移（`reggie.path`） |
| classpath 静态图 | 不管 |
| 缺预置图 | 互联网搜索下载补入 |
