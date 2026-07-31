<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">{{ modelName || '业务数据' }}</span>
        <a-space>
          <a-input-search
            v-model:value="keyword"
            placeholder="关键字检索"
            style="width: 240px"
            allow-clear
            @search="handleSearch"
          />
          <a-button v-if="hasPerm(`model-data:${modelKey}:create`)" type="primary" @click="showEdit()">新建</a-button>
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
          <template v-if="column.key === 'action'">
            <span
              v-if="hasPerm(`model-data:${modelKey}:view`)"
              class="action-link"
              @click="showDetail(record)"
            >详情</span>
            <a-divider v-if="hasPerm(`model-data:${modelKey}:view`)" type="vertical" />
            <span
              v-if="hasPerm(`model-data:${modelKey}:update`)"
              class="action-link"
              @click="showEdit(record)"
            >编辑</span>
            <a-divider v-if="hasPerm(`model-data:${modelKey}:update`)" type="vertical" />
            <span
              v-if="hasPerm(`model-data:${modelKey}:delete`)"
              class="action-link danger"
              @click="handleDelete(record)"
            >删除</span>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新建/编辑抽屉 -->
    <a-drawer
      v-model:open="editVisible"
      :title="editingId ? '编辑数据' : '新建数据'"
      placement="right"
      :width="720"
      :body-style="{ paddingBottom: '64px' }"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col v-for="field in mainFields" :key="field.fieldKey" :span="12">
            <a-form-item :label="field.label || field.fieldKey" :required="field.required">
              <a-input-number
                v-if="field.type === 'number' || field.type === 'amount'"
                v-model:value="formState[field.fieldKey]"
                style="width: 100%"
                :placeholder="`请输入${field.label || field.fieldKey}`"
              />
              <a-date-picker
                v-else-if="field.type === 'date'"
                v-model:value="formState[field.fieldKey]"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <a-date-picker
                v-else-if="field.type === 'datetime'"
                v-model:value="formState[field.fieldKey]"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
              <a-input
                v-else-if="field.type === 'computed'"
                v-model:value="formState[field.fieldKey]"
                disabled
                placeholder="计算字段"
              />
              <a-input
                v-else
                v-model:value="formState[field.fieldKey]"
                :placeholder="`请输入${field.label || field.fieldKey}`"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 子表编辑 -->
        <template v-for="sub in subTables" :key="sub.tableName">
          <div class="sub-table-header">
            <span class="sub-table-title">{{ sub.label || sub.tableName }}</span>
            <a-button type="dashed" size="small" @click="addSubRow(sub)">+ 添加行</a-button>
          </div>
          <a-table
            :columns="subEditColumns(sub)"
            :data-source="formState[sub.tableName]"
            :pagination="false"
            size="small"
            :row-key="(_, idx) => idx"
            :bordered="true"
            style="margin-bottom: 16px"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === '__action'">
                <a-button type="link" danger size="small" @click="removeSubRow(sub, index)">删除</a-button>
              </template>
              <template v-else>
                <a-input-number
                  v-if="column.fieldType === 'number' || column.fieldType === 'amount' || column.fieldType === 'computed'"
                  v-model:value="record[column.key]"
                  size="small"
                  style="width: 100%"
                />
                <a-input v-else v-model:value="record[column.key]" size="small" />
              </template>
            </template>
          </a-table>
        </template>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="editVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="handleSubmit">保存</a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      title="详细信息"
      placement="right"
      :width="720"
    >
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="ID">{{ detailData.id }}</a-descriptions-item>
        <a-descriptions-item
          v-for="field in mainFields"
          :key="field.fieldKey"
          :label="field.label || field.fieldKey"
        >
          {{ detailData[field.fieldKey] }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detailData.create_time }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ detailData.update_time }}</a-descriptions-item>
      </a-descriptions>

      <template v-for="sub in subTables" :key="sub.tableName">
        <div class="sub-table-header" style="margin-top: 16px">
          <span class="sub-table-title">{{ sub.label || sub.tableName }}</span>
        </div>
        <a-table
          :columns="subDetailColumns(sub)"
          :data-source="(detailData.subTables && detailData.subTables[sub.tableName]) || []"
          :pagination="false"
          size="small"
          row-key="id"
          :bordered="true"
        />
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, createVNode } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { getDataModel } from '../../api/model'
import {
  getModelDataPage,
  getModelDataDetail,
  createModelData,
  updateModelData,
  deleteModelData
} from '../../api/modelData'
import { usePermission } from '../../composables/usePermission'

const route = useRoute()
const { hasPerm } = usePermission()

const modelKey = ref(route.params.modelKey)
const modelName = ref('')
const mainFields = ref([])
const subTables = ref([])

