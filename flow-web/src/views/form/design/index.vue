<template>
  <div class="page-wrap">
    <div class="card-wrap designer-wrap">
      <!-- 左侧面板 -->
      <div class="designer-sidebar">
        <div class="sidebar-title">布局容器</div>
        <div class="component-list layout-list">
          <div class="component-item layout-item" draggable="true" @dragstart="handleDragLayout($event, 'section')"><BorderOutlined /><span>分栏</span></div>
          <div class="component-item layout-item" draggable="true" @dragstart="handleDragLayout($event, 'row-1col')"><AppstoreOutlined /><span>一行一列</span></div>
          <div class="component-item layout-item" draggable="true" @dragstart="handleDragLayout($event, 'row-2col')"><AppstoreOutlined /><span>一行两列</span></div>
        </div>
        <div class="sidebar-title">表单组件</div>
        <div class="component-list">
          <div v-for="comp in componentTypes" :key="comp.type" class="component-item" draggable="true" @dragstart="handleDragField($event, comp)">
            <component :is="comp.icon" /><span>{{ comp.label }}</span>
          </div>
        </div>
      </div>

      <!-- 中间画布 -->
      <div class="designer-canvas" @dragover.prevent>
        <div class="canvas-header">
          <div class="canvas-header-left">
            <span class="page-title">表单设计</span>
            <a-select v-model:value="bindModelKey" placeholder="选择已发布模型" style="width: 200px; margin-left: 16px" allow-clear @change="handleModelChange">
              <a-select-option v-for="model in modelList" :key="model.modelKey" :value="model.modelKey">{{ model.modelName }}</a-select-option>
            </a-select>
          </div>
          <div class="canvas-header-right">
            <a-button @click="showPreview = true" style="margin-right: 8px"><EyeOutlined /> 预览</a-button>
            <a-button type="primary" @click="handleSave">保存</a-button>
          </div>
        </div>
        <div class="canvas-content">
          <!-- Top-level drop zone for sections -->
          <div class="top-drop-zone" @drop="handleDropOnCanvas" @dragover.prevent>
            <div v-if="sections.length === 0" class="canvas-empty">拖入「分栏」开始设计</div>
            <div v-for="(section, sIdx) in sections" :key="section.id" class="section-container"
              :class="{ active: selectedSection?.id === section.id }" @click.stop="selectSection(section)">
              <!-- Section header -->
              <div class="section-header">
                <input class="section-title-input" v-model="section.title" placeholder="分栏名称" @click.stop />
                <div class="section-actions">
                  <UpOutlined @click.stop="moveSection(sIdx, -1)" :disabled="sIdx === 0" />
                  <DownOutlined @click.stop="moveSection(sIdx, 1)" :disabled="sIdx === sections.length - 1" />
                  <DeleteOutlined @click.stop="removeSection(sIdx)" />
                </div>
              </div>
              <!-- Section body: drop zone for rows -->
              <div class="section-body" @drop.stop="handleDropOnSection($event, section)" @dragover.prevent.stop>
                <div v-if="section.children.length === 0" class="section-empty">拖入「一行一列」或「一行两列」布局</div>
                <div v-for="(row, rIdx) in section.children" :key="row.id" class="row-container"
                  :class="{ active: selectedRow?.id === row.id }" @click.stop="selectRow(row)">
                  <div class="row-header">
                    <span class="row-label">{{ row.columns === 1 ? '一行一列' : '一行两列' }}</span>
                    <div class="row-actions">
                      <UpOutlined @click.stop="moveRow(section, rIdx, -1)" :disabled="rIdx === 0" />
                      <DownOutlined @click.stop="moveRow(section, rIdx, 1)" :disabled="rIdx === section.children.length - 1" />
                      <DeleteOutlined @click.stop="removeRow(section, rIdx)" />
                    </div>
                  </div>
                  <div class="row-cells" :class="{ 'cells-2': row.columns === 2 }">
                    <div v-for="cell in row.cells" :key="cell.id" class="layout-cell"
                      @drop.stop="handleDropOnCell($event, section, row, cell)" @dragover.prevent.stop @click.stop="selectCell(cell)">
                      <div v-if="cell.fields.length === 0" class="cell-empty">拖入组件</div>
                      <div v-for="(field, fIdx) in cell.fields" :key="field.id" class="cell-field"
                        :class="{ active: selectedField?.id === field.id }" @click.stop="selectField(field)"
                        draggable="true" @dragstart="handleDragExistingField($event, section, row, cell, field)">
                        <div class="field-label">
                          {{ field.label }}
                          <a-tag v-if="field.modelField" size="small" color="blue">绑定</a-tag>
                          <a-tag v-if="field.dictCode" size="small" color="green">字典</a-tag>
                          <span v-if="field.required" class="required-tag">必填</span>
                        </div>
                        <div class="field-preview">
                          <a-input v-if="['text'].includes(field.type)" placeholder="请输入" disabled />
                          <a-textarea v-if="field.type === 'textarea'" placeholder="请输入" :rows="2" disabled />
                          <a-input-number v-if="['number','amount'].includes(field.type)" placeholder="请输入" disabled style="width:100%" />
                          <a-date-picker v-if="field.type === 'date'" style="width:100%" disabled />
                          <a-time-picker v-if="field.type === 'time'" style="width:100%" disabled />
                          <a-date-picker v-if="field.type === 'datetime'" show-time style="width:100%" disabled />
                          <a-select v-if="['select','user'].includes(field.type)" placeholder="请选择" style="width:100%" disabled show-search />
                          <a-tree-select v-if="field.type === 'dept'" placeholder="选择部门" style="width:100%" disabled />
                          <a-cascader v-if="field.type === 'cascader'" placeholder="请选择" style="width:100%" disabled />
                          <a-radio-group v-if="field.type === 'radio'"><a-radio value="1">选项</a-radio></a-radio-group>
                          <a-checkbox-group v-if="field.type === 'checkbox'"><a-checkbox value="1">选项</a-checkbox></a-checkbox-group>
                          <a-upload v-if="field.type === 'file'" disabled><a-button>上传</a-button></a-upload>
                          <div v-if="field.type === 'calculation'" class="calc-preview">
                            <FunctionOutlined style="margin-right: 4px; color: #722ed1" />
                            <span class="calc-formula">{{ field.formula || '未配置公式' }}</span>
                          </div>
                        </div>
                        <DeleteOutlined class="field-delete-btn" @click.stop="removeField(cell, fIdx)" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧属性 -->
      <div class="designer-properties">
        <div class="sidebar-title">属性配置</div>
        <div v-if="selectedField" class="properties-form">
          <a-form layout="vertical">
            <a-form-item label="字段标识"><a-input v-model:value="selectedField.field" /></a-form-item>
            <a-form-item label="字段标签"><a-input v-model:value="selectedField.label" /></a-form-item>
            <a-form-item label="占位符"><a-input v-model:value="selectedField.placeholder" /></a-form-item>
            <a-form-item label="必填"><a-switch v-model:checked="selectedField.required" /></a-form-item>
            <a-form-item label="默认值"><a-input v-model:value="selectedField.defaultValue" /></a-form-item>
            <!-- 模型属性绑定 -->
            <a-form-item v-if="bindModelKey" label="绑定模型属性">
              <a-select v-model:value="selectedField.modelField" placeholder="选择模型字段(可选)" allow-clear>
                <a-select-option v-for="mf in modelFields" :key="mf.fieldKey" :value="mf.fieldKey">{{ mf.label }} ({{ mf.fieldKey }})</a-select-option>
              </a-select>
            </a-form-item>
            <!-- 选项配置 -->
            <template v-if="['select','radio','checkbox','cascader'].includes(selectedField.type)">
              <a-form-item label="选项来源">
                <a-radio-group v-model:value="selectedField.optionsSource" @change="handleOptionsSourceChange">
                  <a-radio value="manual">手动</a-radio><a-radio value="dict">字典</a-radio>
                </a-radio-group>
              </a-form-item>
              <a-form-item v-if="selectedField.optionsSource === 'dict'" label="数据字典">
                <a-select v-model:value="selectedField.dictCode" placeholder="选择字典" allow-clear @change="handleDictChange">
                  <a-select-option v-for="dt in dictTypeList" :key="dt.dictCode" :value="dt.dictCode">{{ dt.dictName }}</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item v-if="selectedField.optionsSource === 'manual' && selectedField.type !== 'cascader'" label="选项(每行value:text)">
                <a-textarea v-model:value="selectedField.optionsText" :rows="3" placeholder="1:选项1&#10;2:选项2" />
              </a-form-item>
              <a-form-item v-if="selectedField.optionsSource === 'manual' && selectedField.type === 'cascader'" label="级联选项(JSON)">
                <a-textarea v-model:value="selectedField.cascaderOptionsJson" :rows="4" placeholder='[{"value":"a","label":"A","children":[{"value":"a1","label":"A1"}]}]' />
              </a-form-item>
            </template>
            <!-- 计算控件公式配置 -->
            <template v-if="selectedField.type === 'calculation'">
              <a-form-item label="计算公式">
                <a-textarea v-model:value="selectedField.formula" :rows="3"
                  placeholder="如：${endTime} - ${startTime}&#10;使用 ${字段标识} 引用其他控件值&#10;日期相减结果为天数差" />
              </a-form-item>
              <a-form-item label="结果单位">
                <a-select v-model:value="selectedField.calcUnit" placeholder="选择单位">
                  <a-select-option value="day">天</a-select-option>
                  <a-select-option value="hour">小时</a-select-option>
                  <a-select-option value="minute">分钟</a-select-option>
                </a-select>
              </a-form-item>
              <div class="formula-help">
                <div class="formula-help-title">公式说明：</div>
                <div>• 使用 <code>${字段标识}</code> 引用其他控件的值</div>
                <div>• 日期控件相减自动计算天数差</div>
                <div>• 数字控件支持加减乘除运算</div>
                <div>• 示例：<code>${endTime} - ${startTime}</code></div>
              </div>
            </template>
          </a-form>
        </div>
        <div v-else class="properties-empty">
          <span v-if="selectedSection">{{ selectedSection.title || '分栏' }} (选中字段编辑属性)</span>
          <span v-else>选择字段进行配置</span>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <a-modal v-model:open="showPreview" title="表单预览" :width="720" :footer="null" @after-open="onPreviewOpen">
      <a-form :model="previewData" layout="vertical" class="preview-form">
        <template v-for="section in sections" :key="section.id">
          <a-divider orientation="left">{{ section.title || '分栏' }}</a-divider>
          <template v-for="row in section.children" :key="row.id">
            <a-row :gutter="row.columns === 2 ? 16 : 0" style="margin-bottom: 8px">
              <a-col v-for="cell in row.cells" :key="cell.id" :span="row.columns === 2 ? 12 : 24">
                <a-form-item v-for="field in cell.fields" :key="field.id" :label="field.label" :required="field.required">
                  <a-input v-if="['text'].includes(field.type)" v-model:value="previewData[field.field]" :placeholder="field.placeholder||'请输入'" />
                  <a-textarea v-else-if="field.type==='textarea'" v-model:value="previewData[field.field]" :rows="3" />
                  <a-input-number v-else-if="['number','amount'].includes(field.type)" v-model:value="previewData[field.field]" style="width:100%" />
                  <a-date-picker v-else-if="field.type==='date'" v-model:value="previewData[field.field]" style="width:100%" />
                  <a-time-picker v-else-if="field.type==='time'" v-model:value="previewData[field.field]" style="width:100%" />
                  <a-date-picker v-else-if="field.type==='datetime'" v-model:value="previewData[field.field]" show-time style="width:100%" />
                  <a-select v-else-if="field.type==='select'" v-model:value="previewData[field.field]" style="width:100%">
                    <a-select-option v-for="opt in getFieldOptions(field)" :key="opt.value" :value="opt.value">{{ opt.text }}</a-select-option>
                  </a-select>
                  <a-radio-group v-else-if="field.type==='radio'" v-model:value="previewData[field.field]">
                    <a-radio v-for="opt in getFieldOptions(field)" :key="opt.value" :value="opt.value">{{ opt.text }}</a-radio>
                  </a-radio-group>
                  <a-checkbox-group v-else-if="field.type==='checkbox'" v-model:value="previewData[field.field]">
                    <a-checkbox v-for="opt in getFieldOptions(field)" :key="opt.value" :value="opt.value">{{ opt.text }}</a-checkbox>
                  </a-checkbox-group>
                  <a-select v-else-if="field.type==='user'" v-model:value="previewData[field.field]" show-search :filter-option="false"
                    :placeholder="field.placeholder||'搜索人员'" style="width:100%" @search="handleUserSearch" :loading="userLoading" :options="userOptions" />
                  <a-tree-select v-else-if="field.type==='dept'" v-model:value="previewData[field.field]" :placeholder="field.placeholder||'选择部门'"
                    style="width:100%" :tree-data="deptTreeData" show-search tree-node-filter-prop="title" allow-clear />
                  <a-cascader v-else-if="field.type==='cascader'" v-model:value="previewData[field.field]" :options="getCascaderOptions(field)" style="width:100%" change-on-select />
                  <a-upload v-else-if="field.type==='file'"><a-button>上传文件</a-button></a-upload>
                  <div v-else-if="field.type==='calculation'" class="calc-result-preview">
                    <FunctionOutlined style="margin-right: 4px; color: #722ed1" />
                    <span>{{ computeFormula(field, previewData) }}</span>
                  </div>
                  <a-input v-else v-model:value="previewData[field.field]" />
                </a-form-item>
              </a-col>
            </a-row>
          </template>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined, BorderOutlined, EyeOutlined,
  UpOutlined, DownOutlined, DeleteOutlined,
  FontSizeOutlined, AlignLeftOutlined, NumberOutlined,
  DollarOutlined, CalendarOutlined, ClockCircleOutlined,
  CheckCircleOutlined, CheckSquareOutlined,
  UploadOutlined, SelectOutlined, UserOutlined, TeamOutlined, ApartmentOutlined,
  FunctionOutlined
} from '@ant-design/icons-vue'
import { getForm, updateForm } from '../../../api/form'
import { getDataModelList } from '../../../api/model'
import { getDictTypes, getDictItemsByCode } from '../../../api/dict'
import { getUsersPage, getDeptTree } from '../../../api/system'

