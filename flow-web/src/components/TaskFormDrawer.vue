<template>
  <a-drawer
    :open="open"
    :title="null"
    :closable="true"
    width="720"
    :destroy-on-close="true"
    @close="$emit('cancel')"
    class="task-form-drawer"
  >
    <!-- ====== 头部 ====== -->
    <div class="drawer-header">
      <div class="header-title">{{ processInfo?.processName || '流程' }}</div>
      <div class="header-meta">
        <span class="meta-left">
          <span class="meta-key">{{ processInfo?.processKey || '-' }}</span>
          <a-divider type="vertical" />
          <span>{{ processInfo?.startTime || formatNow() }}</span>
        </span>
        <span class="meta-right">
          <span>{{ processInfo?.startUser || '-' }}</span>
          <a-divider type="vertical" />
          <span>{{ processInfo?.deptName || '-' }}</span>
        </span>
      </div>
    </div>
    <a-divider style="margin: 0 0 8px" />

    <!-- ====== Tab 页 ====== -->
    <a-tabs v-model:activeKey="activeTab" class="drawer-tabs">
      <a-tab-pane key="form" tab="表单">
        <a-spin :spinning="loading">
          <div class="drawer-content">
            <a-form layout="vertical" :model="formValues" v-if="formFields.length > 0">
              <a-form-item
                v-for="field in formFields"
                :key="field.key || field.id"
                :label="field.label"
                :required="field.required"
              >
                <a-input v-if="field.type === 'text' || !field.type"
                  v-model:value="formValues[field.key]"
                  :placeholder="field.placeholder || ('请输入' + field.label)" allow-clear />
                <a-textarea v-else-if="field.type === 'textarea'"
                  v-model:value="formValues[field.key]"
                  :placeholder="field.placeholder || ('请输入' + field.label)" :rows="3" allow-clear />
                <a-input-number v-else-if="field.type === 'number' || field.type === 'amount'"
                  v-model:value="formValues[field.key]"
                  :placeholder="field.placeholder || ('请输入' + field.label)"
                  style="width: 100%" :precision="field.type === 'amount' ? 2 : 0" :min="0" />
                <a-date-picker v-else-if="field.type === 'date'"
                  v-model:value="formValues[field.key]" style="width: 100%"
                  value-format="YYYY-MM-DD" :placeholder="field.placeholder || ('请选择' + field.label)" />
                <a-time-picker v-else-if="field.type === 'time'"
                  v-model:value="formValues[field.key]" style="width: 100%"
                  value-format="HH:mm:ss" :placeholder="field.placeholder || ('请选择' + field.label)" />
                <a-date-picker v-else-if="field.type === 'datetime'"
                  v-model:value="formValues[field.key]" style="width: 100%" show-time
                  value-format="YYYY-MM-DD HH:mm:ss" :placeholder="field.placeholder || ('请选择' + field.label)" />
                <a-select v-else-if="field.type === 'select'"
                  v-model:value="formValues[field.key]"
                  :placeholder="field.placeholder || ('请选择' + field.label)" allow-clear>
                  <a-select-option v-for="opt in getFieldOptions(field)" :key="opt.value" :value="opt.value">
                    {{ opt.text }}
                  </a-select-option>
                </a-select>
                <a-radio-group v-else-if="field.type === 'radio'" v-model:value="formValues[field.key]">
                  <a-radio v-for="opt in getFieldOptions(field)" :key="opt.value" :value="opt.value">{{ opt.text }}</a-radio>
                </a-radio-group>
                <a-checkbox-group v-else-if="field.type === 'checkbox'" v-model:value="formValues[field.key]">
                  <a-checkbox v-for="opt in getFieldOptions(field)" :key="opt.value" :value="opt.value">{{ opt.text }}</a-checkbox>
                </a-checkbox-group>
                <a-input v-else v-model:value="formValues[field.key]"
                  :placeholder="field.placeholder || ('请输入' + field.label)" allow-clear />
              </a-form-item>
            </a-form>
            <a-empty v-else-if="!loading" description="暂无表单字段" />
          </div>
        </a-spin>
      </a-tab-pane>

      <!-- 流程图 Tab -->
      <a-tab-pane key="flow" tab="流程图">
        <div class="flow-diagram-wrap">
          <div class="flow-legend">
            <span class="legend-item"><span class="legend-dot dot-current"></span>当前节点</span>
            <span class="legend-item"><span class="legend-dot dot-completed"></span>已办结</span>
            <span class="legend-item"><span class="legend-dot dot-pending"></span>未执行</span>
          </div>
          <div class="flow-canvas" ref="flowCanvasRef">
            <svg :width="svgWidth" :height="svgHeight" class="flow-svg">
              <!-- 箭头标记定义 -->
              <defs>
                <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
                  <polygon points="0 0, 10 3.5, 0 7" fill="#bbb" />
                </marker>
                <marker id="arrowhead-completed" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
                  <polygon points="0 0, 10 3.5, 0 7" fill="#52c41a" />
                </marker>
                <marker id="arrowhead-current" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
                  <polygon points="0 0, 10 3.5, 0 7" fill="#1677ff" />
                </marker>
              </defs>

              <!-- 连线 -->
              <g v-for="(edge, i) in computedEdges" :key="'edge-' + i">
                <line
                  :x1="edge.x1" :y1="edge.y1"
                  :x2="edge.x2" :y2="edge.y2"
                  :stroke="edge.color"
                  stroke-width="2"
                  :marker-end="edge.markerEnd"
                />
                <text v-if="edge.label"
                  :x="(edge.x1 + edge.x2) / 2"
                  :y="(edge.y1 + edge.y2) / 2 - 6"
                  text-anchor="middle" font-size="11" fill="#999"
                >{{ edge.label }}</text>
              </g>

              <!-- 节点 -->
              <g v-for="(node, i) in layoutNodes" :key="'node-' + i">
                <!-- 开始节点 - 圆形 -->
                <circle v-if="node.nodeType === 'start'"
                  :cx="node.cx" :cy="node.cy" r="18"
                  :fill="node.fillColor" :stroke="node.strokeColor" stroke-width="2"
                />
                <text v-if="node.nodeType === 'start'"
                  :x="node.cx" :y="node.cy + 4"
                  text-anchor="middle" font-size="16" fill="#fff" font-weight="bold"
                >▶</text>

                <!-- 结束节点 - 圆形 -->
                <circle v-else-if="node.nodeType === 'end'"
                  :cx="node.cx" :cy="node.cy" r="18"
                  :fill="node.fillColor" :stroke="node.strokeColor" stroke-width="3"
                />
                <text v-else-if="node.nodeType === 'end'"
                  :x="node.cx" :y="node.cy + 5"
                  text-anchor="middle" font-size="16" fill="#fff" font-weight="bold"
                >■</text>

                <!-- 网关 - 菱形 -->
                <polygon v-else-if="isGateway(node.nodeType)"
                  :points="node.diamondPoints"
                  :fill="node.fillColor" :stroke="node.strokeColor" stroke-width="2"
                />
                <text v-if="isGateway(node.nodeType)"
                  :x="node.cx" :y="node.cy + 4"
                  text-anchor="middle" font-size="14" fill="#fff" font-weight="bold"
                >{{ gatewayIcon(node.nodeType) }}</text>

                <!-- 其他节点 - 圆角矩形 -->
                <rect v-else
                  :x="node.cx - 60" :y="node.cy - 22"
                  width="120" height="44" rx="8" ry="8"
                  :fill="node.fillColor" :stroke="node.strokeColor" stroke-width="2"
                />
                <text v-if="!isGateway(node.nodeType) && node.nodeType !== 'start' && node.nodeType !== 'end'"
                  :x="node.cx" :y="node.cy + 1"
                  text-anchor="middle" font-size="12" fill="#fff" font-weight="500"
                  dominant-baseline="middle"
                >{{ node.name || node.nodeType }}</text>

                <!-- 节点名称标签（在下方） -->
                <text
                  :x="node.cx"
                  :y="node.nodeType === 'start' || node.nodeType === 'end' ? node.cy + 32 : node.cy + 34"
                  text-anchor="middle" font-size="11" :fill="node.strokeColor"
                >{{ node.name || nodeLabel(node.nodeType) }}</text>

                <!-- 状态标签 -->
                <text
                  v-if="node.status === 'completed'"
                  :x="node.cx"
                  :y="node.nodeType === 'start' || node.nodeType === 'end' ? node.cy + 46 : node.cy + 48"
                  text-anchor="middle" font-size="10" fill="#52c41a"
                >✓ 已办结</text>
                <text
                  v-else-if="node.status === 'current'"
                  :x="node.cx"
                  :y="node.nodeType === 'start' || node.nodeType === 'end' ? node.cy + 46 : node.cy + 48"
                  text-anchor="middle" font-size="10" fill="#1677ff" font-weight="bold"
                >● 当前</text>
              </g>
            </svg>
          </div>
          <a-empty v-if="!flowNodes || flowNodes.length === 0" description="暂无流程图数据" style="margin-top: 40px" />
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- ====== 底部操作区 ====== -->
    <template #footer>
      <!-- 开始节点：提交 / 保存 / 取消 -->
      <div v-if="mode === 'start'" class="drawer-footer footer-start">
        <a-button @click="$emit('cancel')">取消</a-button>
        <a-button @click="$emit('save', { ...formValues })">保存草稿</a-button>
        <a-button type="primary" @click="handleSubmit" :loading="submitLoading">提交</a-button>
      </div>

      <!-- 审批节点：结论 + 意见 + 操作 -->
      <div v-else-if="mode === 'approval'" class="drawer-footer footer-approval">
        <a-form layout="vertical" style="width: 100%">
          <a-form-item label="审批结论" required style="margin-bottom: 8px">
            <a-radio-group v-model:value="approvalConclusion">
              <a-radio value="approve"><a-tag color="green">通过</a-tag></a-radio>
              <a-radio value="reject"><a-tag color="red">驳回</a-tag></a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="approvalConclusion === 'reject' ? '驳回原因' : '审批意见'" required style="margin-bottom: 8px">
            <a-textarea v-model:value="approvalComment" :rows="3"
              :placeholder="approvalConclusion === 'reject' ? '请输入驳回原因（必填）' : '请输入审批意见（必填）'" />
          </a-form-item>
        </a-form>
        <div class="approval-actions">
          <a-button @click="$emit('cancel')">取消</a-button>
          <a-button v-if="approvalConclusion === 'reject'" danger @click="handleReject" :loading="submitLoading">
            驳回
          </a-button>
          <a-button v-else type="primary" @click="handleApprove" :loading="submitLoading">
            通过
          </a-button>
        </div>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'

