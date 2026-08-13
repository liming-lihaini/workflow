<template>
  <div>
    <a-card :bordered="false" class="toolbar">
      <a-space class="mb12">
        <a-segmented v-model:value="statusFilter" :options="statusOptions" @change="reload" />
        <a-button v-if="hasPerm('ems:report:create')" type="primary" @click="openCreate"><plus-outlined /> 生成报告</a-button>
      </a-space>

      <a-table
        rowKey="id"
        :columns="columns"
        :dataSource="reports"
        :pagination="{ pageSize: 10 }"
        :loading="loading"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'exceedCount'">
            <span :style="{ color: record.exceedCount > 0 ? '#cf1322' : '#389e0d' }">
              {{ record.exceedCount }}
            </span>
          </template>
          <template v-else-if="column.key === 'reviewer'">{{ realName(record.reviewer) }}</template>
          <template v-else-if="column.key === 'approver'">{{ realName(record.approver) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openView(record)">查看</a>
              <a v-if="record.status === '草稿' && hasPerm('ems:report:submit')" @click="onSubmit(record)">提交</a>
              <template v-if="record.status === '待审核' && hasPerm('ems:report:audit')">
                <a-button type="link" size="small" @click="onApprove(record)">通过</a-button>
                <a-button type="link" size="small" danger @click="onReject(record)">退回</a-button>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 生成报告抽屉 -->
    <a-drawer
      v-model:open="createOpen"
      title="生成报告"
      width="760"
      :destroyOnClose="true"
      @close="createOpen = false"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="客户" required>
          <a-select
            v-model:value="form.customerId"
            show-search
            :options="customerOptions"
            :filter-option="filterOption"
            placeholder="请选择客户"
            allow-clear
            @change="onCustomerChange"
          />
        </a-form-item>
        <a-form-item label="检测委托" required>
          <a-select
            v-model:value="form.entrustId"
            show-search
            :options="entrustOptions"
            :filter-option="filterOption"
            :disabled="!form.customerId"
            placeholder="请选择检测委托"
            allow-clear
            @change="onEntrustChange"
          />
        </a-form-item>
        <a-form-item label="报告名称" required>
          <a-input v-model:value="form.name" placeholder="请输入报告名称" />
        </a-form-item>
        <a-form-item label="CMA 资质认定证书号" required>
          <a-input v-model:value="form.cmaCertNo" placeholder="如 2026***01Z" />
        </a-form-item>
        <a-form-item label="复核人" required>
          <a-select
            v-model:value="form.reviewer"
            show-search
            :options="userOptions"
            :filter-option="filterOption"
            placeholder="请选择复核人"
          />
        </a-form-item>
        <a-form-item label="批准人" required>
          <a-select
            v-model:value="form.approver"
            show-search
            :options="userOptions"
            :filter-option="filterOption"
            placeholder="请选择批准人"
          />
        </a-form-item>
      </a-form>

      <a-divider orientation="left">检测任务清单（委托关联样品）</a-divider>
      <a-table
        rowKey="taskId"
        :columns="taskColumns"
        :dataSource="entrustTasks"
        :loading="tasksLoading"
        :row-selection="{ selectedRowKeys, onChange: keys => (selectedRowKeys = keys) }"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '已复核' ? 'green' : 'default'">{{ record.status }}</a-tag>
          </template>
        </template>
      </a-table>

      <template #footer>
        <div style="text-align: right">
          <a-space>
            <a-button @click="createOpen = false">取消</a-button>
            <a-button :loading="saving" @click="onCreate(true)">保存草稿</a-button>
            <a-button type="primary" :loading="saving" @click="onCreate(false)">生成报告</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  getReports,
  approveReport,
  rejectReport,
  createMonitorReport,
  submitReportDraft,
  getEntrustReportTasks,
  getCustomers,
  getEntrusts
} from '../../../api/ems'
import { getUsers } from '../../../api/system'
import { useUserMap } from '../../../composables/useUserMap'
import { usePermission } from '../../../composables/usePermission'

const { hasPerm } = usePermission()
const router = useRouter()
const { realName, buildUserMap } = useUserMap()

const loading = ref(false)
const statusFilter = ref('')
const reports = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '草稿', value: '草稿' },
  { label: '待审核', value: '待审核' },
  { label: '已发布', value: '已发布' },
  { label: '已退回', value: '已退回' }
]

const columns = [
  { title: '报告编号', dataIndex: 'reportNo', width: 150 },
  { title: '报告名称', dataIndex: 'title' },
  { title: '委托单位', dataIndex: 'client' },
  { title: 'CMA 证书号', dataIndex: 'cmaCertNo', width: 120 },
  { title: '复核人', key: 'reviewer', width: 90 },
  { title: '批准人', key: 'approver', width: 90 },
  { title: '明细项', dataIndex: 'itemCount', width: 70 },
  { title: '超标项', key: 'exceedCount', width: 70 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 150 }
]

const taskColumns = [
  { title: '任务编号', dataIndex: 'taskNo', width: 140 },
  { title: '样品条码', dataIndex: 'barcode', width: 140 },
  { title: '样品名称', dataIndex: 'sampleName' },
  { title: '监测项目', dataIndex: 'monitorItems' },
  { title: '状态', key: 'status', width: 90 }
]

