package com.reggie.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * OpenAPI（Swagger）文档配置类。
 * </p>
 *
 * <p><b>职责</b>：</p>
 * <ol>
 *     <li>定义文档全局信息：项目简介、接入方式、统一响应结构、HTTP 状态码、分页与权限规范。</li>
 *     <li>定义可复用的标准响应组件（{@code components.responses}），
 *         避免 700+ 个接口重复声明同一套错误响应。</li>
 *     <li>通过 {@link OpenApiCustomiser} 对 springdoc 自动生成的文档做统一增强：
 *         补全缺失的 summary / description、为所有接口挂载标准响应、为分组补充中文说明。</li>
 * </ol>
 *
 * <p><b>设计约定</b>：接口自身的业务语义（summary、description、参数说明）优先写在
 * Controller 的 {@code @Operation} / {@code @Parameter} 注解中；本类只做<b>兜底补全</b>与
 * <b>全局规范</b>，不覆盖已有注解内容。</p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Configuration
public class OpenApiConfig {

    /** 分组（Tag）中文说明映射表：分组名 → 说明 */
    private static final Map<String, String> TAG_DESCRIPTIONS = new HashMap<String, String>();

    /** 文档首页描述分段 */
    private static final String DESC_OVERVIEW =
            "## 项目简介\n\n"
            + "**瑞吉外卖**是一套面向连锁餐饮品牌的全栈管理系统，采用 Spring Boot 2.4.5 + MyBatis-Plus 3.4.2 单体架构，"
            + "内嵌管理后台（Element UI）与移动端 H5（Vant UI），支持私有化部署在品牌总部云服务器。\n\n"
            + "覆盖堂食、外卖、进销存、会员营销、支付、发票、打印、报表、加盟连锁、"
            + "多平台渠道（美团 / 饿了么 / 抖音）等全业务场景。\n\n"
            + "| 指标 | 数量 |\n|------|------|\n"
            + "| 业务模块 | 39 个 |\n"
            + "| REST 接口 | 约 730 个 |\n"
            + "| 数据表 | 115 张 |\n";

    private static final String DESC_ACCESS =
            "## 接入说明\n\n"
            + "| 项目 | 说明 |\n|------|------|\n"
            + "| 认证方式 | **Session + Cookie**。调用登录接口后服务端下发 `JSESSIONID`，后续请求自动携带 |\n"
            + "| Swagger 调试 | 点击页面右上角 **Authorize**，填入 `JSESSIONID=xxx` 后即可调试需登录接口 |\n"
            + "| 租户隔离 | 后台接口按会话中的 `tenant_id` 自动做行级隔离，无需也不允许前端传租户参数 |\n"
            + "| 限流 | 部分接口启用 Redis 滑动窗口限流，超限返回 `429` |\n"
            + "| 请求格式 | 除文件上传外统一 `application/json` |\n";

    private static final String DESC_RESPONSE =
            "## 统一响应结构\n\n"
            + "所有接口返回同一包装体 `R<T>`：\n\n"
            + "```json\n{\n"
            + "  \"code\": 1,\n"
            + "  \"msg\": null,\n"
            + "  \"data\": {},\n"
            + "  \"map\": {},\n"
            + "  \"timestamp\": 1756723200000,\n"
            + "  \"requestId\": \"a1b2c3d4e5f6\"\n"
            + "}\n```\n\n"
            + "| 字段 | 类型 | 说明 |\n|------|------|------|\n"
            + "| code | int | **1** 成功，**0** 失败 |\n"
            + "| msg | string | 错误信息，成功时通常为 `null` |\n"
            + "| data | T | 业务数据 |\n"
            + "| map | object | 动态附加数据 |\n"
            + "| timestamp | long | 服务端时间戳（毫秒） |\n"
            + "| requestId | string | 链路追踪 ID，与日志 traceId 一致，报障请提供 |\n";

