<template>
  <div class="page-wrap">
    <div class="card-wrap designer-wrap">
      <!-- 左侧组件面板 -->
      <div class="designer-sidebar">
        <div class="sidebar-title">组件</div>
        <div class="component-list">
          <div
            v-for="comp in componentTypes"
            :key="comp.type"
            class="component-item"
            draggable="true"
            @dragstart="handleDragStart($event, comp)"
          >
            <component :is="comp.icon" />
            <span>{{ comp.label }}</span>
          </div>
        </div>
      </div>

      <!-- 中间画布 -->
      <div
        class="designer-canvas"
        @drop="handleDrop"
        @dragover.prevent
      >
        <div class="canvas-header">
          <div class="canvas-header-left">
            <span class="page-title">表单画布</span>
            <!-- 绑定模型下拉框 -->
            <a-select
              v-model:value="bindModelKey"
              placeholder="绑定数据模型"
              style="width: 180px; margin-left: 16px"
              allow-clear
              @change="handleModelChange"
            >
              <a-select-option v-for="model in modelList" :key="model.modelKey" :value="model.modelKey">
                {{ model.modelName }}
              </a-select-option>
            </a-select>
          </div>
          <a-button type="primary" size="small" @click="handleSave">保存</a-button>
        </div>
        <div class="canvas-content">
          <div v-if="formFields.length === 0" class="canvas-empty">
            从左侧拖拽组件到此处，或选择绑定数据模型
          </div>
          <div
            v-for="(field, index) in formFields"
            :key="field.id"
            class="canvas-field"
            :class="{ active: selectedField?.id === field.id }"
            @click="selectField(field)"
          >
            <div class="field-label">
              {{ field.label }}
              <a-tag v-if="field.fromModel" size="small" color="blue">模型</a-tag>
              <span v-if="field.required" class="required-tag">必填</span>
            </div>
            <div class="field-preview">
              <a-input v-if="field.type === 'text'" placeholder="请输入" disabled />
              <a-textarea v-if="field.type === 'textarea'" placeholder="请输入" :rows="2" disabled />
              <a-input-number v-if="field.type === 'number'" placeholder="请输入" disabled style="width: 100%" />
              <a-input-number v-if="field.type === 'amount'" placeholder="请输入金额" disabled style="width: 100%" />
              <a-date-picker v-if="field.type === 'date'" style="width: 100%" disabled />
              <a-time-picker v-if="field.type === 'time'" style="width: 100%" disabled />
              <a-date-picker v-if="field.type === 'datetime'" show-time style="width: 100%" disabled />
              <a-select v-if="field.type === 'select'" placeholder="请选择" style="width: 100%" disabled />
              <a-radio-group v-if="field.type === 'radio'">
                <a-radio value="1">选项1</a-radio>
                <a-radio value="2">选项2</a-radio>
              </a-radio-group>
              <a-checkbox-group v-if="field.type === 'checkbox'">
                <a-checkbox value="1">选项1</a-checkbox>
                <a-checkbox value="2">选项2</a-checkbox>
              </a-checkbox-group>
              <a-upload v-if="field.type === 'file'" disabled>
                <a-button>上传文件</a-button>
              </a-upload>
              <a-input v-if="field.type === 'user'" placeholder="选择人员" disabled>
                <template #prefix><UserOutlined /></template>
              </a-input>
              <a-input v-if="field.type === 'dept'" placeholder="选择部门" disabled>
                <template #prefix><TeamOutlined /></template>
              </a-input>
            </div>
            <div class="field-actions">
              <UpOutlined @click.stop="moveField(index, -1)" :disabled="index === 0" />
              <DownOutlined @click.stop="moveField(index, 1)" :disabled="index === formFields.length - 1" />
              <DeleteOutlined @click.stop="removeField(index)" />
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧属性配置 -->
      <div class="designer-properties">
        <div class="sidebar-title">属性配置</div>
        <div v-if="selectedField" class="properties-form">
          <a-form layout="vertical">
            <a-form-item label="字段标识">
              <a-input v-model:value="selectedField.field" :disabled="selectedField.fromModel" />
            </a-form-item>
            <a-form-item label="字段标签">
              <a-input v-model:value="selectedField.label" />
            </a-form-item>
            <a-form-item label="占位符">
              <a-input v-model:value="selectedField.placeholder" />
            </a-form-item>
            <a-form-item label="必填">
              <a-switch v-model:checked="selectedField.required" />
            </a-form-item>
            <a-form-item label="默认值">
              <a-input v-model:value="selectedField.defaultValue" />
            </a-form-item>
            <!-- 特殊属性 -->
            <template v-if="selectedField.type === 'select' || selectedField.type === 'radio' || selectedField.type === 'checkbox'">
              <a-form-item label="选项配置">
                <a-textarea
                  v-model:value="selectedField.optionsText"
                  :rows="3"
                  placeholder="每行一个选项，格式：value:text"
                />
              </a-form-item>
            </template>
          </a-form>
        </div>
        <div v-else class="properties-empty">
          请选择字段进行配置
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  TeamOutlined,
  UpOutlined,
  DownOutlined,
  DeleteOutlined,
  FontSizeOutlined,
  AlignLeftOutlined,
  NumberOutlined,
  DollarOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  MinusCircleOutlined,
  CheckSquareOutlined,
  UploadOutlined,
  SelectOutlined
} from '@ant-design/icons-vue'
import { getForm, updateForm } from '../../../api/form'
import { getDataModelList } from '../../../api/model'