const loading = ref(false)
const dataList = ref([])
const keyword = ref('')
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 条`
})

// 主列表列：模型字段 + 创建时间 + 操作
const columns = computed(() => {
  const cols = mainFields.value.map(field => ({
    title: field.label || field.fieldKey,
    dataIndex: field.fieldKey,
    key: field.fieldKey,
    ellipsis: true
  }))
  cols.push({ title: '创建时间', dataIndex: 'create_time', key: 'create_time', width: 170 })
  cols.push({ title: '操作', key: 'action', width: 180, fixed: 'right' })
  return cols
})

async function loadModel() {
  try {
    const res = await getDataModel(modelKey.value)
    const model = res.data || res
    modelName.value = model.modelName
    mainFields.value = (model.mainTable && model.mainTable.fields) || []
    subTables.value = model.subTables || []
  } catch {
    modelName.value = ''
    mainFields.value = []
    subTables.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getModelDataPage(modelKey.value, {
      keyword: keyword.value || undefined,
      page: pagination.current,
      size: pagination.pageSize
    })
    const data = res.data || res
    dataList.value = data.list || []
    pagination.total = data.total || 0
  } catch {
    dataList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// ========== 新建/编辑 ==========
const editVisible = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formState = reactive({})

function resetForm() {
  Object.keys(formState).forEach(key => delete formState[key])
  mainFields.value.forEach(field => { formState[field.fieldKey] = null })
  subTables.value.forEach(sub => { formState[sub.tableName] = [] })
}

async function showEdit(record) {
  resetForm()
  editingId.value = record ? record.id : null
  if (record) {
    try {
      const res = await getModelDataDetail(modelKey.value, record.id)
      const detail = res.data || res
      mainFields.value.forEach(field => { formState[field.fieldKey] = detail[field.fieldKey] })
      subTables.value.forEach(sub => {
        const rows = (detail.subTables && detail.subTables[sub.tableName]) || []
        formState[sub.tableName] = rows.map(row => ({ ...row }))
      })
    } catch {
      return
    }
  }
  editVisible.value = true
}

function addSubRow(sub) {
  const row = {}
  ;(sub.fields || []).forEach(field => { row[field.fieldKey] = null })
  formState[sub.tableName].push(row)
}

function removeSubRow(sub, index) {
  formState[sub.tableName].splice(index, 1)
}

function subEditColumns(sub) {
  const cols = (sub.fields || []).map(field => ({
    title: field.label || field.fieldKey,
    dataIndex: field.fieldKey,
    key: field.fieldKey,
    fieldType: field.type
  }))
  cols.push({ title: '操作', key: '__action', width: 70 })
  return cols
}

function subDetailColumns(sub) {
  return (sub.fields || []).map(field => ({
    title: field.label || field.fieldKey,
    dataIndex: field.fieldKey,
    key: field.fieldKey,
    ellipsis: true
  }))
}

async function handleSubmit() {
  // 必填校验
  for (const field of mainFields.value) {
    const value = formState[field.fieldKey]
    if (field.required && (value === null || value === undefined || value === '')) {
      message.warning(`请填写：${field.label || field.fieldKey}`)
      return
    }
  }

  const data = {}
  mainFields.value.forEach(field => { data[field.fieldKey] = formState[field.fieldKey] })
  subTables.value.forEach(sub => { data[sub.tableName] = formState[sub.tableName] })

  submitting.value = true
  try {
    if (editingId.value) {
      await updateModelData(modelKey.value, editingId.value, data)
      message.success('修改成功')
    } else {
      await createModelData(modelKey.value, data)
      message.success('新增成功')
    }
    editVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ========== 删除（本项目 popconfirm 失效，统一用 Modal.confirm） ==========
function handleDelete(record) {
  Modal.confirm({
    title: '删除数据',
    icon: createVNode(ExclamationCircleOutlined),
    content: `确定删除 ID 为 ${record.id} 的数据吗？关联的子表数据将一并删除。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteModelData(modelKey.value, record.id)
      message.success('删除成功')
      loadData()
    }
  })
}

// ========== 详情 ==========
const detailVisible = ref(false)
const detailData = ref({})

async function showDetail(record) {
  try {
    const res = await getModelDataDetail(modelKey.value, record.id)
    detailData.value = res.data || res
    detailVisible.value = true
  } catch { /* 请求层已提示 */ }
}

// 路由切换到其他模型时重新初始化
watch(() => route.params.modelKey, async (val) => {
  if (!val || route.name !== 'ModelDataManage') return
  modelKey.value = val
  keyword.value = ''
  pagination.current = 1
  await loadModel()
  await loadData()
}, { immediate: true })
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.sub-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.sub-table-title {
  font-weight: 600;
  font-size: 14px;
}

.drawer-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 100%;
  padding: 10px 16px;
  text-align: right;
  background: #fff;
  border-top: 1px solid var(--border-color, #f0f0f0);
}
</style>
