package com.yky.blog.common.result;

import lombok.Getter;

/**
 * 响应状态码枚举
 * 用枚举维护所有的状态码，避免魔法数字散落在各处。
 * 枚举本身就是类型，用的时候直接用枚举名作为类型声明
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),    // 枚举值，不需要类型
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "未登录或登录过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试");

    private final int code;  // 附加字段，需要声明类型
    private final String message;

    ResultCode(int code, String message){
        this.code = code;
        this.message = message;
    }
}
