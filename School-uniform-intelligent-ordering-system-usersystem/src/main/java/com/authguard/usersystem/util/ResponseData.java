package com.authguard.usersystem.util;

public class ResponseData {

    private String status;      // 状态：success 或 error
    private String message;     // 消息
    private String redirectUrl; // 跳转路径
    private String token;       // 新增的 Token 字段

    // 构造方法
    public ResponseData(String status, String message, String redirectUrl) {
        this.status = status;
        this.message = message;
        this.redirectUrl = redirectUrl;
    }

    public ResponseData(String status, String message, String redirectUrl, String token) {
        this.status = status;
        this.message = message;
        this.redirectUrl = redirectUrl;
        this.token = token;
    }

    // Getter 和 Setter 方法
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
