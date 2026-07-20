<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">部门管理</span>
        <a-button type="primary" @click="showModal()">新建部门</a-button>
      </div>

      <!-- 搜索区域 -->
      <div class="search-bar">
        <a-input
          v-model:value="searchForm.deptName"
          placeholder="部门名称"
          style="width: 200px"
          allow-clear
          @press-enter="handleSearch"
        />
        <a-select
          v-model:value="searchForm.status"
          placeholder="状态"
          style="width: 120px"
          allow-clear
        >
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="showModal(record, 'add')">添加子部门</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="showModal(record, 'edit')">编辑</span>
            <a-divider type="vertical" />
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
              <span class="action-link danger">删除</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="部门名称" required>
          <a-input v-model:value="formState.deptName" placeholder="请输入部门名称" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formState.sortOrder" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formState.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getDeptsPage, createDept, updateDept, deleteDept } from '../../api/system'

const loading = ref(false)
const dataList = ref([])
const modalVisible = ref(false)
const submitLoading = ref(false)
const modalTitle = ref('')
const editMode = ref('add') // add | edit | addChild
const parentRecord = ref(null)
const editingRecord = ref(null)

// 搜索表单
const searchForm = reactive({
  deptName: '',
  status: undefined
})

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: '部门名称', dataIndex: 'deptName', key: 'deptName' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 240 }
]

const formState = reactive({
  deptName: '',
  sortOrder: 0,
  status: 1
})

function showModal(record, mode) {
  formState.deptName = ''
  formState.sortOrder = 0
  formState.status = 1

  if (mode === 'edit' && record) {
    editMode.value = 'edit'
    editingRecord.value = record
    parentRecord.value = null
    formState.deptName = record.deptName
    formState.sortOrder = record.sortOrder
    formState.status = record.status
    modalTitle.value = '编辑部门'
  } else if (mode === 'add' && record) {
    editMode.value = 'addChild'
    parentRecord.value = record
    editingRecord.value = null
    modalTitle.value = `添加子部门（${record.deptName}）`
  } else {
    editMode.value = 'add'
    parentRecord.value = null
    editingRecord.value = null
    modalTitle.value = '新建部门'
  }
  modalVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDeptsPage({
      deptName: searchForm.deptName || undefined,
      status: searchForm.status,
      page: pagination.current,
      size: pagination.pageSize
    })
    const data = res.data || res
    dataList.value = data.list || []
    pagination.total = data.total || 0
  } catch {
    // ignore
  }
  loading.value = false
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  searchForm.deptName = ''
  searchForm.status = undefined
  pagination.current = 1
  loadData()
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

async function handleSubmit() {
  if (!formState.deptName) {
    message.warning('请输入部门名称')
    return
  }
  submitLoading.value = true
  try {
    const payload = { ...formState }
    if (editMode.value === 'addChild' && parentRecord.value) {
      payload.parentId = parentRecord.value.id
    }
    if (editMode.value === 'edit' && editingRecord.value) {
      await updateDept(editingRecord.value.id, payload)
      message.success('更新成功')
    } else {
      await createDept(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // ignore
  }
  submitLoading.value = false
}

async function handleDelete(record) {
  try {
    await deleteDept(record.id)
    message.success('删除成功')
    loadData()
  } catch {
    // ignore
  }
}

onMounted(loadData)
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
