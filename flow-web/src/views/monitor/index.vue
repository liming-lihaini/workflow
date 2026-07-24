<template>
  <div class="page-wrap">
    <a-row :gutter="16">
      <!-- 左侧运行中流程列表 -->
      <a-col :span="8">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">运行中流程</span>
            <a-button size="small" @click="loadRunning">刷新</a-button>
          </div>
          <a-list
            :data-source="runningList"
            :loading="runningLoading"
            size="small"
          >
            <template #renderItem="{ item }">
              <a-list-item
                :class="{ 'active-item': selectedInstance?.id === item.id }"
                @click="selectInstance(item)"
              >
                <a-list-item-meta
                  :title="`实例 #${item.id}`"
                  :description="`当前节点: ${item.currentNodeId || '-'}`"
                />
              </a-list-item>
            </template>
          </a-list>
        </div>
      </a-col>

      <!-- 右侧详情 -->
      <a-col :span="16">
        <div class="card-wrap" v-if="selectedInstance">
          <div class="page-header">
            <span class="page-title">实例 #{{ selectedInstance.id }} 监控</span>
            <a-space>
              <a-button v-if="hasPerm('monitor:intervene')" @click="showInterveneModal">管理员干预</a-button>
              <a-button v-if="hasPerm('monitor:export')" @click="handleExport">导出</a-button>
            </a-space>
          </div>

          <a-tabs v-model:activeKey="detailTab">
            <!-- 执行轨迹 -->
            <a-tab-pane key="history" tab="执行轨迹">
              <a-timeline>
                <a-timeline-item
                  v-for="(item, idx) in historyList"
                  :key="idx"
                  :color="item.nodeType === 'END' ? 'green' : 'blue'"
                >
                  <p><strong>{{ item.nodeName }}</strong> ({{ item.nodeType }})</p>
                  <p style="color: var(--text-placeholder); font-size: 12px;">
                    {{ formatDate(item.startTime) }} ~ {{ formatDate(item.endTime) || '进行中' }}
                    <span v-if="item.duration"> | 耗时 {{ item.duration }}ms</span>
                  </p>
                </a-timeline-item>
              </a-timeline>
              <a-empty v-if="!historyList.length" description="暂无执行轨迹" />
            </a-tab-pane>

            <!-- 变量历史 -->
            <a-tab-pane key="variables" tab="变量历史">
              <a-table
                :columns="varColumns"
                :data-source="variableList"
                :pagination="false"
                size="small"
                row-key="id"
              />
              <a-empty v-if="!variableList.length" description="暂无变量数据" />
            </a-tab-pane>

            <!-- 耗时统计 -->
            <a-tab-pane key="statistics" tab="耗时统计">
              <a-row :gutter="16">
                <a-col :span="8">
                  <a-statistic
                    title="总耗时"
                    :value="statistics.totalDuration || 0"
                    suffix="ms"
                  />
                </a-col>
                <a-col :span="8">
                  <a-statistic
                    title="节点数"
                    :value="statistics.nodeCount || 0"
                  />
                </a-col>
              </a-row>
              <a-divider />
              <a-table
                :columns="nodeTimeColumns"
                :data-source="statistics.nodeDurations || []"
                :pagination="false"
                size="small"
                row-key="nodeId"
              />
              <a-empty v-if="!statistics.nodeDurations?.length" description="暂无统计数据" />
            </a-tab-pane>
          </a-tabs>
        </div>
        <div class="card-wrap" v-else>
          <a-empty description="请选择一个运行中的流程实例" />
        </div>
      </a-col>
    </a-row>

    <!-- 干预弹窗 -->
    <a-modal v-model:open="interveneVisible" title="管理员干预" @ok="handleIntervene">
      <a-form layout="vertical">
        <a-form-item label="目标节点ID" required>
          <a-input v-model:value="interveneForm.targetNodeId" placeholder="请输入目标节点ID" />
        </a-form-item>
        <a-form-item label="干预原因" required>
          <a-textarea v-model:value="interveneForm.reason" :rows="3" placeholder="请输入干预原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import {
  getRunningProcesses,
  getExecutionHistory,
  getVariableHistory,
  getMonitorStatistics,
  exportMonitorData,
  interveneProcess
} from '../../api/monitor'
import { formatDate } from '../../utils/date'
import { usePermission } from '../../composables/usePermission'

const { hasPerm } = usePermission()
const runningLoading = ref(false)
const runningList = ref([])
const selectedInstance = ref(null)
const detailTab = ref('history')

const historyList = ref([])
const variableList = ref([])
const statistics = reactive({ totalDuration: 0, nodeCount: 0, nodeDurations: [] })

const interveneVisible = ref(false)
const interveneForm = reactive({ targetNodeId: '', reason: '' })

const varColumns = [
  { title: '变量名', dataIndex: 'name', key: 'name' },
  { title: '值', dataIndex: 'value', key: 'value' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 }
]

const nodeTimeColumns = [
  { title: '节点ID', dataIndex: 'nodeId', key: 'nodeId' },
  { title: '节点名', dataIndex: 'nodeName', key: 'nodeName' },
  { title: '耗时(ms)', dataIndex: 'duration', key: 'duration', width: 120 }
]

async function loadRunning() {
  runningLoading.value = true
  try {
    const res = await getRunningProcesses()
    const data = res.data || res
    runningList.value = data.list || data.records || (Array.isArray(data) ? data : [])
  } catch {
    // ignore
  }
  runningLoading.value = false
}

async function selectInstance(instance) {
  selectedInstance.value = instance
  detailTab.value = 'history'
  await Promise.all([
    loadHistory(instance.id),
    loadVariables(instance.id),
    loadStatistics(instance.id)
  ])
}

async function loadHistory(id) {
  try {
    const res = await getExecutionHistory(id)
    const data = res.data || res
    historyList.value = Array.isArray(data) ? data : (data.list || [])
  } catch {
    historyList.value = []
  }
}

async function loadVariables(id) {
  try {
    const res = await getVariableHistory(id)
    const data = res.data || res
    variableList.value = Array.isArray(data) ? data : (data.list || [])
  } catch {
    variableList.value = []
  }
}

async function loadStatistics(id) {
  try {
    const res = await getMonitorStatistics(id)
    const data = res.data || res
    Object.assign(statistics, data)
  } catch {
    statistics.totalDuration = 0
    statistics.nodeCount = 0
    statistics.nodeDurations = []
  }
}

function showInterveneModal() {
  interveneForm.targetNodeId = ''
  interveneForm.reason = ''
  interveneVisible.value = true
}

async function handleIntervene() {
  if (!interveneForm.targetNodeId || !interveneForm.reason) {
    message.warning('请填写必填项')
    return
  }
  try {
    await interveneProcess(selectedInstance.value.id, interveneForm)
    message.success('干预成功')
    interveneVisible.value = false
    selectInstance(selectedInstance.value)
    loadRunning()
  } catch {
    // ignore
  }
}

async function handleExport() {
  try {
    await exportMonitorData(selectedInstance.value.id)
    message.success('导出成功')
  } catch {
    // ignore
  }
}

// 初始化加载
loadRunning()
</script>

<style scoped>
.active-item {
  background: var(--color-primary-light);
}
</style>
