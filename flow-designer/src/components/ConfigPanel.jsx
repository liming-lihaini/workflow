import { useState, useEffect, useRef } from 'react'
import { NODE_TYPES_CONFIG, EDGE_SCHEMA } from '../nodeTypes'
import { getFormAll, getForm, getUsersPage, getRoles, getWebhooks, createWebhook, updateWebhook, deleteWebhook } from '../api'

function ConfigPanel({ selectedNode, selectedEdge, processFormKey, onNodeConfigChange, onEdgeConfigChange }) {
  const [formList, setFormList] = useState([])
  const [formFields, setFormFields] = useState([])
  const [activeSection, setActiveSection] = useState('basic')
  const [userOptions, setUserOptions] = useState([])
  const [userSearchLoading, setUserSearchLoading] = useState(false)
  const [processFormName, setProcessFormName] = useState('')
  const [roleList, setRoleList] = useState([])
  const [webhookList, setWebhookList] = useState([])
  const [webhookFormVisible, setWebhookFormVisible] = useState(false)
  const [webhookForm, setWebhookForm] = useState({ webhookKey: '', name: '', url: '', method: 'POST', triggerEvents: [], timeout: 5000, retryCount: 3, processKey: '', nodeId: '' })
  const searchTimer = useRef(null)

  useEffect(() => {
    getFormAll().then(res => {
      const list = Array.isArray(res.data) ? res.data : (res || [])
      setFormList(list)
    }).catch(() => {})
    getRoles({ page: 1, size: 100 }).then(res => {
      const d = res.data || res
      setRoleList(Array.isArray(d) ? d : (d.records || d.list || []))
    }).catch(() => {})
  }, [])

  // 当流程定义绑定了表单时，获取表单名称
  useEffect(() => {
    if (processFormKey && formList.length > 0) {
      const form = formList.find(f => f.formKey === processFormKey)
      if (form) setProcessFormName(form.formName || processFormKey)
    }
  }, [processFormKey, formList])

  // 表单字段加载：优先使用流程级 formKey，否则使用节点级
  useEffect(() => {
    const effectiveFormKey = processFormKey || selectedNode?.data?.formKey
    if (effectiveFormKey) { loadFormFields(effectiveFormKey) }
    else { setFormFields([]) }
  }, [processFormKey, selectedNode?.data?.formKey])

  async function loadFormFields(formKey) {
    try {
      const res = await getForm(formKey)
      const form = res.data || res
      if (form?.formJson) {
        try {
          const formJson = JSON.parse(form.formJson)
          let fields = []
          if (formJson.sections) {
            formJson.sections.forEach(sec => {
              (sec.children || []).forEach(row => {
                (row.cells || []).forEach(cell => {
                  (cell.fields || []).forEach(f => {
                    fields.push({ key: f.field || f.id, label: f.label || f.field, type: f.type })
                  })
                })
              })
            })
          }
          if (fields.length === 0) fields = formJson.components || formJson.fields || []
          setFormFields(fields)
        } catch { setFormFields([]) }
      }
    } catch { setFormFields([]) }
  }

  function getFormPermissions() {
    if (!selectedNode?.data?.formPermissions) return {}
    if (typeof selectedNode.data.formPermissions === 'string') {
      try { return JSON.parse(selectedNode.data.formPermissions) } catch { return {} }
    }
    return selectedNode.data.formPermissions
  }

  function updateFieldPermission(fieldKey, permission) {
    const current = getFormPermissions()
    const fields = current.fields || {}
    fields[fieldKey] = permission
    onNodeConfigChange(selectedNode.id, 'formPermissions', { ...current, fields })
  }

  function updateButtonPermission(buttonKey, key, value) {
    const current = getFormPermissions()
    const buttons = current.buttons || {}
    buttons[buttonKey] = { ...buttons[buttonKey], [key]: value }
    onNodeConfigChange(selectedNode.id, 'formPermissions', { ...current, buttons })
  }

  function updateNodePermission(permission) {
    const current = getFormPermissions()
    onNodeConfigChange(selectedNode.id, 'formPermissions', { ...current, nodePermission: permission })
  }

  function handleFormKeyChange(newFormKey) {
    onNodeConfigChange(selectedNode.id, 'formKey', newFormKey)
    if (newFormKey) loadFormFields(newFormKey)
    else { setFormFields([]); onNodeConfigChange(selectedNode.id, 'formPermissions', null) }
  }

  // --- User remote search ---
  function handleUserSearch(keyword) {
    clearTimeout(searchTimer.current)
    if (!keyword) { setUserOptions([]); return }
    searchTimer.current = setTimeout(async () => {
      setUserSearchLoading(true)
      try {
        const res = await getUsersPage({ keyword, page: 1, size: 10 })
        const d = res.data || res
        const records = Array.isArray(d) ? d : (d.records || d.list || [])
        setUserOptions(records)
      } catch { setUserOptions([]) }
      finally { setUserSearchLoading(false) }
    }, 300)
  }

  // Multi-select assignees (comma separated)
  function getAssigneeList() {
    const val = selectedNode?.data?.candidateUsers || selectedNode?.data?.assignee || ''
    return val ? val.split(',').filter(Boolean) : []
  }

  function toggleAssignee(username) {
    let list = getAssigneeList()
    if (list.includes(username)) { list = list.filter(u => u !== username) }
    else { list.push(username) }
    const joined = list.join(',')
    onNodeConfigChange(selectedNode.id, 'candidateUsers', joined)
    onNodeConfigChange(selectedNode.id, 'assignee', joined)
  }

  function removeAssignee(username) {
    let list = getAssigneeList().filter(u => u !== username)
    const joined = list.join(',')
    onNodeConfigChange(selectedNode.id, 'candidateUsers', joined)
    onNodeConfigChange(selectedNode.id, 'assignee', joined)
  }

  const showFormConfig = selectedNode && ['userTask'].includes(selectedNode.data.nodeType)
  const isUserTask = selectedNode?.data?.nodeType === 'userTask'

  const SectionTabs = ({ tabs }) => (
    <div style={{ display: 'flex', borderBottom: '1px solid #e8e8e8', marginBottom: 8 }}>
      {tabs.map(t => (
        <div key={t.key} onClick={() => setActiveSection(t.key)}
          style={{
            padding: '6px 14px', fontSize: 12, cursor: 'pointer',
            borderBottom: activeSection === t.key ? '2px solid #1677ff' : '2px solid transparent',
            color: activeSection === t.key ? '#1677ff' : '#666',
            fontWeight: activeSection === t.key ? 600 : 400, transition: 'all 0.2s'
          }}>
          {t.label}
        </div>
      ))}
    </div>
  )

  if (selectedNode) {
    const config = NODE_TYPES_CONFIG[selectedNode.data.nodeType] || NODE_TYPES_CONFIG.custom
    const schema = config.schema
    const permissions = getFormPermissions()
    const assigneeType = selectedNode.data.assigneeType || 'user'

    const tabs = [{ key: 'basic', label: '基础配置' }]
    if (showFormConfig) {
      tabs.push({ key: 'formPerm', label: '表单权限' })
      tabs.push({ key: 'opPerm', label: '操作权限' })
    }
    tabs.push({ key: 'event', label: '事件定义' })
    tabs.push({ key: 'webhook', label: 'Webhook' })

    return (
      <div className="config-panel">
        <div className="config-title">
          {config.label}
          <span style={{ fontSize: 11, color: '#999', marginLeft: 8 }}>{selectedNode.data.nodeId}</span>
        </div>

        <SectionTabs tabs={tabs} />

        <div style={{ flex: 1, overflowY: 'auto', padding: '0 2px' }}>
          {/* ===== 基础配置 ===== */}
          {activeSection === 'basic' && (
            <div>
              {/* 通用字段 (名称、描述) */}
              <div className="config-form-item">
                <label className="config-label">节点名称 <span style={{ color: '#f53f3f' }}>*</span></label>
                <input className="config-input" value={selectedNode.data.name || ''}
                  placeholder="请输入节点名称"
                  onChange={(e) => onNodeConfigChange(selectedNode.id, 'name', e.target.value)} />
              </div>

              {/* userTask: 处理人配置 */}
              {isUserTask && (
                <>
                  <div className="config-form-item">
                    <label className="config-label">分配方式</label>
                    <select className="config-select" value={assigneeType}
                      onChange={(e) => {
                        onNodeConfigChange(selectedNode.id, 'assigneeType', e.target.value)
                        onNodeConfigChange(selectedNode.id, 'assignee', '')
                        onNodeConfigChange(selectedNode.id, 'candidateUsers', '')
                      }}>
                      <option value="user">指定用户</option>
                      <option value="role">指定角色</option>
                      <option value="dept">部门领导</option>
                      <option value="expression">表单表达式</option>
                    </select>
                  </div>

                  {/* user: 多用户远程搜索 */}
                  {assigneeType === 'user' && (
                    <div className="config-form-item">
                      <label className="config-label">处理人（支持多选）</label>
                      <input className="config-input" placeholder="输入姓名搜索用户"
                        onChange={(e) => handleUserSearch(e.target.value)} />
                      {userSearchLoading && <div style={{ fontSize: 11, color: '#999', padding: 4 }}>搜索中...</div>}
                      {userOptions.length > 0 && (
                        <div className="handler-dropdown">
                          {userOptions.map(u => {
                            const uname = u.username || u.id
                            const selected = getAssigneeList().includes(uname)
                            return (
                              <div key={uname} className="handler-dropdown-item" onClick={() => toggleAssignee(uname)}>
                                <span style={{ marginRight: 6, color: selected ? '#1677ff' : '#ccc' }}>{selected ? '✓' : '○'}</span>
                                {u.realName || u.username} ({u.username})
                              </div>
                            )
                          })}
                        </div>
                      )}
                      {/* 已选处理人标签 */}
                      <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                        {getAssigneeList().map(u => (
                          <span key={u} className="handler-tag">
                            {u}
                            <span className="handler-tag-close" onClick={() => removeAssignee(u)}>×</span>
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* role: 角色选择 */}
                  {assigneeType === 'role' && (
                    <div className="config-form-item">
                      <label className="config-label">选择角色</label>
                      <select className="config-select" value={selectedNode.data.assignee || ''}
                        onChange={(e) => {
                          onNodeConfigChange(selectedNode.id, 'assignee', e.target.value)
                          onNodeConfigChange(selectedNode.id, 'candidateUsers', e.target.value)
                        }}>
                        <option value="">请选择角色</option>
                        {roleList.map(r => (
                          <option key={r.roleKey || r.id} value={r.roleKey || r.id}>{r.roleName}</option>
                        ))}
                      </select>
                    </div>
                  )}

                  {/* dept: 部门领导 */}
                  {assigneeType === 'dept' && (
                    <div className="config-form-item">
                      <label className="config-label">领导类型</label>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                        <label className="handler-radio-label">
                          <input type="radio" name="deptType" checked={selectedNode.data.assignee === 'deptLeader'}
                            onChange={() => {
                              onNodeConfigChange(selectedNode.id, 'assignee', 'deptLeader')
                              onNodeConfigChange(selectedNode.id, 'candidateUsers', 'deptLeader')
                            }} />
                          直属部门领导
                        </label>
                        <label className="handler-radio-label">
                          <input type="radio" name="deptType" checked={selectedNode.data.assignee === 'parentDeptLeader'}
                            onChange={() => {
                              onNodeConfigChange(selectedNode.id, 'assignee', 'parentDeptLeader')
                              onNodeConfigChange(selectedNode.id, 'candidateUsers', 'parentDeptLeader')
                            }} />
                          二级部门领导
                        </label>
                      </div>
                    </div>
                  )}

                  {/* expression: 表达式 */}
                  {assigneeType === 'expression' && (
                    <div className="config-form-item">
                      <label className="config-label">处理人表达式</label>
                      <input className="config-input" value={selectedNode.data.assignee || ''}
                        placeholder="如: ${formData.approver}"
                        onChange={(e) => {
                          onNodeConfigChange(selectedNode.id, 'assignee', e.target.value)
                          onNodeConfigChange(selectedNode.id, 'candidateUsers', e.target.value)
                        }} />
                      <div style={{ fontSize: 11, color: '#999', marginTop: 4 }}>
                        从表单变量中提取处理人，如 ${formData.approver}
                      </div>
                    </div>
                  )}

                  <div className="config-form-item">
                    <label className="config-label">截止时间</label>
                    <input className="config-input" value={selectedNode.data.dueDate || ''}
                      placeholder="如: 2024-12-31"
                      onChange={(e) => onNodeConfigChange(selectedNode.id, 'dueDate', e.target.value)} />
                  </div>
                </>
              )}

              {/* 非 userTask 节点: 渲染其他 schema 字段 */}
              {!isUserTask && schema.filter(f => f.key !== 'name').map((field) => (
                <div key={field.key} className="config-form-item">
                  <label className="config-label">{field.label}</label>
                  {field.type === 'input' && (
                    <input className="config-input" value={selectedNode.data[field.key] || field.default || ''}
                      placeholder={field.placeholder || ''}
                      onChange={(e) => onNodeConfigChange(selectedNode.id, field.key, e.target.value)} />
                  )}
                  {field.type === 'textarea' && (
                    <textarea className="config-textarea" value={selectedNode.data[field.key] || ''}
                      placeholder={field.placeholder || ''}
                      onChange={(e) => onNodeConfigChange(selectedNode.id, field.key, e.target.value)} />
                  )}
                  {field.type === 'select' && (
                    <select className="config-select" value={selectedNode.data[field.key] || field.default || ''}
                      onChange={(e) => onNodeConfigChange(selectedNode.id, field.key, e.target.value)}>
                      <option value="">请选择</option>
                      {field.options.map((opt) => (<option key={opt} value={opt}>{opt}</option>))}
                    </select>
                  )}
                </div>
              ))}

              {/* 描述字段 */}
              <div className="config-form-item">
                <label className="config-label">描述</label>
                <textarea className="config-textarea" value={selectedNode.data.description || ''}
                  placeholder="节点描述"
                  onChange={(e) => onNodeConfigChange(selectedNode.id, 'description', e.target.value)} />
              </div>

              {/* 关联表单 */}
              {showFormConfig && (
                <div className="config-form-item">
                  <label className="config-label">关联表单</label>
                  {processFormKey ? (
                    <div style={{
                      padding: '6px 10px', background: '#f5f5f5', borderRadius: 4,
                      border: '1px solid #e8e8e8', fontSize: 13, color: '#333',
                      display: 'flex', alignItems: 'center', gap: 6
                    }}>
                      <span style={{ color: '#1677ff', fontSize: 11 }}>📋</span>
                      {processFormName || processFormKey}
                      <span style={{ fontSize: 11, color: '#999', marginLeft: 'auto' }}>继承自流程</span>
                    </div>
                  ) : (
                    <select className="config-select" value={selectedNode.data.formKey || ''}
                      onChange={(e) => handleFormKeyChange(e.target.value)}>
                      <option value="">不关联表单</option>
                      {formList.map(f => (<option key={f.formKey} value={f.formKey}>{f.formName}</option>))}
                    </select>
                  )}
                </div>
              )}
            </div>
          )}

          {/* ===== 表单权限 ===== */}
          {activeSection === 'formPerm' && showFormConfig && (
            <div>
              {!processFormKey && !selectedNode.data.formKey ? (
                <div style={{ padding: 16, textAlign: 'center', color: '#999', fontSize: 12 }}>请先在「基础配置」中关联表单</div>
              ) : (
                <>
                  <div className="config-form-item">
                    <label className="config-label">节点级权限</label>
                    <select className="config-select" value={permissions.nodePermission || 'edit'}
                      onChange={(e) => updateNodePermission(e.target.value)}>
                      <option value="edit">可编辑</option>
                      <option value="readonly">只读</option>
                      <option value="hidden">隐藏</option>
                    </select>
                  </div>
                  <div style={{ marginTop: 8 }}>
                    <div className="config-label" style={{ marginBottom: 6, fontWeight: 500 }}>字段级权限 ({formFields.length})</div>
                    <div style={{ maxHeight: 300, overflowY: 'auto' }}>
                      {formFields.length === 0 && <div style={{ color: '#999', fontSize: 12, padding: 8 }}>无字段</div>}
                      {formFields.map(field => {
                        const fieldKey = field.key || field.id || field.name
                        const fieldPerm = permissions.fields?.[fieldKey] || 'edit'
                        return (
                          <div key={fieldKey} className="perm-field-row">
                            <span className="perm-field-name" title={field.label || field.title || fieldKey}>
                              {field.label || field.title || fieldKey}
                            </span>
                            <div style={{ display: 'flex', gap: 2 }}>
                              {['edit', 'readonly', 'hidden'].map(p => (
                                <button key={p} onClick={() => updateFieldPermission(fieldKey, p)}
                                  style={{
                                    fontSize: 11, padding: '2px 8px', border: '1px solid', borderRadius: 3, cursor: 'pointer',
                                    borderColor: fieldPerm === p ? '#1677ff' : '#d9d9d9',
                                    background: fieldPerm === p ? '#1677ff' : '#fff',
                                    color: fieldPerm === p ? '#fff' : '#333',
                                  }}>
                                  {p === 'edit' ? '编辑' : p === 'readonly' ? '只读' : '隐藏'}
                                </button>
                              ))}
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {/* ===== 操作权限 ===== */}
          {activeSection === 'opPerm' && showFormConfig && (
            <div>
              {!processFormKey && !selectedNode.data.formKey ? (
                <div style={{ padding: 16, textAlign: 'center', color: '#999', fontSize: 12 }}>请先在「基础配置」中关联表单</div>
              ) : (
                <>
                  <div className="config-label" style={{ marginBottom: 8, fontWeight: 500 }}>按钮权限</div>
                  {['submit', 'reject', 'transfer', 'delegate'].map(btn => {
                    const btnPerm = permissions.buttons?.[btn] || {}
                    const btnLabel = { submit: '提交', reject: '驳回', transfer: '转办', delegate: '委派' }[btn] || btn
                    return (
                      <div key={btn} className="perm-btn-row">
                        <span className="perm-btn-name">{btnLabel}</span>
                        <label className="perm-checkbox-label">
                          <input type="checkbox" checked={btnPerm.visible !== false}
                            onChange={(e) => updateButtonPermission(btn, 'visible', e.target.checked)} />
                          显示
                        </label>
                        <label className="perm-checkbox-label">
                          <input type="checkbox" checked={btnPerm.enabled !== false}
                            onChange={(e) => updateButtonPermission(btn, 'enabled', e.target.checked)} />
                          可用
                        </label>
                      </div>
                    )
                  })}
                </>
              )}
            </div>
          )}

          {/* ===== 事件定义 ===== */}
          {activeSection === 'event' && selectedNode && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <div className="config-label" style={{ fontWeight: 500, margin: 0 }}>节点事件处理器</div>
                <button onClick={() => {
                  const events = selectedNode.data.events || []
                  onNodeConfigChange(selectedNode.id, 'events', [...events, {
                    id: 'evt_' + Date.now(), eventType: 'beforeEnter', language: 'groovy', script: ''
                  }])
                }} style={{ fontSize: 11, padding: '2px 10px', border: '1px solid #1677ff', background: '#fff', color: '#1677ff', borderRadius: 3, cursor: 'pointer' }}>
                  + 添加事件
                </button>
              </div>
              {(selectedNode.data.events || []).length === 0 && (
                <div style={{ color: '#999', fontSize: 12, padding: 16, textAlign: 'center' }}>暂无事件处理器</div>
              )}
              {(selectedNode.data.events || []).map((evt, idx) => (
                <div key={evt.id || idx} style={{ border: '1px solid #e8e8e8', borderRadius: 4, padding: 8, marginBottom: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                    <select value={evt.eventType} onChange={(e) => {
                      const events = [...(selectedNode.data.events || [])]
                      events[idx] = { ...events[idx], eventType: e.target.value }
                      onNodeConfigChange(selectedNode.id, 'events', events)
                    }} style={{ fontSize: 12, padding: '2px 6px', border: '1px solid #d9d9d9', borderRadius: 3 }}>
                      <option value="beforeEnter">节点进入前</option>
                      <option value="afterEnter">节点进入后</option>
                      <option value="afterComplete">节点通过后</option>
                      <option value="afterReject">节点驳回后</option>
                    </select>
                    <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                      <select value={evt.language} onChange={(e) => {
                        const events = [...(selectedNode.data.events || [])]
                        events[idx] = { ...events[idx], language: e.target.value }
                        onNodeConfigChange(selectedNode.id, 'events', events)
                      }} style={{ fontSize: 12, padding: '2px 6px', border: '1px solid #d9d9d9', borderRadius: 3 }}>
                        <option value="groovy">Groovy</option>
                        <option value="python">Python</option>
                      </select>
                      <button onClick={() => {
                        const events = [...(selectedNode.data.events || [])]
                        events.splice(idx, 1)
                        onNodeConfigChange(selectedNode.id, 'events', events)
                      }} style={{ fontSize: 11, color: '#f53f3f', background: 'none', border: 'none', cursor: 'pointer' }}>删除</button>
                    </div>
                  </div>
                  <textarea value={evt.script || ''} onChange={(e) => {
                    const events = [...(selectedNode.data.events || [])]
                    events[idx] = { ...events[idx], script: e.target.value }
                    onNodeConfigChange(selectedNode.id, 'events', events)
                  }} placeholder="输入脚本代码..." rows={4}
                    style={{ width: '100%', fontSize: 11, fontFamily: 'monospace', padding: 6, border: '1px solid #d9d9d9', borderRadius: 3, resize: 'vertical', boxSizing: 'border-box' }} />
                </div>
              ))}
              <div style={{ fontSize: 11, color: '#999', lineHeight: 1.6, padding: '8px 0' }}>
                <div>可用变量：processInstanceId, nodeId, variables</div>
                <div>事件触发顺序：beforeEnter → afterEnter → afterComplete/afterReject</div>
              </div>
            </div>
          )}

          {/* ===== Webhook ===== */}
          {activeSection === 'webhook' && selectedNode && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <div className="config-label" style={{ fontWeight: 500, margin: 0 }}>Webhook 配置</div>
                <button onClick={() => {
                  setWebhookForm({ webhookKey: '', name: '', url: '', method: 'POST', triggerEvents: ['NODE_ENTERED'], timeout: 5000, retryCount: 3, processKey: '', nodeId: selectedNode.data.nodeId || '' })
                  setWebhookFormVisible(true)
                }} style={{ fontSize: 11, padding: '2px 10px', border: '1px solid #1677ff', background: '#fff', color: '#1677ff', borderRadius: 3, cursor: 'pointer' }}>
                  + 新建
                </button>
              </div>
              {webhookFormVisible && (
                <div style={{ border: '1px solid #1677ff', borderRadius: 4, padding: 8, marginBottom: 8 }}>
                  <div className="config-form-item">
                    <label className="config-label">Webhook Key</label>
                    <input className="config-input" value={webhookForm.webhookKey}
                      onChange={(e) => setWebhookForm({ ...webhookForm, webhookKey: e.target.value })}
                      placeholder="唯一标识" />
                  </div>
                  <div className="config-form-item">
                    <label className="config-label">名称</label>
                    <input className="config-input" value={webhookForm.name}
                      onChange={(e) => setWebhookForm({ ...webhookForm, name: e.target.value })}
                      placeholder="Webhook名称" />
                  </div>
                  <div className="config-form-item">
                    <label className="config-label">回调URL</label>
                    <input className="config-input" value={webhookForm.url}
                      onChange={(e) => setWebhookForm({ ...webhookForm, url: e.target.value })}
                      placeholder="https://example.com/callback" />
                  </div>
                  <div className="config-form-item">
                    <label className="config-label">HTTP方法</label>
                    <select className="config-select" value={webhookForm.method}
                      onChange={(e) => setWebhookForm({ ...webhookForm, method: e.target.value })}>
                      <option value="POST">POST</option>
                      <option value="PUT">PUT</option>
                      <option value="GET">GET</option>
                    </select>
                  </div>
                  <div className="config-form-item">
                    <label className="config-label">触发事件</label>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                      {['PROCESS_STARTED', 'NODE_ENTERED', 'NODE_COMPLETED', 'PROCESS_COMPLETED'].map(ev => (
                        <label key={ev} style={{ fontSize: 11, display: 'flex', alignItems: 'center', gap: 2 }}>
                          <input type="checkbox" checked={webhookForm.triggerEvents.includes(ev)}
                            onChange={(e) => {
                              const events = e.target.checked
                                ? [...webhookForm.triggerEvents, ev]
                                : webhookForm.triggerEvents.filter(x => x !== ev)
                              setWebhookForm({ ...webhookForm, triggerEvents: events })
                            }} />
                          {ev.replace('PROCESS_', '').replace('NODE_', '')}
                        </label>
                      ))}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 4, marginTop: 8 }}>
                    <button onClick={async () => {
                      try {
                        if (webhookForm.webhookKey) {
                          await createWebhook(webhookForm)
                        }
                        setWebhookFormVisible(false)
                      } catch (e) { alert(e.message) }
                    }} style={{ fontSize: 11, padding: '4px 12px', background: '#1677ff', color: '#fff', border: 'none', borderRadius: 3, cursor: 'pointer' }}>保存</button>
                    <button onClick={() => setWebhookFormVisible(false)}
                      style={{ fontSize: 11, padding: '4px 12px', background: '#fff', border: '1px solid #d9d9d9', borderRadius: 3, cursor: 'pointer' }}>取消</button>
                  </div>
                </div>
              )}
              <div style={{ fontSize: 11, color: '#999', padding: '8px 0' }}>
                <div>Webhook 在流程事件发生时自动触发回调。</div>
                <div>绑定节点ID：{selectedNode.data.nodeId || selectedNode.id}</div>
              </div>
            </div>
          )}
        </div>
      </div>
    )
  }

  if (selectedEdge) {
    return (
      <div className="config-panel">
        <div className="config-title">连线条件配置</div>
        {EDGE_SCHEMA.map((field) => (
          <div key={field.key} className="config-form-item">
            <label className="config-label">{field.label}</label>
            {field.type === 'input' && (
              <input className="config-input" value={selectedEdge.data?.[field.key] || selectedEdge[field.key] || ''}
                placeholder={field.placeholder || ''}
                onChange={(e) => onEdgeConfigChange(selectedEdge.id, field.key, e.target.value)} />
            )}
            {field.type === 'select' && (
              <select className="config-select" value={selectedEdge.data?.[field.key] || field.default || ''}
                onChange={(e) => onEdgeConfigChange(selectedEdge.id, field.key, e.target.value)}>
                <option value="">请选择</option>
                {field.options.map((opt) => (<option key={opt} value={opt}>{opt}</option>))}
              </select>
            )}
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="config-panel">
      <div className="config-title">属性面板</div>
      <div className="empty-state"><p>选中节点或连线查看配置</p></div>
    </div>
  )
}

export default ConfigPanel
