package com.suios.admin.auth.service;

import com.suios.admin.auth.dto.AdminInfoResponse;
import com.suios.admin.auth.dto.LoginRequest;
import com.suios.admin.auth.dto.LoginResponse;
import com.suios.admin.auth.dto.UpdatePasswordRequest;
import com.suios.admin.auth.entity.AdminUser;
import com.suios.admin.auth.jwt.JwtTokenProvider;
import com.suios.admin.auth.mapper.AdminUserMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserMapper adminUserMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthContext authContext;

    public LoginResponse login(LoginRequest request) {
        AdminUser adminUser = adminUserMapper.selectByUsername(request.getUsername());
        if (adminUser == null || !passwordEncoder.matches(request.getPassword(), adminUser.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (adminUser.getStatus() == null || adminUser.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }

        adminUserMapper.touchLastLogin(adminUser.getUserId());
        String token = jwtTokenProvider.createToken(adminUser);
        return new LoginResponse(token, toAdminInfo(adminUser));
    }

    public void logout() {
        // Stateless JWT logout is handled by the client discarding the token.
    }

    public AdminInfoResponse getCurrentAdminInfo() {
        AdminUser adminUser = adminUserMapper.selectById(requireCurrentUserId());
        if (adminUser == null) {
            throw new BizException("管理员不存在");
        }
        return toAdminInfo(adminUser);
    }

    public void updatePassword(UpdatePasswordRequest request) {
        AdminUser adminUser = adminUserMapper.selectById(requireCurrentUserId());
        if (adminUser == null) {
            throw new BizException("管理员不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), adminUser.getPassword())) {
            throw new BizException("旧密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BizException("新密码不能与旧密码相同");
        }
        adminUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminUserMapper.updateById(adminUser);
    }

    private Long requireCurrentUserId() {
        Long currentUserId = authContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BizException("登录状态无效");
        }
        return currentUserId;
    }

    private AdminInfoResponse toAdminInfo(AdminUser adminUser) {
        return AdminInfoResponse.builder()
                .userId(adminUser.getUserId())
                .username(adminUser.getUsername())
                .nickname(adminUser.getNickname())
                .avatar(adminUser.getAvatar())
                .build();
    }
}
