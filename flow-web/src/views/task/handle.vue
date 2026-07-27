<template>
  <div class="handle-page">
    <!-- 顶部深蓝标题栏 -->
    <div class="top-bar">
      <div class="top-title">
        {{ processInfo.processName || '任务办理' }} - 流程处置
        <a-tag v-if="currentTask" :color="currentTask.status === 2 ? 'green' : 'blue'" style="margin-left: 8px">
          {{ currentTask.statusDesc || (currentTask.status === 2 ? '已完成' : '处理中') }}
        </a-tag>
      </div>
      <div class="top-opt">
        <a-button size="small" ghost @click="handlePrint" :loading="printLoading" style="margin-right:8px">🖨 打印</a-button>
        <a-button size="small" ghost @click="handleDownloadPdf" :loading="pdfLoading" style="margin-right:8px">📄 下载PDF</a-button>
        <a @click="goBack">返回列表</a>
      </div>
    </div>

    <!-- Tab 切换栏 -->
    <div class="tab-wrap">
      <div class="tab-list">
        <div :class="['tab-item', activeTab === 'form' ? 'active' : '']" @click="activeTab = 'form'">表单详情</div>
        <div :class="['tab-item', activeTab === 'flow' ? 'active' : '']" @click="activeTab = 'flow'">流程图</div>
      </div>
    </div>

    <!-- Tab 内容区 -->
    <div class="tab-content">
      <!-- Tab1: 表单详情 - 左右分栏 -->
      <div v-show="activeTab === 'form'" class="tab-panel">
        <div class="form-container">
          <!-- 左侧：表单 + 审批意见 + 操作按钮 -->
          <div class="left-form">
            <!-- 基本信息表格 -->
            <div class="form-base">
              <table class="form-table">
                <tr>
                  <td class="label">流程标识</td>
                  <td>{{ processInfo.processKey || '-' }}</td>
                  <td class="label">流程名称</td>
                  <td>{{ processInfo.processName || '-' }}</td>
                </tr>
                <tr>
                  <td class="label">申请人</td>
                  <td>{{ processInfo.startUser || '-' }}</td>
                  <td class="label">所属部门</td>
                  <td>{{ processInfo.deptName || '-' }}</td>
                </tr>
                <tr>
                  <td class="label">发起时间</td>
                  <td>{{ formatDate(processInfo.startTime) }}</td>
                  <td class="label">当前节点</td>
                  <td>{{ currentTask?.nodeName || '-' }}</td>
                </tr>
              </table>
            </div>

            <!-- 表单内容（节点权限 hidden 时隐藏） -->
            <template v-if="nodePermission !== 'hidden'">
              <div class="panel-title">表单内容</div>
              <div class="form-body">
                <a-spin :spinning="formLoading">
                  <FormRenderer
                    :form-json="formJson"
                    :fields="formFields"
                    v-model="formValues"
                    :mode="formMode"
                    :field-permissions="fieldPermMap"
                    :node-permission="nodePermission"
                  />
                </a-spin>
              </div>
            </template>

            <!-- 审批意见区域（仅待处理时显示） -->
            <template v-if="currentTask && currentTask.status !== 2">
              <div class="opinion-area">
                <div class="opinion-title">填写本次审批意见（必填）</div>
                <!-- 快捷意见 -->
                <div class="common-word">
                  <span v-for="(text, idx) in quickPhrases" :key="idx" @click="approvalComment = text">
                    {{ text }}
                  </span>
                </div>
                <a-textarea
                  v-model:value="approvalComment"
                  :rows="4"
                  placeholder="请输入审批意见..."
                />
                <div class="tip-text">提示：意见不能为空，填写后点击下方操作按钮完成处置</div>
              </div>

              <!-- 底部操作按钮 -->
              <div class="opt-bottom">
                <div class="btn-group">
                  <template v-if="nodeActions.includes('approve') && hasPerm('task:todo:complete') && isButtonVisible('submit')">
                    <a-button class="btn-agree" type="primary" @click="handleApprove" :loading="submitLoading">
                      提交
                    </a-button>
                  </template>
                  <template v-if="nodeActions.includes('reject') && hasPerm('task:todo:reject') && isButtonVisible('reject')">
                    <a-button class="btn-refuse" danger @click="handleReject" :loading="submitLoading">
                      驳回
                    </a-button>
                  </template>
                  <template v-if="nodeActions.includes('transfer') && hasPerm('task:todo:transfer') && isButtonVisible('transfer')">
                    <a-button class="btn-transfer" @click="showTransferModal">转发他人处理</a-button>
                  </template>
                  <template v-if="nodeActions.includes('addSign') && isButtonVisible('addSign')">
                    <a-button class="btn-addsign" @click="showAddSignModal">加签</a-button>
                  </template>
                  <a-button class="btn-back" @click="goBack">返回列表</a-button>
                </div>
              </div>
            </template>
          </div>

          <!-- 右侧：审批处置历史（常驻侧边栏） -->
          <div class="right-history">
            <div class="panel-title">审批处置历史</div>
            <a-spin :spinning="historyLoading">
              <div class="history-inner" v-if="enhancedHistory.length > 0">
                <div :class="['history-item', item.isStartNode ? 'history-start' : '', item.isEndNode ? 'history-end' : '']" v-for="(item, idx) in enhancedHistory" :key="idx">
                  <div class="history-user">{{ item.assignee || '-' }}（{{ item.nodeName || item.nodeId }}）</div>
                  <div class="history-row">
                    <span :class="['history-opt', getActionTagClass(item.taskAction)]">
                      {{ getActionLabel(item) }}
                    </span>
                    <span class="history-time">{{ formatDate(item.completeTime || item.createTime) }}</span>
                  </div>
                  <div v-if="item.actualOperatorId && item.actualOperatorId !== item.assignee" style="font-size: 12px; color: #8c8c8c; margin-top: 2px">
                    实际办理人：{{ item.actualOperatorId }}
                  </div>
                  <div class="history-opinion" v-if="getOpinion(item)">
                    {{ getOpinion(item) }}
                  </div>
                </div>
              </div>
              <div v-else style="padding: 24px; text-align: center; color: #999">暂无审批记录</div>
            </a-spin>
          </div>
        </div>
      </div>

      <!-- Tab2: 流程图 -->
      <div v-show="activeTab === 'flow'" class="tab-panel">
        <div class="panel-title">流程配置拓扑图</div>
        <FlowViewer
          :nodes="flowNodesRaw"
          :edges="flowEdgesRaw"
          :node-status="nodeStatusMap"
        />
      </div>
    </div>

    <!-- 转办弹窗 -->
    <a-modal v-model:open="transferVisible" title="转办任务" @ok="doTransfer" :width="480">
      <a-form layout="vertical">
        <a-form-item label="转办给（必填）" required>
          <a-select
            v-model:value="transferForm.targetUserId"
            show-search
            :filter-option="false"
            placeholder="搜索用户姓名/用户名"
            :options="transferUserOptions"
            @search="onTransferSearch"
            :loading="transferSearchLoading"
            style="width: 100%"
          >
            <template #option="{ value, label, deptName, postName }">
              <span>{{ label }} - {{ deptName || '-' }} / {{ postName || '-' }}</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="转办原因（必填）" required>
          <a-textarea v-model:value="transferForm.reason" :rows="3" placeholder="请输入转办原因" :maxlength="200" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 加签弹窗 -->
    <a-modal v-model:open="addSignVisible" title="加签" @ok="doAddSign" :width="520">
      <a-form layout="vertical">
        <a-form-item label="加签类型" required>
          <a-radio-group v-model:value="addSignForm.signType">
            <a-radio value="before">前加签（被加签人先审批）</a-radio>
            <a-radio value="after">后加签（原审批人先审批）</a-radio>
            <a-radio value="parallel">并行加签（同时审批）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="被加签人（必填）" required>
          <a-select
            v-model:value="addSignForm.targetUsers"
            mode="multiple"
            show-search
            :filter-option="false"
            placeholder="搜索用户姓名/用户名"
            :options="addSignUserOptions"
            @search="onAddSignSearch"
            :loading="addSignSearchLoading"
            style="width: 100%"
          >
            <template #option="{ value, label, deptName, postName }">
              <span>{{ label }} - {{ deptName || '-' }} / {{ postName || '-' }}</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="加签原因">
          <a-textarea v-model:value="addSignForm.reason" :rows="3" placeholder="请输入加签原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { getTaskDetail, getTasksByInstance, completeTask, rejectTask, transferTask, addSign, getFormPermissions, searchUsers } from '../../api/task'