    private static final String DESC_STATUS_CODE =
            "## HTTP 状态码\n\n"
            + "| 状态码 | 含义 | 触发场景 |\n|--------|------|----------|\n"
            + "| 200 | 成功 | 请求正常处理 |\n"
            + "| 400 | 参数错误 | 参数校验失败、缺失必填参数、请求体格式错误 |\n"
            + "| 401 | 未登录 | 会话缺失或已失效 |\n"
            + "| 403 | 无权限 | 已登录但缺少所需角色或按钮权限 |\n"
            + "| 404 | 资源不存在 | 路径错误或数据不存在 |\n"
            + "| 409 | 数据冲突 | 唯一约束冲突（如账号、编码重复） |\n"
            + "| 415 | 媒体类型不支持 | 请使用 `application/json` |\n"
            + "| 422 | 业务校验失败 | 业务规则不允许（如库存不足、状态流转非法） |\n"
            + "| 429 | 请求过于频繁 | 触发接口限流 |\n"
            + "| 500 | 系统异常 | 未预期的服务端错误 |\n\n"
            + "> 约定：HTTP 状态码表示<b>失败类别</b>，响应体 `code` 字段表示<b>业务成败</b>。"
            + "客户端应先判断 HTTP 状态码，再判断 `code`。\n";

    private static final String DESC_CONVENTION =
            "## 分页规范\n\n"
            + "列表接口统一使用 `page`（默认 1）+ `pageSize`（默认 10）参数，**`pageSize` 上限 100**（超出自动截断）。"
            + "分页结果返回 MyBatis-Plus `IPage` 结构：`records`（当前页数据）、`total`（总数）、"
            + "`size`（每页条数）、`current`（当前页）、`pages`（总页数）。\n\n"
            + "## 权限注解\n\n"
            + "| 注解 | 说明 |\n|------|------|\n"
            + "| `@RequireEmployee` | 需员工登录会话（后台接口） |\n"
            + "| `@RequiresPermission(\"key\")` | 需指定按钮权限，超管（SUPER_ADMIN）自动放行 |\n"
            + "| `@RequiresAdmin` | 仅超管可访问 |\n\n"
            + "## 通用约定\n\n"
            + "- **时间格式**：统一 `yyyy-MM-dd HH:mm:ss`\n"
            + "- **金额**：统一 `BigDecimal`，单位「元」，保留 2 位小数\n"
            + "- **删除**：业务数据一律**逻辑删除**（`is_deleted` 标记），不做物理删除\n"
            + "- **枚举**：状态类字段取值见「元数据」分组的枚举字典接口，避免两端硬编码\n"
            + "- **幂等**：下单、支付回调、平台拉单等接口均做幂等处理，重复提交不会产生脏数据\n"
            + "- **打印**：门店 PC 运行打印代理，服务器只负责任务入队，不直连打印机\n";

