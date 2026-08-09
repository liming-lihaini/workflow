<template>
  <div class="page">
    <!-- 页面标题 -->
    <div class="page-title">
      <h2>样品管理 / 留样库</h2>
      <div class="title-actions">
        <a-button>导出留样台账</a-button>
        <a-button type="primary">
          批量销毁申请<span v-if="pendingDisposeCount" class="badge">{{ pendingDisposeCount }}</span>
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-label">在库留样</div>
        <div class="stat-value">{{ stats.inStock }}<span class="stat-unit">份</span></div>
      </div>
      <div class="stat-card stat-warn">
        <div class="stat-label">3日内到期</div>
        <div class="stat-value">{{ stats.expireSoon }}<span class="stat-unit">份</span></div>
      </div>
      <div class="stat-card stat-info">
        <div class="stat-label">待销毁审批</div>
        <div class="stat-value">{{ stats.pendingDispose }}<span class="stat-unit">份</span></div>
      </div>
      <div class="stat-card stat-blue">
        <div class="stat-label">本月复检领用</div>
        <div class="stat-value">{{ stats.monthlyReuse }}<span class="stat-unit">次</span></div>
      </div>
    </div>

    <!-- 留样列表 -->
    <div class="card-section">
      <a-table
        :columns="columns"
        :data-source="filteredList"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'retainNo'">
            <span class="link">{{ record.retainNo || record.barcode || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'sampleRef'">
            <span>{{ record.barcode || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'category'">
            <span>{{ record.category || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'location'">
            <span>{{ record.retainLocation || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'retainRange'">
            <span>{{ record.retainTime || '-' }} ~ {{ record.retainUntil || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'remainDays'">
            <span :class="{ 'remain-warn': (record.remainDays ?? 99) <= 3 }">{{ formatRemainDays(record) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status, record)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDisposeModal(record)" >销毁申请</a-button>
            <a-button type="link" size="small" @click="onReuse(record)" >领用复检</a-button>
            <a-button type="link" size="small" @click="onViewFlow(record)" v-if="record.status === '销毁审批中' || record.status === '已销毁'">查看流程</a-button>
            <a-popconfirm title="确认删除该留样记录？" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 销毁申请弹窗 -->
    <a-modal v-model:open="disposeModalOpen" title="留样销毁申请" @ok="submitDispose" :confirm-loading="disposeSubmitting" width="560px">
      <a-form layout="vertical" v-if="currentRetain">
        <div class="dispose-info-bar">
          <span>留样编码：<b>{{ currentRetain.retainNo }}</b></span>
          <span>样品条码：<b>{{ currentRetain.barcode }}</b></span>
          <span>监测类别：<b>{{ currentRetain.category || '-' }}</b></span>
        </div>
        <a-form-item label="销毁原因" required>
          <a-textarea v-model:value="disposeForm.disposeReason" placeholder="请填写销毁原因" :rows="3" />
        </a-form-item>
        <a-form-item label="销毁方式" required>
          <a-select v-model:value="disposeForm.disposeMethod" placeholder="选择销毁方式">
            <a-select-option value="焚烧">焚烧</a-select-option>
            <a-select-option value="安全填埋">安全填埋</a-select-option>
            <a-select-option value="化学处理">化学处理</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="预计销毁日期" required>
          <a-date-picker v-model:value="disposeForm.disposeDate" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 设计说明 -->
    <div class="design-note">
      <div class="note-title">设计说明：</div>
      <ol>
        <li>留样期按监测类别默认规则自动计算（可配置），到期前 3 天推送预警。</li>
        <li>销毁走审批流程（留样管理员发起 → 技术负责人审批），销毁记录进入合规台账，不可删除。</li>
        <li>复检领用后新数据自动与原检测数据并列展示，供偏差比对。</li>
      </ol>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getRetains, getRetainStats, applyDispose, deleteRetain } from '../../../api/ems'
import { useUserStore } from '../../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const searchText = ref('')
const statusFilter = ref('all')

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const stats = reactive({ inStock: 0, expireSoon: 0, pendingDispose: 0, monthlyReuse: 0 })
const pendingDisposeCount = ref(0)

// 销毁弹窗
const disposeModalOpen = ref(false)
const disposeSubmitting = ref(false)
const currentRetain = ref(null)
const disposeForm = reactive({ disposeReason: '', disposeMethod: undefined, disposeDate: '' })

const columns = [
  { title: '留样编码', dataIndex: 'retainNo', key: 'retainNo', width: 150 },
  { title: '关联样品', dataIndex: 'barcode', key: 'sampleRef', width: 180 },
  { title: '监测类别', dataIndex: 'category', key: 'category', width: 110 },
  { title: '库位', dataIndex: 'retainLocation', key: 'location', width: 150 },
  { title: '留样起止', key: 'retainRange', width: 220 },
  { title: '剩余天数', key: 'remainDays', width: 100 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const [res, stRes] = await Promise.all([
      getRetains({ page: pagination.current, size: pagination.pageSize }),
      getRetainStats()
    ])
    const page = res.data || {}
    list.value = (page.records || []).map(r => ({
      ...r,
      remainDays: computeRemainDays(r.retainUntil)
    }))
    pagination.total = page.total || 0
    const s = stRes.data || {}
    stats.inStock = s.inStock || 0
    stats.expireSoon = s.expireSoon || 0
    stats.pendingDispose = s.pendingDispose || 0
    stats.monthlyReuse = s.monthlyReuse || 0
    pendingDisposeCount.value = stats.pendingDispose
  } finally {
    loading.value = false
  }
}

function computeRemainDays(until) {
  if (!until) return null
  const u = new Date(until)
  if (isNaN(u.getTime())) return null
  const now = new Date(); now.setHours(0, 0, 0, 0)
  u.setHours(0, 0, 0, 0)
  return Math.round((u - now) / 86400000)
}

function formatRemainDays(r) {
  if (r.remainDays === null || r.remainDays === undefined) return '-'
  if (r.remainDays < 0) return `已超期 ${Math.abs(r.remainDays)} 天`
  return `${r.remainDays} 天`
}

function statusLabel(s) {
  return ({ '留样中': '在库', '已处置': '已处置', '已销毁': '已销毁', '销毁审批中': '销毁审批中' })[s] || s
}

function statusColor(s, r) {
  if (s === '销毁审批中') return 'blue'
  if (s === '已销毁' || s === '已处置') return 'default'
  if ((r?.remainDays ?? 99) <= 3) return 'orange'
  return 'green'
}

const filteredList = computed(() => {
  let arr = list.value
  if (statusFilter.value !== 'all') arr = arr.filter(r => r.status === statusFilter.value)
  if (searchText.value) {
    const k = searchText.value.toLowerCase()
    arr = arr.filter(r => (r.barcode || '').toLowerCase().includes(k))
  }
  return arr
})

// 销毁申请：打开编号为 LYXHSQ 的流程，并自动回填留样编号与样品编号
function openDisposeModal(record) {
  currentRetain.value = record
  router.push({
    path: '/task/start-detail',
    query: {
      processKey: 'LYXHSQ',
      retainNo: record.retainNo || '',
      sampleNo: record.barcode || record.sampleId || '',
    },
  })
}

async function submitDispose() {
  if (!disposeForm.disposeReason) return message.warning('请填写销毁原因')
  if (!disposeForm.disposeMethod) return message.warning('请选择销毁方式')
  if (!disposeForm.disposeDate) return message.warning('请选择预计销毁日期')
  disposeSubmitting.value = true
  try {
    const startUser = userStore.username || localStorage.getItem('username') || 'admin'
    await applyDispose(currentRetain.value.id, startUser, {
      disposeReason: disposeForm.disposeReason,
      disposeMethod: disposeForm.disposeMethod,
      disposeDate: disposeForm.disposeDate
    })
    message.success('销毁申请已提交，等待技术负责人审批')
    disposeModalOpen.value = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    disposeSubmitting.value = false
  }
}

function onReuse(record) {
  stats.monthlyReuse += 1
  message.info('领用复检功能开发中')
}

function onViewFlow(record) {
  if (record.processInstanceId) {
    router.push({ path: '/task/my-request', query: { instanceId: record.processInstanceId } })
  } else {
    message.info('暂无关联流程')
  }
}

function handleDelete(record) {
  deleteRetain(record.id).then(() => {
    message.success('留样记录已删除')
    loadData()
  }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 16px; }
.page-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title h2 { margin: 0; font-size: 18px; font-weight: 600; color: #333; }
.title-actions { display: flex; gap: 8px; align-items: center; }
.title-actions .badge { display: inline-block; margin-left: 4px; min-width: 18px; padding: 0 6px; height: 18px; line-height: 18px; background: #fff; color: #fa8c16; border-radius: 9px; font-size: 12px; font-weight: 600; text-align: center; }

.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.stat-card { background: #fff; border: 1px solid #f0f0f0; border-radius: 8px; padding: 16px 20px; }
.stat-label { font-size: 13px; color: #888; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 600; color: #333; line-height: 1; }
.stat-unit { font-size: 13px; font-weight: 400; color: #999; margin-left: 4px; }
.stat-warn .stat-value { color: #fa541c; }
.stat-info .stat-value { color: #fa8c16; }
.stat-blue .stat-value { color: #1677ff; }

.card-section { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
.link { color: #1677ff; cursor: pointer; font-family: 'Courier New', monospace; }
.remain-warn { color: #fa541c; font-weight: 600; }

.dispose-info-bar { display: flex; flex-wrap: wrap; gap: 16px; padding: 10px 14px; background: #f5f7fa; border-radius: 6px; margin-bottom: 16px; font-size: 13px; color: #666; }
.dispose-info-bar b { color: #333; }

.design-note { margin-top: 16px; padding: 14px 20px; background: #f6ffed; border: 1px dashed #b7eb8f; border-radius: 8px; color: #555; font-size: 13px; line-height: 1.8; }
.design-note .note-title { color: #389e0d; font-weight: 600; margin-bottom: 4px; }
.design-note ol { margin: 0; padding-left: 22px; }
</style>