const route = useRoute()
const formKey = ref(route.query.formKey || '')
const fieldTypeMap = { text:'text', number:'number', amount:'amount', date:'date', datetime:'datetime', file:'file', person:'user', department:'dept' }

const componentTypes = [
  { type: 'text', label: '单行文本', icon: FontSizeOutlined },
  { type: 'textarea', label: '多行文本', icon: AlignLeftOutlined },
  { type: 'number', label: '数字', icon: NumberOutlined },
  { type: 'amount', label: '金额', icon: DollarOutlined },
  { type: 'date', label: '日期', icon: CalendarOutlined },
  { type: 'time', label: '时间', icon: ClockCircleOutlined },
  { type: 'datetime', label: '日期时间', icon: CalendarOutlined },
  { type: 'select', label: '下拉选择', icon: SelectOutlined },
  { type: 'cascader', label: '级联选择', icon: ApartmentOutlined },
  { type: 'radio', label: '单选框', icon: CheckCircleOutlined },
  { type: 'checkbox', label: '复选框', icon: CheckSquareOutlined },
  { type: 'file', label: '文件上传', icon: UploadOutlined },
  { type: 'user', label: '人员选择', icon: UserOutlined },
  { type: 'dept', label: '部门选择', icon: TeamOutlined },
  { type: 'calculation', label: '计算控件', icon: FunctionOutlined }
]

