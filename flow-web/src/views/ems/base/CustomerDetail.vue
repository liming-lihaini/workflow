<template>
  <div class="page-container">
    <div class="page-toolbar">
      <a-button @click="goBack">
        <template #icon><span class="btn-icon">←</span></template>
        返回
      </a-button>
      <span class="page-title">{{ detail.customer ? detail.customer.custName : '' }} · 客户详情</span>
    </div>

    <a-card class="block" size="small" title="基本信息">
      <a-skeleton v-if="loading" active />
      <a-descriptions v-else :column="2" bordered size="small" :label-style="{ width: '120px' }">
        <a-descriptions-item label="客户名称">{{ c.custName || '—' }}</a-descriptions-item>
        <a-descriptions-item label="统一信用代码">{{ c.creditCode || '—' }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ c.contact || '—' }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ c.tel || '—' }}</a-descriptions-item>
        <a-descriptions-item label="所在城市">{{ c.city || '—' }}</a-descriptions-item>
        <a-descriptions-item label="详细地址">{{ c.address || '—' }}</a-descriptions-item>
        <a-descriptions-item label="发票抬头">{{ c.invoiceTitle || '—' }}</a-descriptions-item>
        <a-descriptions-item label="税号">{{ c.taxNo || '—' }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="c.status === 1 ? 'green' : 'red'">{{ c.status === 1 ? '启用' : '停用' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ c.createTime || '—' }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ c.updateTime || '—' }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card class="block" size="small" title="检测委托清单">
      <a-table
        :columns="columns"
        :data-source="entrusts"
        :loading="loading"
        :pagination="{ pageSize: 10, showTotal: (t) => `共 ${t} 条` }"
        row-key="id"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'urgent'">
            <a-tag :color="record.urgent === 1 ? 'red' : 'default'">{{ record.urgent === 1 ? '紧急' : '普通' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="goEntrustDetail(record)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCustomerDetail } from '../../../api/ems'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = reactive({ customer: null, entrusts: [] })

const c = computed(() => detail.customer || {})
const entrusts = computed(() => detail.entrusts || [])

const columns = [
  { title: '委托编号', dataIndex: 'entrustNo', key: 'entrustNo', width: 160 },
  { title: '委托名称', dataIndex: 'entrustName', key: 'entrustName', ellipsis: true },
  { title: '来源', dataIndex: 'sourceName', key: 'sourceName', width: 100 },
  { title: '采集频率', dataIndex: 'sampleFreqName', key: 'sampleFreqName', width: 110 },
  { title: '紧急', dataIndex: 'urgent', key: 'urgent', width: 80, align: 'center' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'createName', key: 'createName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

function statusColor(s) {
  switch (s) {
    case '草稿': return 'default'
    case '已提交': return 'blue'
    case '已确认': return 'cyan'
    case '已退回': return 'orange'
    case '已完成': return 'green'
    default: return 'default'
  }
}

function load() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  getCustomerDetail(id).then((res) => {
    const data = res.data || res
    detail.customer = data.customer || null
    detail.entrusts = data.entrusts || []
  }).catch(() => {}).finally(() => { loading.value = false })
}

function goBack() {
  router.push({ name: 'EmsCustomer' })
}

function goEntrustDetail(record) {
  router.push({ name: 'EmsEntrustDetail', params: { id: record.id } })
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
  background-color: #FFF;
}
.page-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.block {
  margin-bottom: 16px;
}
.btn-icon {
  display: inline-block;
}
</style>