const NODE_COLORS = {
  current:  { fill: '#1677ff', stroke: '#0958d9' },
  completed:{ fill: '#52c41a', stroke: '#389e0d' },
  pending:  { fill: '#d9d9d9', stroke: '#bfbfbf' }
}
const NODE_LABELS = {
  start: '开始', end: '结束', userTask: '用户任务', serviceTask: '服务任务',
  scriptTask: '脚本任务', exclusiveGateway: '排他网关', parallelGateway: '并行网关',
  inclusiveGateway: '包容网关', subProcess: '子流程', custom: '自定义'
}

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'start' },
  loading: { type: Boolean, default: false },
  processInfo: { type: Object, default: () => ({}) },
  formFields: { type: Array, default: () => [] },
  initialValues: { type: Object, default: () => ({}) },
  taskInfo: { type: Object, default: () => ({}) },
  flowNodes: { type: Array, default: () => [] },
  flowEdges: { type: Array, default: () => [] }
})

const emit = defineEmits(['submit', 'save', 'cancel', 'approve', 'reject', 'update:open'])

const activeTab = ref('form')
const formValues = ref({})
const submitLoading = ref(false)
const approvalConclusion = ref('approve')
const approvalComment = ref('')

watch(() => props.initialValues, (val) => {
  formValues.value = { ...val }
}, { deep: true, immediate: true })

