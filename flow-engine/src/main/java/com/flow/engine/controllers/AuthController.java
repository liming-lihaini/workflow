package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.Role;
import com.flow.engine.service.AuthService;
import com.flow.engine.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证API（ISSUE-013）
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RolePermissionService rolePermissionService;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.get("username"), body.get("password"));
        return Result.ok(Map.of("token", token));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return Result.ok();
    }

    /**
     * 获取当前用户信息（含角色、权限）
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        AuthService.SessionInfo session = authService.validateToken(token);

        Long userId = session.getUserId();
        String username = session.getUsername();

        // 查询角色
        List<Role> roles = rolePermissionService.getUserRoles(userId);
        List<Map<String, String>> roleList = roles.stream().map(r -> {
            Map<String, String> m = new HashMap<>();
            m.put("roleKey", r.getRoleKey());
            m.put("roleName", r.getRoleName());
            return m;
        }).collect(Collectors.toList());

        // 查询权限
        List<String> permissions = rolePermissionService.getUserPermissionKeys(userId);
        boolean isAdmin = permissions.contains("*");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("username", username);
        result.put("roles", roleList);
        result.put("permissions", permissions);
        result.put("isAdmin", isAdmin);

        return Result.ok(result);
    }
}
