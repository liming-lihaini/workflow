import { useState, useEffect } from 'react'
import { NODE_TYPES_CONFIG, EDGE_SCHEMA } from '../nodeTypes'
import { getFormAll, getForm } from '../api'

function ConfigPanel({ selectedNode, selectedEdge, onNodeConfigChange, onEdgeConfigChange }) {
  const [formList, setFormList] = useState([])
  const [formFields, setFormFields] = useState([])
  const [showPermissionPanel, setShowPermissionPanel] = useState(false)

  // 加载表单列表
  useEffect(() => {
    getFormAll().then(res => {
      setFormList(res.data || res || [])
    }).catch(() => {})
  }, [])

  // 当选中的节点有 formKey 时，加载表单字段
  useEffect(() => {
    if (selectedNode?.data?.formKey) {
      loadFormFields(selectedNode.data.formKey)
    } else {
      setFormFields([])
    }
  }, [selectedNode?.data?.formKey])

  async function loadFormFields(formKey) {
    try {
      const res = await getForm(formKey)
      const form = res.data || res
      if (form?.formJson) {
        try {
          const formJson = JSON.parse(form.formJson)
          setFormFields(formJson.components || formJson.fields || [])
        } catch {
          setFormFields([])
        }
      }
    } catch {
      setFormFields([])
    }
  }

  // 获取当前节点的表单权限配置
  function getFormPermissions() {
    if (!selectedNode?.data?.formPermissions) return {}
    if (typeof selectedNode.data.formPermissions === 'string') {
      try { return JSON.parse(selectedNode.data.formPermissions) } catch { return {} }
    }
    return selectedNode.data.formPermissions
  }

  // 更新字段权限
  function updateFieldPermission(fieldKey, permission) {
    const current = getFormPermissions()
    const fields = current.fields || {}
    fields[fieldKey] = permission
    const updated = { ...current, fields }
    onNodeConfigChange(selectedNode.id, 'formPermissions', updated)
  }

  // 更新按钮权限
  function updateButtonPermission(buttonKey, key, value) {
    const current = getFormPermissions()
    const buttons = current.buttons || {}
    buttons[buttonKey] = { ...buttons[buttonKey], [key]: value }
    const updated = { ...current, buttons }
    onNodeConfigChange(selectedNode.id, 'formPermissions', updated)
  }

  // 更新节点级权限
  function updateNodePermission(permission) {
    const current = getFormPermissions()
    const updated = { ...current, nodePermission: permission }
    onNodeConfigChange(selectedNode.id, 'formPermissions', updated)
  }

  // 处理表单选择变化
  function handleFormKeyChange(newFormKey) {
    onNodeConfigChange(selectedNode.id, 'formKey', newFormKey)
    // 清空旧的权限
    if (newFormKey) {
      loadFormFields(newFormKey)
    } else {
      setFormFields([])
      onNodeConfigChange(selectedNode.id, 'formPermissions', null)
    }
  }

  // 是否显示表单权限配置（仅用户任务节点）
  const showFormConfig = selectedNode && ['userTask'].includes(selectedNode.data.nodeType)

  // 选中了节点
  if (selectedNode) {
    const config = NODE_TYPES_CONFIG[selectedNode.data.nodeType] || NODE_TYPES_CONFIG.custom
    const schema = config.schema
    const permissions = getFormPermissions()

    return (
      <div className="config-panel">
        <div className="config-title">
          {config.label} 配置
          <span style={{ fontSize: 12, color: 'var(--text-placeholder)', marginLeft: 8 }}>
            {selectedNode.data.nodeId}
          </span>
        </div>

        {/* 基础配置 */}
        <div style={{ maxHeight: showFormConfig ? '40%' : '100%', overflowY: 'auto' }}>
          {schema.map((field) => (
            <div key={field.key} className="config-form-item">
              <label className="config-label">
                {field.label}
                {field.required && <span style={{ color: 'var(--color-danger)' }}> *</span>}
              </label>
              {field.type === 'input' && (
                <input
                  className="config-input"
                  value={selectedNode.data[field.key] || field.default || ''}
                  placeholder={field.placeholder || ''}
                  onChange={(e) => onNodeConfigChange(selectedNode.id, field.key, e.target.value)}
                />
              )}
              {field.type === 'textarea' && (
                <textarea
                  className="config-textarea"
                  value={selectedNode.data[field.key] || ''}
                  placeholder={field.placeholder || ''}
                  onChange={(e) => onNodeConfigChange(selectedNode.id, field.key, e.target.value)}
                />
              )}
              {field.type === 'select' && (
                <select
                  className="config-select"
                  value={selectedNode.data[field.key] || field.default || ''}
                  onChange={(e) => onNodeConfigChange(selectedNode.id, field.key, e.target.value)}
                >
                  <option value="">请选择</option>
                  {field.options.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
              )}
            </div>
          ))}
        </div>

        {/* 表单权限配置（仅用户任务） */}
        {showFormConfig && (
          <div style={{ borderTop: '1px solid var(--border-color, #e8e8e8)', paddingTop: 8, marginTop: 4 }}>
            <div className="config-form-item">
              <label className="config-label">关联表单</label>
              <select
                className="config-select"
                value={selectedNode.data.formKey || ''}
                onChange={(e) => handleFormKeyChange(e.target.value)}
              >
                <option value="">不关联表单</option>
                {formList.map(f => (
                  <option key={f.formKey} value={f.formKey}>{f.formName} ({f.formKey})</option>
                ))}
              </select>
            </div>

            {selectedNode.data.formKey && (
              <>
                <div className="config-form-item">
                  <label className="config-label">节点级权限</label>
                  <select
                    className="config-select"
                    value={permissions.nodePermission || 'edit'}
                    onChange={(e) => updateNodePermission(e.target.value)}
                  >
                    <option value="edit">可编辑</option>
                    <option value="readonly">只读</option>
                    <option value="hidden">隐藏</option>
                  </select>
                </div>

                <div className="config-form-item">
                  <label className="config-label" style={{ cursor: 'pointer' }}
                    onClick={() => setShowPermissionPanel(!showPermissionPanel)}>
                    {showPermissionPanel ? '▼' : '▶'} 字段级权限 ({formFields.length} 个字段)
                  </label>
                </div>

                {showPermissionPanel && formFields.length > 0 && (
                  <div style={{ maxHeight: 200, overflowY: 'auto', padding: '0 4px' }}>
                    {formFields.map(field => {
                      const fieldKey = field.key || field.id || field.name
                      const fieldPerm = permissions.fields?.[fieldKey] || 'edit'
                      return (
                        <div key={fieldKey} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4, fontSize: 12 }}>
                          <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                            title={field.label || field.title || fieldKey}>
                            {field.label || field.title || fieldKey}
                          </span>
                          <select
                            style={{ width: 72, fontSize: 11, padding: '2px 4px', border: '1px solid #d9d9d9', borderRadius: 3 }}
                            value={fieldPerm}
                            onChange={(e) => updateFieldPermission(fieldKey, e.target.value)}
                          >
                            <option value="edit">编辑</option>
                            <option value="readonly">只读</option>
                            <option value="hidden">隐藏</option>
                          </select>
                        </div>
                      )
                    })}
                  </div>
                )}

                {/* 按钮权限 */}
                <div className="config-form-item" style={{ marginTop: 4 }}>
                  <label className="config-label">按钮权限</label>
                </div>
                {['submit', 'reject', 'transfer', 'delegate'].map(btn => {
                  const btnPerm = permissions.buttons?.[btn] || {}
                  return (
                    <div key={btn} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4, fontSize: 12, paddingLeft: 4 }}>
                      <span style={{ width: 50 }}>{btn}</span>
                      <label style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <input type="checkbox"
                          checked={btnPerm.visible !== false}
                          onChange={(e) => updateButtonPermission(btn, 'visible', e.target.checked)}
                        />
                        显示
                      </label>
                      <label style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <input type="checkbox"
                          checked={btnPerm.enabled !== false}
                          onChange={(e) => updateButtonPermission(btn, 'enabled', e.target.checked)}
                        />
                        可用
                      </label>
                    </div>
                  )
                })}
              </>
            )}
          </div>
        )}
      </div>
    )
  }

  // 选中了连线
  if (selectedEdge) {
    return (
      <div className="config-panel">
        <div className="config-title">连线条件配置</div>
        {EDGE_SCHEMA.map((field) => (
          <div key={field.key} className="config-form-item">
            <label className="config-label">{field.label}</label>
            {field.type === 'input' && (
              <input
                className="config-input"
                value={selectedEdge.data?.[field.key] || selectedEdge[field.key] || ''}
                placeholder={field.placeholder || ''}
                onChange={(e) => onEdgeConfigChange(selectedEdge.id, field.key, e.target.value)}
              />
            )}
            {field.type === 'select' && (
              <select
                className="config-select"
                value={selectedEdge.data?.[field.key] || field.default || ''}
                onChange={(e) => onEdgeConfigChange(selectedEdge.id, field.key, e.target.value)}
              >
                <option value="">请选择</option>
                {field.options.map((opt) => (
                  <option key={opt} value={opt}>{opt}</option>
                ))}
              </select>
            )}
          </div>
        ))}
      </div>
    )
  }

  // 未选中
  return (
    <div className="config-panel">
      <div className="config-title">属性面板</div>
      <div className="empty-state">
        <p>选中节点或连线查看配置</p>
      </div>
    </div>
  )
}

export default ConfigPanel