watch(() => props.open, (val) => {
  if (val) {
    formValues.value = { ...props.initialValues }
    approvalConclusion.value = 'approve'
    approvalComment.value = ''
    submitLoading.value = false
    activeTab.value = 'form'
  }
})

function formatNow() { return new Date().toLocaleString('zh-CN') }

function getFieldOptions(field) {
  if (field.optionsSource === 'manual' && field.optionsText) {
    return field.optionsText.split('\n').filter(Boolean).map(line => {
      const [v, ...r] = line.split(':')
      return { value: v.trim(), text: r.join(':').trim() || v.trim() }
    })
  }
  if (field.options && Array.isArray(field.options)) {
    return field.options.map(o => typeof o === 'string' ? { value: o, text: o } : o)
  }
  return []
}

function validateForm() {
  for (const field of props.formFields) {
    if (field.required && field.key) {
      const val = formValues.value[field.key]
      if (val === undefined || val === null || val === '' || (Array.isArray(val) && val.length === 0)) {
        message.warning(`请填写 ${field.label}`)
        return false
      }
    }
  }
  return true
}

async function handleSubmit() {
  if (!validateForm()) return
  submitLoading.value = true
  try { emit('submit', { ...formValues.value }) }
  finally { submitLoading.value = false }
}

async function handleApprove() {
  if (!approvalComment.value.trim()) { message.warning('请输入审批意见'); return }
  submitLoading.value = true
  try { emit('approve', { comment: approvalComment.value, ...formValues.value }) }
  finally { submitLoading.value = false }
}

async function handleReject() {
  if (!approvalComment.value.trim()) { message.warning('请输入驳回原因'); return }
  submitLoading.value = true
  try { emit('reject', { comment: approvalComment.value }) }
  finally { submitLoading.value = false }
}

