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
          <a-button @click="handleOpenDesigner">设计流程图</a-button>
        </a-space>
      </div>

      <a-spin :spinning="loading">
        <!-- 基本信息 -->
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
                  <a-tag color="blue">{{ selectedForm?.formName }}</a-tag>
                  <span style="color: var(--text-secondary); font-size: 12px">{{ selectedFormKey }}</span>
                </a-form-item>
              </a-form>
            </a-col>
            <a-col :span="14">
              <div v-if="selectedForm" class="form-preview">
                <div class="form-preview-title">表单字段预览</div>
                <a-table
                  :columns="formFieldColumns"
                  :data-source="formFields"
                  :pagination="false"
                  size="small"
                  row-key="key"
                />
              </div>
              <a-empty v-else description="请选择表单以预览字段" />
            </a-col>
          </a-row>
        </a-card>

        <!-- 节点配置说明 -->
        <a-card title="节点表单权限">
          <a-alert
            message="节点级表单权限在设计器中配置"
            description="点击「设计流程图」按钮打开设计器，选中节点后可在右侧面板配置该节点的表单内容权限（字段编辑/只读/隐藏）。"
            type="info"
            show-icon
          />
          <div v-if="processDef?.processJson" style="margin-top: 16px">
            <div class="section-subtitle">已定义的节点</div>
            <a-table
              :columns="nodeColumns"
              :data-source="parsedNodes"
              :pagination="false"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'formPermissions'">
                  <template v-if="record.formPermissions">
                    <a-tag color="green">已配置</a-tag>
                  </template>
                  <template v-else>
                    <a-tag>未配置</a-tag>
                  </template>
                </template>
              </template>
            </a-table>
          </div>
          <a-empty v-else description="尚未设计流程图，请先点击「设计流程图」" />
        </a-card>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { RollbackOutlined } from '@ant-design/icons-vue'
import { getProcessDefinitionByKey, updateProcessDefinition } from '../../../api/process'
import { getFormAll, getForm } from '../../../api/form'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const saveLoading = ref(false)
const processDef = ref(null)
const formList = ref([])
const selectedFormKey = ref(null)
const selectedForm = ref(null)
const formFields = ref([])

const processKey = computed(() => route.query.processKey)

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

const formFieldColumns = [
  { title: '字段Key', dataIndex: 'key', key: 'key' },
  { title: '字段名称', dataIndex: 'label', key: 'label' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '必填', dataIndex: 'required', key: 'required', width: 60,
    customRender: ({ text }) => text ? '是' : '否' }
]

const nodeColumns = [
  { title: '节点ID', dataIndex: 'id', key: 'id' },
  { title: '节点名称', dataIndex: 'name', key: 'name' },
  { title: '节点类型', dataIndex: 'type', key: 'type' },
  { title: '关联表单', dataIndex: 'formKey', key: 'formKey' },
  { title: '表单权限', key: 'formPermissions' }
]

const parsedNodes = computed(() => {
  if (!processDef.value?.processJson) return []
  try {
    const json = JSON.parse(processDef.value.processJson)
    return (json.nodes || []).map(n => ({
      id: n.id,
      name: n.name,
      type: n.type,
      formKey: n.properties?.formKey || '-',
      formPermissions: n.properties?.formPermissions || null
    }))
  } catch {
    return []
  }
})

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
    formFields.value = []
    return
  }
  try {
    const res = await getForm(formKey)
    selectedForm.value = res.data || res
    // 解析表单字段
    if (selectedForm.value?.formJson) {
      try {
        const formJson = JSON.parse(selectedForm.value.formJson)
        formFields.value = (formJson.components || formJson.fields || []).map(c => ({
          key: c.key || c.id || c.name,
          label: c.label || c.title || c.key,
          type: c.type || c.component,
          required: c.required || false
        }))
      } catch {
        formFields.value = []
      }
    } else {
      formFields.value = []
    }
  } catch {
    message.error('加载表单详情失败')
  }
}

async function handleSaveConfig() {
  if (!processDef.value) return
  saveLoading.value = true
  try {
    // 将表单绑定信息写入 processJson
    let processJson = processDef.value.processJson
    if (processJson) {
      try {
        const json = JSON.parse(processJson)
        if (selectedFormKey.value) {
          json.formKey = selectedFormKey.value
        } else {
          delete json.formKey
        }
        processJson = JSON.stringify(json)
      } catch {
        // processJson 解析失败，仅保存基本信息
      }
    }

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

function handleOpenDesigner() {
  // 跳转到设计器页面，携带 processKey 参数
  router.push(`/process/designer?processKey=${processKey.value}&id=${processDef.value?.id || ''}`)
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
.form-preview {
  border: 1px solid var(--border-color, #f0f0f0);
  border-radius: 6px;
  padding: 12px;
  background: var(--bg-color-container, #fafafa);
}
.form-preview-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-secondary, #666);
}
.section-subtitle {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-primary, #333);
}
</style>
