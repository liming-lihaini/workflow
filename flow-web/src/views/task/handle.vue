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

            <!-- 表单内容 -->
            <div class="panel-title">表单内容</div>
            <div class="form-body">
              <a-spin :spinning="formLoading">
                <FormRenderer
                  :form-json="formJson"
                  :fields="formFields"
                  v-model="formValues"
                  mode="readonly"
                />
              </a-spin>
            </div>

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
                  <template v-if="nodeActions.includes('approve')">
                    <a-button class="btn-agree" type="primary" @click="handleApprove" :loading="submitLoading">
                      同意提交下一节点
                    </a-button>
                  </template>
                  <template v-if="nodeActions.includes('reject')">
                    <a-button class="btn-refuse" danger @click="handleReject" :loading="submitLoading">
                      驳回至申请人
                    </a-button>
                  </template>
                  <template v-if="nodeActions.includes('transfer')">
                    <a-button class="btn-transfer" @click="showTransferModal">转发他人处理</a-button>
                  </template>
                  <template v-if="nodeActions.includes('delegate')">
                    <a-button class="btn-addsign" @click="showDelegateModal">委派协同审批</a-button>
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
              <div class="history-inner" v-if="taskHistory.length > 0">
                <div class="history-item" v-for="(item, idx) in taskHistory" :key="idx">
                  <div class="history-user">{{ item.assignee || '-' }}（{{ item.nodeName || item.nodeId }}）</div>
                  <div class="history-row">
                    <span :class="['history-opt', getActionTagClass(item.taskAction)]">
                      {{ getActionLabel(item) }}
                    </span>
                    <span class="history-time">{{ formatDate(item.completeTime || item.createTime) }}</span>
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
        <div class="panel-title">流程轨迹拓扑图</div>
        <div class="flow-chart-box" v-if="flowSequence.length > 0">
          <template v-for="(item, idx) in flowSequence" :key="idx">
            <div :class="['flow-node', item.statusClass]">
              <div class="flow-node-name">{{ item.name || item.ntLabel }}</div>
              <div class="flow-node-user">{{ item.assignee || item.ntLabel }}</div>
            </div>
            <div v-if="idx < flowSequence.length - 1" :class="['flow-line', item.lineClass]"></div>
          </template>
        </div>
        <a-empty v-else description="暂无流程图数据" style="margin-top: 40px" />
      </div>
    </div>

    <!-- 转办弹窗 -->
    <a-modal v-model:open="transferVisible" title="转办" @ok="doTransfer">
      <a-form layout="vertical">
        <a-form-item label="转办目标用户ID" required>
          <a-input v-model:value="transferUserId" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 委派弹窗 -->
    <a-modal v-model:open="delegateVisible" title="委派" @ok="doDelegate">
      <a-form layout="vertical">
        <a-form-item label="委派目标用户ID" required>
          <a-input v-model:value="delegateUserId" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getTaskDetail, getTasksByInstance, completeTask, rejectTask, transferTask, delegateTask } from '../../api/task'
import { getProcessInstance, getProcessVariables, getProcessDefinitionByKey } from '../../api/process'
import { getForm } from '../../api/form'
import { formatDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'
import FormRenderer from '../../components/FormRenderer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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

// 流程图
const flowNodesRaw = ref([])
const flowEdgesRaw = ref([])

// 办理记录
const taskHistory = ref([])
const historyLoading = ref(false)

// 操作栏
const approvalComment = ref('')
const submitLoading = ref(false)
const nodeActions = ref(['approve', 'reject'])

// 快捷审批语
const quickPhrases = ref([
  '同意', '同意，请继续办理', '已阅，同意',
  '不同意，请修改后重新提交', '材料不全，请补充', '退回修改'
])

// 转办/委派
const transferVisible = ref(false)
const transferUserId = ref('')
const delegateVisible = ref(false)
const delegateUserId = ref('')

const ACTION_MAP = { 0: '操作', 1: '审批操作：同意', 2: '操作：驳回', 3: '操作：转办', 4: '操作：委派', 5: '操作：取消' }

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
  if (item.taskActionDesc) return item.taskActionDesc
  return ACTION_MAP[item.taskAction] || '操作'
}
function getActionTagClass(action) {
  if (action === 1) return 'tag-pass'
  if (action === 2) return 'tag-reject'
  return 'tag-pending'
}
function getOpinion(item) {
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
        deptName: '-'
      }
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
        deptName: '-'
      }
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

function showTransferModal() { transferUserId.value = ''; transferVisible.value = true }
function showDelegateModal() { delegateUserId.value = ''; delegateVisible.value = true }

async function doTransfer() {
  if (!transferUserId.value.trim()) { message.warning('请输入转办目标用户ID'); return }
  try {
    await transferTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      targetUserId: transferUserId.value.trim()
    })
    message.success('转办成功'); transferVisible.value = false; goBack()
  } catch {}
}

async function doDelegate() {
  if (!delegateUserId.value.trim()) { message.warning('请输入委派目标用户ID'); return }
  try {
    await delegateTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      delegateUserId: delegateUserId.value.trim()
    })
    message.success('委派成功'); delegateVisible.value = false; goBack()
  } catch {}
}

onMounted(loadAll)
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
.history-user { font-weight: bold; font-size: 14px; }
.history-row { margin: 6px 0; }
.history-opt { margin-right: 16px; font-weight: 500; }
.tag-pass { color: #00a854; }
.tag-pending { color: #ff7d00; }
.tag-reject { color: #f53f3f; }
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

/* 流程图 - 横向节点流 */
.flow-chart-box {
  padding: 30px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  overflow-x: auto;
}
.flow-node {
  width: 130px;
  min-height: 70px;
  border: 2px solid #d0d7df;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #fafbfd;
  flex-shrink: 0;
  padding: 8px;
}
.flow-node.finish {
  border-color: #00a854;
  background: #e6fffb;
}
.flow-node.current {
  border-color: #0052a5;
  background: #e1ecf9;
}
.flow-node-name { font-weight: bold; margin-bottom: 4px; font-size: 13px; text-align: center; }
.flow-node-user { font-size: 12px; color: #666; text-align: center; }
.flow-line {
  width: 40px;
  height: 3px;
  background: #d0d7df;
  margin: 0 -2px;
  z-index: -1;
  flex-shrink: 0;
}
.flow-line.finish { background: #00a854; }
</style>
