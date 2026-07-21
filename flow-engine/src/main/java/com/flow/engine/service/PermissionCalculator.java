package com.flow.engine.service;

import com.flow.engine.common.utils.ExpressionUtils;
import com.flow.engine.dto.FormPermissionResponse;
import com.flow.engine.dto.FormPermissionResponse.FieldPermission;
import com.flow.engine.dto.FormPermissionResponse.ButtonPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 权限计算器（ISSUE-009，TRD §4.5.1）
 * <p>
 * 负责：
 * 1. 根据节点配置计算表单权限
 * 2. 合并继承权限与覆盖权限
 * 3. 评估条件化权限
 */
@Slf4j
@Service
public class PermissionCalculator {

    /** 默认节点权限 */
    private static final String DEFAULT_NODE_PERMISSION = "edit";

    /** 默认按钮权限 */
    private static final List<ButtonPermission> DEFAULT_BUTTON_PERMISSIONS;

    static {
        DEFAULT_BUTTON_PERMISSIONS = new ArrayList<>();
        DEFAULT_BUTTON_PERMISSIONS.add(createButton("submit", true, true));
        DEFAULT_BUTTON_PERMISSIONS.add(createButton("reject", true, true));
        DEFAULT_BUTTON_PERMISSIONS.add(createButton("transfer", true, true));
        DEFAULT_BUTTON_PERMISSIONS.add(createButton("delegate", true, true));
    }

    /**
     * 计算任务表单权限
     *
     * @param nodePermissions 节点配置的权限（来自流程定义JSON的节点properties）
     * @param variables       流程变量（用于条件化权限）
     * @return 计算后的表单权限
     */
    public FormPermissionResponse calculatePermissions(
            Map<String, Object> nodePermissions,
            Map<String, Object> variables) {

        FormPermissionResponse response = new FormPermissionResponse();

        if (nodePermissions == null || nodePermissions.isEmpty()) {
            // 无权限配置，返回默认（全部可编辑）
            response.setNodePermission(DEFAULT_NODE_PERMISSION);
            response.setFieldPermissions(Collections.emptyList());
            response.setButtonPermissions(new ArrayList<>(DEFAULT_BUTTON_PERMISSIONS));
            return response;
        }

        // 1. 节点级权限
        String nodePermission = (String) nodePermissions.getOrDefault("nodePermission", DEFAULT_NODE_PERMISSION);
        response.setNodePermission(nodePermission);

        // 如果节点级为 hidden，整个表单隐藏
        if ("hidden".equals(nodePermission)) {
            response.setFieldPermissions(Collections.emptyList());
            response.setButtonPermissions(Collections.emptyList());
            return response;
        }

        // 2. 字段级权限（含条件化评估）
        Object rawFieldPerms = nodePermissions.get("fieldPermissions");
        List<Map<String, Object>> fieldPermConfigs = normalizeFieldPermissions(rawFieldPerms);
        List<FieldPermission> fieldPermissions = calculateFieldPermissions(fieldPermConfigs, variables);
        response.setFieldPermissions(fieldPermissions);

        // 3. 按钮级权限（含条件化评估）
        Object rawButtonPerms = nodePermissions.get("buttonPermissions");
        List<Map<String, Object>> buttonPermConfigs = normalizeButtonPermissions(rawButtonPerms);
        List<ButtonPermission> buttonPermissions = calculateButtonPermissions(buttonPermConfigs, variables);
        response.setButtonPermissions(buttonPermissions);

        return response;
    }

    /**
     * 合并字段权限：继承 + 覆盖
     * <p>
     * 规则：
     * - 子节点未配置某字段权限时，继承父节点配置
     * - 子节点显式配置某字段权限时，覆盖父节点配置
     * - 子节点新增的字段权限追加到列表
     *
     * @param inherited 继承的字段权限（来自父节点）
     * @param overrides 覆盖的字段权限（当前节点配置）
     * @return 合并后的字段权限列表
     */
    public List<FieldPermission> mergePermissions(
            List<FieldPermission> inherited,
            List<FieldPermission> overrides) {

        if (inherited == null || inherited.isEmpty()) {
            return overrides != null ? overrides : Collections.emptyList();
        }
        if (overrides == null || overrides.isEmpty()) {
            return new ArrayList<>(inherited);
        }

        // 以 fieldKey 为键合并
        Map<String, FieldPermission> merged = new LinkedHashMap<>();

        // 先放入继承的
        for (FieldPermission fp : inherited) {
            merged.put(fp.getFieldKey(), fp);
        }

        // 覆盖或追加
        for (FieldPermission fp : overrides) {
            merged.put(fp.getFieldKey(), fp);
        }

        return new ArrayList<>(merged.values());
    }

    /**
     * 评估条件化权限
     * <p>
     * 如果字段权限有条件表达式，根据流程变量评估结果决定最终权限。
     *
     * @param permission 字段权限配置
     * @param variables  流程变量
     * @return 评估后的字段权限（permission字段被替换为实际值）
     */
    public FieldPermission evaluateCondition(FieldPermission permission, Map<String, Object> variables) {
        if (permission == null) {
            return null;
        }

        // 无条件表达式，直接返回
        if (permission.getCondition() == null || permission.getCondition().isBlank()) {
            return permission;
        }

        FieldPermission result = new FieldPermission();
        result.setFieldKey(permission.getFieldKey());
        result.setCondition(permission.getCondition());
        result.setDefaultPermission(permission.getDefaultPermission());

        try {
            boolean conditionMet = ExpressionUtils.evalBoolean(permission.getCondition(), variables);
            if (conditionMet) {
                // 条件满足，使用配置的权限
                result.setPermission(permission.getPermission());
            } else {
                // 条件不满足，使用默认权限
                String defaultPerm = permission.getDefaultPermission() != null
                        ? permission.getDefaultPermission()
                        : "edit";
                result.setPermission(defaultPerm);
            }
        } catch (Exception e) {
            log.warn("条件表达式求值失败: {}, 使用默认权限", permission.getCondition(), e);
            String defaultPerm = permission.getDefaultPermission() != null
                    ? permission.getDefaultPermission()
                    : "edit";
            result.setPermission(defaultPerm);
        }

        return result;
    }

