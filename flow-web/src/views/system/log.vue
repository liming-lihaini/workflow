<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 访问日志 -->
        <a-tab-pane key="access" tab="访问日志">
          <div class="page-header">
            <a-space>
              <a-input-search
                v-model:value="accessQuery.keyword"
                placeholder="搜索URL/IP"
                style="width: 200px"
                @search="loadAccessLogs"
              />
              <a-button @click="loadAccessLogs">查询</a-button>
              <a-button @click="handleExportAccess">导出</a-button>
            </a-space>
          </div>
          <a-table
            :columns="accessColumns"
            :data-source="accessList"
            :loading="accessLoading"
            :pagination="accessPagination"
            row-key="id"
            size="small"
            @change="handleAccessTableChange"
          />
        </a-tab-pane>

        <!-- 操作日志 -->
        <a-tab-pane key="operation" tab="操作日志">
          <div class="page-header">
            <a-space>
              <a-input-search
                v-model:value="opQuery.keyword"
                placeholder="搜索模块/操作"
                style="width: 200px"
                @search="loadOperationLogs"
              />
              <a-button @click="loadOperationLogs">查询</a-button>
              <a-button @click="handleExportOperation">导出</a-button>
            </a-space>
          </div>
          <a-table
            :columns="opColumns"
            :data-source="opList"
            :loading="opLoading"
            :pagination="opPagination"
            row-key="id"
            size="small"
            @change="handleOpTableChange"
          />
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getAccessLogs, getOperationLogs, exportAccessLogs, exportOperationLogs } from '../../api/log'

const activeTab = ref('access')

// 访问日志
const accessLoading = ref(false)
const accessList = ref([])
const accessQuery = reactive({ keyword: '' })
const accessPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})
const accessColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '请求方式', dataIndex: 'method', width: 80 },
  { title: 'URL', dataIndex: 'url', ellipsis: true },
  { title: 'IP', dataIndex: 'ip', width: 140 },
  { title: '用户', dataIndex: 'username', width: 100 },
  { title: '结果', dataIndex: 'result', width: 80 },
  { title: '访问时间', dataIndex: 'accessTime', width: 180 }
]

// 操作日志
const opLoading = ref(false)
const opList = ref([])
const opQuery = reactive({ keyword: '' })
const opPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})
const opColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '模块', dataIndex: 'module', width: 100 },
  { title: '操作', dataIndex: 'operation', width: 120 },
  { title: '用户', dataIndex: 'username', width: 100 },
  { title: '结果', dataIndex: 'result', width: 80 },
  { title: '操作时间', dataIndex: 'operationTime', width: 180 }
]

async function loadAccessLogs() {
  accessLoading.value = true
  try {
    const res = await getAccessLogs({
      page: accessPagination.current,
      size: accessPagination.pageSize,
      keyword: accessQuery.keyword || undefined
    })
    const data = res.data || res
    accessList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    accessPagination.total = data.total || accessList.value.length
  } catch {
    // ignore
  }
  accessLoading.value = false
}

async function loadOperationLogs() {
  opLoading.value = true
  try {
    const res = await getOperationLogs({
      page: opPagination.current,
      size: opPagination.pageSize,
      keyword: opQuery.keyword || undefined
    })
    const data = res.data || res
    opList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    opPagination.total = data.total || opList.value.length
  } catch {
    // ignore
  }
  opLoading.value = false
}

async function handleExportAccess() {
  try {
    await exportAccessLogs({})
    message.success('导出成功')
  } catch {
    // ignore
  }
}

async function handleExportOperation() {
  try {
    await exportOperationLogs({})
    message.success('导出成功')
  } catch {
    // ignore
  }
}

function handleAccessTableChange(pag) {
  accessPagination.current = pag.current
  accessPagination.pageSize = pag.pageSize
  loadAccessLogs()
}

function handleOpTableChange(pag) {
  opPagination.current = pag.current
  opPagination.pageSize = pag.pageSize
  loadOperationLogs()
}

onMounted(() => {
  loadAccessLogs()
  loadOperationLogs()
})
</script>
