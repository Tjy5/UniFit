package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.LoginRequestDto;
import com.authguard.usersystem.dto.LoginResponseDto;
import com.authguard.usersystem.dto.RegisterRequestDto;
import com.authguard.usersystem.dto.UpdatePasswordRequestDto;
import com.authguard.usersystem.dto.UserInfoResponseDto;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.exception.UnauthenticatedException;
import com.authguard.usersystem.security.UserPrincipal;
import com.authguard.usersystem.service.UserAccountService;
import com.authguard.usersystem.util.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    @Autowired
    private UserAccountService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequestDto request) {
        userService.register(toUserAccount(request));
        return ResponseEntity.ok(ApiResponse.success("注册成功", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        var loggedInUser = userService.login(request.getUserAccount(), request.getUserPassword());
        String token = jwtUtils.generateToken(loggedInUser.getUserId(), loggedInUser.getUserAccount());
        return ResponseEntity.ok(ApiResponse.success("登录成功",
                new LoginResponseDto(token, loggedInUser.getUserId(), loggedInUser.getUserAccount())));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserInfoResponseDto>> getUserInfo(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserAccount() == null) {
            throw new UnauthenticatedException("未登录或Token无效");
        }
        var user = userService.getUserByAccount(principal.getUserAccount());
        if (user == null) {
            throw new NotFoundException("用户信息不存在");
        }
        return ResponseEntity.ok(ApiResponse.success("获取用户信息成功",
                new UserInfoResponseDto(user.getUserId(), user.getUserAccount())));
    }

    @PutMapping("/updatePassword")
    public ResponseEntity<ApiResponse<Boolean>> updatePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                                @Valid @RequestBody UpdatePasswordRequestDto request) {
        if (principal == null || principal.getUserAccount() == null) {
            throw new UnauthenticatedException("未登录或Token无效");
        }
        boolean updated = userService.updatePassword(principal.getUserAccount(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("密码更新成功", updated));
    }

    private com.authguard.usersystem.entity.UserAccount toUserAccount(RegisterRequestDto request) {
        com.authguard.usersystem.entity.UserAccount user = new com.authguard.usersystem.entity.UserAccount();
        user.setUserAccount(request.getUserAccount());
        user.setUserPassword(request.getUserPassword());
        return user;
    }
}
