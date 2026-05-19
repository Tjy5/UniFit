package com.suios.admin.auth.controller;

import com.suios.admin.auth.dto.AdminInfoResponse;
import com.suios.admin.auth.dto.LoginRequest;
import com.suios.admin.auth.dto.LoginResponse;
import com.suios.admin.auth.dto.UpdatePasswordRequest;
import com.suios.admin.auth.service.AuthService;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.success(authService.login(request));
    }

    @PostMapping("/logout")
    @OperLog(module = "认证管理", operation = "LOGOUT", saveRequestBody = false)
    public R<Void> logout() {
        authService.logout();
        return R.success("logout success");
    }

    @GetMapping("/info")
    public R<AdminInfoResponse> getInfo() {
        return R.success(authService.getCurrentAdminInfo());
    }

    @PutMapping("/password")
    @OperLog(module = "认证管理", operation = "UPDATE_PASSWORD")
    public R<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(request);
        return R.success("password updated");
    }
}
