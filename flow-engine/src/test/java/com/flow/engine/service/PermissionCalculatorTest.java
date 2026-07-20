package com.flow.engine.service;

import com.flow.engine.dto.FormPermissionResponse;
import com.flow.engine.dto.FormPermissionResponse.FieldPermission;
import com.flow.engine.dto.FormPermissionResponse.ButtonPermission;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限计算器测试（ISSUE-009）
 * 测试合并、条件化、继承覆盖逻辑
 */
class PermissionCalculatorTest {

    private PermissionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PermissionCalculator();
    }

    // ==================== 节点级权限 ====================

    @Test
    @DisplayName("无权限配置时返回默认权限（edit）")
    void testDefaultPermissions() {
        FormPermissionResponse resp = calculator.calculatePermissions(null, null);
        assertEquals("edit", resp.getNodePermission());
        assertTrue(resp.getButtonPermissions().size() >= 4);
    }

    @Test
    @DisplayName("节点级 readonly")
    void testNodeReadonly() {
        Map<String, Object> perms = new HashMap<>();
        perms.put("nodePermission", "readonly");

        FormPermissionResponse resp = calculator.calculatePermissions(perms, null);
        assertEquals("readonly", resp.getNodePermission());
    }

    @Test
    @DisplayName("节点级 hidden 时字段和按钮都为空")
    void testNodeHidden() {
        Map<String, Object> perms = new HashMap<>();
        perms.put("nodePermission", "hidden");

        FormPermissionResponse resp = calculator.calculatePermissions(perms, null);
        assertEquals("hidden", resp.getNodePermission());
        assertTrue(resp.getFieldPermissions().isEmpty());
        assertTrue(resp.getButtonPermissions().isEmpty());
    }

    // ==================== 字段级权限 ====================

    @Test
    @DisplayName("字段级权限正确返回")
    void testFieldPermissions() {
        Map<String, Object> perms = new HashMap<>();
        List<Map<String, Object>> fieldPerms = new ArrayList<>();

        Map<String, Object> fp1 = new HashMap<>();
        fp1.put("fieldKey", "applicant");
        fp1.put("permission", "readonly");
        fieldPerms.add(fp1);

        Map<String, Object> fp2 = new HashMap<>();
        fp2.put("fieldKey", "reason");
        fp2.put("permission", "edit");
        fieldPerms.add(fp2);

        Map<String, Object> fp3 = new HashMap<>();
        fp3.put("fieldKey", "secret");
        fp3.put("permission", "hidden");
        fieldPerms.add(fp3);

        perms.put("fieldPermissions", fieldPerms);

        FormPermissionResponse resp = calculator.calculatePermissions(perms, null);
        assertEquals(3, resp.getFieldPermissions().size());
        assertEquals("readonly", resp.getFieldPermissions().get(0).getPermission());
        assertEquals("edit", resp.getFieldPermissions().get(1).getPermission());
        assertEquals("hidden", resp.getFieldPermissions().get(2).getPermission());
    }

    // ==================== 条件化权限 ====================

    @Test
    @DisplayName("条件化权限：条件满足时使用配置的权限")
    void testConditionalPermissionMet() {
        Map<String, Object> perms = new HashMap<>();
        List<Map<String, Object>> fieldPerms = new ArrayList<>();

        Map<String, Object> fp = new HashMap<>();
        fp.put("fieldKey", "amount");
        fp.put("permission", "readonly");
        fp.put("condition", "amount > 1000");
        fp.put("defaultPermission", "edit");
        fieldPerms.add(fp);

        perms.put("fieldPermissions", fieldPerms);

        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 2000);

        FormPermissionResponse resp = calculator.calculatePermissions(perms, variables);
        assertEquals("readonly", resp.getFieldPermissions().get(0).getPermission(),
                "条件满足时应为 readonly");
    }

    @Test
    @DisplayName("条件化权限：条件不满足时使用默认权限")
    void testConditionalPermissionNotMet() {
        Map<String, Object> perms = new HashMap<>();
        List<Map<String, Object>> fieldPerms = new ArrayList<>();

        Map<String, Object> fp = new HashMap<>();
        fp.put("fieldKey", "amount");
        fp.put("permission", "readonly");
        fp.put("condition", "amount > 1000");
        fp.put("defaultPermission", "edit");
        fieldPerms.add(fp);

        perms.put("fieldPermissions", fieldPerms);

        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 500);

        FormPermissionResponse resp = calculator.calculatePermissions(perms, variables);
        assertEquals("edit", resp.getFieldPermissions().get(0).getPermission(),
                "条件不满足时应为 edit");
    }

    // ==================== 按钮级权限 ====================

    @Test
    @DisplayName("按钮级权限：隐藏特定按钮")
    void testButtonPermissions() {
        Map<String, Object> perms = new HashMap<>();
        List<Map<String, Object>> buttonPerms = new ArrayList<>();

        Map<String, Object> bp = new HashMap<>();
        bp.put("buttonKey", "reject");
        bp.put("visible", false);
        buttonPerms.add(bp);

        perms.put("buttonPermissions", buttonPerms);

        FormPermissionResponse resp = calculator.calculatePermissions(perms, null);
        // 找到 reject 按钮
        ButtonPermission rejectBtn = resp.getButtonPermissions().stream()
                .filter(b -> "reject".equals(b.getButtonKey()))
                .findFirst().orElse(null);
        assertNotNull(rejectBtn);
        assertFalse(rejectBtn.getVisible(), "reject 按钮应隐藏");
    }

    @Test
    @DisplayName("条件化按钮：条件不满足时隐藏")
    void testConditionalButton() {
        Map<String, Object> perms = new HashMap<>();
        List<Map<String, Object>> buttonPerms = new ArrayList<>();

        Map<String, Object> bp = new HashMap<>();
        bp.put("buttonKey", "transfer");
        bp.put("visible", true);
        bp.put("condition", "amount > 5000");
        buttonPerms.add(bp);

        perms.put("buttonPermissions", buttonPerms);

        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 100);

        FormPermissionResponse resp = calculator.calculatePermissions(perms, variables);
        ButtonPermission transferBtn = resp.getButtonPermissions().stream()
                .filter(b -> "transfer".equals(b.getButtonKey()))
                .findFirst().orElse(null);
        assertNotNull(transferBtn);
        assertFalse(transferBtn.getVisible(), "条件不满足时 transfer 应隐藏");
    }

    // ==================== 合并/继承覆盖 ====================

    @Test
    @DisplayName("合并权限：子节点覆盖父节点同字段权限")
    void testMergeOverride() {
        List<FieldPermission> inherited = new ArrayList<>();
        inherited.add(createFieldPerm("applicant", "readonly"));
        inherited.add(createFieldPerm("reason", "edit"));

        List<FieldPermission> overrides = new ArrayList<>();
        overrides.add(createFieldPerm("applicant", "edit")); // 覆盖

        List<FieldPermission> merged = calculator.mergePermissions(inherited, overrides);
        assertEquals(2, merged.size());

        // applicant 被覆盖为 edit
        FieldPermission applicant = merged.stream()
                .filter(f -> "applicant".equals(f.getFieldKey()))
                .findFirst().orElse(null);
        assertNotNull(applicant);
        assertEquals("edit", applicant.getPermission());
    }

    @Test
    @DisplayName("合并权限：子节点新增字段权限追加")
    void testMergeAppend() {
        List<FieldPermission> inherited = new ArrayList<>();
        inherited.add(createFieldPerm("applicant", "readonly"));

        List<FieldPermission> overrides = new ArrayList<>();
        overrides.add(createFieldPerm("newField", "edit"));

        List<FieldPermission> merged = calculator.mergePermissions(inherited, overrides);
        assertEquals(2, merged.size());
    }

    @Test
    @DisplayName("合并权限：父为空时直接返回子")
    void testMergeEmptyParent() {
        List<FieldPermission> overrides = new ArrayList<>();
        overrides.add(createFieldPerm("field1", "edit"));

        List<FieldPermission> merged = calculator.mergePermissions(null, overrides);
        assertEquals(1, merged.size());
    }

    @Test
    @DisplayName("合并权限：子为空时返回父")
    void testMergeEmptyChild() {
        List<FieldPermission> inherited = new ArrayList<>();
        inherited.add(createFieldPerm("field1", "readonly"));

        List<FieldPermission> merged = calculator.mergePermissions(inherited, null);
        assertEquals(1, merged.size());
        assertEquals("readonly", merged.get(0).getPermission());
    }

    // ==================== evaluateCondition ====================

    @Test
    @DisplayName("evaluateCondition: 无条件直接返回")
    void testEvaluateNoCondition() {
        FieldPermission fp = createFieldPerm("field1", "edit");
        FieldPermission result = calculator.evaluateCondition(fp, null);
        assertEquals("edit", result.getPermission());
    }

    @Test
    @DisplayName("evaluateCondition: 条件为null返回原权限")
    void testEvaluateNullCondition() {
        FieldPermission fp = createFieldPerm("field1", "readonly");
        fp.setCondition(null);
        FieldPermission result = calculator.evaluateCondition(fp, Map.of("x", 1));
        assertEquals("readonly", result.getPermission());
    }

    // ==================== 辅助方法 ====================

    private FieldPermission createFieldPerm(String fieldKey, String permission) {
        FieldPermission fp = new FieldPermission();
        fp.setFieldKey(fieldKey);
        fp.setPermission(permission);
        return fp;
    }
}
