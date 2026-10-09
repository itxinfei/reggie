package com.reggie.module.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.module.payment.service.PaymentChannelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付渠道配置 Controller
 * <p>
 * 后台管理接口，仅员工可访问；管理动作需 payment:manage 权限（超管自动放行）。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@RestController
@RequestMapping("/admin/payment/channel")
@RequireEmployee
@Tag(name = "支付渠道配置管理")
public class PaymentChannelConfigController {

    @Autowired
    private PaymentChannelConfigService configService;

    @Autowired
    private com.reggie.module.payment.config.PaymentConfigProperties paymentConfigProperties;

    /** 分页查询渠道配置（敏感字段掩码） */
    @Operation(summary = "分页查询支付渠道配置",
            description = "按渠道/启用状态筛选分页查询；敏感字段（微信APIv3密钥、商户私钥、支付宝私钥）返回掩码。")
    @GetMapping("/list")
    @RequiresPermission("payment:manage")
    public R<IPage<PaymentChannelConfig>> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，上限 100") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "渠道筛选 WECHAT/ALIPAY") @RequestParam(required = false) String channel,
            @Parameter(description = "启用状态 1启 0停") @RequestParam(required = false) Integer enabled) {
        IPage<PaymentChannelConfig> pageReq = PageUtils.of(page, pageSize);
        return R.success(configService.pageMasked(pageReq, channel, enabled));
    }

    /** 查询配置详情（敏感字段掩码） */
    @Operation(summary = "查询支付渠道配置详情", description = "按主键查询；敏感字段掩码。")
    @GetMapping("/detail")
    @RequiresPermission("payment:manage")
    public R<PaymentChannelConfig> detail(
            @Parameter(description = "配置主键", required = true) @RequestParam Long id) {
        return R.success(configService.getMaskedById(id));
    }

    /** 新增渠道配置（同租户同渠道仅允许一条启用配置） */
    @Operation(summary = "新增支付渠道配置",
            description = "同租户同一渠道仅允许一条启用配置，重复提交提示「该支付渠道已存在配置」。")
    @PostMapping("/add")
    @RequiresPermission("payment:manage")
    public R<PaymentChannelConfig> add(
            @Parameter(description = "支付渠道配置", required = true) @RequestBody PaymentChannelConfig config) {
        if (configService.existsActiveConfig(config.getChannel(), null)) {
            return R.error("该支付渠道已存在配置");
        }
        return R.success(configService.addConfig(config));
    }

    /** 更新渠道配置（密钥留空不修改；渠道不可改） */
    @Operation(summary = "更新支付渠道配置", description = "敏感字段留空则保留原值；渠道不可跨型修改。")
    @PostMapping("/update")
    @RequiresPermission("payment:manage")
    public R<Boolean> update(
            @Parameter(description = "支付渠道配置（含ID）", required = true) @RequestBody PaymentChannelConfig config) {
        if (config.getId() == null) {
            return R.error("缺少主键 ID");
        }
        return R.success(configService.updateConfig(config));
    }

    /** 删除渠道配置（逻辑删除） */
    @Operation(summary = "删除支付渠道配置", description = "逻辑删除，历史支付单仍可追溯。")
    @PostMapping("/delete")
    @RequiresPermission("payment:manage")
    public R<Boolean> delete(
            @Parameter(description = "配置主键", required = true) @RequestParam Long id) {
        return R.success(configService.removeById(id));
    }

    /** 启用 / 停用渠道配置 */
    @Operation(summary = "启用或停用支付渠道配置", description = "停用后该渠道不再被下单/退款使用。")
    @PostMapping("/toggle")
    @RequiresPermission("payment:manage")
    public R<Boolean> toggle(
            @Parameter(description = "配置主键", required = true) @RequestParam Long id,
            @Parameter(description = "目标状态 1启 0停", required = true) @RequestParam Integer enabled) {
        return R.success(configService.setEnabled(id, enabled));
    }

    /** 渠道配置统计 */
    @Operation(summary = "支付渠道配置统计", description = "总数/启用/停用/微信数/支付宝数，供统计卡片展示。")
    @GetMapping("/stats")
    @RequiresPermission("payment:manage")
    public R<Map<String, Object>> stats() {
        long total = configService.count();
        long enabledCount = configService.count(
                new LambdaQueryWrapper<PaymentChannelConfig>().eq(PaymentChannelConfig::getEnabled, 1));
        long disabledCount = configService.count(
                new LambdaQueryWrapper<PaymentChannelConfig>().eq(PaymentChannelConfig::getEnabled, 0));
        long wechatCount = configService.count(
                new LambdaQueryWrapper<PaymentChannelConfig>().eq(PaymentChannelConfig::getChannel, "WECHAT"));
        long alipayCount = configService.count(
                new LambdaQueryWrapper<PaymentChannelConfig>().eq(PaymentChannelConfig::getChannel, "ALIPAY"));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("enabledCount", enabledCount);
        result.put("disabledCount", disabledCount);
        result.put("wechatCount", wechatCount);
        result.put("alipayCount", alipayCount);
        return R.success(result);
    }

    /**
     * 测试渠道连通性（不写库、不写缓存）。
     * <p>
     * 微信：用表单 + 库内补齐的密文构建一次 SDK 配置，{@code RSAAutoCertificateConfig}
     * 构建成功即商户私钥/APIv3 密钥/平台证书下载链路正常（APIv3 密钥在首笔真实回调时最终验证）。<br>
     * 支付宝：构建客户端后发一次随机单号的查询请求，收到业务响应（预期为"订单不存在"）即网关可达、
     * 应用私钥/支付宝公钥签名链路正常；签名类错误码判为失败。
     * </p>
     */
    @Operation(summary = "测试支付渠道连通性", description = "不写库、不写缓存；微信构建 SDK 配置（下载平台证书），支付宝发一次查询验证网关与签名。")
    @PostMapping("/test-connection")
    @RequiresPermission("payment:manage")
    public R<String> testConnection(
            @Parameter(description = "表单配置（密钥留空时按 ID 取库内密文补齐）", required = true)
            @RequestBody PaymentChannelConfig form) {
        if (form == null || form.getChannel() == null
                || form.getChannel().trim().isEmpty()) {
            return R.error("请先选择支付渠道");
        }
        String ch = form.getChannel().trim().toUpperCase();
        PaymentChannelConfig merged = mergeForConnectionTest(form);
        try {
            if ("WECHAT".equals(ch)) {
                // build 内部解密 + 下载并校验平台证书，成功即链路通
                com.reggie.module.payment.channel.sdk.WechatSdkBuilder.build(merged);
                return R.success("微信配置连通正常：商户私钥 / APIv3 密钥 / 平台证书链路通过"
                        + "（APIv3 密钥将在首笔真实回调时最终验证）");
            }
            if ("ALIPAY".equals(ch)) {
                return testAlipayConnection(merged);
            }
            return R.error("不支持的支付渠道: " + ch);
        } catch (Exception e) {
            log.warn("[支付渠道] 连通测试失败 channel={}, err={}", ch, e.getMessage());
            return R.error("连通失败: " + e.getMessage());
        }
    }

    /**
     * 支付宝发一次随机单号查询：网关可达且签名通过时返回成功；签名/APP 错误码判失败。
     */
    private R<String> testAlipayConnection(PaymentChannelConfig merged) throws Exception {
        com.alipay.api.AlipayClient client =
                com.reggie.module.payment.channel.sdk.AlipaySdkBuilder.build(merged,
                        paymentConfigProperties.getAlipayGateway());
        com.alipay.api.request.AlipayTradeQueryRequest queryRequest =
                new com.alipay.api.request.AlipayTradeQueryRequest();
        com.alipay.api.domain.AlipayTradeQueryModel model =
                new com.alipay.api.domain.AlipayTradeQueryModel();
        model.setOutTradeNo("CONN_TEST_" + System.currentTimeMillis());
        queryRequest.setBizModel(model);
        com.alipay.api.response.AlipayTradeQueryResponse response = client.execute(queryRequest);
        if (response == null) {
            return R.error("支付宝网关无响应");
        }
        String subCode = response.getSubCode();
        if (isAlipaySignError(subCode)) {
            return R.error("支付宝签名/应用配置异常：code=" + response.getCode()
                    + ", subCode=" + subCode);
        }
        // 正常应收到 ACQ.TRADE_NOT_EXIST（业务响应而非网络/签名错误），即链路通
        return R.success("支付宝连通正常：网关可达、应用私钥/支付宝公钥签名通过（查询业务码 "
                + response.getCode()
                + (subCode != null ? "/" + subCode : "") + "）");
    }

    /** 判定支付宝签名/应用类错误码（这些说明配置不正确而非单纯业务未命中）。 */
    private boolean isAlipaySignError(String subCode) {
        return "aop.invalid-signature".equals(subCode)
                || "isv.invalid-signature".equals(subCode)
                || "isv.invalid-app-id".equals(subCode);
    }

    /**
     * 将表单归一化为「密钥列恒为密文」的配置：新输入明文 → 加密；留空且有 ID → 取库内原密文；
     * 其余字段保留表单。保证 SDK Builder 走与生产一致的解密路径。不写库、不写缓存。
     */
    private PaymentChannelConfig mergeForConnectionTest(PaymentChannelConfig form) {
        PaymentChannelConfig stored = null;
        if (form.getId() != null) {
            stored = configService.getEntityById(form.getId());
        }
        form.setWxApiV3Key(normalizeSecret(form.getWxApiV3Key(),
                stored == null ? null : stored.getWxApiV3Key()));
        form.setWxMchPrivateKey(normalizeSecret(form.getWxMchPrivateKey(),
                stored == null ? null : stored.getWxMchPrivateKey()));
        form.setAliPrivateKey(normalizeSecret(form.getAliPrivateKey(),
                stored == null ? null : stored.getAliPrivateKey()));
        return form;
    }

    /**
     * 表单密钥非空（用户新输入明文）→ 加密为密文；为空（留空不修改）→ 回退库内原密文（可能为 null）。
     */
    private String normalizeSecret(String formPlain, String storedCipher) {
        if (formPlain != null && !formPlain.trim().isEmpty()) {
            return com.reggie.module.payment.util.PaymentCredentialEncryptor.encrypt(formPlain);
        }
        return storedCipher;
    }
}