import dayjs from 'dayjs'
import { getProcessInstance, getProcessVariables, getProcessDefinitionByKey } from '../../api/process'
import { getForm } from '../../api/form'
import { formatDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'
import { usePermission } from '../../composables/usePermission'
import FormRenderer from '../../components/FormRenderer.vue'
import FlowViewer from '../../components/FlowViewer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { hasPerm } = usePermission()

const taskId = computed(() => route.query.id)
const instanceId = computed(() => route.query.instanceId)
const activeTab = ref('form')

// 流程信息
const processInfo = ref({})
const currentTask = ref(null)
const formLoading = ref(false)
const formJson = ref(null)
const formFields = ref([])
const formValues = ref({})

// 表单权限
const fieldPermMap = ref({})
const nodePermission = ref('')
const buttonPermMap = ref({})

// 表单模式动态计算：基于节点权限 + 任务状态
const formMode = computed(() => {
  // 任务已完成 → 始终只读
  if (currentTask.value?.status === 2) return 'readonly'
  // 节点级权限为 readonly → 只读
  if (nodePermission.value === 'readonly') return 'readonly'
  // 节点级权限为 edit 或未配置 → 可编辑
  return 'editable'
})

// 判断按钮是否可见（基于表单权限配置）
function isButtonVisible(buttonKey) {
  const perms = buttonPermMap.value
  // 无权限配置时默认全部可见
  if (!perms || Object.keys(perms).length === 0) return true
  const perm = perms[buttonKey]
  if (!perm) return true  // 未配置的按钮默认可见
  return perm.visible !== false
}

// 流程图
const flowNodesRaw = ref([])
const flowEdgesRaw = ref([])

// 办理记录
const taskHistory = ref([])
const historyLoading = ref(false)
const processInstanceStatus = ref(0)  // 0=运行中, 1=已完成, 2=已终止

// 操作栏
const approvalComment = ref('')
const submitLoading = ref(false)
const nodeActions = ref(['approve', 'reject'])

// 快捷审批语
const quickPhrases = ref([
  '同意', '同意，请继续办理', '已阅，同意',
  '不同意，请修改后重新提交', '材料不全，请补充', '退回修改'
])

// 转办
const transferVisible = ref(false)
const transferForm = reactive({ targetUserId: undefined, reason: '' })
const transferUserOptions = ref([])
const transferSearchLoading = ref(false)
let transferSearchTimer = null

// 加签
const addSignVisible = ref(false)
const addSignForm = reactive({
  signType: 'parallel',
  targetUsers: [],
  reason: ''
})
const addSignUserOptions = ref([])
const addSignSearchLoading = ref(false)
let addSignSearchTimer = null

const ACTION_MAP = { '-1': '流程发起', '-2': '流程结束', 0: '操作', 1: '审批操作：同意', 2: '操作：驳回', 3: '操作：转办', 4: '操作：委派', 5: '操作：取消' }

/**
 * 容错解析子表字符串（兼容 Java toString 格式和标准 JSON）
 * Java toString: [{key=value, key2=value2}, ...]
 * 标准 JSON:     [{"key":"value"}, ...]
 */
function tryParseSubTableStr(str) {
  if (!str || typeof str !== 'string') return null
  // 1. 尝试标准 JSON 解析
  try {
    const parsed = JSON.parse(str)
    if (Array.isArray(parsed)) return parsed
  } catch { /* 不是标准 JSON */ }

  // 2. 解析 Java toString 格式: [{k1=v1, k2=v2}, {k1=v1, k2=v2}]
  try {
    const inner = str.slice(1, -1) // 去掉外层 [ ]
    const rows = []
    let depth = 0, current = ''
    for (const ch of inner) {
      if (ch === '{') depth++
      if (ch === '}') depth--
      if (ch === ',' && depth === 0) {
        rows.push(current.trim())
        current = ''
      } else {
        current += ch
      }
    }
    if (current.trim()) rows.push(current.trim())

    return rows.map(row => {
      const obj = {}
      const content = row.replace(/^\{|\}$/g, '')
      let d = 0, seg = ''
      for (const ch of content) {
        if (ch === '{' || ch === '[') d++
        if (ch === '}' || ch === ']') d--
        if (ch === ',' && d === 0) {
          const eqIdx = seg.indexOf('=')
          if (eqIdx > 0) obj[seg.slice(0, eqIdx).trim()] = seg.slice(eqIdx + 1).trim()
          seg = ''
        } else {
          seg += ch
        }
      }
      if (seg.trim()) {
        const eqIdx = seg.indexOf('=')
        if (eqIdx > 0) obj[seg.slice(0, eqIdx).trim()] = seg.slice(eqIdx + 1).trim()
      }
      return obj
    })
  } catch { return null }
}

function goBack() {
  const from = route.query.from
  if (from === 'done') {
    router.push('/task/done')
  } else if (instanceId.value) {
    router.push('/task/my-request')
  } else {
    router.push('/task/todo')
  }
}

function getActionLabel(item) {
  if (item.taskAction === -1 || item.taskAction === -2) return ACTION_MAP[item.taskAction] || ''
  if (item.taskActionDesc) return item.taskActionDesc
  return ACTION_MAP[item.taskAction] || '操作'
}
function getActionTagClass(action) {
  if (action === -1) return 'tag-start'
  if (action === -2) return 'tag-end'
  if (action === 1) return 'tag-pass'
  if (action === 2) return 'tag-reject'
  if (action === 3) return 'tag-transfer'
  if (action === 4) return 'tag-delegate'
  if (action === 5) return 'tag-cancel'
  return 'tag-pending'
}
function getOpinion(item) {
  // 转办/委托操作显示原因
  if (item.taskAction === 3 && item.reason) return `转办原因：${item.reason}`
  if (item.taskAction === 4 && item.reason) return `委托说明：${item.reason}`
  // 尝试从变量中获取意见
  if (item.taskActionDesc && item.taskActionDesc !== ACTION_MAP[item.taskAction]) return item.taskActionDesc
  return null
}

// ====== 流程图：横向节点序列 ======
const NT_LABELS = {
  start: '开始', end: '结束', userTask: '用户任务', serviceTask: '服务任务',
  scriptTask: '脚本任务', exclusiveGateway: '排他网关', parallelGateway: '并行网关',
  inclusiveGateway: '包容网关'
}

const flowSequence = computed(() => {
  if (!flowNodesRaw.value.length) return []
  // 按流程定义顺序排列节点，过滤掉纯网关节点（可选保留）
  const nodes = flowNodesRaw.value
    .filter(n => {
      const t = n.type || n.nodeType
      return t !== 'exclusiveGateway' && t !== 'parallelGateway' && t !== 'inclusiveGateway'
    })
    .map(n => {
      const nt = n.type || n.nodeType || 'custom'
      const st = n.status || 'pending'
      let statusClass = ''
      if (st === 'completed') statusClass = 'finish'
      else if (st === 'current') statusClass = 'current'
      // 找到该节点对应的办理人
      const hist = taskHistory.value.find(h => h.nodeId === (n.id || n.nodeId))
      let assignee = hist?.assignee || ''
      if (st === 'current' && currentTask.value) assignee = '当前：' + (currentTask.value.assignee || '')
      if (nt === 'start') assignee = processInfo.value.startUser || ''
      return {
        id: n.id || n.nodeId,
        name: n.name || NT_LABELS[nt] || nt,
        ntLabel: NT_LABELS[nt] || nt,
        status: st,
        statusClass,
        assignee,
        lineClass: st === 'completed' ? 'finish' : ''
      }
    })
  return nodes
})

const enhancedHistory = computed(() => {
  const list = []
  // 开始节点
  list.push({
    isStartNode: true,
    nodeName: '开始节点',
    assignee: processInfo.value.startUser || '-',
    completeTime: processInfo.value.startTime,
    taskAction: -1
  })
  // 原有审批记录
  list.push(...taskHistory.value)
  // 结束节点（流程已完成时）
  if (processInstanceStatus.value === 1) {
    list.push({
      isEndNode: true,
      nodeName: '结束节点',
      assignee: processInfo.value.startUser || '-',
      completeTime: processInfo.value.endTime,
      taskAction: -2
    })
  }
  return list
})

const nodeStatusMap = computed(() => {
  const map = {}
  if (!flowNodesRaw.value.length) return map
  const completedIds = taskHistory.value.map(h => h.nodeId)
  const currentId = currentTask.value?.nodeId
  flowNodesRaw.value.forEach(n => {
    const id = n.id || n.nodeId
    const nt = n.type || n.nodeType
    if (id === currentId) map[id] = 'current'
    else if (completedIds.includes(id)) map[id] = 'completed'
    // 流程已完成时，结束节点标记为completed
    else if (processInstanceStatus.value === 1 && nt === 'end') map[id] = 'completed'
  })
  return map
})

// ====== 数据加载 ======
async function loadAll() {
  if (!taskId.value && !instanceId.value) { message.error('缺少任务ID'); return }
  formLoading.value = true

  try {
    let instId, processKey

    if (taskId.value) {
      // === 通过任务ID加载（待办/已办入口）===
      const taskRes = await getTaskDetail(taskId.value)
      currentTask.value = taskRes.data || taskRes
      instId = currentTask.value.processInstanceId
      processKey = currentTask.value.processKey
    } else if (instanceId.value) {
      // === 通过流程实例ID加载（我的申请入口）===
      instId = instanceId.value
      const instRes = await getProcessInstance(instId)
      const inst = instRes.data || instRes
      processKey = inst.processKey
      processInfo.value = {
        processName: inst.processName || processKey,
        processKey: processKey,
        startTime: inst.startTime || inst.createTime || '-',
        startUser: inst.startUser || '-',
        endTime: inst.endTime || '-',
        deptName: '-'
      }
      processInstanceStatus.value = inst.status ?? 0
      // 查找当前活跃任务（进行中）或最近完成的任务
      try {
        const tasksRes = await getTasksByInstance(instId)
        const tasks = tasksRes.data || tasksRes || []
        // 优先找进行中的任务，否则取最新的已完成任务
        const activeTask = tasks.find(t => t.status === 0 || t.status === 'PENDING' || t.status === 1 || t.status === 'IN_PROGRESS')
        const latestTask = tasks.length > 0 ? tasks[tasks.length - 1] : null
        currentTask.value = activeTask || latestTask || null
      } catch {
        currentTask.value = null
      }
    }

    // 2. 获取流程实例（任务ID入口时需要）
    if (taskId.value || !processInfo.value.processName) {
      const instRes = await getProcessInstance(instId)
      const inst = instRes.data || instRes
      processInfo.value = {
        processName: inst.processName || processKey,
        processKey: processKey,
        startTime: inst.startTime || inst.createTime || '-',
        startUser: inst.startUser || '-',
        endTime: inst.endTime || '-',
        deptName: '-'
      }
      processInstanceStatus.value = inst.status ?? 0
    }

    // 3. 获取流程变量
    try {
      const varRes = await getProcessVariables(instId)
      formValues.value = varRes.data || varRes || {}
    } catch { formValues.value = {} }

    // 4. 获取流程定义
    let pj = null
    try {
      const defRes = await getProcessDefinitionByKey(processKey)
      const def = defRes.data || defRes
      if (def?.processJson) {
        pj = typeof def.processJson === 'string' ? JSON.parse(def.processJson) : def.processJson
        flowNodesRaw.value = pj.nodes || []
        flowEdgesRaw.value = pj.edges || []

        // 提取 formKey
        let formKey = null
        const curNodeId = currentTask.value?.nodeId
        if (curNodeId) {
          for (const n of (pj.nodes || [])) {
            if (n.id === curNodeId && n.properties?.formKey) { formKey = n.properties.formKey; break }
          }
        }
        if (!formKey) {
          for (const n of (pj.nodes || [])) {
            if (n.type === 'userTask' && n.properties?.formKey) { formKey = n.properties.formKey; break }
          }
        }
        // 提取节点操作权限
        if (curNodeId) {
          for (const n of (pj.nodes || [])) {
            if (n.id === curNodeId && n.properties?.actions) {
              nodeActions.value = n.properties.actions
              break
            }
          }
        }

        // 加载表单
        if (formKey) {
          const formRes = await getForm(formKey)
          const formDef = formRes.data || formRes
          if (formDef.formJson) {
            formJson.value = typeof formDef.formJson === 'string'
              ? JSON.parse(formDef.formJson) : formDef.formJson
          }
        }
      }
    } catch { /* ignore */ }

    // 4.5 容错：解析子表字段中可能残留的 Java toString 格式字符串
    const rawVals = formValues.value
    for (const [key, val] of Object.entries(rawVals)) {
      if (typeof val === 'string' && val.startsWith('[{') && val.endsWith('}]')) {
        const parsed = tryParseSubTableStr(val)
        if (parsed) rawVals[key] = parsed
      }
    }
    formValues.value = { ...rawVals }

    // 5. 加载办理记录 + 节点状态着色
    historyLoading.value = true
    try {
      const tasksRes = await getTasksByInstance(instId)
      const tasks = tasksRes.data || tasksRes || []
      const completedIds = tasks
        .filter(t => t.status === 'completed' || t.status === 'COMPLETED' || t.status === 2)
        .map(t => t.nodeId)
      const currentNodeId = currentTask.value?.nodeId || processInfo.value.currentNodeId
      flowNodesRaw.value = (pj?.nodes || []).map(n => {
        let st = 'pending'
        if (completedIds.includes(n.id)) st = 'completed'
        if (n.id === currentNodeId) st = 'current'
        return { ...n, status: st }
      })
      taskHistory.value = tasks
        .filter(t => t.status === 'completed' || t.status === 'COMPLETED' || t.status === 2)
    } catch { taskHistory.value = [] }
    historyLoading.value = false

    // 如果是我的申请入口且无当前任务，隐藏审批操作按钮
    if (instanceId.value && !currentTask.value) {
      nodeActions.value = []
    }

    // 6. 加载表单权限（仅当有任务ID时）
    if (currentTask.value?.id) {
      try {
        const permRes = await getFormPermissions(currentTask.value.id)
        const perm = permRes.data || permRes
        if (perm) {
          nodePermission.value = perm.nodePermission || ''
          // 将字段权限数组转换为 Map: fieldKey -> permission
          const fpMap = {}
          if (Array.isArray(perm.fieldPermissions)) {
            perm.fieldPermissions.forEach(fp => {
              if (fp.fieldKey) fpMap[fp.fieldKey] = fp.permission || 'edit'
            })
          }
          fieldPermMap.value = fpMap
          // 按钮权限
          const bpMap = {}
          if (Array.isArray(perm.buttonPermissions)) {
            perm.buttonPermissions.forEach(bp => {
              if (bp.buttonKey) bpMap[bp.buttonKey] = { visible: bp.visible, enabled: bp.enabled }
            })
          }
          buttonPermMap.value = bpMap
        }
      } catch { /* 权限加载失败不阻塞页面 */ }
    }

  } catch (e) {
    message.error('加载任务详情失败: ' + (e.message || ''))
  }
  formLoading.value = false
}

// ====== 操作 ======
async function handleApprove() {
  if (!approvalComment.value.trim()) { message.warning('请填写审批意见后再提交'); return }
  const userId = userStore.username || localStorage.getItem('username') || ''
  submitLoading.value = true
  try {
    await completeTask(currentTask.value.id, { userId, variables: { comment: approvalComment.value, ...formValues.value } })
    message.success('审批同意成功，流程自动流转至下一节点')
    goBack()
  } catch { /* handled by interceptor */ }
  submitLoading.value = false
}

async function handleReject() {
  if (!approvalComment.value.trim()) { message.warning('请填写驳回原因后再提交'); return }
  const userId = userStore.username || localStorage.getItem('username') || ''
  submitLoading.value = true
  try {
    await rejectTask(currentTask.value.id, { userId, comment: approvalComment.value })
    message.success('单据已驳回，退回申请人修改')
    goBack()
  } catch { /* handled by interceptor */ }
  submitLoading.value = false
}

// ====== 用户搜索（通用） ======
async function doSearchUsers(keyword) {
  if (!keyword || keyword.trim().length < 1) return []
  try {
    const res = await searchUsers({ keyword: keyword.trim(), size: 20 })
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    return list.map(u => ({
      value: u.username,
      label: u.realName || u.username,
      deptName: u.deptName || '',
      postName: u.postName || ''
    }))
  } catch { return [] }
}

function onTransferSearch(val) {
  clearTimeout(transferSearchTimer)
  transferSearchLoading.value = true
  transferSearchTimer = setTimeout(async () => {
    transferUserOptions.value = await doSearchUsers(val)
    transferSearchLoading.value = false
  }, 300)
}

function showTransferModal() {
  transferForm.targetUserId = undefined
  transferForm.reason = ''
  transferUserOptions.value = []
  transferVisible.value = true
}

async function doTransfer() {
  if (!transferForm.targetUserId) { message.warning('请选择转办目标用户'); return }
  if (!transferForm.reason.trim()) { message.warning('请填写转办原因'); return }
  try {
    await transferTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      targetUserId: transferForm.targetUserId,
      reason: transferForm.reason.trim()
    })
    message.success('转办成功'); transferVisible.value = false; goBack()
  } catch {}
}

