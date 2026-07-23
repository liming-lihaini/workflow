<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">待办任务</span>
        <a-button @click="loadData">刷新</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <template v-if="record.assignee">
              <span class="action-link" @click="openApprovalDrawer(record)">处理</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showTransferModal(record)">转办</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showDelegateModal(record)">委派</span>
            </template>
            <span v-else class="action-link" @click="handleClaim(record)">签收</span>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 审批处理抽屉 -->
    <TaskFormDrawer
      v-model:open="approvalDrawerVisible"
      mode="approval"
      :loading="approvalLoading"
      :process-info="approvalProcessInfo"
      :form-fields="approvalFormFields"
      :initial-values="approvalFormValues"
      :task-info="currentTask"
      :flow-nodes="flowNodes"
      :flow-edges="flowEdges"
      @approve="handleApprove"
      @reject="handleReject"
      @cancel="approvalDrawerVisible = false"
    />

    <!-- 转办弹窗 -->
    <a-modal v-model:open="transferVisible" title="转办" @ok="handleTransfer">
      <a-form layout="vertical">
        <a-form-item label="转办人ID" required>
          <a-input-number v-model:value="transferUserId" style="width: 100%" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 委派弹窗 -->
    <a-modal v-model:open="delegateVisible" title="委派" @ok="handleDelegate">
      <a-form layout="vertical">
        <a-form-item label="委派人ID" required>
          <a-input-number v-model:value="delegateUserId" style="width: 100%" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getTodoTasks, claimTask, completeTask, rejectTask, transferTask, delegateTask, getTasksByInstance } from '../../api/task'
import { getProcessInstance, getProcessVariables } from '../../api/process'
import { getProcessDefinitionByKey } from '../../api/process'
import { getForm } from '../../api/form'
import { useUserStore } from '../../stores/user'
import TaskFormDrawer from '../../components/TaskFormDrawer.vue'

const userStore = useUserStore()
const loading = ref(false)
const dataList = ref([])
const currentTask = ref(null)

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '流程实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId' },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName' },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200 }
]

// 转办
const transferVisible = ref(false)
const transferUserId = ref(null)
// 委派
const delegateVisible = ref(false)
const delegateUserId = ref(null)

// 审批抽屉
const approvalDrawerVisible = ref(false)
const approvalLoading = ref(false)
const approvalProcessInfo = ref({})
const approvalFormFields = ref([])
const approvalFormValues = ref({})
// 流程图数据
const flowNodes = ref([])
const flowEdges = ref([])

function showTransferModal(record) {
  currentTask.value = record
  transferUserId.value = null
  transferVisible.value = true
}
function showDelegateModal(record) {
  currentTask.value = record
  delegateUserId.value = null
  delegateVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getTodoTasks({ userId, page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
  } catch { /* ignore */ }
  loading.value = false
}

/** 打开审批抽屉，加载流程实例 + 表单定义 + 当前变量 + 流程图 */
async function openApprovalDrawer(record) {
  currentTask.value = record
  approvalFormFields.value = []
  approvalFormValues.value = {}
  flowNodes.value = []
  flowEdges.value = []
  approvalDrawerVisible.value = true
  approvalLoading.value = true

  try {
    // 1. 获取流程实例信息
    const instRes = await getProcessInstance(record.processInstanceId)
    const inst = instRes.data || instRes

    approvalProcessInfo.value = {
      processName: inst.processName || record.processKey,
      processKey: inst.processKey || record.processKey,
      startTime: inst.startTime || inst.createTime || '-',
      startUser: inst.startUser || '-',
      deptName: '-'
    }

    // 2. 获取流程变量（作为表单初始值）
    try {
      const varRes = await getProcessVariables(record.processInstanceId)
      const vars = varRes.data || varRes || {}
      approvalFormValues.value = vars
    } catch { approvalFormValues.value = {} }

    // 3. 获取流程定义 → 提取 formKey + 流程图数据
    try {
      const defRes = await getProcessDefinitionByKey(record.processKey || inst.processKey)
      const def = defRes.data || defRes
      if (def?.processJson) {
        const pj = typeof def.processJson === 'string' ? JSON.parse(def.processJson) : def.processJson

        // 3a. 流程图数据：节点和边
        flowNodes.value = pj.nodes || []
        flowEdges.value = pj.edges || []

        // 3b. 提取 formKey
        let formKey = null
        for (const node of (pj.nodes || [])) {
          if (node.id === record.nodeId && node.properties?.formKey) {
            formKey = node.properties.formKey
            break
          }
        }
        if (!formKey) {
          for (const node of (pj.nodes || [])) {
            if (node.type === 'userTask' && node.properties?.formKey) {
              formKey = node.properties.formKey
              break
            }
          }
        }

        if (formKey) {
          const formRes = await getForm(formKey)
          const formDef = formRes.data || formRes
          if (formDef.formJson) {
            const fj = typeof formDef.formJson === 'string' ? JSON.parse(formDef.formJson) : formDef.formJson
            approvalFormFields.value = fj.fields || []
          }
        }
      }
    } catch { /* formKey not found */ }

    // 4. 获取流程实例的任务历史（用于节点状态着色）
    try {
      const tasksRes = await getTasksByInstance(record.processInstanceId)
      const tasks = tasksRes.data || tasksRes || []
      // 已完成节点ID列表
      const completedNodeIds = tasks
        .filter(t => t.status === 'completed' || t.status === 'COMPLETED')
        .map(t => t.nodeId)
      // 当前节点 = 实例的 currentNodeId
      const currentNodeId = inst.currentNodeId || record.nodeId
      // 把状态信息附加到节点数据上
      flowNodes.value = (pj?.nodes || []).map(node => {
        let nodeStatus = 'pending' // 未执行
        if (completedNodeIds.includes(node.id)) nodeStatus = 'completed' // 已办结
        if (node.id === currentNodeId) nodeStatus = 'current' // 当前节点
        return { ...node, status: nodeStatus }
      })
    } catch { /* ignore */ }

  } catch {
    message.error('加载任务详情失败')
  }

  approvalLoading.value = false
}

async function handleApprove(data) {
  const userId = userStore.username || localStorage.getItem('username') || ''
  try {
    await completeTask(currentTask.value.id, {
      userId: userId,
      variables: { ...data }
    })
    message.success('审批通过')
    approvalDrawerVisible.value = false
    loadData()
  } catch { /* ignore */ }
}

async function handleReject(data) {
  const userId = userStore.username || localStorage.getItem('username') || ''
  try {
    await rejectTask(currentTask.value.id, {
      userId: userId,
      comment: data.comment
    })
    message.success('已驳回')
    approvalDrawerVisible.value = false
    loadData()
  } catch { /* ignore */ }
}

async function handleClaim(record) {
  try {
    await claimTask(record.id, { userId: record.assignee || 'current-user' })
    message.success('签收成功')
    loadData()
  } catch { /* ignore */ }
}

async function handleTransfer() {
  if (!transferUserId.value) { message.warning('请输入转办人ID'); return }
  try {
    await transferTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      targetUserId: String(transferUserId.value)
    })
    message.success('转办成功')
    transferVisible.value = false
    loadData()
  } catch { /* ignore */ }
}

async function handleDelegate() {
  if (!delegateUserId.value) { message.warning('请输入委派人ID'); return }
  try {
    await delegateTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      delegateUserId: String(delegateUserId.value)
    })
    message.success('委派成功')
    delegateVisible.value = false
    loadData()
  } catch { /* ignore */ }
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(loadData)
</script>
