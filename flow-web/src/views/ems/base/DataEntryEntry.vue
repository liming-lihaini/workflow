<template>
  <div class="page-container">
    <div class="entry-header">
      <a-button type="link" class="entry-back" @click="goBack">
        <left-outlined />
        <span>返回</span>
      </a-button>
      <span class="entry-title">检测结果录入</span>
      <span class="entry-subtitle">{{ task?.barcode || '' }}</span>
      <a-space class="entry-actions">
        <a-button :loading="saving" @click="saveResults">临时保存草稿</a-button>
        <a-button type="primary" :loading="saving" @click="saveAndSubmit">✅ 提交全部检验数据</a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16">
        <!-- 左侧：样品基础信息 & 检测责任人 -->
        <a-col :span="9">
          <a-card size="small" title="样品基础信息" class="sample-info">
            <a-descriptions bordered :column="1" size="small">
              <a-descriptions-item label="样品编号">{{ task?.barcode || '-' }}</a-descriptions-item>
              <a-descriptions-item label="样品名称">{{ task?.sampleName || '-' }}</a-descriptions-item>
              <a-descriptions-item label="样品类型">{{ sampleTypeText(sample?.type) }}</a-descriptions-item>
              <a-descriptions-item label="采样人员">{{ sample?.sampler || '-' }}</a-descriptions-item>
              <a-descriptions-item label="采样完成时间">{{ sample?.sampleTime || sample?.receiveTime || '-' }}</a-descriptions-item>
              <a-descriptions-item label="当前状态">
                <a-tag :color="statusColor(task?.status)">{{ task?.status || '-' }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <a-card size="small" title="检测责任人" class="mt-16">
            <a-form layout="vertical">
              <a-form-item label="当前责任人">
                <a-tag color="blue">{{ realName(entryForm.entryBy) || '未指定' }}</a-tag>
              </a-form-item>
              <a-form-item label="调整检测责任人">
                <a-select v-model:value="entryForm.entryBy" placeholder="请选择检测责任人" show-search option-filter-prop="label">
                  <a-select-option v-for="u in userOptions" :key="u.value" :value="u.value" :label="u.label">{{ u.label }}</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="检测环境温度">
                <a-input v-model:value="entryForm.envTemp" addon-after="℃" placeholder="如 25.0" />
              </a-form-item>
              <a-form-item label="环境湿度">
                <a-input v-model:value="entryForm.envHumidity" addon-after="%RH" placeholder="如 55" />
              </a-form-item>
            </a-form>
          </a-card>

          <a-card size="small" title="整体检验备注" class="mt-16">
            <a-textarea v-model:value="entryForm.remark" :rows="4" placeholder="填写检测异常、仪器情况、环境干扰等说明..." />
          </a-card>
        </a-col>

        <!-- 右侧：检测项目 & 数据录入 -->
        <a-col :span="15">
          <a-card size="small" title="检测项目 & 检验数据录入表">
            <a-table :columns="resultColumns" :data-source="resultRows" :pagination="false" size="small" row-key="monitorItem">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'value'">
                  <a-input v-model:value="record.value" placeholder="实测结果" />
                </template>
                <template v-else-if="column.key === 'conclusion'">
                  <a-select v-model:value="record.conclusion" style="width:100%" placeholder="单项判定">
                    <a-select-option value="待判定">待判定</a-select-option>
                    <a-select-option value="达标">合格</a-select-option>
                    <a-select-option value="超标">不合格</a-select-option>
                  </a-select>
                </template>
              </template>
            </a-table>

            <a-form layout="inline" class="mt-16">
              <a-form-item label="样品综合检验结论">
                <a-select v-model:value="entryForm.conclusion" style="width:200px">
                  <a-select-option value="pending">待确认</a-select-option>
                  <a-select-option value="ok">全部项目-合格</a-select-option>
                  <a-select-option value="ng">存在不合格项</a-select-option>
                  <a-select-option value="abnormal">检测异常，需复检</a-select-option>
                </a-select>
              </a-form-item>
            </a-form>
          </a-card>

          <!-- 检测录入附件 -->
          <a-card size="small" title="检测录入附件" class="mt-16">
            <a-upload
              v-model:file-list="fileList"
              :before-upload="beforeUpload"
              :custom-request="customUpload"
              @preview="handlePreview"
              @remove="handleRemove"
              multiple
              :show-upload-list="{ showPreviewIcon: true, showRemoveIcon: true }"
            >
              <a-button>
                <plus-outlined />
                上传附件
              </a-button>
            </a-upload>
            <div class="att-hint">
              <paper-clip-outlined /> 支持上传检测原始记录、图谱、报告等文件，单个不超过 20MB；点击文件名可下载。
            </div>
          </a-card>

          <a-card size="small" title="样品操作日志" class="mt-16">
            <a-timeline v-if="entryLogs.length">
              <a-timeline-item v-for="(lg, i) in entryLogs" :key="i" :color="i === 0 ? 'blue' : 'gray'">
                {{ lg }}
              </a-timeline-item>
            </a-timeline>
            <a-empty v-else description="暂无操作日志" />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { LeftOutlined, PlusOutlined, PaperClipOutlined } from '@ant-design/icons-vue'
import {
  getDetectionTaskDetail, saveDetectionResults, submitDetection, getSampleParamConfigs
} from '../../../api/ems'
import { getUsers } from '../../../api/system'
import { getDictItemsByCode } from '../../../api/dict'
import { uploadAttachment, downloadAttachment } from '../../../api/attachment'
import { useUserMap } from '../../../composables/useUserMap'

const router = useRouter()
const route = useRoute()
const { realName, buildUserMap } = useUserMap()

const loading = ref(false)
const saving = ref(false)
const taskId = route.params.taskId

const task = ref(null)
const sample = ref(null)
const entryLogs = ref([])
const userOptions = ref([])
const resultRows = ref([])

const entryForm = reactive({ entryBy: undefined, envTemp: '', envHumidity: '', conclusion: 'pending', remark: '' })

// 检测录入附件
const fileList = ref([])
const previewOpen = ref(false)
const previewImage = ref('')
async function customUpload({ file, onSuccess, onError }) {
  try {
    const res = await uploadAttachment(file)
    const data = res.data || res || {}
    const idx = fileList.value.findIndex(f => f.uid === file.uid)
    const target = idx > -1 ? fileList.value[idx] : file
    target.path = data.path
    target.name = data.name || file.name
    target.status = 'done'
    target.response = res
    fileList.value = [...fileList.value]
    onSuccess(res, target)
    message.success(`${file.name} 上传成功`)
  } catch (e) {
    const idx = fileList.value.findIndex(f => f.uid === file.uid)
    if (idx > -1) fileList.value[idx].status = 'error'
    fileList.value = [...fileList.value]
    onError(e)
    message.error(`${file.name} 上传失败`)
  }
}
function beforeUpload(file) {
  const ok = file.size / 1024 / 1024 < 20
  if (!ok) message.error('单个附件不能超过 20MB')
  return ok
}
async function handlePreview(file) {
  if (file.path) {
    try { await downloadAttachment(file.path, file.name) } catch (e) { message.error('下载失败') }
  }
}
function handleRemove(file) {
  const idx = fileList.value.findIndex(f => f.uid === file.uid)
  if (idx > -1) fileList.value.splice(idx, 1)
}
function attachmentsPayload() {
  return JSON.stringify(fileList.value
    .filter(f => f.status === 'done' && f.path)
    .map(f => ({ name: f.name, path: f.path })))
}

const statusColor = (s) => ({ '录入中': 'blue', '已提交': 'orange', '已复核': 'green', '已退回': 'red' }[s] || 'default')

const resultColumns = [
  { title: '检测项目名称', dataIndex: 'monitorItem', key: 'monitorItem' },
  { title: '检测标准', dataIndex: 'method', key: 'method' },
  { title: '合格限值', dataIndex: 'limitValue', key: 'limitValue' },
  { title: '内控限制', dataIndex: 'innerLimit', key: 'innerLimit', width: 180 },
  { title: '实测结果', key: 'value', width: 120 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单项判定', key: 'conclusion', width: 120 }
]

// 样品类型字典映射
const sampleTypeMap = reactive({})
async function loadSampleTypeDict() {
  try {
    const res = await getDictItemsByCode('moni_sample_type')
    const data = res.data || res || []
    data.forEach(d => { sampleTypeMap[d.itemValue] = d.itemText })
  } catch (e) { /* 忽略 */ }
}
function sampleTypeText(key) {
  if (!key) return '-'
  return sampleTypeMap[key] || key
}

async function loadUsers() {
  try {
    const ures = await getUsers()
    const udata = ures.data || ures
    const records = Array.isArray(udata) ? udata : (udata.list || udata.records || [])
    userOptions.value = records.map(u => ({ value: u.username || u.name || String(u.id), label: u.realName || u.name || u.username || String(u.id) }))
  } catch (e) { userOptions.value = [] }
}

// 按 监测类别 + 检测项目 匹配 标准 / 限值 / 单位
async function loadConfigMap(sampleType) {
  const map = {}
  if (!sampleType) return map
  try {
    const res = await getSampleParamConfigs({ type: sampleType })
    const data = res.data || res
    const list = data.records || data.list || data || []
    list.forEach(c => {
      if (c.item) map[c.item] = { method: c.standard || '', limitValue: c.limit || '', innerLimit: c.innerLimit || '', unit: c.unit || '' }
    })
  } catch (e) { /* 忽略 */ }
  return map
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getDetectionTaskDetail(taskId)
    const d = res.data || res
    sample.value = d.sample || null
    const t = d.task || {}
    task.value = t
    entryForm.entryBy = t.entryBy || undefined
    entryForm.envTemp = t.envTemp || ''
    entryForm.envHumidity = t.envHumidity || ''
    entryForm.conclusion = t.conclusion || 'pending'
    entryForm.remark = t.remark || ''
    // 回显已保存附件
    fileList.value = []
    if (t.attachments) {
      try {
        const arr = JSON.parse(t.attachments)
        if (Array.isArray(arr)) {
          fileList.value = arr.map((a, i) => ({ uid: 'att-' + i, name: a.name, path: a.path, status: 'done' }))
        }
      } catch (e) { /* 忽略损坏的附件数据 */ }
    }

    const logs = []
    if (t.createTime) logs.push(t.createTime + ' | 创建检测任务 ' + (t.taskNo || ''))
    if (t.entryTime) logs.push(t.entryTime + ' | 录入员 ' + (t.entryBy || '') + ' 进入检测录入')
    if (t.status) logs.push('当前状态：' + t.status)
    entryLogs.value = logs

    const configMap = await loadConfigMap(sample.value ? sample.value.category : '')
    const raw = (d.results && d.results.length) ? d.results : String(t.monitorItems || '').split(',').map(m => ({ monitorItem: m.trim() }))
    resultRows.value = raw.map(r => {
      const item = r.monitorItem || ''
      const cfg = configMap[item] || {}
      return {
        monitorItem: item,
        method: r.method || cfg.method || '',
        limitValue: r.limitValue || cfg.limitValue || '',
        innerLimit: r.innerLimit || cfg.innerLimit || '',
        value: r.value || '',
        unit: r.unit || cfg.unit || '',
        conclusion: r.conclusion || '待判定',
        remark: r.remark || ''
      }
    })
  } finally { loading.value = false }
}

async function saveResults() {
  if (!resultRows.value.length) { message.warning('暂无检测项目'); return }
  saving.value = true
  try {
    await saveDetectionResults(taskId, {
      entryBy: entryForm.entryBy,
      envTemp: entryForm.envTemp,
      envHumidity: entryForm.envHumidity,
      conclusion: entryForm.conclusion,
      remark: entryForm.remark,
      results: resultRows.value,
      attachments: attachmentsPayload()
    })
    message.success('检验草稿已保存')
  } finally { saving.value = false }
}

async function saveAndSubmit() {
  if (!resultRows.value.length) { message.warning('暂无检测项目'); return }
  const emptyCount = resultRows.value.filter(r => !String(r.value || '').trim()).length
  if (emptyCount > 0) { message.warning(`尚有 ${emptyCount} 条检测项目未录入实测结果`); return }
  saving.value = true
  try {
    await saveDetectionResults(taskId, {
      entryBy: entryForm.entryBy,
      envTemp: entryForm.envTemp,
      envHumidity: entryForm.envHumidity,
      conclusion: entryForm.conclusion,
      remark: entryForm.remark,
      results: resultRows.value,
      attachments: attachmentsPayload()
    })
    await submitDetection(taskId)
    message.success('已保存并提交复核')
    goBack()
  } finally { saving.value = false }
}

function goBack() {
  router.push('/ems/base/data-entry')
}

onMounted(() => {
  buildUserMap()
  loadUsers()
  loadSampleTypeDict()
  loadDetail()
})
</script>

<style scoped>
.mt-16 { margin-top: 16px; }
/* 录入页头部：返回按钮 + 标题 + 右侧操作 */
.entry-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}
.entry-back { padding-left: 0; }
.entry-title { font-size: 16px; font-weight: 600; color: rgba(0, 0, 0, 0.88); }
.entry-subtitle { color: rgba(0, 0, 0, 0.45); font-size: 14px; }
.entry-actions { margin-left: auto; }
.att-hint { margin-top: 8px; color: rgba(0, 0, 0, 0.45); font-size: 12px; }
/* 样品基础信息卡片：超长名称/编号自动换行，避免被截断 */
.sample-info :deep(.ant-descriptions-item-content),
.sample-info :deep(.ant-descriptions-item-label) {
  word-break: break-all;
  white-space: normal;
}
</style>
