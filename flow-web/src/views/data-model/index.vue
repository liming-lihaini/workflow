<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">数据模型</span>
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
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '已发布' : '草稿' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="handleEdit(record)">编辑</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="handlePublish(record)" v-if="record.status !== 1">发布</span>
            <a-divider type="vertical" v-if="record.status !== 1" />
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
      :title="editingRecord ? '编辑数据模型' : '新建数据模型'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
      width="720px"
    >
      <a-form :model="formState" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型Key" required>
              <a-input v-model:value="formState.modelKey" :disabled="!!editingRecord" placeholder="如：leave-data" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模型名称" required>
              <a-input v-model:value="formState.modelName" placeholder="请输入模型名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 主表定义 -->
        <div style="margin-bottom: 8px; font-weight: 600; font-size: 14px;">主表定义</div>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="表名">
              <a-input v-model:value="formState.mainTable.tableName" placeholder="如：main" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="表标签">
              <a-input v-model:value="formState.mainTable.label" placeholder="如：主表" />
            </a-form-item>
          </a-col>
        </a-row>

        <div style="margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 600; font-size: 14px;">主表字段</span>
          <a-button type="dashed" size="small" @click="addField">+ 添加字段</a-button>
        </div>
        <a-table
          :columns="fieldColumns"
          :data-source="formState.mainTable.fields"
          :pagination="false"
          size="small"
          row-key="fieldKey"
          :bordered="true"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'fieldKey'">
              <a-input v-model:value="record.fieldKey" size="small" placeholder="字段标识" />
            </template>
            <template v-if="column.key === 'label'">
              <a-input v-model:value="record.label" size="small" placeholder="字段名称" />
            </template>
            <template v-if="column.key === 'type'">
              <a-select v-model:value="record.type" size="small" style="width: 100%" placeholder="类型">
                <a-select-option value="text">文本</a-select-option>
                <a-select-option value="number">数字</a-select-option>
                <a-select-option value="amount">金额</a-select-option>
                <a-select-option value="date">日期</a-select-option>
                <a-select-option value="datetime">日期时间</a-select-option>
                <a-select-option value="file">文件</a-select-option>
                <a-select-option value="person">人员</a-select-option>
                <a-select-option value="department">部门</a-select-option>
                <a-select-option value="computed">计算字段</a-select-option>
              </a-select>
            </template>
            <template v-if="column.key === 'required'">
              <a-checkbox v-model:checked="record.required" />
            </template>
            <template v-if="column.key === 'action'">
              <a-popconfirm title="确定删除该字段？" @confirm="removeField(index)">
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>

        <!-- 子表定义 -->
        <div style="margin-top: 16px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 600; font-size: 14px;">子表定义</span>
          <a-button type="dashed" size="small" @click="addSubTable">+ 添加子表</a-button>
        </div>
        <a-collapse v-model:activeKey="activeSubTables" v-if="formState.subTables.length > 0">
          <a-collapse-panel v-for="(subTable, stIdx) in formState.subTables" :key="String(stIdx)" :header="subTable.label || subTable.tableName || ('子表 ' + (stIdx + 1))">
            <template #extra>
              <a-popconfirm title="确定删除该子表？" @confirm.stop="removeSubTable(stIdx)">
                <a-button type="link" danger size="small" @click.stop>删除子表</a-button>
              </a-popconfirm>
            </template>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="表名">
                  <a-input v-model:value="subTable.tableName" placeholder="如：items" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="表标签">
                  <a-input v-model:value="subTable.label" placeholder="如：明细" />
                </a-form-item>
              </a-col>
            </a-row>
            <div style="margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
              <span style="font-size: 13px;">字段列表</span>
              <a-button type="dashed" size="small" @click="addSubField(stIdx)">+ 添加字段</a-button>
            </div>
            <a-table
              :columns="fieldColumns"
              :data-source="subTable.fields"
              :pagination="false"
              size="small"
              row-key="fieldKey"
              :bordered="true"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'fieldKey'">
                  <a-input v-model:value="record.fieldKey" size="small" placeholder="字段标识" />
                </template>
                <template v-if="column.key === 'label'">
                  <a-input v-model:value="record.label" size="small" placeholder="字段名称" />
                </template>
                <template v-if="column.key === 'type'">
                  <a-select v-model:value="record.type" size="small" style="width: 100%" placeholder="类型">
                    <a-select-option value="text">文本</a-select-option>
                    <a-select-option value="number">数字</a-select-option>
                    <a-select-option value="amount">金额</a-select-option>
                    <a-select-option value="date">日期</a-select-option>
                    <a-select-option value="datetime">日期时间</a-select-option>
                    <a-select-option value="file">文件</a-select-option>
                    <a-select-option value="person">人员</a-select-option>
                    <a-select-option value="department">部门</a-select-option>
                    <a-select-option value="computed">计算字段</a-select-option>
                  </a-select>
                </template>
                <template v-if="column.key === 'required'">
                  <a-checkbox v-model:checked="record.required" />
                </template>
                <template v-if="column.key === 'action'">
                  <a-popconfirm title="确定删除该字段？" @confirm="removeSubField(stIdx, index)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </template>
              </template>
            </a-table>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getDataModelList,
  createDataModel,
  updateDataModel,
  deleteDataModel,
  publishDataModel
} from '../../api/model'

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const modalVisible = ref(false)
const editingRecord = ref(null)
const activeSubTables = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '模型Key', dataIndex: 'modelKey', key: 'modelKey' },
  { title: '模型名称', dataIndex: 'modelName', key: 'modelName' },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200 }
]