const auditor = localStorage.getItem('realName') || localStorage.getItem('username') || 'sys_admin'
const generator = localStorage.getItem('username') || 'sys_admin'

function statusColor(s) {
  if (s === '已发布') return 'green'
  if (s === '已退回') return 'red'
  if (s === '草稿') return 'default'
  return 'orange'
}

function filterOption(input, option) {
  return (option.label || '').toLowerCase().includes(String(input).toLowerCase())
}

async function reload() {
  loading.value = true
  try {
    const res = await getReports(statusFilter.value || '')
    reports.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openView(record) {
  router.push(`/ems/report/view/${record.id}`)
}

async function onSubmit(record) {
  try {
    await submitReportDraft(record.id)
    message.success('已提交审核')
    await reload()
  } catch (e) {
    message.error('提交失败：' + (e.response?.data?.message || e.message))
  }
}

async function onApprove(record) {
  try {
    await approveReport(record.id, { auditor })
    message.success('已通过并发布，防伪码已生成')
    await reload()
  } catch (e) {
    message.error('操作失败：' + (e.response?.data?.message || e.message))
  }
}

function onReject(record) {
  let opinion = ''
  Modal.confirm({
    title: '退回报告',
    content: '请输入退回意见',
    okText: '退回',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await rejectReport(record.id, { auditor, opinion: opinion || '不符合要求' })
        message.success('已退回')
        await reload()
      } catch (e) {
        message.error('操作失败：' + (e.response?.data?.message || e.message))
      }
    }
  })
}

/* ---------------- 生成报告抽屉 ---------------- */

const createOpen = ref(false)
const saving = ref(false)
const form = ref(emptyForm())
const customerOptions = ref([])
const entrustAll = ref([])
const entrustOptions = ref([])
const userOptions = ref([])
const entrustTasks = ref([])
const tasksLoading = ref(false)
const selectedRowKeys = ref([])

function emptyForm() {
  return { customerId: null, entrustId: null, name: '', cmaCertNo: '', reviewer: null, approver: null }
}

async function openCreate() {
  form.value = emptyForm()
  entrustOptions.value = []
  entrustTasks.value = []
  selectedRowKeys.value = []
  createOpen.value = true
  try {
    const [cres, eres, ures] = await Promise.all([getCustomers({}), getEntrusts({}), getUsers()])
    const cdata = cres.data || cres
    customerOptions.value = (Array.isArray(cdata) ? cdata : (cdata.list || cdata.records || [])).map(c => ({
      value: c.id,
      label: c.custName || c.custNo || String(c.id)
    }))
    const edata = eres.data || eres
    entrustAll.value = Array.isArray(edata) ? edata : (edata.list || edata.records || [])
    const udata = ures.data || ures
    userOptions.value = (Array.isArray(udata) ? udata : (udata.list || udata.records || [])).map(u => ({
      value: u.username || u.name || String(u.id),
      label: u.realName || u.name || u.username || String(u.id)
    }))
  } catch (e) {
    message.error('加载基础数据失败：' + (e.response?.data?.message || e.message))
  }
}

function onCustomerChange() {
  form.value.entrustId = null
  entrustTasks.value = []
  selectedRowKeys.value = []
  entrustOptions.value = entrustAll.value
    .filter(e => !form.value.customerId || Number(e.custId) === Number(form.value.customerId))
    .map(e => ({ value: e.id, label: `${e.entrustNo || ''} ${e.entrustName || ''}`.trim() }))
}

async function onEntrustChange() {
  selectedRowKeys.value = []
  entrustTasks.value = []
  if (!form.value.entrustId) return
  tasksLoading.value = true
  try {
    const res = await getEntrustReportTasks(form.value.entrustId)
    entrustTasks.value = res.data || []
    // 默认全选
    selectedRowKeys.value = entrustTasks.value.map(t => t.taskId)
  } finally {
    tasksLoading.value = false
  }
}

async function onCreate(draft) {
  const f = form.value
  if (!f.name || !f.name.trim()) { message.warning('请填写报告名称'); return }
  if (!f.cmaCertNo || !f.cmaCertNo.trim()) { message.warning('请填写 CMA 资质认定证书号'); return }
  if (!f.reviewer) { message.warning('请选择复核人'); return }
  if (!f.approver) { message.warning('请选择批准人'); return }
  if (!f.entrustId) { message.warning('请选择检测委托'); return }
  if (!selectedRowKeys.value.length) { message.warning('请至少勾选一个检测任务'); return }
  saving.value = true
  try {
    await createMonitorReport({
      name: f.name.trim(),
      cmaCertNo: f.cmaCertNo.trim(),
      reviewer: f.reviewer,
      approver: f.approver,
      entrustId: f.entrustId,
      taskIds: selectedRowKeys.value,
      generator,
      draft
    })
    message.success(draft ? '草稿已保存' : '报告已生成，状态为待审核')
    createOpen.value = false
    await reload()
  } catch (e) {
    message.error('操作失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  buildUserMap()
  reload()
})
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.mb12 { margin-bottom: 12px; }
</style>
