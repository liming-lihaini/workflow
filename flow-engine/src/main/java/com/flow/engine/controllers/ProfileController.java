package com.flow.engine.controllers;

import com.flow.engine.annotation.OpLog;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.Result;
import com.flow.engine.entity.User;
import com.flow.engine.service.ApiTokenService;
import com.flow.engine.service.AuthService;
import com.flow.engine.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人中心API：个人信息维护、登录密码修改、API Token管理
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;
    private final UserService userService;
    private final ApiTokenService apiTokenService;

    /**
     * 获取当前用户个人信息
     */
    @GetMapping
    public Result<Map<String, Object>> getProfile(@RequestHeader("Authorization") String authHeader) {
        User user = currentUser(authHeader);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        return Result.ok(result);
    }

    /**
     * 修改个人信息（姓名、邮箱、手机号）
     */
    @PutMapping
    @OpLog(module = "个人中心", operation = "修改个人信息")
    public Result<Void> updateProfile(@RequestHeader("Authorization") String authHeader,
                                      @RequestBody Map<String, String> body) {
        User user = currentUser(authHeader);
        User update = new User();
        update.setRealName(body.get("realName"));
        update.setEmail(body.get("email"));
        update.setPhone(body.get("phone"));
        userService.updateUser(user.getId(), update);
        return Result.ok();
    }

    /**
     * 修改登录密码（需验证原密码）
     */
    @PutMapping("/password")
    @OpLog(module = "个人中心", operation = "修改登录密码", recordParams = false)
    public Result<Void> changePassword(@RequestHeader("Authorization") String authHeader,
                                       @RequestBody Map<String, String> body) {
        User user = currentUser(authHeader);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "新密码长度不能少于6位");
        }
        if (oldPassword == null || !AuthService.hashPassword(oldPassword).equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "原密码错误");
        }
        userService.resetPassword(user.getId(), newPassword);
        return Result.ok();
    }

    /**
     * 查询个人API Token列表（Token值脱敏）
     */
    @GetMapping("/tokens")
    public Result<List<Map<String, Object>>> listTokens(@RequestHeader("Authorization") String authHeader) {
        User user = currentUser(authHeader);
        return Result.ok(apiTokenService.listTokens(user.getId()));
    }

    /**
     * 生成API Token（完整Token值仅本次返回）
     */
    @PostMapping("/tokens")
    @OpLog(module = "个人中心", operation = "生成API Token", recordParams = false)
    public Result<Map<String, Object>> createToken(@RequestHeader("Authorization") String authHeader,
                                                   @RequestBody Map<String, Object> body) {
        User user = currentUser(authHeader);
        String tokenName = body.get("tokenName") != null ? String.valueOf(body.get("tokenName")) : null;
        Integer expireDays = body.get("expireDays") != null
                ? Integer.valueOf(String.valueOf(body.get("expireDays"))) : null;
        return Result.ok(apiTokenService.createToken(user.getId(), tokenName, expireDays));
    }

    /**
     * 删除API Token
     */
    @DeleteMapping("/tokens/{id}")
    @OpLog(module = "个人中心", operation = "删除API Token")
    public Result<Void> deleteToken(@RequestHeader("Authorization") String authHeader,
                                    @PathVariable Long id) {
        User user = currentUser(authHeader);
        apiTokenService.deleteToken(user.getId(), id);
        return Result.ok();
    }

    /** 解析Authorization头并返回当前登录用户 */
    private User currentUser(String authHeader) {
        String token = authHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        AuthService.SessionInfo session = authService.validateToken(token);
        return userService.getUser(session.getUserId());
    }
}
