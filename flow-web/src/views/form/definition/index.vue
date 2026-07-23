<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">表单定义</span>
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
          <template v-if="column.key === 'category'">
            <a-tag color="blue">{{ categoryLabelMap[record.category] || record.category || '-' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="action-link" @click="handleEdit(record)">编辑</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="handleDesign(record)">设计</span>
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
      :title="editingRecord ? '编辑表单' : '新建表单'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
      width="500px"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="表单Key" required>
          <a-input v-model:value="formState.formKey" :disabled="!!editingRecord" placeholder="如：leave-form" />
        </a-form-item>
        <a-form-item label="表单名称" required>
          <a-input v-model:value="formState.formName" placeholder="请输入表单名称" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="formState.category" placeholder="请选择分类" allow-clear>
            <a-select-option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
          </a-select>
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
  getFormList,
  createForm,
  updateForm,
  deleteForm
} from '../../../api/form'
import { getDictItemsByCode } from '../../../api/dict'
import { renderDate } from '../../../utils/date'

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const modalVisible = ref(false)
const editingRecord = ref(null)
const categoryOptions = ref([])
const categoryLabelMap = ref({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '表单Key', dataIndex: 'formKey', key: 'formKey' },
  { title: '表单名称', dataIndex: 'formName', key: 'formName' },
  { title: '分类', dataIndex: 'category', key: 'category' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate },
  { title: '操作', key: 'action', width: 200 }
]

const formState = reactive({
  formKey: '',
  formName: '',
  category: ''
})

function resetForm() {
  formState.formKey = ''
  formState.formName = ''
  formState.category = ''
  editingRecord.value = null
}

function showModal(record) {
  if (record) {
    editingRecord.value = record
    formState.formKey = record.formKey
    formState.formName = record.formName
    formState.category = record.category
  } else {
    resetForm()
  }
  modalVisible.value = true
}

function handleEdit(record) {
  showModal(record)
}

function handleDesign(record) {
  router.push(`/form/design?formKey=${record.formKey}`)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getFormList({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = data.records || data.list || []
    pagination.total = data.total || dataList.value.length
  } catch {
    // ignore
  }
  loading.value = false
}

async function handleSubmit() {
  if (!formState.formKey || !formState.formName) {
    message.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (editingRecord.value) {
      await updateForm(editingRecord.value.formKey, formState)
      message.success('更新成功')
    } else {
      await createForm(formState)
      message.success('创建成功')
    }
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
    modalVisible.value = false
    loadData()
  }
}

async function handleDelete(record) {
  try {
    await deleteForm(record.formKey)
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
  loadData()
  loadCategoryOptions()
})

async function loadCategoryOptions() {
  try {
    const res = await getDictItemsByCode('form_category')
    const items = res.data || res || []
    const arr = Array.isArray(items) ? items : (items.list || [])
    categoryOptions.value = arr.map(item => ({
      value: item.itemValue || item.value,
      label: item.itemText || item.label || item.itemValue
    }))
    const map = {}
    categoryOptions.value.forEach(item => { map[item.value] = item.label })
    categoryLabelMap.value = map
  } catch {
    categoryOptions.value = [
      { value: 'approval', label: '审批表单' },
      { value: 'apply', label: '申请表单' },
      { value: 'reimbursement', label: '报销表单' },
      { value: 'attendance', label: '考勤表单' },
      { value: 'other', label: '其他' }
    ]
    const map = {}
    categoryOptions.value.forEach(item => { map[item.value] = item.label })
    categoryLabelMap.value = map
  }
}
</script>