// Nested structure: sections → children(rows) → cells → fields
const sections = ref([])
const modelList = ref([])
const dictTypeList = ref([])
const dictItemsCache = reactive({})
const modelFields = ref([])
const bindModelKey = ref(null)
const selectedField = ref(null)
const selectedSection = ref(null)
const selectedRow = ref(null)
const selectedCell = ref(null)
const showPreview = ref(false)
const previewData = reactive({})
const userLoading = ref(false)
const userOptions = ref([])
const deptTreeData = ref([])

let idCounter = 0
function genId(p) { return `${p}_${++idCounter}` }

function getFieldOptions(field) {
  if (field.optionsSource === 'dict' && field.dictCode) return dictItemsCache[field.dictCode] || []
  if (!field.optionsText) return []
  return field.optionsText.split('\n').filter(Boolean).map(l => { const [v, ...r] = l.split(':'); return { value: v.trim(), text: r.join(':').trim() || v.trim() } })
}
function getCascaderOptions(field) {
  if (field.optionsSource === 'dict' && field.dictCode) return dictItemsCache[field.dictCode] || []
  try { return JSON.parse(field.cascaderOptionsJson || '[]') } catch { return [] }
}

// --- User search ---
let userSearchTimer = null
async function handleUserSearch(keyword) {
  clearTimeout(userSearchTimer)
  if (!keyword) { userOptions.value = []; return }
  userSearchTimer = setTimeout(async () => {
    userLoading.value = true
    try { const res = await getUsersPage({ keyword, page: 1, size: 10 }); const d = res.data || res; const records = d.records || d.list || d || []; userOptions.value = (Array.isArray(records) ? records : []).map(u => ({ value: u.username || u.id, label: `${u.realName || u.username} (${u.username || u.id})` })) }
    catch { userOptions.value = [] } finally { userLoading.value = false }
  }, 300)
}
async function loadDeptTree() { try { const res = await getDeptTree(); const d = res.data || res; deptTreeData.value = convertTree(Array.isArray(d) ? d : []) } catch {} }
function convertTree(nodes) { return nodes.map(n => ({ title: n.deptName || n.name, value: String(n.id), key: String(n.id), children: n.children ? convertTree(n.children) : [] })) }
function onPreviewOpen() { if (showPreview.value) loadDeptTree() }

