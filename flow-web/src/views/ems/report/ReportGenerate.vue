<template>
  <div>
    <a-card :bordered="false" class="toolbar">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="generate" tab="生成报告">
          <a-form layout="vertical" :model="form">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="报告模板" required>
                  <a-select
                    v-model:value="form.tplId"
                    placeholder="选择模板"
                    :options="templateOptions"
                    @change="onTplChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="报告标题" required>
                  <a-input v-model:value="form.title" placeholder="如 2026年第二季度环境检测报告" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="委托单位">
                  <a-input v-model:value="form.client" placeholder="委托单位名称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="报告周期">
                  <a-input v-model:value="form.period" placeholder="如 2026-Q2" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="选择检测任务（仅已复核）" required>
              <a-table
                rowKey="taskId"
                :columns="taskColumns"
                :dataSource="pendingTasks"
                :rowSelection="rowSelection"
                :pagination="{ pageSize: 8 }"
                size="small"
              />
            </a-form-item>

            <a-space>
              <a-button type="primary" :loading="generating" @click="onGenerate">生成报告</a-button>
              <span v-if="preview" class="summary">
                共 <b>{{ preview.itemCount }}</b> 项，其中超标 <b style="color:#cf1322">{{ preview.exceedCount }}</b> 项
              </span>
            </a-space>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="template" tab="模板管理">
          <a-space style="margin-bottom:12px">
            <a-button type="primary" @click="showTplModal = true">新增模板</a-button>
          </a-space>
          <a-table
            rowKey="id"
            :columns="tplColumns"
            :dataSource="templates"
            :pagination="{ pageSize: 10 }"
            size="small"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="showTplModal"
      title="新增报告模板"
      @ok="onCreateTemplate"
      :confirmLoading="tplSaving"
    >
      <a-form layout="vertical" :model="tplForm">
        <a-form-item label="模板名称" required>
          <a-input v-model:value="tplForm.name" />
        </a-form-item>
        <a-form-item label="模板类型" required>
          <a-select v-model:value="tplForm.type" :options="typeOptions" />
        </a-form-item>
        <a-form-item label="模板内容说明">
          <a-textarea v-model:value="tplForm.content" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getReportTemplates,
  createReportTemplate,
  getReportPendingTasks,
  generateReport
} from '../../../api/ems'

const activeTab = ref('generate')

const form = reactive({ tplId: undefined, title: '', client: '', period: '' })
const templates = ref([])
const pendingTasks = ref([])
const generating = ref(false)
const preview = ref(null)

const templateOptions = computed(() =>
  templates.value.map(t => ({ value: t.id, label: `${t.name}（${t.type}）` }))
)

const typeOptions = [
  { value: '季度', label: '季度' },
  { value: '月度', label: '月度' },
  { value: '委托', label: '委托' }
]

const selectedIds = ref([])
const rowSelection = {
  onChange: (keys) => { selectedIds.value = keys }
}

const taskColumns = [
  { title: '任务编号', dataIndex: 'taskNo' },
  { title: '样品名称', dataIndex: 'sampleName' },
  { title: '样品编号', dataIndex: 'sampleCode' },
  { title: '监测项目', dataIndex: 'monitorItem' },
  { title: '复核人', dataIndex: 'reviewBy' }
]

const tplColumns = [
  { title: '模板编号', dataIndex: 'tplNo' },
  { title: '名称', dataIndex: 'name' },
  { title: '类型', dataIndex: 'type' },
  { title: '状态', dataIndex: 'enabled', customRender: ({ text }) => (text === '1' ? '启用' : '停用') }
]

const showTplModal = ref(false)
const tplSaving = ref(false)
const tplForm = reactive({ name: '', type: '季度', content: '' })

const generator = localStorage.getItem('realName') || localStorage.getItem('username') || 'sys_admin'

async function loadTemplates() {
  const res = await getReportTemplates()
  templates.value = res.data || []
}
async function loadPendingTasks() {
  const res = await getReportPendingTasks()
  pendingTasks.value = res.data || []
}

function onTplChange() {
  const t = templates.value.find(x => x.id === form.tplId)
  if (t && !form.title) {
    form.title = `${t.name}报告`
  }
}

async function onGenerate() {
  if (!form.tplId) { message.warning('请选择报告模板'); return }
  if (!form.title) { message.warning('请填写报告标题'); return }
  if (selectedIds.value.length === 0) { message.warning('请至少选择一个检测任务'); return }
  generating.value = true
  try {
    const res = await generateReport({
      tplId: form.tplId,
      title: form.title,
      client: form.client,
      period: form.period,
      taskIds: selectedIds.value,
      generator
    })
    const id = res.data
    preview.value = { itemCount: selectedIds.value.length, exceedCount: '—' }
    message.success(`报告生成成功，报告ID=${id}`)
    await loadPendingTasks()
  } catch (e) {
    message.error('生成失败：' + (e.response?.data?.message || e.message))
  } finally {
    generating.value = false
  }
}

async function onCreateTemplate() {
  if (!tplForm.name) { message.warning('请填写模板名称'); return }
  tplSaving.value = true
  try {
    await createReportTemplate({ ...tplForm })
    message.success('模板已创建')
    showTplModal.value = false
    tplForm.name = ''
    tplForm.content = ''
    await loadTemplates()
  } catch (e) {
    message.error('创建失败：' + (e.response?.data?.message || e.message))
  } finally {
    tplSaving.value = false
  }
}

onMounted(() => {
  loadTemplates()
  loadPendingTasks()
})
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.summary { color: #595959; }
</style>