const fieldColumns = [
  { title: '字段标识', key: 'fieldKey', width: 140 },
  { title: '字段名称', key: 'label', width: 140 },
  { title: '类型', key: 'type', width: 120 },
  { title: '必填', key: 'required', width: 60, align: 'center' },
  { title: '操作', key: 'action', width: 70, align: 'center' }
]

function createEmptyField() {
  return { fieldKey: '', label: '', type: 'text', required: false }
}

const formState = reactive({
  modelKey: '',
  modelName: '',
  mainTable: {
    tableName: 'main',
    label: '主表',
    fields: []
  },
  subTables: []
})

function resetForm() {
  formState.modelKey = ''
  formState.modelName = ''
  formState.mainTable = { tableName: 'main', label: '主表', fields: [createEmptyField()] }
  formState.subTables = []
  editingRecord.value = null
  activeSubTables.value = []
}

function showModal(record) {
  if (record) {
    editingRecord.value = record
    formState.modelKey = record.modelKey
    formState.modelName = record.modelName
    // 从 record 中恢复主表和子表
    if (record.mainTable) {
      formState.mainTable = {
        tableName: record.mainTable.tableName || 'main',
        label: record.mainTable.label || '主表',
        fields: (record.mainTable.fields || []).map(f => ({ ...f }))
      }
    } else {
      formState.mainTable = { tableName: 'main', label: '主表', fields: [createEmptyField()] }
    }
    if (record.subTables && record.subTables.length > 0) {
      formState.subTables = record.subTables.map(st => ({
        tableName: st.tableName || '',
        label: st.label || '',
        fields: (st.fields || []).map(f => ({ ...f }))
      }))
      activeSubTables.value = record.subTables.map((_, i) => String(i))
    } else {
      formState.subTables = []
      activeSubTables.value = []
    }
  } else {
    resetForm()
  }
  modalVisible.value = true
}

function handleEdit(record) {
  showModal(record)
}

function addField() {
  formState.mainTable.fields.push(createEmptyField())
}

function removeField(index) {
  formState.mainTable.fields.splice(index, 1)
}

function addSubTable() {
  const idx = formState.subTables.length
  formState.subTables.push({
    tableName: `sub_table_${idx + 1}`,
    label: `子表 ${idx + 1}`,
    fields: [createEmptyField()]
  })
  activeSubTables.value.push(String(idx))
}

function removeSubTable(index) {
  formState.subTables.splice(index, 1)
  activeSubTables.value = formState.subTables.map((_, i) => String(i))
}

function addSubField(stIdx) {
  formState.subTables[stIdx].fields.push(createEmptyField())
}

function removeSubField(stIdx, fIdx) {
  formState.subTables[stIdx].fields.splice(fIdx, 1)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDataModelList({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = data.records || data.list || (Array.isArray(data) ? data : [])
    pagination.total = data.total || dataList.value.length
  } catch {
    // ignore
  }
  loading.value = false
}

async function handleSubmit() {
  if (!formState.modelKey || !formState.modelName) {
    message.warning('请填写模型Key和模型名称')
    return
  }
  // 过滤掉空字段
  const mainFields = formState.mainTable.fields.filter(f => f.fieldKey && f.label && f.type)
  if (mainFields.length === 0) {
    message.warning('主表至少需要一个有效字段')
    return
  }

  const payload = {
    modelKey: formState.modelKey,
    modelName: formState.modelName,
    mainTable: {
      tableName: formState.mainTable.tableName || 'main',
      label: formState.mainTable.label || '主表',
      fields: mainFields
    },
    subTables: formState.subTables
      .filter(st => st.tableName && st.fields.some(f => f.fieldKey))
      .map(st => ({
        tableName: st.tableName,
        label: st.label || st.tableName,
        fields: st.fields.filter(f => f.fieldKey && f.label && f.type)
      }))
  }

  submitLoading.value = true
  try {
    if (editingRecord.value) {
      await updateDataModel(editingRecord.value.modelKey, payload)
      message.success('更新成功')
    } else {
      await createDataModel(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // ignore
  }
  submitLoading.value = false
}

async function handlePublish(record) {
  try {
    await publishDataModel(record.modelKey)
    message.success('发布成功')
    loadData()
  } catch {
    // ignore
  }
}

async function handleDelete(record) {
  try {
    await deleteDataModel(record.modelKey)
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