// --- Drag & Drop ---
function handleDragLayout(event, type) { event.dataTransfer.setData('dragType', 'layout'); event.dataTransfer.setData('layoutType', type) }
function handleDragField(event, comp) { event.dataTransfer.setData('dragType', 'newField'); event.dataTransfer.setData('compType', JSON.stringify(comp)) }

function handleDropOnCanvas(event) {
  const dragType = event.dataTransfer.getData('dragType')
  if (dragType === 'layout' && event.dataTransfer.getData('layoutType') === 'section') {
    sections.value.push({ id: genId('section'), title: '新分栏', children: [] })
  }
}

function handleDropOnSection(event, section) {
  const dragType = event.dataTransfer.getData('dragType')
  if (dragType !== 'layout') return
  const layoutType = event.dataTransfer.getData('layoutType')
  if (layoutType === 'section') { sections.value.push({ id: genId('section'), title: '新分栏', children: [] }); return }
  const cols = layoutType === 'row-2col' ? 2 : 1
  section.children.push({ id: genId('row'), columns: cols, cells: Array.from({ length: cols }, () => ({ id: genId('cell'), fields: [] })) })
}

function handleDropOnCell(event, section, row, cell) {
  event.stopPropagation()
  const dragType = event.dataTransfer.getData('dragType')
  if (dragType === 'newField') {
    const comp = JSON.parse(event.dataTransfer.getData('compType'))
    const field = createField(comp); cell.fields.push(field); selectField(field)
  } else if (dragType === 'existingField') {
    // Remove from source
    const srcData = JSON.parse(event.dataTransfer.getData('srcInfo'))
    for (const sec of sections.value) for (const r of sec.children) for (const c of r.cells) {
      const idx = c.fields.findIndex(f => f.id === srcData.fieldId)
      if (idx >= 0) { c.fields.splice(idx, 1); break }
    }
    cell.fields.push(JSON.parse(event.dataTransfer.getData('fieldData'))); selectField(cell.fields[cell.fields.length - 1])
  }
}