function showAddSignModal() {
  addSignForm.signType = 'parallel'
  addSignForm.targetUsers = []
  addSignForm.reason = ''
  addSignUserOptions.value = []
  addSignVisible.value = true
}

function onAddSignSearch(val) {
  clearTimeout(addSignSearchTimer)
  if (!val || val.trim().length < 1) return
  addSignSearchLoading.value = true
  addSignSearchTimer = setTimeout(async () => {
    try {
      const res = await searchUsers({ keyword: val.trim(), size: 20 })
      const data = res.data || res
      const list = Array.isArray(data) ? data : (data.list || data.records || [])
      addSignUserOptions.value = list.map(u => ({
        value: u.username,
        label: u.realName || u.username,
        deptName: u.deptName || '',
        postName: u.postName || ''
      }))
    } catch { addSignUserOptions.value = [] }
    addSignSearchLoading.value = false
  }, 300)
}

async function doAddSign() {
  if (!addSignForm.targetUsers || addSignForm.targetUsers.length === 0) { message.warning('请选择被加签人'); return }
  try {
    await addSign(currentTask.value.id, {
      operatorId: userStore.username || currentTask.value.assignee,
      signType: addSignForm.signType,
      targetUsers: addSignForm.targetUsers,
      reason: addSignForm.reason
    })
    message.success('加签成功')
    addSignVisible.value = false
    loadAll() // 刷新任务详情
  } catch {}
}