    static {
        // ===== 核心业务 =====
        TAG_DESCRIPTIONS.put("员工管理", "员工 CRUD、登录登出、密码修改、启用禁用与角色分配");
        TAG_DESCRIPTIONS.put("用户管理", "C 端用户管理：手机号登录、用户信息、状态管理");
        TAG_DESCRIPTIONS.put("分类管理", "菜品分类与套餐分类的增删改查、排序与启停");
        TAG_DESCRIPTIONS.put("菜品管理", "菜品 CRUD、口味管理、图片上传、起售停售、批量操作");
        TAG_DESCRIPTIONS.put("菜品口味管理", "菜品口味（辣度、规格等）独立 CRUD 接口");
        TAG_DESCRIPTIONS.put("菜品规格管理", "菜品规格组与规格选项管理，支持多选加价");
        TAG_DESCRIPTIONS.put("套餐管理", "套餐 CRUD、套餐内菜品组合、起售停售");
        TAG_DESCRIPTIONS.put("套餐菜品关联管理", "套餐与菜品关联关系的独立 CRUD 接口");
        TAG_DESCRIPTIONS.put("购物车管理", "购物车增删、数量修改、清空与列表查询");
        TAG_DESCRIPTIONS.put("地址簿管理", "C 端收货地址簿 CRUD、默认地址设置");
        TAG_DESCRIPTIONS.put("订单管理", "订单提交、分页查询、状态流转（待付款→待接单→配送中→已完成/已取消/已退款）、再来一单");
        TAG_DESCRIPTIONS.put("订单明细", "订单明细（菜品行项目）查询接口");
        TAG_DESCRIPTIONS.put("菜品评价", "菜品评价管理：评价列表、回复、评分统计");

        // ===== 支付 / 财务 / 发票 =====
        TAG_DESCRIPTIONS.put("聚合支付", "统一支付下单、支付查询、退款、支付回调等接口");
        TAG_DESCRIPTIONS.put("收银管理", "收银记录登记、日结对账、收银流水查询");
        TAG_DESCRIPTIONS.put("财务管理", "收支流水、利润分析、对账单生成与确认");
        TAG_DESCRIPTIONS.put("提现管理", "提现申请、审核、打款与驳回");
        TAG_DESCRIPTIONS.put("发票管理", "发票抬头管理、开票申请、开具与作废（状态机：待申请→已申请→已开具/已作废）");
        TAG_DESCRIPTIONS.put("成本核算管理", "菜品成本、人工成本、其他成本的录入核算与成本分析");

        // ===== 进销存 =====
        TAG_DESCRIPTIONS.put("食材管理", "原材料（食材）CRUD、库存查询、预警阈值设置");
        TAG_DESCRIPTIONS.put("食材分类管理", "食材分类的增删改查");
        TAG_DESCRIPTIONS.put("供应商管理", "供应商档案 CRUD 与累计采购额汇总");
        TAG_DESCRIPTIONS.put("采购单管理", "采购单创建、审核、收货入库、取消与查询");
        TAG_DESCRIPTIONS.put("采购单明细", "采购单明细行项目查询");
        TAG_DESCRIPTIONS.put("盘点管理", "库存盘点单创建、录入实盘数量、差异处理");
        TAG_DESCRIPTIONS.put("出入库记录", "库存流水记录查询（采购入库、盘点、手动调整等）");
        TAG_DESCRIPTIONS.put("智能补货", "基于库存与销量生成智能补货建议与看板");
        TAG_DESCRIPTIONS.put("进销存统计", "进销存模块数据统计：库存总额、预警数、月度趋势");
        TAG_DESCRIPTIONS.put("供应商结算管理", "供应商对账结算单生成、确认与付款");
        TAG_DESCRIPTIONS.put("菜品食材关联", "菜品食材关联（BOM 配方）管理，支撑成本与库存扣减");

        // ===== 会员 / 营销 =====
        TAG_DESCRIPTIONS.put("会员管理", "会员档案 CRUD、等级调整、积分调整、余额充值");
        TAG_DESCRIPTIONS.put("会员等级管理", "会员等级规则配置：成长值、折扣、升级条件");
        TAG_DESCRIPTIONS.put("会员标签管理", "会员标签 CRUD 与会员打标，支撑精准营销");
        TAG_DESCRIPTIONS.put("积分记录", "积分变动流水查询（获取、消费、调整）");
        TAG_DESCRIPTIONS.put("充值记录", "会员余额充值流水查询");
        TAG_DESCRIPTIONS.put("优惠券模板", "优惠券模板 CRUD：满减券、折扣券、发放规则");
        TAG_DESCRIPTIONS.put("用户优惠券", "优惠券发放、核销、状态查询（管理端视角）");
        TAG_DESCRIPTIONS.put("C端优惠券", "C 端用户在会员中心与下单页可用的优惠券接口");
        TAG_DESCRIPTIONS.put("营销活动管理", "秒杀、满减、折扣等营销活动 CRUD、启停与最优优惠计算");
        TAG_DESCRIPTIONS.put("营销工具管理", "营销工具配置：新客优惠、秒杀、买赠活动的设置与试算");
        TAG_DESCRIPTIONS.put("拼团活动管理", "拼团活动 CRUD、成团判定与参团记录");
        TAG_DESCRIPTIONS.put("会员留存自动化", "会员流失预警扫描、分级召回、发券召回与积分排行");
        TAG_DESCRIPTIONS.put("智能推荐", "菜品推荐、用户偏好分析、浏览记录与推荐位管理");

        // ===== 堂食 / 配送 =====
        TAG_DESCRIPTIONS.put("堂食区域管理", "堂食区域（大厅、包间等）CRUD");
        TAG_DESCRIPTIONS.put("堂食桌台管理", "桌台 CRUD、开台、并台、转台、清台与状态看板");
        TAG_DESCRIPTIONS.put("预订管理", "堂食预订：创建、到店、取消与预订查询");
        TAG_DESCRIPTIONS.put("排队管理", "取号排队、叫号、过号处理与队列查询");
        TAG_DESCRIPTIONS.put("外卖平台对接", "外卖平台配送订单对接与配送状态同步");
        TAG_DESCRIPTIONS.put("配送增强管理", "配送范围围栏与阶梯配送费规则配置");
        TAG_DESCRIPTIONS.put("配送跟踪管理", "配送订单跟踪、骑手信息管理、配送时效记录");
        TAG_DESCRIPTIONS.put("行政区划管理", "省市区行政区划数据查询与维护");
        TAG_DESCRIPTIONS.put("催单管理", "订单催单、未接单实时预警扫描与分级告警");

        // ===== 平台外卖 =====
        TAG_DESCRIPTIONS.put("平台配置管理", "外卖平台（美团/饿了么/抖音）接入配置：新增、编辑、启停、凭据脱敏");
        TAG_DESCRIPTIONS.put("商品平台映射管理", "本店菜品/套餐与平台商品的映射关系配置");
        TAG_DESCRIPTIONS.put("平台同步触发", "手动触发商品、库存、营业状态同步到外卖平台");
        TAG_DESCRIPTIONS.put("平台同步日志", "平台同步操作日志查询（成功/失败明细）");
        TAG_DESCRIPTIONS.put("平台订单拉取", "从外卖平台手动拉单并幂等落库");
        TAG_DESCRIPTIONS.put("平台对账管理", "平台账单对账：生成对账单、差异核对与确认");

        // ===== 门店 / 加盟 / 租户 =====
        TAG_DESCRIPTIONS.put("门店管理", "门店 CRUD、数据同步、商品下发、导出与门店配置");
        TAG_DESCRIPTIONS.put("总部控制台", "跨门店经营数据汇总与门店排行");
        TAG_DESCRIPTIONS.put("加盟管理-加盟商", "加盟商档案 CRUD");
        TAG_DESCRIPTIONS.put("加盟管理-加盟合同", "加盟合同 CRUD（含抽成规则）");
        TAG_DESCRIPTIONS.put("加盟管理-分账结算", "加盟分账结算单：生成、确认、结算与查询");
        TAG_DESCRIPTIONS.put("租户管理", "租户注册与基础信息");

        // ===== 系统 / 权限 =====
        TAG_DESCRIPTIONS.put("系统管理-角色管理", "角色 CRUD、权限分配（菜单/按钮）与角色用户");
        TAG_DESCRIPTIONS.put("系统管理-系统配置", "系统参数配置 CRUD（如预警阈值、开关项）");
        TAG_DESCRIPTIONS.put("系统管理-通知模板", "通知模板 CRUD（短信/推送/站内信）");
        TAG_DESCRIPTIONS.put("系统管理-操作日志", "操作日志查询：操作人、模块、时间与详情");
        TAG_DESCRIPTIONS.put("考勤管理", "员工考勤打卡与考勤统计");
        TAG_DESCRIPTIONS.put("排班管理", "员工排班表管理与查询");
        TAG_DESCRIPTIONS.put("数据导出", "订单、菜品、员工、报表等业务数据的 Excel / PDF 导出");

        // ===== 打印 =====
        TAG_DESCRIPTIONS.put("打印机配置", "打印机配置 CRUD：类型、模板、关联门店");
        TAG_DESCRIPTIONS.put("打印机日志", "打印日志查询：打印时间、内容、结果");
        TAG_DESCRIPTIONS.put("订单打印（入队门店PC终端）", "订单打印触发：按订单生成打印任务并入队门店终端");
        TAG_DESCRIPTIONS.put("打印终端管理（门店PC打印代理）", "门店打印终端注册、启停、在线状态与测试打印");
        TAG_DESCRIPTIONS.put("打印任务查询（门店PC打印代理执行流水）", "打印任务执行流水查询、重打与重试");
        TAG_DESCRIPTIONS.put("打印代理（门店PC本地打印）", "门店 PC 打印代理接口：注册、心跳拉取任务、回执回调（匿名 + 终端 Token）");

        // ===== 数据 / AI / 其他 =====
        TAG_DESCRIPTIONS.put("数据概览仪表盘", "后台首页实时统计：营业额、订单数、客单价、趋势");
        TAG_DESCRIPTIONS.put("经营报表", "营业额统计、菜品销量排行、时段分布等经营报表");
        TAG_DESCRIPTIONS.put("增强报表管理", "多维经营分析报表：同比环比、门店对比、客户画像");
        TAG_DESCRIPTIONS.put("AI智能助手", "AI 智能点餐推荐、菜品描述生成、经营数据分析、流式对话");
        TAG_DESCRIPTIONS.put("AI供应商管理", "大模型供应商配置与切换（密钥存库，支持热切换）");
        TAG_DESCRIPTIONS.put("消息通知", "通知模板管理、消息发送、发送记录查询");
        TAG_DESCRIPTIONS.put("用户体验管理", "订单、后厨、系统等场景化的用户体验通知推送");
        TAG_DESCRIPTIONS.put("客服管理", "客服会话管理、工单处理与投诉跟踪");
        TAG_DESCRIPTIONS.put("公共接口", "文件上传下载等通用公共接口");
        TAG_DESCRIPTIONS.put("元数据", "枚举字典（订单状态、支付方式等），供后台与 C 端共用，避免两端硬编码");
        TAG_DESCRIPTIONS.put("商家信息", "商家基本信息、营业状态与配送参数查询");
    }