function handleDragExistingField(event, section, row, cell, field) {
  event.dataTransfer.setData('dragType', 'existingField')
  event.dataTransfer.setData('srcInfo', JSON.stringify({ fieldId: field.id }))
  event.dataTransfer.setData('fieldData', JSON.stringify(field))
}

function createField(comp) {
  return {
    id: genId('field'), type: comp.type, field: `${comp.type}_${idCounter}`,
    label: comp.label, placeholder: `请输入${comp.label}`, required: false, defaultValue: '',
    optionsText: ['select','radio','checkbox'].includes(comp.type) ? '1:选项1\n2:选项2' : '',
    optionsSource: ['select','radio','checkbox','cascader'].includes(comp.type) ? 'manual' : undefined,
    dictCode: null, modelField: null,
    cascaderOptionsJson: comp.type === 'cascader' ? '[{"value":"1","label":"选项1","children":[{"value":"1-1","label":"子选项1"}]}]' : undefined,
    formula: comp.type === 'calculation' ? '' : undefined,
    calcUnit: comp.type === 'calculation' ? 'day' : undefined
  }
}

// --- Selection ---
function selectSection(s) { selectedSection.value = s; selectedRow.value = null; selectedCell.value = null; selectedField.value = null }
function selectRow(r) { selectedRow.value = r; selectedField.value = null }
function selectCell(c) { selectedCell.value = c; selectedField.value = null }
function selectField(f) { selectedField.value = f }

// --- Operations ---
function moveSection(i, d) { const n = i + d; if (n < 0 || n >= sections.value.length) return; [sections.value[i], sections.value[n]] = [sections.value[n], sections.value[i]] }
function removeSection(i) { sections.value.splice(i, 1); selectedSection.value = null; selectedField.value = null }
function moveRow(section, i, d) { const n = i + d; if (n < 0 || n >= section.children.length) return; [section.children[i], section.children[n]] = [section.children[n], section.children[i]] }
function removeRow(section, i) { section.children.splice(i, 1); selectedRow.value = null; selectedField.value = null }
function removeField(cell, i) { cell.fields.splice(i, 1); selectedField.value = null }

