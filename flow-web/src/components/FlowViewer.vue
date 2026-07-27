<template>
  <div class="flow-viewer" ref="containerRef">
    <VueFlow
      :nodes="vfNodes"
      :edges="vfEdges"
      :node-types="nodeTypes"
      :fit-view-on-init="true"
      :pan-on-drag="true"
      :zoom-on-scroll="true"
      :zoom-on-pinch="true"
      :zoom-on-double-click="false"
      :selection-on-drag="false"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :pan-on-scroll="true"
      :min-zoom="0.3"
      :max-zoom="2"
      :default-viewport="{ x: 0, y: 0, zoom: 0.8 }"
      class="vue-flow-container"
    >
      <template #node-flowNode="{ data }">
        <div
          :class="['fv-node', data.isGateway ? 'fv-gateway' : '', data.statusClass || '']"
          :style="{ borderColor: data.borderColor }"
        >
          <div class="fv-node-header">
            <div class="fv-node-icon" :style="{ background: data.color }">{{ data.icon }}</div>
            <div>
              <div class="fv-node-label">{{ data.name }}</div>
              <div class="fv-node-type">{{ data.typeLabel }}</div>
            </div>
          </div>
        </div>
      </template>
    </VueFlow>
    <div class="fv-controls">
      <button class="fv-btn" @click="zoomIn" title="放大">＋</button>
      <button class="fv-btn" @click="zoomOut" title="缩小">－</button>
      <button class="fv-btn" @click="fitView" title="适应窗口">⊡</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, markRaw, h } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

const props = defineProps({
  /** processJson 中的 nodes 数组 */
  nodes: { type: Array, default: () => [] },
  /** processJson 中的 edges 数组 */
  edges: { type: Array, default: () => [] },
  /** 节点状态 Map: nodeId -> 'completed' | 'current' | 'pending' */
  nodeStatus: { type: Object, default: () => ({}) }
})

// 节点类型配置（与 flow-designer 一致）
const NODE_CONFIG = {
  start:             { label: '开始节点', color: '#00b42a', icon: '▶' },
  end:               { label: '结束节点', color: '#f53f3f', icon: '■' },
  userTask:          { label: '用户任务', color: '#1677ff', icon: '👤' },
  serviceTask:       { label: '服务任务', color: '#722ed1', icon: '⚙' },
  scriptTask:        { label: '脚本任务', color: '#13c2c2', icon: '📝' },
  exclusiveGateway:  { label: '排他网关', color: '#ff7d00', icon: '✕' },
  parallelGateway:   { label: '并行网关', color: '#faad14', icon: '＋' },
  inclusiveGateway:  { label: '包容网关', color: '#eb2f96', icon: '⊕' },
  subProcess:        { label: '子流程',   color: '#2f54eb', icon: '📋' },
  custom:            { label: '自定义',   color: '#86909c', icon: '⬡' }
}

// 简单自动布局：如果节点没有 position 或 position 全为 0，则自动排列
function autoLayout(nodes, edges) {
  const hasPosition = nodes.some(n => n.position && (n.position.x !== 0 || n.position.y !== 0))
  if (hasPosition) return nodes

  // 拓扑排序
  const adj = {}, inDeg = {}, nodeMap = {}
  nodes.forEach(n => {
    const id = n.id || n.nodeId
    adj[id] = []; inDeg[id] = 0; nodeMap[id] = n
  })
  edges.forEach(e => {
    const src = e.source || e.from, tgt = e.target || e.to
    if (src && tgt && adj[src]) { adj[src].push(tgt); inDeg[tgt] = (inDeg[tgt] || 0) + 1 }
  })
  const queue = [], sorted = []
  Object.keys(inDeg).forEach(id => { if (inDeg[id] === 0) queue.push(id) })
  while (queue.length) {
    const id = queue.shift(); sorted.push(id);
    (adj[id] || []).forEach(next => { inDeg[next]--; if (inDeg[next] === 0) queue.push(next) })
  }
  nodes.forEach(n => { const id = n.id || n.nodeId; if (!sorted.includes(id)) sorted.push(id) })

  // 按层级布局
  const layerMap = {}
  sorted.forEach((id, idx) => {
    const parents = edges.filter(e => (e.target || e.to) === id).map(e => e.source || e.from)
    if (parents.length === 0) {
      layerMap[id] = 0
    } else {
      layerMap[id] = Math.max(...parents.map(p => (layerMap[p] ?? 0) + 1), 0)
    }
  })

  const layers = {}
  sorted.forEach(id => {
    const layer = layerMap[id] || 0
    if (!layers[layer]) layers[layer] = []
    layers[layer].push(id)
  })

  const xGap = 220, yGap = 120
  return sorted.map((id, idx) => {
    const layer = layerMap[id] || 0
    const posInLayer = (layers[layer] || []).indexOf(id)
    const layerSize = (layers[layer] || []).length
    const y = posInLayer * yGap - (layerSize - 1) * yGap / 2
    return {
      ...nodeMap[id],
      position: { x: layer * xGap + 50, y: y + 200 }
    }
  })
}

