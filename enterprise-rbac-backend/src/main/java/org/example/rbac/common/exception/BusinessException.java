package org.example.rbac.common.exception;

import lombok.Getter;

/**
 * 可预期的业务失败。code 同时作为 HTTP 状态，由 GlobalExceptionHandler 转成 Result.fail。
 * 骨架阶段“尚未实现”也是这个异常，见 Unimplemented。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
