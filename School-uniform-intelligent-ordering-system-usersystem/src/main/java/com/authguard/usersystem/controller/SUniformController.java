package com.authguard.usersystem.controller;

import com.authguard.usersystem.entity.SUniform;
import com.authguard.usersystem.security.UserPrincipal;
import com.authguard.usersystem.service.SUniformService;
import com.authguard.usersystem.service.ISUserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/s-uniform")
public class SUniformController {
    @Autowired
    private SUniformService sUniformService;

    @Autowired
    private ISUserActivityLogService activityLogService;

    @GetMapping("/active")
    public List<SUniform> getAllActiveUniforms(HttpServletRequest request) {
        activityLogService.recordActivityAsync(null, "VIEW_ACTIVE_UNIFORMS", null, null, null, request);
        return sUniformService.getAllActiveUniforms();
    }

    @GetMapping("/{uniformId}")
    public ResponseEntity<SUniform> getUniformDetail(@PathVariable Long uniformId,
                                                     HttpServletRequest request) {
        SUniform uniform = sUniformService.getUniformById(uniformId);
        if (uniform == null) {
            return ResponseEntity.notFound().build();
        }
        activityLogService.recordActivityAsync(
                resolveOptionalUserId(),
                "VIEW_UNIFORM_DETAIL",
                "UNIFORM",
                String.valueOf(uniformId),
                null,
                request);
        return ResponseEntity.ok(uniform);
    }

    /**
     * 详情接口允许匿名访问，因此从 SecurityContext 中尽力解析当前登录用户。
     * 当请求未携带有效 token 或鉴权未执行时，返回 null 让漏斗分析按匿名会话归集。
     */
    private Long resolveOptionalUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
