package com.suios.admin.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> success(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> success(String msg, T data) {
        return new R<>(200, msg, data);
    }

    public static R<Void> success() {
        return success(null);
    }

    public static R<Void> success(String msg) {
        return new R<>(200, msg, null);
    }

    public static R<Void> fail(String msg) {
        return new R<>(500, msg, null);
    }

    public static R<Void> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public static R<Void> unauthorized(String msg) {
        return new R<>(401, msg, null);
    }

    public static R<Void> forbidden(String msg) {
        return new R<>(403, msg, null);
    }
}
