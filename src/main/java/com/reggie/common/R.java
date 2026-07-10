package com.reggie.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用返回结果，服务端响应的数据最终都会封装成此对象
 *
 * @param <T> 数据类型
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class R<T> {

    /** 编码：1成功，0和其它数字为失败 */
    private Integer code;

    /** 错误信息 */
    private String msg;

    /** 数据 */
    private T data;

    /** 动态数据 */
    private Map<String, Object> map = new HashMap<>();

    /** 时间戳 */
    private Long timestamp;

    /** 请求ID */
    private String requestId;

    /**
     * 构造方法，初始化请求ID和时间戳
     */
    public R() {
        this.requestId = UUID.randomUUID().toString().replace("-", "");
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 返回成功结果
     *
     * @param object 返回数据
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> R<T> success(T object) {
        R<T> r = new R<>();
        r.data = object;
        r.code = 1;
        return r;
    }

    /**
     * 返回失败结果
     *
     * @param msg 错误信息
     * @param <T> 数据类型
     * @return 失败响应对象
     */
    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.msg = msg;
        r.code = 0;
        return r;
    }

    /**
     * 添加动态数据
     *
     * @param key 键
     * @param value 值
     * @return 当前响应对象
     */
    public R<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}