function handleOptionsSourceChange() {
  if (selectedField.value.optionsSource === 'dict') selectedField.value.optionsText = ''
  else selectedField.value.dictCode = null
}
async function handleDictChange(code) {
  if (!code || dictItemsCache[code]) return
  try { const res = await getDictItemsByCode(code); const items = res.data || res; dictItemsCache[code] = (Array.isArray(items) ? items : []).map(it => ({ value: it.itemValue, text: it.itemText })) } catch {}
}

/** 计算公式：替换 ${fieldKey} 为实际值，支持日期差和数值运算 */
function computeFormula(field, data) {
  if (!field.formula) return '未配置公式'
  try {
    const unit = field.calcUnit || 'day'
    // 提取所有 ${xxx} 引用
    const refs = field.formula.match(/\$\{(\w+)\}/g) || []
    if (refs.length === 0) return field.formula

    // 检查是否为日期差计算模式（两个日期字段相减）
    const refKeys = refs.map(r => r.match(/\$\{(\w+)\}/)[1])
    const values = refKeys.map(k => data[k])

    // 如果所有引用的值都存在且看起来是日期
    if (values.every(v => v && isDateString(String(v)))) {
      if (refKeys.length === 2 && field.formula.includes('-')) {
        const d1 = new Date(values[0])
        const d2 = new Date(values[1])
        const diffMs = Math.abs(d2 - d1)
        if (unit === 'hour') return Math.round(diffMs / 3600000) + ' 小时'
        if (unit === 'minute') return Math.round(diffMs / 60000) + ' 分钟'
        return Math.round(diffMs / 86400000) + ' 天'
      }
    }

    // 数值运算模式：替换变量为数值
    let expr = field.formula
    refs.forEach(ref => {
      const key = ref.match(/\$\{(\w+)\}/)[1]
      const val = parseFloat(data[key]) || 0
      expr = expr.replace(ref, val)
    })
    // 简单安全计算：只允许数字和 +-*/()
    if (/^[\d\s+\-*/().]+$/.test(expr)) {
      const result = Function('"use strict"; return (' + expr + ')')()
      return Number.isFinite(result) ? Math.round(result * 100) / 100 : '计算错误'
    }
    return expr
  } catch {
    return '公式错误'
  }
}

function isDateString(str) {
  return /^\d{4}-\d{2}-\d{2}/.test(str) || /^\d{4}\/\d{2}\/\d{2}/.test(str)
}

// --- Model ---
async function loadModelList() { try { const res = await getDataModelList({ page: 1, size: 100 }); const d = res.data || res; const list = Array.isArray(d) ? d : (d.records || d.list || []); modelList.value = list.filter(m => m.status === 1) } catch {} }
async function loadDictTypes() { try { const res = await getDictTypes({ page: 1, size: 100 }); const d = res.data || res; dictTypeList.value = d.records || d.list || d || [] } catch {} }

async function handleModelChange(modelKey) {
  if (!modelKey) { modelFields.value = []; return }
  try {
    const res = await getDataModelList({}); const d = res.data || res; const allList = Array.isArray(d) ? d : (d.records || d.list || [])
    const model = allList.find(m => m.modelKey === modelKey)
    if (!model) { message.warning('未找到数据模型'); return }
    let mj; try { mj = JSON.parse(model.modelJson || '{}') } catch { return }
    modelFields.value = mj.mainTable?.fields || []
    // Auto-generate: create one section with rows for model fields
    if (sections.value.length === 0 && modelFields.value.length > 0) {
      const sec = { id: genId('section'), title: model.modelName || '基本信息', children: [] }
      const fields = modelFields.value.map(f => ({
        id: genId('field'), type: fieldTypeMap[f.type] || 'text', field: f.fieldKey,
        label: f.label || f.fieldKey, placeholder: `请输入${f.label}`, required: f.required || false,
        defaultValue: '', fromModel: true, modelKey, modelField: f.fieldKey, optionsSource: null, dictCode: null
      }))
      // Group: 2 fields per row
      for (let i = 0; i < fields.length; i += 2) {
        const rowFields = fields.slice(i, i + 2)
        const cells = rowFields.map(f => ({ id: genId('cell'), fields: [f] }))
        while (cells.length < 2) cells.push({ id: genId('cell'), fields: [] })
        sec.children.push({ id: genId('row'), columns: 2, cells })
      }
      sections.value.push(sec)
      message.success(`已绑定模型，生成 ${sec.children.length} 个布局行`)
    }
  } catch { message.error('加载模型失败') }
}

