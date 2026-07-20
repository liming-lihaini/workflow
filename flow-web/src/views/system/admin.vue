<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 三员列表 -->
        <a-tab-pane key="users" tab="三员列表">
          <div class="page-header">
            <a-space>
              <a-select
                v-model:value="adminType"
                placeholder="管理员类型"
                style="width: 150px"
                allow-clear
                @change="loadUsers"
              >
                <a-select-option :value="1">系统管理员</a-select-option>
                <a-select-option :value="2">安全管理员</a-select-option>
                <a-select-option :value="3">审计管理员</a-select-option>
              </a-select>
              <a-button @click="loadUsers">查询</a-button>
            </a-space>
          </div>
          <a-table
            :columns="userColumns"
            :data-source="userList"
            :loading="userLoading"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'adminType'">
                <a-tag :color="typeColor(record.adminType)">{{ typeName(record.adminType) }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 审计日志 -->
        <a-tab-pane key="audit" tab="审计日志">
          <div class="page-header">
            <a-space>
              <a-input-search
                v-model:value="auditQuery.module"
                placeholder="搜索模块"
                style="width: 160px"
                @search="loadAuditLogs"
              />
              <a-button @click="loadAuditLogs">查询</a-button>
            </a-space>
          </div>
          <a-table
            :columns="auditColumns"
            :data-source="auditList"
            :loading="auditLoading"
            :pagination="auditPagination"
            row-key="id"
            size="small"
            @change="handleAuditTableChange"
          />
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTripleAdminUsers, getTripleAdminAuditLogs } from '../../api/admin'

const activeTab = ref('users')
const adminType = ref(null)

// 三员列表
const userLoading = ref(false)
const userList = ref([])
const userColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username' },
  { title: '姓名', dataIndex: 'realName' },
  { title: '管理员类型', key: 'adminType', width: 140 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 }
]

// 审计日志
const auditLoading = ref(false)
const auditList = ref([])
const auditQuery = reactive({ module: '' })
const auditPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})
const auditColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '用户', dataIndex: 'username', width: 100 },
  { title: '模块', dataIndex: 'module', width: 100 },
  { title: '操作', dataIndex: 'operation', width: 120 },
  { title: '结果', dataIndex: 'result', width: 80 },
  { title: '操作时间', dataIndex: 'operationTime', width: 180 }
]

function typeColor(type) {
  const map = { 1: 'blue', 2: 'orange', 3: 'green' }
  return map[type] || 'default'
}

function typeName(type) {
  const map = { 1: '系统管理员', 2: '安全管理员', 3: '审计管理员' }
  return map[type] || type
}

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await getTripleAdminUsers({ adminType: adminType.value })
    const data = res.data || res
    userList.value = data.list || data.records || (Array.isArray(data) ? data : [])
  } catch {
    // ignore
  }
  userLoading.value = false
}

async function loadAuditLogs() {
  auditLoading.value = true
  try {
    const res = await getTripleAdminAuditLogs({
      page: auditPagination.current,
      size: auditPagination.pageSize,
      module: auditQuery.module || undefined
    })
    const data = res.data || res
    auditList.value = data.list || data.records || []
    auditPagination.total = data.total || auditList.value.length
  } catch {
    // ignore
  }
  auditLoading.value = false
}

function handleAuditTableChange(pag) {
  auditPagination.current = pag.current
  auditPagination.pageSize = pag.pageSize
  loadAuditLogs()
}

onMounted(() => {
  loadUsers()
  loadAuditLogs()
})
</script>