const route = useRoute()
const formKey = ref(route.query.formKey || '')

// 模型字段类型到表单组件类型的映射
const fieldTypeMap = {
  text: 'text',
  number: 'number',
  amount: 'amount',
  date: 'date',
  datetime: 'datetime',
  file: 'file',
  person: 'user',
  department: 'dept'
}

// 组件类型定义
const componentTypes = [
  { type: 'text', label: '单行文本', icon: FontSizeOutlined },
  { type: 'textarea', label: '多行文本', icon: AlignLeftOutlined },
  { type: 'number', label: '数字', icon: NumberOutlined },
  { type: 'amount', label: '金额', icon: DollarOutlined },
  { type: 'date', label: '日期', icon: CalendarOutlined },
  { type: 'time', label: '时间', icon: ClockCircleOutlined },
  { type: 'datetime', label: '日期时间', icon: CalendarOutlined },
  { type: 'select', label: '下拉选择', icon: SelectOutlined },
  { type: 'radio', label: '单选框', icon: CheckCircleOutlined },
  { type: 'checkbox', label: '复选框', icon: CheckSquareOutlined },
  { type: 'file', label: '文件上传', icon: UploadOutlined },
  { type: 'user', label: '人员选择', icon: UserOutlined },
  { type: 'dept', label: '部门选择', icon: TeamOutlined }
]

const formFields = ref([])
const selectedField = ref(null)
const modelList = ref([])
const bindModelKey = ref(null)

let fieldIdCounter = 0

function generateFieldId() {
  return `field_${++fieldIdCounter}`
}

// 加载数据模型列表
async function loadModelList() {
  try {
    const res = await getDataModelList({ page: 1, size: 100 })
    const data = res.data || res
    // 只显示已发布的数据模型
    modelList.value = (data.records || data.list || []).filter(m => m.status === 1)
  } catch {
    // ignore
  }
}

// 选择模型后自动生成字段
async function handleModelChange(modelKey) {
  if (!modelKey) {
    // 清除模型绑定，但保留字段
    formFields.value.forEach(f => {
      f.fromModel = false
    })
    return
  }

  try {
    const res = await getDataModelList({})
    const data = res.data || res
    const allList = data.records || data.list || []
    const model = allList.find(m => m.modelKey === modelKey)

    if (!model) {
      message.warning('未找到对应的数据模型')
      return
    }

    // 解析模型定义，生成表单字段
    let modelJson = null
    try {
      modelJson = JSON.parse(model.modelJson || '{}')
    } catch {
      message.warning('数据模型JSON格式错误')
      return
    }

    const newFields = []

    // 处理主表字段
    if (modelJson.mainTable && modelJson.mainTable.fields) {
      modelJson.mainTable.fields.forEach(f => {
        const fieldType = fieldTypeMap[f.type] || 'text'
        newFields.push({
          id: generateFieldId(),
          type: fieldType,
          field: f.fieldKey,
          label: f.label || f.fieldKey,
          placeholder: `请输入${f.label || f.fieldKey}`,
          required: f.required || false,
          defaultValue: f.defaultValue || '',
          fromModel: true,
          modelKey: modelKey,
          tableName: modelJson.mainTable.tableName
        })
      })
    }

    // 处理子表字段（只取第一个子表作为示例）
    if (modelJson.subTables && modelJson.subTables.length > 0) {
      const firstSubTable = modelJson.subTables[0]
      if (firstSubTable.fields) {
        firstSubTable.fields.forEach(f => {
          const fieldType = fieldTypeMap[f.type] || 'text'
          newFields.push({
            id: generateFieldId(),
            type: fieldType,
            field: `${firstSubTable.tableName}_${f.fieldKey}`,
            label: `[子表] ${f.label || f.fieldKey}`,
            placeholder: `请输入${f.label || f.fieldKey}`,
            required: f.required || false,
            defaultValue: f.defaultValue || '',
            fromModel: true,
            modelKey: modelKey,
            tableName: firstSubTable.tableName
          })
        })
      }
    }

    if (newFields.length > 0) {
      formFields.value = newFields
      message.success(`已从数据模型生成 ${newFields.length} 个字段`)
    } else {
      message.warning('数据模型中没有找到字段定义')
    }
  } catch (e) {
    message.error('加载数据模型失败')
  }
}

