<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">已办任务</span>
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
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 2 ? 'green' : 'orange'">{{ record.status === 2 ? '已完成' : '处理中' }}</a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getDoneTasks } from '../../api/task'

const loading = ref(false)
const dataList = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '流程实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId' },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName' },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee' },
  { title: '状态', key: 'status', width: 100 },
  { title: '完成时间', dataIndex: 'completeTime', key: 'completeTime', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 }
]

async function loadData() {
  loading.value = true
  try {
    const res = await getDoneTasks({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
  } catch {
    // ignore
  }
  loading.value = false
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(loadData)
</script>
