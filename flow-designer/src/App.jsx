import { useState, useCallback, useRef, useMemo, useEffect } from 'react'
import ReactFlow, {
  addEdge,
  useNodesState,
  useEdgesState,
  Controls,
  MiniMap,
  Background,
  ReactFlowProvider,
  MarkerType
} from 'reactflow'
import 'reactflow/dist/style.css'

import FlowNode from './components/FlowNode'
import NodePalette from './components/NodePalette'
import ConfigPanel from './components/ConfigPanel'
import { NODE_TYPES_CONFIG } from './nodeTypes'
import {
  createProcessDefinition,
  updateProcessDefinition,
  getProcessDefinition,
  importProcessDefinition
} from './api'
import request from './api'

import './index.css'

// 自定义节点类型映射
const nodeTypes = {
  start: FlowNode,
  end: FlowNode,
  userTask: FlowNode,
  serviceTask: FlowNode,
  scriptTask: FlowNode,
  exclusiveGateway: FlowNode,
  parallelGateway: FlowNode,
  inclusiveGateway: FlowNode,
  subProcess: FlowNode,
  custom: FlowNode
}

// 初始默认节点
const initialNodes = [
  {
    id: '1',
    type: 'start',
    position: { x: 300, y: 50 },
    data: { nodeType: 'start', nodeId: 'start_1', name: '开始' }
  },
  {
    id: '2',
    type: 'end',
    position: { x: 300, y: 400 },
    data: { nodeType: 'end', nodeId: 'end_1', name: '结束' }
  }
]

let nodeIdCounter = 3

