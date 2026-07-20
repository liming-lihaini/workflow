import { NODE_TYPES_CONFIG, NODE_GROUPS } from '../nodeTypes'

function NodePalette({ onDragStart }) {
  return (
    <div className="node-palette">
      <div className="palette-title">节点面板</div>
      {Object.entries(NODE_GROUPS).map(([groupName, types]) => (
        <div key={groupName} className="palette-group">
          <div className="palette-group-title">{groupName}</div>
          {types.map((type) => {
            const config = NODE_TYPES_CONFIG[type]
            return (
              <div
                key={type}
                className="palette-item"
                draggable
                onDragStart={(e) => onDragStart(e, type)}
              >
                <div
                  className="palette-icon"
                  style={{ background: config.color }}
                >
                  {config.icon}
                </div>
                <span className="palette-label">{config.label}</span>
              </div>
            )
          })}
        </div>
      ))}
    </div>
  )
}

export default NodePalette