onMounted(loadAll)

// ====== 打印 / PDF ======
const printLoading = ref(false)
const pdfLoading = ref(false)

async function handlePrint() {
  printLoading.value = true
  await nextTick()
  try {
    // 获取当前表单Tab内容区域
    const el = document.querySelector('.tab-panel .form-container')
    if (!el) { message.warning('请先切换到表单详情Tab'); return }

    const printWindow = window.open('', '_blank')
    if (!printWindow) { message.warning('请允许弹出窗口以使用打印功能'); return }

    printWindow.document.write(`
      <!DOCTYPE html><html><head><meta charset="utf-8">
      <title>${processInfo.value.processName || '流程表单'}</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; padding: 20px; color: #333; font-size: 14px; }
        h2 { text-align: center; margin-bottom: 16px; font-size: 18px; }
        h3 { font-size: 15px; margin: 16px 0 8px; border-bottom: 1px solid #eee; padding-bottom: 4px; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
        td, th { border: 1px solid #ddd; padding: 8px 10px; font-size: 13px; }
        td.label { background: #f5f5f5; font-weight: 500; width: 100px; text-align: right; }
        th { background: #f0f4f9; font-weight: 500; }
        .ant-form-item { margin-bottom: 10px; }
        .ant-form-item-label { font-weight: 500; color: #555; margin-bottom: 2px; }
        .readonly-value { color: #333; }
        .ant-divider-inner-text { font-weight: 500; color: #0052a5; }
        .ant-divider { margin: 10px 0; }
        .ant-spin-nested-loading, .ant-spin-container { display: block !important; }
        .ant-spin-blur { opacity: 1 !important; pointer-events: auto !important; }
        @media print { body { padding: 0; } }
      </style>
      </head><body>${el.innerHTML}</body></html>
    `)
    printWindow.document.close()
    setTimeout(() => { printWindow.print(); printWindow.close() }, 500)
  } finally { printLoading.value = false }
}

