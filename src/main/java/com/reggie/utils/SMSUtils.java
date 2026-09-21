package com.reggie.utils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.CustomException;
import com.reggie.common.ObjectMapperHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 短信发送工具类，封装阿里云短信服务调用逻辑。
 * 凭证从application.yml注入，不硬编码。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
public final class SMSUtils {

    private SMSUtils() {
        throw new AssertionError();
    }

    /**
     * 阿里云AccessKey ID（从配置注入，使用volatile保证线程可见性）
     */
    private static volatile String accessKeyId;

    /**
     * 阿里云AccessKey Secret（从配置注入，使用volatile保证线程可见性）
     */
    private static volatile String accessKeySecret;

    /**
     * 阿里云短信客户端（线程安全，复用实例避免每次创建）
     */
    private static volatile IAcsClient acsClient;

    /**
     * JSON序列化工具（线程安全，复用实例）
     */
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    /**
     * 初始化短信凭证（由SmsConfig或启动时调用）
     *
     * @param keyId     阿里云AccessKey ID
     * @param keySecret 阿里云AccessKey Secret
     */
    public static void init(String keyId, String keySecret) {
        accessKeyId = keyId;
        accessKeySecret = keySecret;
        // 初始化或更新客户端实例
        if (keyId != null && !keyId.isEmpty() && keySecret != null && !keySecret.isEmpty()) {
            DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", keyId, keySecret);
            acsClient = new DefaultAcsClient(profile);
        } else {
            acsClient = null;
        }
        log.info("SMS凭证已初始化, accessKeyId={}", maskKey(keyId));
    }

	/**
	 * 发送短信
	 *
	 * @param signName     短信签名
	 * @param templateCode 短信模板编号
	 * @param phoneNumbers 手机号
	 * @param param         短信参数
	 */
	public static void sendMessage(String signName, String templateCode,String phoneNumbers,String param){
	    // 真实发送就绪条件：阿里云凭证(accessKeyId/secret) 与短信模板(templateCode) 均配置，且客户端已初始化。
	    // 任一缺失即进入"控制台模式"。
	    boolean smsReady = acsClient != null
	            && accessKeyId != null && !accessKeyId.isEmpty()
	            && accessKeySecret != null && !accessKeySecret.isEmpty()
	            && templateCode != null && !templateCode.isEmpty();
	    if (!smsReady) {
	        // 控制台模式（非 Mock）：不调用任何短信接口，验证码直接打印到服务端控制台，业务流程正常继续。
	        // 开发/测试可从日志读取验证码；生产环境配置 reggie.sms 凭证与模板后即自动切换为真实发送。
	        log.warn("[短信-控制台模式] 短信凭证或模板未配置，验证码不实际发送。phone={}, sign={}, template={}, 验证码={}",
	                LogMaskUtils.maskPhone(phoneNumbers), signName, templateCode, param);
	        return;
	    }

		SendSmsRequest request = new SendSmsRequest();
		request.setSysRegionId("cn-hangzhou");
		request.setPhoneNumbers(phoneNumbers);
		request.setSignName(signName);
		request.setTemplateCode(templateCode);
		// 使用 Jackson 序列化模板参数，避免字符串拼接导致的 JSON 注入风险
		Map<String, String> templateParams = new HashMap<>();
		templateParams.put("code", param);
		try {
			request.setTemplateParam(OBJECT_MAPPER.writeValueAsString(templateParams));
		} catch (JsonProcessingException e) {
			log.error("短信模板参数JSON序列化失败，phone={}, param={}", LogMaskUtils.maskPhone(phoneNumbers), param, e);
			throw new CustomException("短信参数处理失败");
		}
		try {
			SendSmsResponse response = acsClient.getAcsResponse(request);
			if ("OK".equals(response.getCode())) {
				// 真实发送同样在控制台打印验证码，便于联调与核验
				log.info("[短信-真实发送] 阿里云短信已发送，phone={}, bizId={}, 验证码={}",
						LogMaskUtils.maskPhone(phoneNumbers), response.getBizId(), param);
			} else {
				log.error("短信发送失败，phone={}, code={}, message={}",
					LogMaskUtils.maskPhone(phoneNumbers), response.getCode(), response.getMessage());
				throw new CustomException("短信发送失败，请稍后重试");
			}
		}catch (ClientException e) {
			log.error("短信发送异常，phone={}", LogMaskUtils.maskPhone(phoneNumbers), e);
			throw new CustomException("短信服务异常，请稍后重试");
		}
	}

    /**
     * 脱敏AccessKey用于日志输出，只显示前4位和后4位
     *
     * @param key 原始密钥字符串
     * @return 脱敏后的密钥字符串
     */
	private static String maskKey(String key) {
	    if (key == null || key.length() <= 8) return "***";
	    return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
	}

    /**
     * 获取阿里云短信客户端实例（线程安全，可复用）
     * 供其他模块调用，避免重复创建客户端
     *
     * @return 客户端实例，未初始化时返回null
     */
    public static IAcsClient getClient() {
        return acsClient;
    }

}


