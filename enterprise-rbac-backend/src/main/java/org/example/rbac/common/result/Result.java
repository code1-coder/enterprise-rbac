package org.example.rbac.common.result;

import lombok.Data;

/**
 * 统一响应体，字段是 code、message、data、timestamp。
 * Controller 查询用 query（“查询成功”），增删改用 success（“操作成功”），HTTP 状态仍是 200。
 * 业务异常由 GlobalExceptionHandler 收成同一结构；未认证和无权限也可由 SecurityConfig 直接写出。
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> Result<T> success() {
        return of(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return of(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return of(200, message, data);
    }

    public static <T> Result<T> query(T data) {
        return of(200, "查询成功", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return of(code, message, null);
    }

    private static <T> Result<T> of(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        return result;
    }
}
