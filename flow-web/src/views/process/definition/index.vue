<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">流程定义</span>
        <a-space>
          <a-button type="primary" @click="showModal()">新建</a-button>
          <a-button @click="loadData">刷新</a-button>
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
          <template v-if="column.key === 'processType'">
            <a-tag :color="getTypeColor(record.processType)">
              {{ getTypeName(record.processType) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '已部署' : '草稿' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="handleConfig(record)">配置</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="showModal(record)">编辑</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="handleDeploy(record)">部署</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="handleExport(record)">导出</span>
            <a-divider type="vertical" />
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
              <span class="action-link danger">删除</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingRecord ? '编辑流程定义' : '新建流程定义'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
      width="600px"
    >
      <a-form :model="formState" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="流程标识" required>
              <a-input v-model:value="formState.processKey" :disabled="!!editingRecord" placeholder="如：leave-approval" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="流程名称" required>
              <a-input v-model:value="formState.processName" placeholder="请输入流程名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="流程类型">
              <a-select v-model:value="formState.processType" placeholder="请选择流程类型">
                <a-select-option value="approval">审批流</a-select-option>
                <a-select-option value="process">业务流程</a-select-option>
                <a-select-option value="callback">回调流程</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="分类">
              <a-input v-model:value="formState.category" placeholder="如：人事、财务" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="用途描述">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="请描述流程的用途" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getProcessDefinitions,
  createProcessDefinition,
  updateProcessDefinition,
  deleteProcessDefinition,
  deployProcessDefinition,
  exportProcessDefinition
} from '../../../api/process'

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const modalVisible = ref(false)
const editingRecord = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '流程标识', dataIndex: 'processKey', key: 'processKey' },
  { title: '流程名称', dataIndex: 'processName', key: 'processName' },
  { title: '类型', key: 'processType', width: 100 },
  { title: '分类', dataIndex: 'category', key: 'category' },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 300 }
]

const formState = reactive({
  processKey: '',
  processName: '',
  processType: 'approval',
  category: '',
  description: ''
})

const typeMap = {
  approval: { name: '审批流', color: 'blue' },
  process: { name: '业务流程', color: 'green' },
  callback: { name: '回调流程', color: 'purple' }
}

function getTypeName(type) {
  return typeMap[type]?.name || type
}

function getTypeColor(type) {
  return typeMap[type]?.color || 'default'
}

function resetForm() {
  formState.processKey = ''
  formState.processName = ''
  formState.processType = 'approval'
  formState.category = ''
  formState.description = ''
  editingRecord.value = null
}

function showModal(record) {
  if (record) {
    editingRecord.value = record
    formState.processKey = record.processKey
    formState.processName = record.processName
    formState.processType = record.processType || 'approval'
    formState.category = record.category || ''
    formState.description = record.description || ''
  } else {
    resetForm()
  }
  modalVisible.value = true
}

function handleConfig(record) {
  router.push(`/process/config?processKey=${record.processKey}`)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProcessDefinitions({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
  } catch {
    // ignore
  }
  loading.value = false
}

async function handleSubmit() {
  if (!formState.processKey || !formState.processName) {
    message.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (editingRecord.value) {
      await updateProcessDefinition(editingRecord.value.id, formState)
      message.success('更新成功')
    } else {
      await createProcessDefinition(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // ignore
  }
  submitLoading.value = false
}

async function handleDeploy(record) {
  try {
    await deployProcessDefinition(record.id)
    message.success('部署成功')
    loadData()
  } catch {
    // ignore
  }
}

async function handleExport(record) {
  try {
    await exportProcessDefinition(record.id)
    message.success('导出成功')
  } catch {
    // ignore
  }
}

async function handleDelete(record) {
  try {
    await deleteProcessDefinition(record.id)
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

onMounted(loadData)
</script>
