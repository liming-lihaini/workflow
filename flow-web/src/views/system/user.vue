<template>
  <div class="page-wrap">
    <a-row :gutter="16">
      <!-- 左侧部门树 -->
      <a-col :span="6">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">部门</span>
          </div>
          <a-tree
            :tree-data="deptTree"
            :field-names="{ title: 'deptName', key: 'id', children: 'children' }"
            default-expand-all
            @select="onDeptSelect"
          />
        </div>
      </a-col>

      <!-- 右侧用户列表 -->
      <a-col :span="18">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">用户管理</span>
            <a-space>
              <a-input-search
                v-model:value="searchText"
                placeholder="搜索用户名/姓名"
                style="width: 200px"
                @search="loadData"
              />
              <a-button type="primary" @click="showModal()">新建用户</a-button>
            </a-space>
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
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <span class="action-link" @click="showModal(record)">编辑</span>
                <a-divider type="vertical" />
                <span class="action-link" @click="handleResetPwd(record)">重置密码</span>
                <a-divider type="vertical" />
                <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
                  <span class="action-link danger">删除</span>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="modalVisible"
      :title="editingRecord ? '编辑用户' : '新建用户'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="用户名" required>
          <a-input v-model:value="formState.username" :disabled="!!editingRecord" />
        </a-form-item>
        <a-form-item label="姓名" required>
          <a-input v-model:value="formState.realName" />
        </a-form-item>
        <a-form-item label="密码" v-if="!editingRecord">
          <a-input-password v-model:value="formState.password" />
        </a-form-item>
        <a-form-item label="部门">
          <a-tree-select
            v-model:value="formState.deptId"
            :tree-data="deptTree"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            allow-clear
          />
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
import { getUsersPage, createUser, updateUser, deleteUser, resetPassword, getDeptTree } from '../../api/system'

const loading = ref(false)
const dataList = ref([])
const deptTree = ref([])
const modalVisible = ref(false)
const submitLoading = ref(false)
const editingRecord = ref(null)
const searchText = ref('')
const selectedDeptId = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '部门', dataIndex: 'deptName', key: 'deptName' },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 220 }
]

const formState = reactive({
  username: '',
  realName: '',
  password: '',
  deptId: null,
  status: 1
})

function showModal(record) {
  if (record) {
    editingRecord.value = record
    formState.username = record.username
    formState.realName = record.realName
    formState.deptId = record.deptId
    formState.status = record.status
  } else {
    editingRecord.value = null
    formState.username = ''
    formState.realName = ''
    formState.password = '123456'
    formState.deptId = selectedDeptId.value
    formState.status = 1
  }
  modalVisible.value = true
}

function onDeptSelect(keys) {
  selectedDeptId.value = keys[0] || null
  pagination.current = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUsersPage({
      page: pagination.current,
      size: pagination.pageSize,
      deptId: selectedDeptId.value,
      keyword: searchText.value || undefined
    })
    const data = res.data || res
    dataList.value = data.list || []
    pagination.total = data.total || 0
  } catch {
    // ignore
  }
  loading.value = false
}

async function loadDepts() {
  try {
    const res = await getDeptTree()
    const data = res.data || res
    deptTree.value = Array.isArray(data) ? data : []
  } catch {
    // ignore
  }
}

async function handleSubmit() {
  if (!formState.username || !formState.realName) {
    message.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (editingRecord.value) {
      await updateUser(editingRecord.value.id, {
        realName: formState.realName,
        deptId: formState.deptId,
        status: formState.status
      })
      message.success('更新成功')
    } else {
      await createUser(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // ignore
  }
  submitLoading.value = false
}

async function handleResetPwd(record) {
  try {
    await resetPassword(record.id, { password: '123456' })
    message.success('密码已重置为 123456')
  } catch {
    // ignore
  }
}

async function handleDelete(record) {
  try {
    await deleteUser(record.id)
    message.success('删除成功')
    loadData()
  } catch {
    // ignore
  }
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(() => {
  loadDepts()
  loadData()
})
</script>