const { zoomIn: vfZoomIn, zoomOut: vfZoomOut, fitView: vfFitView } = useVueFlow()

const layoutNodes = computed(() => autoLayout(props.nodes, props.edges))

const vfNodes = computed(() => {
  return layoutNodes.value.map(n => {
    const id = n.id || n.nodeId
    const type = n.type || n.nodeType || 'custom'
    const config = NODE_CONFIG[type] || NODE_CONFIG.custom
    const status = props.nodeStatus[id] || 'pending'
    const isGateway = type.includes('Gateway')
    let statusClass = ''
    if (status === 'completed') statusClass = 'fv-completed'
    else if (status === 'current') statusClass = 'fv-current'

    return {
      id,
      type: 'flowNode',
      position: n.position || { x: 0, y: 0 },
      data: {
        name: n.name || config.label,
        typeLabel: config.label,
        color: config.color,
        icon: config.icon,
        isGateway,
        statusClass,
        borderColor: status === 'current' ? '#0052a5' : status === 'completed' ? '#00a854' : config.color
      }
    }
  })
})

const vfEdges = computed(() => {
  return props.edges.map(e => ({
    id: e.id || `e-${e.source}-${e.target}`,
    source: e.source || e.from,
    target: e.target || e.to,
    type: 'smoothstep',
    animated: false,
    style: { stroke: '#b1b1b7', strokeWidth: 2 },
    markerEnd: { type: 'arrowclosed', color: '#b1b1b7', width: 16, height: 16 }
  }))
})

const nodeTypes = { flowNode: 'flowNode' }

function zoomIn() { vfZoomIn({ duration: 200 }) }
function zoomOut() { vfZoomOut({ duration: 200 }) }
function fitView() { vfFitView({ padding: 0.2, duration: 200 }) }
</script>

<style scoped>
.flow-viewer {
  position: relative;
  width: 100%;
  height: 500px;
  border: 1px solid #e1e6ef;
  border-radius: 4px;
  overflow: hidden;
  background: #fafbfd;
}

.vue-flow-container {
  width: 100%;
  height: 100%;
}

/* 控制按钮 */
.fv-controls {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  gap: 4px;
  z-index: 10;
}
.fv-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #d0d7df;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #555;
  transition: all 0.2s;
}
.fv-btn:hover {
  background: #e1ecf9;
  border-color: #0052a5;
  color: #0052a5;
}

/* 节点样式（与 flow-designer 一致） */
.fv-node {
  padding: 10px 16px;
  border-radius: 6px;
  background: #fff;
  border: 2px solid #d0d7df;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  min-width: 120px;
  transition: box-shadow 0.2s;
}
.fv-node:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}
.fv-node-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.fv-node-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #fff;
  flex-shrink: 0;
}
.fv-node-label {
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
}
.fv-node-type {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

/* 网关菱形 */
.fv-gateway {
  border-radius: 0;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
  padding: 24px;
}

/* 状态着色 */
.fv-completed {
  border-color: #00a854 !important;
  background: #e6fffb;
}
.fv-current {
  border-color: #0052a5 !important;
  background: #e1ecf9;
  box-shadow: 0 0 0 3px rgba(0, 82, 165, 0.15);
}
</style>