function Designer() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [selectedNode, setSelectedNode] = useState(null)
  const [selectedEdge, setSelectedEdge] = useState(null)
  const [toast, setToast] = useState(null)
  const [definitionId, setDefinitionId] = useState(null)
  const [definitionName, setDefinitionName] = useState('未命名流程')
  const [processKey, setProcessKey] = useState(null)
  const [processFormKey, setProcessFormKey] = useState(null)
  const reactFlowWrapper = useRef(null)

  // 从 URL 参数加载已有流程定义
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const urlId = params.get('id')
    const urlProcessKey = params.get('processKey')

    if (urlId) {
      setDefinitionId(Number(urlId))
      loadExistingDefinition(Number(urlId))
    } else if (urlProcessKey) {
      setProcessKey(urlProcessKey)
      loadDefinitionByKey(urlProcessKey)
    }
  }, [])

  async function loadExistingDefinition(id) {
    try {
      const res = await getProcessDefinition(id)
      const def = res.data || res
      if (def.processName) setDefinitionName(def.processName)
      if (def.processKey) setProcessKey(def.processKey)
      if (def.processJson) {
        parseAndLoadJson(def.processJson)
      }
    } catch (e) {
      showToast('加载流程定义失败: ' + (e.message || ''), 'error')
    }
  }

  async function loadDefinitionByKey(key) {
    try {
      const res = await request.get(`/process/definitions/key/${key}`)
      const def = res.data || res
      if (def.id) setDefinitionId(def.id)
      if (def.processName) setDefinitionName(def.processName)
      if (def.processKey) setProcessKey(def.processKey)
      if (def.processJson) {
        parseAndLoadJson(def.processJson)
      }
    } catch {
      // If loading fails, start fresh with the processKey
    }
  }

  function parseAndLoadJson(jsonStr) {
    try {
      const data = JSON.parse(jsonStr)
      // 提取流程级 formKey
      if (data.formKey) {
        setProcessFormKey(data.formKey)
      }
      if (data.nodes && data.edges) {
        setNodes(data.nodes.map((n, i) => ({
          id: String(i + 1),
          type: n.type,
          position: { x: n.x || 200 + i * 50, y: n.y || 100 + i * 80 },
          data: {
            nodeId: n.id,
            nodeType: n.type,
            name: n.name,
            assignee: n.assignee,
            candidateUsers: n.candidateUsers,
            assigneeType: n.assigneeType,
            assigneeDeptId: n.assigneeDeptId,
            ...(n.properties || {})
          }
        })))
        setEdges(data.edges.map((e, i) => ({
          id: `e${i + 1}`,
          source: String(data.nodes.findIndex(n => n.id === e.source) + 1),
          target: String(data.nodes.findIndex(n => n.id === e.target) + 1),
          data: { condition: e.condition || '', name: e.label || '', priority: '1' },
          markerEnd: { type: MarkerType.ArrowClosed },
          animated: true
        })))
        nodeIdCounter = data.nodes.length + 10
      }
    } catch {
      // Invalid JSON, ignore
    }
  }

  // Toast 提示
  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }, [])

  // 节点拖拽开始
  const onDragStart = useCallback((event, nodeType) => {
    event.dataTransfer.setData('application/reactflow', nodeType)
    event.dataTransfer.effectAllowed = 'move'
  }, [])

  // 节点放置到画布
  const onDrop = useCallback(
    (event) => {
      event.preventDefault()
      const type = event.dataTransfer.getData('application/reactflow')
      if (!type) return

      const config = NODE_TYPES_CONFIG[type]
      const bounds = reactFlowWrapper.current.getBoundingClientRect()
      const position = {
        x: event.clientX - bounds.left - 60,
        y: event.clientY - bounds.top - 30
      }

      const newNode = {
        id: String(nodeIdCounter++),
        type,
        position,
        data: {
          nodeType: type,
          nodeId: `${type}_${nodeIdCounter}`,
          name: config.label,
          ...Object.fromEntries(
            config.schema
              .filter(f => f.default !== undefined)
              .map(f => [f.key, f.default])
          )
        }
      }

      setNodes((nds) => [...nds, newNode])
    },
    [setNodes]
  )

  const onDragOver = useCallback((event) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
  }, [])

  // 连线
  const onConnect = useCallback(
    (params) => {
      setEdges((eds) =>
        addEdge(
          {
            ...params,
            data: { condition: '', name: '', priority: '1' },
            markerEnd: { type: MarkerType.ArrowClosed },
            animated: true
          },
          eds
        )
      )
    },
    [setEdges]
  )

  // 选中节点
  const onNodeClick = useCallback((event, node) => {
    setSelectedNode(node)
    setSelectedEdge(null)
  }, [])

  // 选中连线
  const onEdgeClick = useCallback((event, edge) => {
    setSelectedEdge(edge)
    setSelectedNode(null)
  }, [])

  // 取消选中
  const onPaneClick = useCallback(() => {
    setSelectedNode(null)
    setSelectedEdge(null)
  }, [])

  // 更新节点配置
  const onNodeConfigChange = useCallback(
    (nodeId, key, value) => {
      setNodes((nds) =>
        nds.map((n) =>
          n.id === nodeId
            ? { ...n, data: { ...n.data, [key]: value } }
            : n
        )
      )
      setSelectedNode((prev) =>
        prev && prev.id === nodeId
          ? { ...prev, data: { ...prev.data, [key]: value } }
          : prev
      )
    },
    [setNodes]
  )

  // 更新连线配置
  const onEdgeConfigChange = useCallback(
    (edgeId, key, value) => {
      setEdges((eds) =>
        eds.map((e) =>
          e.id === edgeId
            ? { ...e, data: { ...e.data, [key]: value } }
            : e
        )
      )
      setSelectedEdge((prev) =>
        prev && prev.id === edgeId
          ? { ...prev, data: { ...prev.data, [key]: value } }
          : prev
      )
    },
    [setEdges]
  )

  // 删除选中节点
  const handleDelete = useCallback(() => {
    if (selectedNode) {
      setNodes((nds) => nds.filter((n) => n.id !== selectedNode.id))
      setSelectedNode(null)
    }
    if (selectedEdge) {
      setEdges((eds) => eds.filter((e) => e.id !== selectedEdge.id))
      setSelectedEdge(null)
    }
  }, [selectedNode, selectedEdge, setNodes, setEdges])

  // 校验流程
  const validateProcess = useCallback(() => {
    const errors = []
    const hasStart = nodes.some((n) => n.data.nodeType === 'start')
    const hasEnd = nodes.some((n) => n.data.nodeType === 'end')

    if (!hasStart) errors.push('缺少开始节点')
    if (!hasEnd) errors.push('缺少结束节点')

    // 检查必填项
    nodes.forEach((n) => {
      const config = NODE_TYPES_CONFIG[n.data.nodeType]
      if (config) {
        config.schema.forEach((field) => {
          if (field.required && !n.data[field.key]) {
            errors.push(`节点 "${n.data.name || config.label}" 缺少必填项: ${field.label}`)
          }
        })
      }
    })

    return errors
  }, [nodes])

  // 导出 JSON
  const exportJSON = useCallback(() => {
    const processJson = {
      processKey: definitionName.replace(/\s+/g, '-').toLowerCase(),
      name: definitionName,
      nodes: nodes.map((n) => ({
        nodeId: n.data.nodeId,
        nodeType: n.data.nodeType,
        name: n.data.name,
        position: n.position,
        config: Object.fromEntries(
          Object.entries(n.data).filter(([k]) => !['nodeType', 'nodeId', 'name'].includes(k))
        )
      })),
      edges: edges.map((e) => ({
        source: e.source,
        target: e.target,
        condition: e.data?.condition || '',
        name: e.data?.name || '',
        priority: e.data?.priority || '1'
      }))
    }

    const json = JSON.stringify(processJson, null, 2)
    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${processJson.processKey}.json`
    a.click()
    URL.revokeObjectURL(url)
    showToast('导出成功')
    return processJson
  }, [nodes, edges, definitionName, showToast])

  // 导入 JSON
  const importJSON = useCallback(() => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.json'
    input.onchange = async (e) => {
      const file = e.target.files[0]
      if (!file) return
      const text = await file.text()
      try {
        const data = JSON.parse(text)
        if (data.nodes && data.edges) {
          setNodes(data.nodes.map((n, i) => ({
            id: String(i + 1),
            type: n.nodeType,
            position: n.position || { x: 200 + i * 50, y: 100 + i * 80 },
            data: {
              nodeId: n.nodeId,
              nodeType: n.nodeType,
              name: n.name,
              ...n.config
            }
          })))
          setEdges(data.edges.map((e, i) => ({
            id: `e${i + 1}`,
            source: e.source,
            target: e.target,
            data: { condition: e.condition, name: e.name, priority: e.priority },
            markerEnd: { type: MarkerType.ArrowClosed },
            animated: true
          })))
          if (data.name) setDefinitionName(data.name)
          showToast('导入成功')
        } else {
          showToast('JSON 格式不正确', 'error')
        }
      } catch {
        showToast('JSON 解析失败', 'error')
      }
    }
    input.click()
  }, [setNodes, setEdges, showToast])

  // 保存到后端
  const handleSave = useCallback(async () => {
    const errors = validateProcess()
    if (errors.length > 0) {
      showToast(errors.join('; '), 'error')
      return
    }

    const pk = processKey || definitionName.replace(/\s+/g, '-').toLowerCase()

    // 构建符合后端 ProcessModel 的 JSON 结构
    const processModel = {
      processKey: pk,
      processName: definitionName,
      nodes: nodes.map((n) => ({
        id: n.data.nodeId,
        type: n.data.nodeType,
        name: n.data.name,
        assignee: n.data.assignee || null,
        candidateUsers: n.data.candidateUsers || null,
        assigneeType: n.data.assigneeType || null,
        assigneeDeptId: n.data.assigneeDeptId || null,
        properties: Object.fromEntries(
          Object.entries(n.data).filter(([k]) => !['nodeType', 'nodeId', 'name', 'assignee', 'candidateUsers', 'assigneeType', 'assigneeDeptId'].includes(k))
        ),
        x: Math.round(n.position.x),
        y: Math.round(n.position.y)
      })),
      edges: edges.map((e, i) => {
        // 将 ReactFlow 内部节点 ID 映射为 nodeId
        const sourceNode = nodes.find((n) => n.id === e.source)
        const targetNode = nodes.find((n) => n.id === e.target)
        return {
          id: `edge_${i + 1}`,
          source: sourceNode ? sourceNode.data.nodeId : e.source,
          target: targetNode ? targetNode.data.nodeId : e.target,
          label: e.data?.name || '',
          condition: e.data?.condition || ''
        }
      })
    }

    try {
      if (definitionId) {
        await updateProcessDefinition(definitionId, {
          processName: definitionName,
          processJson: JSON.stringify(processModel)
        })
        showToast('保存成功')
      } else {
        const res = await createProcessDefinition({
          processKey: pk,
          processName: definitionName,
          processJson: JSON.stringify(processModel)
        })
        const newId = res.data?.id || res.id
        setDefinitionId(newId)
        if (!processKey) setProcessKey(pk)
        showToast('创建成功')
      }
    } catch (err) {
      showToast(err.message || '保存失败，请检查后端服务', 'error')
    }
  }, [definitionId, definitionName, processKey, nodes, edges, validateProcess, showToast])

  // 键盘事件
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === 'Delete' || e.key === 'Backspace') {
        // 避免在输入框中触发
        if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return
        handleDelete()
      }
    },
    [handleDelete]
  )

  const flowNodes = useMemo(() => nodes, [nodes])

  return (
    <div className="designer-app" onKeyDown={handleKeyDown} tabIndex={0}>
      {/* 工具栏 */}
      <div className="designer-toolbar">
        <div className="toolbar-left">
          <span className="toolbar-title">流程设计器</span>
          <input
            className="config-input"
            style={{ width: 200, height: 28 }}
            value={definitionName}
            onChange={(e) => setDefinitionName(e.target.value)}
            placeholder="流程名称"
          />
        </div>
        <div className="toolbar-right">
          <button className="btn" onClick={importJSON}>导入</button>
          <button className="btn" onClick={exportJSON}>导出</button>
          <button className="btn" onClick={() => {
            const errors = validateProcess()
            if (errors.length === 0) {
              showToast('校验通过')
            } else {
              showToast(errors.join('; '), 'error')
            }
          }}>校验</button>
          <button className="btn btn-primary" onClick={handleSave}>保存</button>
          <button className="btn btn-danger" onClick={handleDelete}>删除</button>
        </div>
      </div>

      {/* 主内容区 */}
      <div className="designer-main">
        {/* 左侧节点面板 */}
        <NodePalette onDragStart={onDragStart} />

        {/* 画布 */}
        <div className="canvas-wrap" ref={reactFlowWrapper}>
          <ReactFlow
            nodes={flowNodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onDrop={onDrop}
            onDragOver={onDragOver}
            onNodeClick={onNodeClick}
            onEdgeClick={onEdgeClick}
            onPaneClick={onPaneClick}
            nodeTypes={nodeTypes}
            fitView
            snapToGrid
            snapGrid={[15, 15]}
          >
            <Controls />
            <MiniMap />
            <Background gap={16} />
          </ReactFlow>
        </div>

        {/* 右侧配置面板 */}
        <ConfigPanel
          selectedNode={selectedNode}
          selectedEdge={selectedEdge}
          processFormKey={processFormKey}
          onNodeConfigChange={onNodeConfigChange}
          onEdgeConfigChange={onEdgeConfigChange}
        />
      </div>

      {/* Toast 提示 */}
      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.message}
        </div>
      )}
    </div>
  )
}

function App() {
  return (
    <ReactFlowProvider>
      <Designer />
    </ReactFlowProvider>
  )
}

export default App
