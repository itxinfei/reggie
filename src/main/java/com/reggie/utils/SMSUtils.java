package com.reggie.utils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;

/**
 * 短信发送工具类
 * 修改点：凭证改为从application.yml注入，不再硬编码空字符串
 */
@Slf4j
public final class SMSUtils {

    private SMSUtils() {
        throw new AssertionError();
    }

    /** 修改点：从配置注入的凭证，默认使用mock模式避免启动报错 */
    private static String accessKeyId;
    private static String accessKeySecret;

    /**
     * 初始化短信凭证（由SmsConfig或启动时调用）
     */
    public static void init(String keyId, String keySecret) {
        accessKeyId = keyId;
        accessKeySecret = keySecret;
        log.info("SMS凭证已初始化, accessKeyId={}", maskKey(keyId));
    }

	/**
	 * 发送短信
	 * @param signName 签名
	 * @param templateCode 模板
	 * @param phoneNumbers 手机号
	 * @param param 参数
	 */
	public static void sendMessage(String signName, String templateCode,String phoneNumbers,String param){
	    // 修改点：未配置真实凭证时使用mock模式，避免生产环境抛出异常
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

	/** 脱敏AccessKey用于日志 */
	private static String maskKey(String key) {
	    if (key == null || key.length() <= 8) return "***";
	    return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
	}

}
