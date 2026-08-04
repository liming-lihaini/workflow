<template>
  <div>
    <a-card :bordered="false" class="toolbar">
      <a-space class="mb12">
        <a-segmented v-model:value="statusFilter" :options="statusOptions" @change="reload" />
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
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openDetail(record)">查看</a>
              <template v-if="record.status === '待审核'">
                <a-button type="link" size="small" @click="onApprove(record)">通过</a-button>
                <a-button type="link" size="small" danger @click="onReject(record)">退回</a-button>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer
      v-model:open="detailOpen"
      :title="`报告详情 - ${current && current.title ? current.title : ''}`"
      width="760"
      @close="detailOpen = false"
    >
      <template v-if="current">
        <a-descriptions bordered size="small" :column="2">
          <a-descriptions-item label="报告编号">{{ current.reportNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(current.status)">{{ current.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="委托单位">{{ current.client }}</a-descriptions-item>
          <a-descriptions-item label="报告周期">{{ current.period }}</a-descriptions-item>
          <a-descriptions-item label="明细项数">{{ current.itemCount }}</a-descriptions-item>
          <a-descriptions-item label="超标项数">
            <span :style="{ color: current.exceedCount > 0 ? '#cf1322' : '#389e0d' }">{{ current.exceedCount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="生成人">{{ current.generator }}</a-descriptions-item>
          <a-descriptions-item label="防伪码">{{ current.antiFakeCode || '—' }}</a-descriptions-item>
        </a-descriptions>

        <a-divider class="title-divider">检测明细</a-divider>
        <a-table
          rowKey="id"
          :columns="itemColumns"
          :dataSource="items"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'conclusion'">
              <a-tag :color="record.conclusion === '超标' ? 'red' : 'green'">{{ record.conclusion }}</a-tag>
            </template>
          </template>
        </a-table>

        <a-divider class="title-divider">审核记录</a-divider>
        <a-timeline v-if="audits.length">
          <a-timeline-item v-for="a in audits" :key="a.id">
            <b>{{ a.auditor }}</b> · {{ a.decision }} · {{ a.createTime }}
            <div style="color:#8c8c8c">{{ a.opinion }}</div>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else description="暂无审核记录" />
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getReports,
  getReportDetail,
  approveReport,
  rejectReport
} from '../../../api/ems'

const loading = ref(false)
const statusFilter = ref('')
const reports = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: '待审核' },
  { label: '已发布', value: '已发布' },
  { label: '已退回', value: '已退回' }
]

const columns = [
  { title: '报告编号', dataIndex: 'reportNo' },
  { title: '标题', dataIndex: 'title' },
  { title: '类型', dataIndex: 'tplType' },
  { title: '委托单位', dataIndex: 'client' },
  { title: '周期', dataIndex: 'period' },
  { title: '明细项', dataIndex: 'itemCount' },
  { title: '超标项', key: 'exceedCount' },
  { title: '状态', key: 'status' },
  { title: '生成人', dataIndex: 'generator' },
  { title: '操作', key: 'action' }
]

const itemColumns = [
  { title: '监测项目', dataIndex: 'item' },
  { title: '样品编号', dataIndex: 'sampleCode' },
  { title: '结果', dataIndex: 'result' },
  { title: '单位', dataIndex: 'unit' },
  { title: '标准限值', dataIndex: 'standardLimit' },
  { title: '结论', key: 'conclusion' }
]

const detailOpen = ref(false)
const current = ref(null)
const items = ref([])
const audits = ref([])

const auditor = localStorage.getItem('realName') || localStorage.getItem('username') || 'sys_admin'

function statusColor(s) {
  if (s === '已发布') return 'green'
  if (s === '已退回') return 'red'
  return 'orange'
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

async function openDetail(record) {
  const res = await getReportDetail(record.id)
  const d = res.data
  current.value = d.report
  items.value = d.items || []
  audits.value = d.audits || []
  detailOpen.value = true
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

async function onReject(record) {
  const opinion = window.prompt('请输入退回意见')
  if (opinion === null) return
  try {
    await rejectReport(record.id, { auditor, opinion: opinion || '不符合要求' })
    message.success('已退回')
    await reload()
  } catch (e) {
    message.error('操作失败：' + (e.response?.data?.message || e.message))
  }
}

onMounted(reload)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.mb12 { margin-bottom: 12px; }
</style>