// 拖拽开始
function handleDragStart(event, comp) {
  event.dataTransfer.setData('componentType', JSON.stringify(comp))
}

// 放置组件
function handleDrop(event) {
  const compData = event.dataTransfer.getData('componentType')
  if (!compData) return

  const comp = JSON.parse(compData)
  const field = {
    id: generateFieldId(),
    type: comp.type,
    field: `${comp.type}_${fieldIdCounter}`,
    label: comp.label,
    placeholder: `请输入${comp.label}`,
    required: false,
    defaultValue: '',
    optionsText: '1:选项1\n2:选项2\n3:选项3',
    fromModel: false
  }
  formFields.value.push(field)
  selectField(field)
}

// 选择字段
function selectField(field) {
  selectedField.value = field
}

// 移动字段
function moveField(index, direction) {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= formFields.value.length) return
  const temp = formFields.value[index]
  formFields.value[index] = formFields.value[newIndex]
  formFields.value[newIndex] = temp
}

// 删除字段
function removeField(index) {
  formFields.value.splice(index, 1)
  if (selectedField.value && selectedField.value.id === formFields.value[index]?.id) {
    selectedField.value = null
  }
}

// 保存表单
async function handleSave() {
  if (!formKey.value) {
    message.warning('请先在表单定义中创建表单')
    return
  }

  const formJson = JSON.stringify(formFields.value, null, 2)
  try {
    await updateForm(formKey.value, {
      formKey: formKey.value,
      formJson,
      modelKey: bindModelKey.value || null
    })
    message.success('保存成功')
  } catch {
    // ignore
  }
}

// 加载表单
async function loadForm() {
  if (!formKey.value) return

  try {
    const res = await getForm(formKey.value)
    const form = res.data || res

    // 设置绑定模型
    bindModelKey.value = form.modelKey || null

    if (form.formJson) {
      const fields = JSON.parse(form.formJson)
      formFields.value = fields || []
      // 重新生成ID以避免冲突
      formFields.value.forEach((f, i) => {
        f.id = `field_${i + 1}`
      })
      fieldIdCounter = formFields.value.length
    }
  } catch {
    // ignore
  }
}

onMounted(() => {
  loadModelList()
  loadForm()
})
</script>

<style scoped>
.designer-wrap {
  display: flex;
  height: calc(100vh - 180px);
  gap: 0;
}

.designer-sidebar,
.designer-properties {
  width: 240px;
  background: #fff;
  border-right: 1px solid var(--border-light, #e8e8e8);
  display: flex;
  flex-direction: column;
}

.designer-properties {
  border-right: none;
  border-left: 1px solid var(--border-light, #e8e8e8);
}

.sidebar-title {
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--border-light, #e8e8e8);
}

.component-list {
  padding: 12px;
  overflow-y: auto;
  flex: 1;
}

.component-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.2s;
}

.component-item:hover {
  background: #e6f0ff;
  border-color: #1677ff;
}

.designer-canvas {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.canvas-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid var(--border-light, #e8e8e8);
}

.canvas-header-left {
  display: flex;
  align-items: center;
}

.canvas-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.canvas-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  color: #999;
}

.canvas-field {
  position: relative;
  padding: 12px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.canvas-field:hover {
  border-color: #1677ff;
}

.canvas-field.active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.2);
}

.field-label {
  margin-bottom: 8px;
  font-weight: 500;
}

.required-tag {
  margin-left: 8px;
  padding: 0 6px;
  font-size: 12px;
  color: #ff4d4f;
  border: 1px solid #ff4d4f;
  border-radius: 2px;
}

.field-preview {
  pointer-events: none;
}

.field-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.canvas-field:hover .field-actions {
  opacity: 1;
}

.field-actions .anticon {
  padding: 4px;
  cursor: pointer;
  color: #999;
}

.field-actions .anticon:hover {
  color: #1677ff;
}

.field-actions .anticon[disabled] {
  color: #d9d9d9;
  cursor: not-allowed;
}

.properties-form {
  padding: 12px;
  overflow-y: auto;
  flex: 1;
}

.properties-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}
</style>