    /**
     * 配置 OpenAPI 文档全局信息
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI reggieOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("瑞吉外卖 API 文档")
                        .description(buildDescription())
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("itxinfei")
                                .email("747011882@qq.com")
                                .url("https://gitee.com/itxinfei"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地开发环境"),
                        new Server()
                                .url("https://api.example.com")
                                .description("生产环境（请替换为实际域名）")))
                .addSecurityItem(new SecurityRequirement().addList("Session"))
                .components(new Components()
                        .addSecuritySchemes("Session",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("JSESSIONID")
                                        .description("Session 认证：调用登录接口后由服务端下发 JSESSIONID Cookie，"
                                                + "调试时在右上角 Authorize 中填入 JSESSIONID=xxx")));
    }

    /**
     * 文档统一增强：补全接口描述、挂载标准响应、补充分组说明
     * <p>
     * 覆盖 springdoc 自动生成的全部 700+ 个接口，保证文档风格一致、响应声明完整。
     * 已在 Controller 中通过 {@code @Operation} / {@code @ApiResponses} 声明的内容不会被覆盖。
     * </p>
     *
     * @return OpenApiCustomiser 定制器
     */
    @Bean
    public OpenApiCustomiser reggieOpenApiCustomiser() {
        return new OpenApiCustomiser() {
            @Override
            public void customise(OpenAPI openApi) {
                addStandardResponses(openApi);
                enhanceOperations(openApi);
                enhanceTags(openApi);
            }
        };
    }