// --- Save / Load ---
async function handleSave() {
  if (!formKey.value) { message.warning('未关联表单'); return }
  const formJson = JSON.stringify({ sections: sections.value, modelKey: bindModelKey.value }, null, 2)
  try { await updateForm(formKey.value, { formKey: formKey.value, formJson, modelKey: bindModelKey.value || null }); message.success('保存成功') }
  catch { message.error('保存失败') }
}

async function loadForm() {
  if (!formKey.value) return
  try {
    const res = await getForm(formKey.value); const form = res.data || res
    bindModelKey.value = form.modelKey || null
    if (form.formJson) {
      const parsed = JSON.parse(form.formJson)
      if (parsed.sections) { sections.value = parsed.sections }
      else if (parsed.layouts) {
        // Migrate legacy flat layouts to nested
        const sec = { id: genId('section'), title: '表单', children: [] }
        parsed.layouts.forEach(l => {
          if (l.type === 'section') { sections.value.push({ ...l, children: l.children || [] }) }
          else { sec.children.push(l) }
        })
        if (sec.children.length > 0) sections.value.push(sec)
      } else if (Array.isArray(parsed)) {
        sections.value = [{ id: genId('section'), title: '表单字段', children: [
          { id: genId('row'), columns: 1, cells: [{ id: genId('cell'), fields: parsed }] }
        ]}]
      }
      // Rebuild counter
      let maxId = 0
      sections.value.forEach(s => {
        const n = parseInt(s.id.split('_')[1]) || 0; if (n > maxId) maxId = n
        s.children?.forEach(r => {
          const rn = parseInt(r.id.split('_')[1]) || 0; if (rn > maxId) maxId = rn
          r.cells?.forEach(c => {
            const cn = parseInt(c.id.split('_')[1]) || 0; if (cn > maxId) maxId = cn
            c.fields?.forEach(f => {
              const fn = parseInt(f.id.split('_')[1]) || 0; if (fn > maxId) maxId = fn
              if (f.dictCode && !dictItemsCache[f.dictCode]) handleDictChange(f.dictCode)
            })
          })
        })
      })
      idCounter = maxId
      if (form.modelKey) handleModelChange(form.modelKey)
    }
  } catch {}
}

onMounted(() => { loadModelList(); loadDictTypes(); loadForm() })
</script>

