<template>
  <div class="page">
    <a-card :bordered="false">
      <template #title>留样库管理</template>
      <template #extra>
        <a-alert
          type="warning"
          show-icon
          style="display:inline-block;width:auto;margin-right:12px;"
          :message="`${expiringCount} 个留样将在 ${threshold} 天内到期`"
        />
        <a-input-number v-model:value="threshold" :min="1" :max="90" style="width:90px" @change="loadExpiring" />
        <a-button style="margin-left:8px;" @click="loadExpiring">预警刷新</a-button>
      </template>

      <a-form layout="inline" class="filter">
        <a-form-item label="状态">
          <a-select v-model:value="filters.status" style="width:140px" allow-clear @change="loadData">
            <a-select-option value="留样中">留样中</a-select-option>
            <a-select-option value="已处置">已处置</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关键字">
          <a-input v-model:value="filters.keyword" placeholder="样品名称/条码" @press-enter="loadData" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button style="margin-left:8px;" @click="resetFilter">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
        style="margin-top:12px;"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '留样中' ? 'purple' : 'default'">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'retainUntil'">
            <span :style="{ color: isExpiring(record) ? '#cf1322' : 'inherit' }">{{ record.retainUntil }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              type="link"
              danger
              :disabled="record.status !== '留样中'"
              @click="openDispose(record)"
            >处置</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 处置 -->
    <a-modal
      v-model:open="disposeOpen"
      title="留样处置"
      @ok="submitDispose"
      @cancel="disposeOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="样品条码">
          <span>{{ current?.barcode }}</span>
        </a-form-item>
        <a-form-item label="留样到期">
          <span>{{ current?.retainUntil }}</span>
        </a-form-item>
        <a-form-item label="处置人" required>
          <a-input v-model:value="disposeForm.disposeBy" />
        </a-form-item>
        <a-form-item label="处置时间">
          <a-date-picker v-model:value="disposeDate" value-format="YYYY-MM-DD" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getRetains, getExpiringRetains, disposeRetain } from '../../../api/ems'

const loading = ref(false)
const list = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: undefined, keyword: '' })

const threshold = ref(7)
const expiringCount = ref(0)

const columns = [
  { title: '样品条码', dataIndex: 'barcode', key: 'barcode' },
  { title: '样品名称', dataIndex: 'name', key: 'name' },
  { title: '留样人', dataIndex: 'retainBy', key: 'retainBy' },
  { title: '留样时间', dataIndex: 'retainTime', key: 'retainTime' },
  { title: '留样天数', dataIndex: 'retainDays', key: 'retainDays' },
  { title: '留样到期', key: 'retainUntil' },
  { title: '状态', key: 'status' },
  { title: '处置人', dataIndex: 'disposeBy', key: 'disposeBy' },
  { title: '操作', key: 'action', width: 100 }
]

const disposeOpen = ref(false)
const disposeForm = reactive({ disposeBy: '' })
const disposeDate = ref(null)
const current = ref(null)
const submitting = ref(false)

function isExpiring(record) {
  if (!record.retainUntil) return false
  const due = new Date(record.retainUntil).getTime()
  const line = Date.now() + threshold.value * 86400000
  return record.status === '留样中' && due <= line
}

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function resetFilter() {
  filters.status = undefined
  filters.keyword = ''
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getRetains({
      page: pagination.current,
      size: pagination.pageSize,
      status: filters.status,
      keyword: filters.keyword || undefined
    })
    const page = res.data || {}
    list.value = page.records || []
    pagination.total = page.total || 0
  } finally {
    loading.value = false
  }
}

async function loadExpiring() {
  const res = await getExpiringRetains({ thresholdDays: threshold.value })
  expiringCount.value = (res.data || []).length
}

function openDispose(record) {
  current.value = record
  disposeForm.disposeBy = ''
  disposeDate.value = null
  disposeOpen.value = true
}

async function submitDispose() {
  if (!disposeForm.disposeBy) return message.warning('请填写处置人')
  submitting.value = true
  try {
    await disposeRetain(current.value.id, {
      disposeBy: disposeForm.disposeBy,
      disposeTime: disposeDate.value
    })
    message.success('处置完成')
    disposeOpen.value = false
    loadData()
    loadExpiring()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
  loadExpiring()
})
</script>

<style scoped>
.page { padding: 4px; }
.filter { margin-bottom: 12px; }
</style>
