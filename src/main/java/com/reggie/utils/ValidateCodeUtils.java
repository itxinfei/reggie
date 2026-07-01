package com.reggie.utils;

import com.reggie.common.CustomException;
import java.util.Random;

/**
 * 随机生成验证码工具类
 */
public final class ValidateCodeUtils {

    private ValidateCodeUtils() {
        throw new AssertionError();
    }

    /**
     * 随机生成验证码
     * @param length 长度为4位或者6位
     * @return
     */
    public static Integer generateValidateCode(int length){
        Integer code = null;
        if(length == 4){
            code = new Random().nextInt(9000) + 1000;
        }else if(length == 6){
            code = new Random().nextInt(900000) + 100000;
        }else{
            throw new CustomException("只能生成4位或6位数字验证码");
        }
        return code;
    }

    /**
     * 随机生成指定长度字符串验证码
     * @param length 长度
     * @return
     */
    public static String generateValidateCode4String(int length){
        if (length <= 0 || length > 8) {
            throw new CustomException("验证码长度必须在1-8之间");
        }
        Random rdm = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(rdm.nextInt(16)));
        }
        return sb.toString();
    }
}