async function handleDownloadPdf() {
  pdfLoading.value = true
  await nextTick()
  try {
    const el = document.querySelector('.tab-panel .form-container')
    if (!el) { message.warning('请先切换到表单详情Tab'); pdfLoading.value = false; return }

    const canvas = await html2canvas(el, {
      scale: 2, useCORS: true, backgroundColor: '#ffffff', logging: false
    })
    const imgWidth = 210
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    const pdf = new jsPDF('p', 'mm', 'a4')
    let position = 0
    const pageHeight = 297
    if (imgHeight <= pageHeight) {
      pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, 0, imgWidth, imgHeight)
    } else {
      while (position < imgHeight) {
        pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, -position, imgWidth, imgHeight)
        position += pageHeight
        if (position < imgHeight) pdf.addPage()
      }
    }
    const fileName = `${processInfo.value.processName || '流程表单'}_${Date.now()}.pdf`
    pdf.save(fileName)
    message.success('PDF 下载成功')
  } catch (e) {
    message.error('PDF 生成失败: ' + (e.message || ''))
  } finally { pdfLoading.value = false }
}
</script>

<style scoped>
.handle-page {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
  margin: -16px;
}

/* 顶部深蓝标题栏 */
.top-bar {
  min-height: 52px;
  background: #0052a5;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}
.top-title {

  margin-top:12px;
  margin-left: 30px;
  font-size: 16px;
  font-weight: normal;
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1;
}
.top-opt {
  margin-top: 12px;
  margin-right: 30px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.top-opt :deep(.ant-btn-ghost) {
  color: #fff;
  border-color: rgba(255,255,255,0.5);
  font-size: 12px;
}
.top-opt :deep(.ant-btn-ghost:hover) {
  color: #fff;
  border-color: #fff;
  background: rgba(255,255,255,0.15);
}
.top-opt a {
  color: #fff;
  margin-left: 16px;
  opacity: 0.9;
  cursor: pointer;
}
.top-opt a:hover { opacity: 1; }

/* Tab 切换栏 */
.tab-wrap {
  background: #fff;
  border: 1px solid #e1e6ef;
  border-bottom: none;
  margin: 10px auto 0;
  max-width: 1400px;
  padding: 0 10px;
}
.tab-list {
  display: flex;
  align-items: center;
}
.tab-item {
  height: 38px;
  display: flex;
  align-items: center;
  padding: 0 22px;
  border-right: 1px solid #e1e6ef;
  cursor: pointer;
  position: relative;
  font-size: 14px;
  color: #333;
}
.tab-item.active {
  color: #0052a5;
  font-weight: bold;
}
.tab-item.active::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 100%;
  height: 2px;
  background: #0052a5;
}

/* Tab 内容 */
.tab-content {
  max-width: 1400px;
  margin: 0 auto 10px;
  padding: 0 10px;
}
.tab-panel {
  background: #fff;
  border: 1px solid #e1e6ef;
  padding: 16px;
}

/* 表单Tab内左右分栏 */
.form-container {
  display: flex;
  gap: 16px;
}
.left-form {
  flex: 0 0 72%;
}
.right-history {
  flex: 0 0 26%;
  border: 1px solid #e1e6ef;
  max-height: 750px;
  overflow-y: auto;
}

/* 面板标题 */
.panel-title {
  height: 36px;
  line-height: 36px;
  background: #f0f4f9;
  padding: 0 12px;
  font-weight: bold;
  border: 1px solid #e1e6ef;
  margin-bottom: 12px;
  font-size: 14px;
}

/* 基本信息表格 */
.form-base { margin-bottom: 20px; }
.form-table { width: 100%; border-collapse: collapse; }
.form-table td {
  border: 1px solid #e8edf3;
  padding: 10px 12px;
  font-size: 14px;
}
.form-table td.label {
  width: 100px;
  background: #f7f9fc;
  text-align: right;
  color: #444;
  font-weight: 500;
}

/* 表单内容区 */
.form-body {
  margin: 0 0 20px;
  padding: 12px;
  background: #f7f9fc;
  min-height: 100px;
  border: 1px solid #e1e6ef;
}

/* 审批意见区域 */
.opinion-area {
  margin: 20px 0;
  padding: 16px;
  border: 1px solid #e1e6ef;
  background: #fafbfd;
}
.opinion-title {
  font-weight: bold;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  font-size: 14px;
}
.opinion-title::before {
  content: "";
  width: 4px;
  height: 14px;
  background: #0052a5;
  margin-right: 8px;
  display: inline-block;
}
.common-word {
  margin: 10px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.common-word span {
  padding: 3px 10px;
  background: #f0f4f9;
  border: 1px solid #d0d7df;
  cursor: pointer;
  font-size: 12px;
  border-radius: 2px;
}
.common-word span:hover { background: #e1ecf9; }
.tip-text {
  color: #999;
  font-size: 12px;
  margin-top: 6px;
}

/* 底部操作按钮栏 */
.opt-bottom {
  padding: 12px 0;
  border-top: 1px solid #e1e6ef;
  margin-top: 12px;
}
.btn-group { display: flex; gap: 10px; flex-wrap: wrap; }
.btn-agree { background: #0052a5 !important; border-color: #0052a5 !important; }
.btn-refuse { color: #f53f3f !important; border-color: #f53f3f !important; }
.btn-transfer { color: #0066cc !important; border-color: #0066cc !important; }
.btn-addsign { color: #00a854 !important; border-color: #00a854 !important; }
.btn-back { color: #666 !important; border-color: #c0c6cf !important; }

/* 右侧审批历史 */
.history-inner { padding: 12px; }
.history-item { border-bottom: 1px dashed #eee; padding: 12px 0; }
.history-item:last-child { border-bottom: none; }
.history-start { background: #f0f9ff; border-radius: 6px; padding: 12px 8px; margin-bottom: 4px; border-bottom: none; }
.history-end { background: #f0fff4; border-radius: 6px; padding: 12px 8px; margin-top: 4px; border-bottom: none; }
.history-user { font-weight: bold; font-size: 14px; }
.history-row { margin: 6px 0; }
.history-opt { margin-right: 16px; font-weight: 500; }
.tag-start { color: #1677ff; font-weight: bold; }
.tag-end { color: #00a854; font-weight: bold; }
.tag-pass { color: #00a854; }
.tag-pending { color: #ff7d00; }
.tag-reject { color: #f53f3f; }
.tag-transfer { color: #1677ff; }
.tag-delegate { color: #722ed1; }
.tag-cancel { color: #999; }
.history-time { color: #999; font-size: 12px; }
.history-opinion {
  background: #f7f9fc;
  padding: 8px;
  margin-top: 6px;
  color: #555;
  line-height: 1.6;
  border-radius: 4px;
  font-size: 13px;
}



</style>