    /**
     * 拼接文档首页描述
     *
     * @return Markdown 描述文本
     */
    private String buildDescription() {
        return DESC_OVERVIEW + "\n" + DESC_ACCESS + "\n" + DESC_RESPONSE + "\n"
                + DESC_STATUS_CODE + "\n" + DESC_CONVENTION;
    }

    /**
     * 注册可复用的标准响应组件（{@code components.responses}）
     *
     * @param openApi OpenAPI 对象
     */
    private void addStandardResponses(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        components.addResponses("BadRequest", buildResponse(
                "参数错误：参数校验失败、缺失必填参数或请求体格式错误", 0, "参数校验失败：xxx"));
        components.addResponses("Unauthorized", buildResponse(
                "未登录：会话缺失或已失效，请重新登录", 0, "未登录"));
        components.addResponses("Forbidden", buildResponse(
                "无权限：已登录但缺少所需角色或按钮权限", 0, "无权限访问"));
        components.addResponses("NotFound", buildResponse(
                "资源不存在：路径错误或数据不存在", 0, "数据不存在"));
        components.addResponses("Conflict", buildResponse(
                "数据冲突：违反唯一约束（如账号、编码重复）", 0, "数据已存在，请勿重复提交"));
        components.addResponses("UnsupportedMediaType", buildResponse(
                "媒体类型不支持：请使用 application/json", 0, "不支持的请求类型，请使用 application/json"));
        components.addResponses("BusinessError", buildResponse(
                "业务校验失败：业务规则不允许（如库存不足、状态流转非法）", 0, "具体业务错误原因"));
        components.addResponses("TooManyRequests", buildResponse(
                "请求过于频繁：已触发接口限流，请稍后重试", 0, "操作过于频繁，请稍后再试"));
        components.addResponses("ServerError", buildResponse(
                "系统异常：未预期的服务端错误", 0, "系统繁忙，请稍后重试"));
    }