<style scoped>
.designer-wrap { display: flex; flex-direction: row; height: calc(100vh - 180px); gap: 0; }
.designer-sidebar { width: 200px; min-width: 200px; flex-shrink: 0; background: #fff; border-right: 1px solid var(--border-light, #e8e8e8); display: flex; flex-direction: column; overflow-y: auto; }
.designer-properties { width: 200px; min-width: 200px; flex-shrink: 0; background: #fff; border-left: 1px solid var(--border-light, #e8e8e8); display: flex; flex-direction: column; overflow-y: auto; }
.sidebar-title { padding: 10px 14px; font-weight: 600; font-size: 13px; border-bottom: 1px solid var(--border-light, #e8e8e8); background: #fafafa; }
.component-list { padding: 8px 10px; overflow-y: auto; flex: 1; }
.layout-list { padding: 8px 10px; flex: none; }
.component-item { display: flex; align-items: center; gap: 8px; padding: 7px 10px; margin-bottom: 6px; background: #f5f7fa; border-radius: 4px; cursor: grab; font-size: 13px; border: 1px solid transparent; transition: all 0.2s; }
.component-item:hover { background: #e6f0ff; border-color: #1677ff; }
.layout-item { background: #f0f5ff; border: 1px dashed #91caff; }
.designer-canvas { flex: 1; display: flex; flex-direction: column; background: #f5f7fa; }
.canvas-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: #fff; border-bottom: 1px solid var(--border-light, #e8e8e8); }
.canvas-header-left, .canvas-header-right { display: flex; align-items: center; }
.canvas-content { flex: 1; padding: 16px; overflow-y: auto; }
.top-drop-zone { min-height: 200px; }
.canvas-empty { display: flex; align-items: center; justify-content: center; height: 200px; border: 2px dashed #d9d9d9; border-radius: 8px; color: #999; }
.section-container { margin-bottom: 16px; border: 1px solid #b7eb8f; border-radius: 8px; background: #fff; }
.section-container.active { border-color: #52c41a; box-shadow: 0 0 0 2px rgba(82,196,26,0.15); }
.section-header { display: flex; align-items: center; padding: 8px 12px; background: #f6ffed; border-bottom: 1px solid #d9f7be; border-radius: 8px 8px 0 0; }
.section-title-input { border: none; background: transparent; font-weight: 600; font-size: 14px; flex: 1; padding: 2px 4px; border-radius: 3px; }
.section-title-input:focus { background: #fff; outline: 1px solid #52c41a; }
.section-actions { display: flex; gap: 6px; }
.section-actions .anticon { cursor: pointer; color: #999; font-size: 12px; }
.section-actions .anticon:hover { color: #52c41a; }
.section-body { padding: 12px; min-height: 60px; }
.section-empty { display: flex; align-items: center; justify-content: center; height: 50px; color: #bbb; font-size: 12px; border: 1px dashed #d9d9d9; border-radius: 4px; }
.row-container { margin-bottom: 10px; border: 1px solid #e8e8e8; border-radius: 6px; background: #fafafa; }
.row-container:last-child { margin-bottom: 0; }
.row-container.active { border-color: #1677ff; }
.row-header { display: flex; justify-content: space-between; align-items: center; padding: 4px 10px; border-bottom: 1px solid #f0f0f0; }
.row-label { font-size: 11px; color: #888; }
.row-actions { display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s; }
.row-container:hover .row-actions { opacity: 1; }
.row-actions .anticon { cursor: pointer; color: #999; font-size: 11px; }
.row-actions .anticon:hover { color: #1677ff; }
.row-cells { display: flex; padding: 10px; gap: 0; }
.row-cells.cells-2 .layout-cell { flex: 1; }
.row-cells.cells-2 .layout-cell:first-child { margin-right: 8px; }
.layout-cell { flex: 1; min-height: 50px; border: 1px dashed #d9d9d9; border-radius: 4px; padding: 8px; background: #fff; }
.layout-cell:hover { border-color: #1677ff; }
.cell-empty { display: flex; align-items: center; justify-content: center; height: 36px; color: #ccc; font-size: 12px; }
.cell-field { position: relative; padding: 8px; margin-bottom: 6px; background: #fff; border: 1px solid #e8e8e8; border-radius: 4px; cursor: pointer; transition: all 0.2s; }
.cell-field:hover { border-color: #1677ff; }
.cell-field.active { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22,119,255,0.15); }
.cell-field:last-child { margin-bottom: 0; }
.field-label { margin-bottom: 4px; font-size: 12px; font-weight: 500; }
.required-tag { margin-left: 6px; padding: 0 4px; font-size: 11px; color: #ff4d4f; border: 1px solid #ff4d4f; border-radius: 2px; }
.field-preview { pointer-events: none; }
.field-delete-btn { position: absolute; top: 4px; right: 4px; opacity: 0; cursor: pointer; color: #999; font-size: 12px; }
.cell-field:hover .field-delete-btn { opacity: 1; }
.field-delete-btn:hover { color: #ff4d4f; }
.properties-form { padding: 12px; overflow-y: auto; flex: 1; }
.properties-empty { display: flex; align-items: center; justify-content: center; height: 150px; color: #999; font-size: 13px; }
.preview-form { padding: 8px 0; }
.calc-preview { display: flex; align-items: center; font-size: 12px; color: #722ed1; padding: 4px 8px; background: #f9f0ff; border: 1px dashed #d3adf7; border-radius: 4px; }
.calc-formula { font-family: monospace; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.calc-result-preview { display: flex; align-items: center; padding: 6px 12px; background: #f9f0ff; border: 1px solid #d3adf7; border-radius: 4px; font-size: 14px; color: #722ed1; font-weight: 500; min-height: 32px; }
.formula-help { padding: 10px; background: #f6f8fa; border-radius: 4px; font-size: 12px; color: #666; line-height: 1.8; }
.formula-help-title { font-weight: 600; color: #333; margin-bottom: 4px; }
.formula-help code { background: #e8e8e8; padding: 1px 4px; border-radius: 2px; font-size: 11px; color: #722ed1; }
</style>
