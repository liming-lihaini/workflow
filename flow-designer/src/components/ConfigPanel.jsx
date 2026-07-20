import { NODE_TYPES_CONFIG, EDGE_SCHEMA } from '../nodeTypes'

function ConfigPanel({ selectedNode, selectedEdge, onNodeConfigChange, onEdgeConfigChange }) {
  // 选中了节点
  if (selectedNode) {
    const config = NODE_TYPES_CONFIG[selectedNode.data.nodeType] || NODE_TYPES_CONFIG.custom
    const schema = config.schema

    return (
      <div className="config-panel">
        <div className="config-title">
          {config.label} 配置
          <span style={{ fontSize: 12, color: 'var(--text-placeholder)', marginLeft: 8 }}>
            {selectedNode.data.nodeId}
          </span>
        </div>
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