    /**
     * 遍历所有接口，补全 summary / description 与标准响应声明
     *
     * @param openApi OpenAPI 对象
     */
    private void enhanceOperations(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        for (Map.Entry<String, PathItem> entry : openApi.getPaths().entrySet()) {
            String path = entry.getKey();
            List<Operation> operations = entry.getValue().readOperations();
            for (Operation operation : operations) {
                enhanceOperation(operation, path);
            }
        }
    }

    /**
     * 增强单个接口：补全摘要、描述与响应声明
     *
     * @param operation 接口操作对象
     * @param path      接口路径
     */
    private void enhanceOperation(Operation operation, String path) {
        String tag = resolveTag(operation);

        // 1. 补全摘要
        if (isBlank(operation.getSummary())) {
            operation.setSummary(tag + "接口（待补充摘要）");
        }

        // 2. 补全描述
        if (isBlank(operation.getDescription())) {
            operation.setDescription(buildOperationDescription(operation, tag));
        }

        // 3. 挂载标准响应
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        // 成功响应：保留 springdoc 生成的结构，仅补全描述
        ApiResponse ok = responses.get("200");
        if (ok == null) {
            ok = responses.get("201");
        }
        if (ok != null && isBlank(ok.getDescription())) {
            ok.setDescription("操作成功（code=1，data 为业务数据）");
        }
        if (ok == null) {
            responses.addApiResponse("200", new ApiResponse()
                    .description("操作成功（code=1，data 为业务数据）"));
        }

        addErrorResponses(responses, path);
    }

    /**
     * 按路径特征挂载对应的错误响应
     *
     * @param responses 响应集合
     * @param path      接口路径
     */
    private void addErrorResponses(ApiResponses responses, String path) {
        responses.addApiResponse("400", refResponse("BadRequest"));
        responses.addApiResponse("422", refResponse("BusinessError"));
        responses.addApiResponse("429", refResponse("TooManyRequests"));
        responses.addApiResponse("500", refResponse("ServerError"));

        // 门店打印代理接口为匿名访问，无登录态概念
        if (path.startsWith("/printer/agent/")) {
            return;
        }
        responses.addApiResponse("409", refResponse("Conflict"));
        responses.addApiResponse("415", refResponse("UnsupportedMediaType"));
        responses.addApiResponse("404", refResponse("NotFound"));

        // 后台管理接口需要员工登录与按钮权限
        if (path.startsWith("/admin/")) {
            responses.addApiResponse("401", refResponse("Unauthorized"));
            responses.addApiResponse("403", refResponse("Forbidden"));
            return;
        }
        // C 端与会员接口需要用户登录
        if (path.startsWith("/user/") || path.startsWith("/api/")) {
            responses.addApiResponse("401", refResponse("Unauthorized"));
            return;
        }
        // 其余公开接口（登录、元数据、商家信息、公开查询等）
        responses.addApiResponse("401", refResponse("Unauthorized"));
    }

