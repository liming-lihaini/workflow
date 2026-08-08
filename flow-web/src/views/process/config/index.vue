<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <a-space>
          <a-button @click="router.back()">
            <template #icon><RollbackOutlined /></template>
            返回
          </a-button>
          <span class="page-title">流程配置</span>
          <a-tag v-if="processDef" :color="processDef.status === 1 ? 'green' : 'default'">
            {{ processDef.status === 1 ? '已部署' : '草稿' }}
          </a-tag>
        </a-space>
        <a-space>
          <a-button type="primary" @click="handleSaveConfig" :loading="saveLoading">保存配置</a-button>
          <a-button @click="activeTab = 'design'">设计流程图</a-button>
        </a-space>
      </div>

      <a-spin :spinning="loading">
        <a-tabs v-model:activeKey="activeTab">
          <!-- 基本信息 -->
          <a-tab-pane key="basic" tab="基本信息">
            <a-card title="基本信息" style="margin-bottom: 16px">
              <a-descriptions :column="2" bordered size="small">
                <a-descriptions-item label="流程标识">{{ processDef?.processKey }}</a-descriptions-item>
                <a-descriptions-item label="流程名称">{{ processDef?.processName }}</a-descriptions-item>
                <a-descriptions-item label="流程类型">
                  <a-tag :color="getTypeColor(processDef?.processType)">{{ getTypeName(processDef?.processType) }}</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="分类">{{ processDef?.category || '-' }}</a-descriptions-item>
                <a-descriptions-item label="用途描述" :span="2">{{ processDef?.description || '-' }}</a-descriptions-item>
              </a-descriptions>
            </a-card>

            <!-- 表单配置 -->
            <a-card title="表单配置" style="margin-bottom: 16px">
              <a-row :gutter="24">
                <a-col :span="10">
                  <a-form layout="vertical">
                    <a-form-item label="选择表单">
                      <a-select
                        v-model:value="selectedFormKey"
                        placeholder="请选择流程关联的表单"
                        allow-clear
                        show-search
                        :filter-option="filterOption"
                        @change="handleFormChange"
                      >
                        <a-select-option v-for="form in formList" :key="form.formKey" :value="form.formKey">
                          {{ form.formName }} ({{ form.formKey }})
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                    <a-form-item v-if="selectedFormKey" label="已选表单">
                      <a-space>
                        <a-tag color="blue">{{ selectedForm?.formName }}</a-tag>
                        <span style="color: var(--text-secondary); font-size: 12px">{{ selectedFormKey }}</span>
                        <a-button size="small" @click="handlePreviewOpen">
                          <template #icon><EyeOutlined /></template>
                          预览
                        </a-button>
                      </a-space>
                    </a-form-item>
                  </a-form>
                </a-col>
              </a-row>
            </a-card>
          </a-tab-pane>

          <!-- 流程图设计 -->
          <a-tab-pane key="design" tab="流程图设计">
            <a-card :body-style="{ padding: 0 }">
              <div class="designer-embed">
                <iframe :src="designerUrl" frameborder="0" allowfullscreen />
              </div>
            </a-card>
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </div>

    <!-- 表单预览抽屉（与表单设计预览一致） -->
    <a-drawer v-model:open="showFormPreview" title="表单预览" placement="right" :width="1000">
      <FormRenderer
        v-if="selectedForm?.formJson"
        :form-json="selectedForm.formJson"
        :dict-data="dictData"
        v-model="formPreviewValues"
        mode="editable"
      />
      <a-empty v-else description="表单暂无内容" />
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { RollbackOutlined, EyeOutlined } from '@ant-design/icons-vue'
import { getProcessDefinitionByKey, updateProcessDefinition } from '../../../api/process'
import { getFormAll, getForm } from '../../../api/form'
import { getDictItemsByCode } from '../../../api/dict'
import FormRenderer from '../../../components/FormRenderer.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const saveLoading = ref(false)
const processDef = ref(null)
const formList = ref([])
const selectedFormKey = ref(null)
const selectedForm = ref(null)
const showFormPreview = ref(false)
const formPreviewValues = ref({})
const dictData = ref({})
const activeTab = ref('basic')

const processKey = computed(() => route.query.processKey)
const definitionId = computed(() => route.query.id || processDef.value?.id || '')

// 监听 flow-designer iframe 的 token 请求，返回当前应用的 token
// （两个应用不同端口，localStorage 不共享）
window.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'FLOW_DESIGNER_TOKEN_REQUEST') {
    const token = localStorage.getItem('token')
    if (token) {
      event.source.postMessage({ type: 'FLOW_DESIGNER_TOKEN_RESPONSE', token }, '*')
    }
  }
})