// === 流程图相关 ===
function isGateway(type) {
  return type === 'exclusiveGateway' || type === 'parallelGateway' || type === 'inclusiveGateway'
}
function gatewayIcon(type) {
  if (type === 'exclusiveGateway') return '✕'
  if (type === 'parallelGateway') return '＋'
  if (type === 'inclusiveGateway') return '⊕'
  return '◇'
}
function nodeLabel(type) { return NODE_LABELS[type] || type }

// 计算布局后的节点
const layoutNodes = computed(() => {
  const nodes = props.flowNodes || []
  if (nodes.length === 0) return []

  return nodes.map((node, i) => {
    // 支持两种格式：{x, y} 或 {position: {x, y}}
    const x = node.x ?? node.position?.x ?? 100
    const y = node.y ?? node.position?.y ?? 80 + i * 100
    const status = node.status || 'pending'
    const colors = NODE_COLORS[status] || NODE_COLORS.pending
    const nodeType = node.type || node.nodeType || 'custom'

    const base = {
      id: node.id || node.nodeId,
      nodeType,
      name: node.name || '',
      status,
      cx: x,
      cy: y,
      fillColor: colors.fill,
      strokeColor: colors.stroke
    }

    if (isGateway(nodeType)) {
      const s = 28
      base.diamondPoints = `${x},${y - s} ${x + s},${y} ${x},${y + s} ${x - s},${y}`
    }

    return base
  })
})

// 计算 SVG 尺寸
const svgWidth = computed(() => {
  const nodes = layoutNodes.value
  if (nodes.length === 0) return 660
  return Math.max(...nodes.map(n => n.cx + 80), 660)
})
const svgHeight = computed(() => {
  const nodes = layoutNodes.value
  if (nodes.length === 0) return 200
  return Math.max(...nodes.map(n => n.cy + 70), 200)
})

// 计算连线坐标
const computedEdges = computed(() => {
  const edges = props.flowEdges || []
  const nodes = layoutNodes.value
  const nodeMap = {}
  nodes.forEach(n => { nodeMap[n.id] = n })

  return edges.map(e => {
    const srcId = e.source
    const tgtId = e.target
    const src = nodeMap[srcId]
    const tgt = nodeMap[tgtId]
    if (!src || !tgt) return null

    // 根据状态决定颜色
    let color = '#bbb'
    let markerEnd = 'url(#arrowhead)'
    if (src.status === 'completed' && tgt.status === 'completed') {
      color = '#52c41a'
      markerEnd = 'url(#arrowhead-completed)'
    } else if (tgt.status === 'current') {
      color = '#1677ff'
      markerEnd = 'url(#arrowhead-current)'
    }

    // 计算连线端点
    let x1 = src.cx, y1 = src.cy + (src.nodeType === 'start' || src.nodeType === 'end' ? 18 : 22)
    let x2 = tgt.cx, y2 = tgt.cy - (tgt.nodeType === 'start' || tgt.nodeType === 'end' ? 18 : 22)

    if (isGateway(src.nodeType)) {
      y1 = src.cy + 28
    }
    if (isGateway(tgt.nodeType)) {
      y2 = tgt.cy - 28
    }

    return { x1, y1, x2, y2, color, markerEnd, label: e.label || e.name || '' }
  }).filter(Boolean)
})
</script>

<style scoped>
.drawer-header { padding: 0 0 12px; }
.header-title {
  font-size: 18px; font-weight: 700; color: #1a1a1a;
  text-align: center; margin-bottom: 10px; line-height: 1.4;
}
.header-meta {
  display: flex; justify-content: space-between;
  font-size: 13px; color: #888; padding: 0 4px;
}
.meta-left, .meta-right { display: flex; align-items: center; gap: 4px; }
.meta-key { color: #1677ff; font-weight: 500; }

.drawer-tabs :deep(.ant-tabs-nav) { margin-bottom: 0; }
.drawer-content { min-height: 120px; padding: 4px 0; }

/* 流程图 */
.flow-diagram-wrap { padding: 8px 0; }
.flow-legend {
  display: flex; gap: 16px; margin-bottom: 12px; font-size: 13px; color: #666;
}
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot {
  display: inline-block; width: 12px; height: 12px; border-radius: 50%;
}
.dot-current { background: #1677ff; }
.dot-completed { background: #52c41a; }
.dot-pending { background: #d9d9d9; }

.flow-canvas {
  overflow: auto; border: 1px solid #f0f0f0; border-radius: 8px;
  background: #fafafa; padding: 12px; min-height: 200px;
}
.flow-svg { display: block; }

.drawer-footer { width: 100%; }
.footer-start { display: flex; justify-content: flex-end; gap: 8px; }
.footer-approval { display: flex; flex-direction: column; gap: 8px; }
.approval-actions {
  display: flex; justify-content: flex-end; gap: 8px;
  padding-top: 8px; border-top: 1px solid #f0f0f0;
}
</style>