    // ==================== 内部方法 ====================

    /**
     * 将 fieldPermissions 规范化为 List<Map> 格式
     * 支持两种输入格式：
     * - Map<String, String>: {"reason": "edit", "days": "readonly"}
     * - List<Map>: [{"fieldKey": "reason", "permission": "edit"}, ...]
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeFieldPermissions(Object raw) {
        if (raw == null) return null;
        if (raw instanceof List) {
            return (List<Map<String, Object>>) raw;
        }
        if (raw instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) raw;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("fieldKey", entry.getKey());
                item.put("permission", String.valueOf(entry.getValue()));
                result.add(item);
            }
            return result;
        }
        return null;
    }

    /**
     * 将 buttonPermissions 规范化为 List<Map> 格式
     * 支持两种输入格式：
     * - Map<String, Map>: {"submit": {"visible": true, "enabled": true}, ...}
     * - List<Map>: [{"buttonKey": "submit", "visible": true, "enabled": true}, ...]
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeButtonPermissions(Object raw) {
        if (raw == null) return null;
        if (raw instanceof List) {
            return (List<Map<String, Object>>) raw;
        }
        if (raw instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) raw;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("buttonKey", entry.getKey());
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> val = (Map<String, Object>) entry.getValue();
                    if (val.containsKey("visible")) item.put("visible", val.get("visible"));
                    if (val.containsKey("enabled")) item.put("enabled", val.get("enabled"));
                }
                result.add(item);
            }
            return result;
        }
        return null;
    }

    /**
     * 计算字段权限（含条件化评估）
     */
    private List<FieldPermission> calculateFieldPermissions(
            List<Map<String, Object>> fieldPermConfigs,
            Map<String, Object> variables) {

        if (fieldPermConfigs == null || fieldPermConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        List<FieldPermission> result = new ArrayList<>();
        for (Map<String, Object> config : fieldPermConfigs) {
            FieldPermission fp = new FieldPermission();
            fp.setFieldKey((String) config.get("fieldKey"));
            fp.setPermission((String) config.getOrDefault("permission", "edit"));
            fp.setCondition((String) config.get("condition"));
            fp.setDefaultPermission((String) config.get("defaultPermission"));

            // 如果有条件表达式，评估
            if (fp.getCondition() != null && !fp.getCondition().isBlank() && variables != null) {
                fp = evaluateCondition(fp, variables);
            }

            result.add(fp);
        }
        return result;
    }

    /**
     * 计算按钮权限（含条件化评估）
     */
    private List<ButtonPermission> calculateButtonPermissions(
            List<Map<String, Object>> buttonPermConfigs,
            Map<String, Object> variables) {

        // 先使用默认按钮权限
        Map<String, ButtonPermission> buttonMap = new LinkedHashMap<>();
        for (ButtonPermission bp : DEFAULT_BUTTON_PERMISSIONS) {
            buttonMap.put(bp.getButtonKey(), copyButton(bp));
        }

        // 用配置覆盖
        if (buttonPermConfigs != null) {
            for (Map<String, Object> config : buttonPermConfigs) {
                String buttonKey = (String) config.get("buttonKey");
                if (buttonKey == null) continue;

                ButtonPermission bp = buttonMap.computeIfAbsent(buttonKey, k -> {
                    ButtonPermission b = new ButtonPermission();
                    b.setButtonKey(k);
                    b.setVisible(true);
                    b.setEnabled(true);
                    return b;
                });

                if (config.containsKey("visible")) {
                    bp.setVisible((Boolean) config.get("visible"));
                }
                if (config.containsKey("enabled")) {
                    bp.setEnabled((Boolean) config.get("enabled"));
                }

                // 条件化按钮权限
                String condition = (String) config.get("condition");
                if (condition != null && !condition.isBlank() && variables != null) {
                    try {
                        boolean conditionMet = ExpressionUtils.evalBoolean(condition, variables);
                        if (!conditionMet) {
                            bp.setVisible(false);
                        }
                    } catch (Exception e) {
                        log.warn("按钮条件表达式求值失败: {}", condition, e);
                    }
                }
            }
        }

        return new ArrayList<>(buttonMap.values());
    }

    private static ButtonPermission createButton(String key, boolean visible, boolean enabled) {
        ButtonPermission bp = new ButtonPermission();
        bp.setButtonKey(key);
        bp.setVisible(visible);
        bp.setEnabled(enabled);
        return bp;
    }

    private ButtonPermission copyButton(ButtonPermission source) {
        ButtonPermission bp = new ButtonPermission();
        bp.setButtonKey(source.getButtonKey());
        bp.setVisible(source.getVisible());
        bp.setEnabled(source.getEnabled());
        return bp;
    }
}