// 内嵌 React 设计器地址（与生产/开发环境一致）
const designerUrl = computed(() => {
  const params = new URLSearchParams()
  if (processKey.value) params.set('processKey', processKey.value)
  if (definitionId.value) params.set('id', definitionId.value)
  const queryStr = params.toString() ? `?${params.toString()}` : ''
  if (import.meta.env.DEV) return `http://localhost:3001${queryStr}`
  return `/designer${queryStr}`
})

const typeMap = {
  approval: { name: '审批流', color: 'blue' },
  process: { name: '业务流程', color: 'green' },
  callback: { name: '回调流程', color: 'purple' }
}

function getTypeName(type) {
  return typeMap[type]?.name || type || '-'
}

function getTypeColor(type) {
  return typeMap[type]?.color || 'default'
}

function filterOption(input, option) {
  return option.children?.[0]?.children?.toLowerCase().includes(input.toLowerCase()) ||
         option.value?.toLowerCase().includes(input.toLowerCase())
}

async function loadProcessDef() {
  if (!processKey.value) return
  loading.value = true
  try {
    const res = await getProcessDefinitionByKey(processKey.value)
    processDef.value = res.data || res
    // 从 processJson 中提取已绑定的表单
    if (processDef.value?.processJson) {
      try {
        const json = JSON.parse(processDef.value.processJson)
        if (json.formKey) {
          selectedFormKey.value = json.formKey
        }
      } catch { /* ignore */ }
    }
  } catch (e) {
    message.error('加载流程定义失败: ' + (e.message || ''))
  }
  loading.value = false
}

async function loadFormList() {
  try {
    const res = await getFormAll()
    formList.value = res.data || res || []
  } catch {
    // ignore
  }
}

async function handleFormChange(formKey) {
  if (!formKey) {
    selectedForm.value = null
    return
  }
  try {
    const res = await getForm(formKey)
    selectedForm.value = res.data || res
  } catch {
    message.error('加载表单详情失败')
  }
}

// 收集表单字段中引用数据字典的 dictCode 并批量加载，供预览渲染下拉项
function collectFormDictCodes(formJson) {
  const codes = new Set()
  try {
    const json = typeof formJson === 'string' ? JSON.parse(formJson) : formJson
    ;(json.sections || []).forEach(s => (s.children || []).forEach(r => (r.cells || []).forEach(c => (c.fields || []).forEach(f => {
      if (f.optionsSource === 'dict' && f.dictCode) codes.add(f.dictCode)
    }))))
  } catch { /* ignore */ }
  return [...codes]
}

async function loadFormDictData(formJson) {
  const codes = collectFormDictCodes(formJson)
  const map = {}
  await Promise.all(codes.map(async code => {
    try {
      const res = await getDictItemsByCode(code)
      const items = res.data || res || []
      map[code] = items.map(it => ({ itemValue: it.itemValue, itemText: it.itemText }))
    } catch { /* 单字典加载失败不影响其余 */ }
  }))
  dictData.value = map
}

async function handlePreviewOpen() {
  if (selectedForm.value?.formJson) {
    await loadFormDictData(selectedForm.value.formJson)
  }
}

async function handleSaveConfig() {
  if (!processDef.value) return
  saveLoading.value = true
  try {
    // 将表单绑定信息写入 processJson（流程图未设计时初始化空结构，保证表单关联可持久化）
    let processJson = processDef.value.processJson
    let json
    if (processJson) {
      try { json = JSON.parse(processJson) } catch { json = null }
    }
    if (!json) json = { nodes: [], edges: [] }
    if (selectedFormKey.value) {
      json.formKey = selectedFormKey.value
    } else {
      delete json.formKey
    }
    processJson = JSON.stringify(json)

    await updateProcessDefinition(processDef.value.id, {
      processName: processDef.value.processName,
      processType: processDef.value.processType,
      description: processDef.value.description,
      category: processDef.value.category,
      processJson: processJson || processDef.value.processJson
    })
    message.success('配置保存成功')
    await loadProcessDef()
  } catch (e) {
    message.error('保存失败: ' + (e.message || ''))
  }
  saveLoading.value = false
}

onMounted(async () => {
  await loadProcessDef()
  await loadFormList()
  // 如果已有选中的表单，加载其详情
  if (selectedFormKey.value) {
    await handleFormChange(selectedFormKey.value)
  }
})
</script>

<style scoped>
.section-subtitle {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-primary, #333);
}
.designer-embed {
  height: calc(100vh - 320px);
  min-height: 480px;
}
.designer-embed iframe {
  width: 100%;
  height: 100%;
  border: 0;
}
</style>
