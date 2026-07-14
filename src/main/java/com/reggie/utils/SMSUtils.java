package com.reggie.utils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;

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
     * 初始化短信凭证（由SmsConfig或启动时调用）
     *
     * @param keyId     阿里云AccessKey ID
     * @param keySecret 阿里云AccessKey Secret
     */
    public static void init(String keyId, String keySecret) {
        accessKeyId = keyId;
        accessKeySecret = keySecret;
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
	    if (accessKeyId == null || accessKeyId.isEmpty() || accessKeySecret == null || accessKeySecret.isEmpty()) {
	        log.warn("[短信Mock] 未配置SMS凭证，模拟发送: phone={}, sign={}, template={}, param={}",
	                phoneNumbers, signName, templateCode, param);
	        return;
	    }
		DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
		IAcsClient client = new DefaultAcsClient(profile);

		SendSmsRequest request = new SendSmsRequest();
		request.setSysRegionId("cn-hangzhou");
		request.setPhoneNumbers(phoneNumbers);
		request.setSignName(signName);
		request.setTemplateCode(templateCode);
		request.setTemplateParam("{\"code\":\""+param+"\"}");
		try {
			SendSmsResponse response = client.getAcsResponse(request);
			if ("OK".equals(response.getCode())) {
				log.info("短信发送成功，phone={}, bizId={}", phoneNumbers, response.getBizId());
			} else {
				log.error("短信发送失败，phone={}, code={}, message={}", 
					phoneNumbers, response.getCode(), response.getMessage());
				throw new RuntimeException("短信发送失败: " + response.getMessage());
			}
		}catch (ClientException e) {
			log.error("短信发送异常，phone={}", phoneNumbers, e);
			throw new RuntimeException("短信发送异常: " + e.getMessage(), e);
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

}