    /**
     * 生成接口描述（仅用于缺失描述的接口兜底）
     *
     * @param operation 接口操作对象
     * @param tag       所属分组
     * @return 规范化描述文本
     */
    private String buildOperationDescription(Operation operation, String tag) {
        return "**所属模块**：" + tag + "\n\n"
                + "**功能**：" + operation.getSummary() + "\n\n"
                + "**响应**：统一返回 `R<T>` 包装体，`code=1` 表示成功、`data` 为业务数据；"
                + "失败时 `code=0`、`msg` 为错误原因，并按场景返回对应 HTTP 状态码"
                + "（详见文档首页「HTTP 状态码」）。";
    }

    /**
     * 补全分组（Tag）说明并按出现顺序重排，保证文档侧边栏可读
     *
     * @param openApi OpenAPI 对象
     */
    private void enhanceTags(OpenAPI openApi) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        if (openApi.getPaths() != null) {
            for (PathItem item : openApi.getPaths().values()) {
                for (Operation operation : item.readOperations()) {
                    List<String> tags = operation.getTags();
                    if (tags == null || tags.isEmpty()) {
                        continue;
                    }
                    String name = tags.get(0);
                    Integer count = counts.get(name);
                    counts.put(name, count == null ? 1 : count + 1);
                }
            }
        }

        List<Tag> tagList = new ArrayList<Tag>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String name = entry.getKey();
            String description = TAG_DESCRIPTIONS.get(name);
            if (description == null) {
                description = name + "相关接口";
            }
            tagList.add(new Tag()
                    .name(name)
                    .description(description + "（共 " + entry.getValue() + " 个接口）"));
        }
        openApi.setTags(tagList);
    }

    /**
     * 构造引用标准响应组件的响应对象
     *
     * @param name 组件名
     * @return 引用型 ApiResponse
     */
    private ApiResponse refResponse(String name) {
        ApiResponse response = new ApiResponse();
        response.set$ref("#/components/responses/" + name);
        return response;
    }

    /**
     * 构造带 R&lt;T&gt; 结构说明的响应对象
     *
     * @param description 响应说明
     * @param code        示例 code 值
     * @param msg         示例 msg 值
     * @return ApiResponse
     */
    @SuppressWarnings("rawtypes")
    private ApiResponse buildResponse(String description, Object code, String msg) {
        Map<String, Schema> properties = new LinkedHashMap<String, Schema>();
        properties.put("code", new Schema<Object>()
                .type("integer").description("1=成功，0=失败").example(code));
        properties.put("msg", new Schema<Object>()
                .type("string").description("错误信息").example(msg));
        properties.put("data", new Schema<Object>()
                .description("业务数据，失败时为 null"));
        properties.put("map", new Schema<Object>()
                .type("object").description("动态附加数据"));
        properties.put("timestamp", new Schema<Object>()
                .type("integer").format("int64").description("服务端时间戳（毫秒）"));
        properties.put("requestId", new Schema<Object>()
                .type("string").description("链路追踪 ID，报障请提供"));

        Schema<Object> schema = new Schema<Object>();
        schema.setType("object");
        schema.setProperties(properties);

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(schema)));
    }

    /**
     * 获取接口所属分组名
     *
     * @param operation 接口操作对象
     * @return 分组名，无分组时返回默认值
     */
    private String resolveTag(Operation operation) {
        List<String> tags = operation.getTags();
        if (tags == null || tags.isEmpty()) {
            return "未分组";
        }
        return tags.get(0);
    }

    /**
     * 判断字符串是否为空
     *
     * @param value 待判断字符串
     * @return 为 null 或空白时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
