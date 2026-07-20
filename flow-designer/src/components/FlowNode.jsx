import { memo } from 'react'
import { Handle, Position } from 'reactflow'
import { NODE_TYPES_CONFIG } from '../nodeTypes'

function FlowNode({ data, selected }) {
  const config = NODE_TYPES_CONFIG[data.nodeType] || NODE_TYPES_CONFIG.custom
  const isGateway = data.nodeType?.includes('Gateway')

  return (
    <>
      {data.nodeType !== 'start' && (
        <Handle type="target" position={Position.Top} />
      )}
      <div
        className={`flow-node ${selected ? 'selected' : ''} ${isGateway ? 'gateway' : ''}`}
        style={{ borderColor: selected ? 'var(--color-primary)' : config.color }}
      >
        <div className="flow-node-header">
          <div
            className="flow-node-icon"
            style={{ background: config.color }}
          >
            {config.icon}
          </div>
          <div>
            <div className="flow-node-label">{data.name || config.label}</div>
            <div className="flow-node-type">{config.label}</div>
          </div>
        </div>
      </div>
      {data.nodeType !== 'end' && (
        <Handle type="source" position={Position.Bottom} />
      )}
    </>
  )
}

export default memo(FlowNode)
