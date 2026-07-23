package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.Role;
import com.flow.engine.entity.User;
import com.flow.engine.entity.UserPost;
import com.flow.engine.service.RolePermissionService;
import com.flow.engine.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理API（ISSUE-013, TRD §3.8）
 */
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RolePermissionService rolePermissionService;

    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) Long deptId) {
        return Result.ok(userService.listUsers(deptId));
    }

    /**
     * 分页查询用户（支持关键字搜索）
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> listPage(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userService.listUsersPage(deptId, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        return Result.ok(userService.getUser(id));
    }

    @PostMapping
    public Result<User> create(@RequestBody User user) {
        return Result.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody User user) {
        return Result.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @PostMapping("/{id}/reset-pwd")
    public Result<Void> resetPwd(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok();
    }

    @GetMapping("/{id}/posts")
    public Result<List<UserPost>> getPosts(@PathVariable Long id) {
        return Result.ok(userService.getUserPosts(id));
    }

    @PostMapping("/{id}/posts")
    public Result<UserPost> addPost(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long deptId = Long.valueOf(body.get("deptId").toString());
        Long postId = Long.valueOf(body.get("postId").toString());
        boolean isMain = Boolean.TRUE.equals(body.get("isMain"));
        return Result.ok(userService.addUserPost(id, deptId, postId, isMain));
    }

    @DeleteMapping("/{id}/posts/{postId}")
    public Result<Void> deletePost(@PathVariable Long id, @PathVariable Long postId) {
        userService.deleteUserPost(id, postId);
        return Result.ok();
    }

    // ========== 用户角色授权 ==========

    @GetMapping("/{id}/roles")
    public Result<List<Role>> getUserRoles(@PathVariable Long id) {
        return Result.ok(rolePermissionService.getUserRoles(id));
    }

    @PostMapping("/{id}/roles")
    public Result<Void> setUserRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        rolePermissionService.setUserRoles(id, roleIds);
        return Result.ok();
    }
}
