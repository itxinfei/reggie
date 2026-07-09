package com.reggie.utils;

import com.reggie.common.CustomException;
import lombok.extern.slf4j.Slf4j;
import java.util.Random;

/**
 * 随机生成验证码工具类
 */
@Slf4j
public final class ValidateCodeUtils {

    private ValidateCodeUtils() {
        throw new AssertionError();
    }

    /**
     * 4位验证码长度
     */
    public static final int CODE_LENGTH_4 = 4;

    /**
     * 6位验证码长度
     */
    public static final int CODE_LENGTH_6 = 6;

    /**
     * 4位验证码最小值
     */
    private static final int CODE_4_MIN = 1000;

    /**
     * 4位验证码最大值
     */
    private static final int CODE_4_MAX = 9000;

    /**
     * 6位验证码最小值
     */
    private static final int CODE_6_MIN = 100000;

    /**
     * 6位验证码最大值
     */
    private static final int CODE_6_MAX = 900000;

    /**
     * 字符串验证码最小长度
     */
    private static final int STRING_CODE_MIN_LENGTH = 1;

    /**
     * 字符串验证码最大长度
     */
    private static final int STRING_CODE_MAX_LENGTH = 8;

    /**
     * 十六进制基数
     */
    private static final int HEX_RADIX = 16;

    /**
     * 随机生成验证码
     * @param length 长度为4位或者6位
     * @return 验证码
     */
    public static Integer generateValidateCode(int length){
        Integer code = null;
        if(length == CODE_LENGTH_4){
            code = new Random().nextInt(CODE_4_MAX) + CODE_4_MIN;
        }else if(length == CODE_LENGTH_6){
            code = new Random().nextInt(CODE_6_MAX) + CODE_6_MIN;
        }else{
            log.warn("不支持的验证码长度: {}", length);
            throw new CustomException("只能生成4位或6位数字验证码");
        }
        return code;
    }

    /**
     * 随机生成指定长度字符串验证码
     * @param length 长度（1-8位）
     * @return 十六进制字符串验证码
     */
    public static String generateValidateCode4String(int length){
        if (length <= STRING_CODE_MIN_LENGTH || length > STRING_CODE_MAX_LENGTH) {
            log.warn("验证码长度不符合要求: {}", length);
            throw new CustomException("验证码长度必须在1-8之间");
        }
        Random rdm = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(rdm.nextInt(HEX_RADIX)));
        }
        return sb.toString();
    }
}
