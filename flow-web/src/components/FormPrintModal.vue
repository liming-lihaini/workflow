<template>
  <a-modal
    v-model:open="visible"
    :title="modalTitle"
    :width="800"
    :footer="null"
    :body-style="{ padding: '16px', maxHeight: '70vh', overflowY: 'auto' }"
    @cancel="handleClose"
  >
    <div class="print-toolbar">
      <a-button type="primary" @click="handlePrint" :loading="printLoading">
        <template #icon><span style="margin-right:4px">🖨</span></template>
        打印
      </a-button>
      <a-button @click="handleDownloadPdf" :loading="pdfLoading">
        <template #icon><span style="margin-right:4px">📄</span></template>
        下载 PDF
      </a-button>
    </div>

    <a-spin :spinning="loading">
      <div ref="printAreaRef" class="print-area">
        <!-- 基本信息 -->
        <div class="print-header">
          <h2 class="print-title">{{ processInfo.processName || '流程表单' }}</h2>
          <table class="print-info-table">
            <tr>
              <td class="label">流程标识</td>
              <td>{{ processInfo.processKey || '-' }}</td>
              <td class="label">流程名称</td>
              <td>{{ processInfo.processName || '-' }}</td>
            </tr>
            <tr>
              <td class="label">申请人</td>
              <td>{{ processInfo.startUser || '-' }}</td>
              <td class="label">发起时间</td>
              <td>{{ formatDate(processInfo.startTime) }}</td>
            </tr>
          </table>
        </div>

        <!-- 表单内容 -->
        <div class="print-form-body">
          <FormRenderer
            :form-json="formJson"
            :fields="formFields"
            v-model="formValues"
            mode="readonly"
          />
        </div>

        <!-- 审批历史 -->
        <div class="print-history" v-if="taskHistory.length > 0">
          <h3 class="section-title">审批处置历史</h3>
          <table class="print-history-table">
            <thead>
              <tr>
                <th>处理人</th>
                <th>节点</th>
                <th>操作</th>
                <th>时间</th>
                <th>意见</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in taskHistory" :key="idx">
                <td>{{ item.assignee || '-' }}</td>
                <td>{{ item.nodeName || item.nodeId || '-' }}</td>
                <td>{{ getActionLabel(item) }}</td>
                <td>{{ formatDate(item.completeTime || item.createTime) }}</td>
                <td>{{ item.comment || item.taskActionDesc || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { getTaskDetail, getTasksByInstance } from '../api/task'
import { getProcessInstance, getProcessVariables, getProcessDefinitionByKey } from '../api/process'
import { getForm } from '../api/form'
import { formatDate } from '../utils/date'
import FormRenderer from './FormRenderer.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  taskId: { type: [String, Number], default: null },
  title: { type: String, default: '' }
})

const emit = defineEmits(['update:open', 'close'])

const visible = ref(false)
const loading = ref(false)
const printLoading = ref(false)
const pdfLoading = ref(false)
const printAreaRef = ref(null)

const processInfo = ref({})
const formJson = ref(null)
const formFields = ref([])
const formValues = ref({})
const taskHistory = ref([])

const modalTitle = ref('')

const ACTION_MAP = { 0: '操作', 1: '审批同意', 2: '驳回', 3: '转办', 4: '委派', 5: '取消' }
function getActionLabel(item) {
  if (item.taskActionDesc && item.taskActionDesc !== ACTION_MAP[item.taskAction]) return item.taskActionDesc
  return ACTION_MAP[item.taskAction] || '操作'
}

// Watch open prop
import { watch } from 'vue'
watch(() => props.open, async (val) => {
  visible.value = val
  if (val && props.taskId) {
    modalTitle.value = props.title || '表单打印 / PDF下载'
    await loadData()
  }
})

watch(visible, (val) => {
  if (!val) { emit('update:open', false); emit('close') }
})

async function loadData() {
  loading.value = true
  try {
    // 1. Task detail
    const taskRes = await getTaskDetail(props.taskId)
    const task = taskRes.data || taskRes
    const instId = task.processInstanceId
    const processKey = task.processKey

    // 2. Process instance
    try {
      const instRes = await getProcessInstance(instId)
      const inst = instRes.data || instRes
      processInfo.value = {
        processName: inst.processName || processKey,
        processKey: processKey,
        startTime: inst.startTime || inst.createTime || '-',
        startUser: inst.startUser || '-'
      }
    } catch {
      processInfo.value = { processName: processKey, processKey, startTime: '-', startUser: '-' }
    }

    // 3. Process variables
    try {
      const varRes = await getProcessVariables(instId)
      formValues.value = varRes.data || varRes || {}
    } catch { formValues.value = {} }

    // 4. Process definition → form
    try {
      const defRes = await getProcessDefinitionByKey(processKey)
      const def = defRes.data || defRes
      if (def?.processJson) {
        const pj = typeof def.processJson === 'string' ? JSON.parse(def.processJson) : def.processJson
        // Extract formKey from userTask nodes
        let formKey = null
        for (const n of (pj.nodes || [])) {
          if (n.type === 'userTask' && n.properties?.formKey) { formKey = n.properties.formKey; break }
        }
        if (formKey) {
          const formRes = await getForm(formKey)
          const formDef = formRes.data || formRes
          if (formDef.formJson) {
            formJson.value = typeof formDef.formJson === 'string'
              ? JSON.parse(formDef.formJson) : formDef.formJson
          }
        }
      }
    } catch { /* ignore */ }

    // 5. Task history
    try {
      const tasksRes = await getTasksByInstance(instId)
      const tasks = tasksRes.data || tasksRes || []
      taskHistory.value = tasks.filter(t =>
        t.status === 'completed' || t.status === 'COMPLETED' || t.status === 2
      )
    } catch { taskHistory.value = [] }

  } catch (e) {
    message.error('加载表单数据失败: ' + (e.message || ''))
  }
  loading.value = false
}

async function handlePrint() {
  printLoading.value = true
  await nextTick()
  try {
    const el = printAreaRef.value
    if (!el) return

    // Create a new window for printing
    const printWindow = window.open('', '_blank')
    if (!printWindow) { message.warning('请允许弹出窗口以使用打印功能'); return }

    printWindow.document.write(`
      <!DOCTYPE html>
      <html><head>
        <meta charset="utf-8">
        <title>${processInfo.value.processName || '流程表单'}</title>
        <style>
          body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; padding: 20px; color: #333; font-size: 14px; }
          h2 { text-align: center; margin-bottom: 16px; font-size: 18px; }
          h3 { font-size: 15px; margin: 16px 0 8px; border-bottom: 1px solid #eee; padding-bottom: 4px; }
          table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
          td, th { border: 1px solid #ddd; padding: 8px 10px; font-size: 13px; }
          td.label { background: #f5f5f5; font-weight: 500; width: 100px; text-align: right; }
          th { background: #f0f4f9; font-weight: 500; }
          .ant-form-item { margin-bottom: 10px; }
          .ant-form-item-label { font-weight: 500; color: #555; margin-bottom: 2px; }
          .readonly-value { color: #333; }
          .ant-divider-inner-text { font-weight: 500; color: #0052a5; }
          .ant-divider { margin: 10px 0; }
          @media print {
            body { padding: 0; }
            .no-print { display: none; }
          }
        </style>
      </head><body>
        ${el.innerHTML}
      </body></html>
    `)
    printWindow.document.close()
    setTimeout(() => { printWindow.print(); printWindow.close() }, 500)
  } finally {
    printLoading.value = false
  }
}

async function handleDownloadPdf() {
  pdfLoading.value = true
  await nextTick()
  try {
    const el = printAreaRef.value
    if (!el) return

    const canvas = await html2canvas(el, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false
    })

    const imgWidth = 210 // A4 width in mm
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    const pdf = new jsPDF('p', 'mm', 'a4')

    let position = 0
    const pageHeight = 297 // A4 height in mm

    if (imgHeight <= pageHeight) {
      pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, 0, imgWidth, imgHeight)
    } else {
      // Multi-page
      while (position < imgHeight) {
        pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, -position, imgWidth, imgHeight)
        position += pageHeight
        if (position < imgHeight) pdf.addPage()
      }
    }

    const fileName = `${processInfo.value.processName || '流程表单'}_${Date.now()}.pdf`
    pdf.save(fileName)
    message.success('PDF 下载成功')
  } catch (e) {
    message.error('PDF 生成失败: ' + (e.message || ''))
  } finally {
    pdfLoading.value = false
  }
}

function handleClose() {
  visible.value = false
  processInfo.value = {}
  formJson.value = null
  formFields.value = []
  formValues.value = {}
  taskHistory.value = []
}
</script>

<style scoped>
.print-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8e8e8;
}
.print-area {
  background: #fff;
}
.print-header {
  margin-bottom: 16px;
}
.print-title {
  text-align: center;
  font-size: 18px;
  margin-bottom: 12px;
  color: #1d2129;
}
.print-info-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 16px;
}
.print-info-table td {
  border: 1px solid #e8edf3;
  padding: 8px 10px;
  font-size: 13px;
}
.print-info-table td.label {
  width: 100px;
  background: #f7f9fc;
  text-align: right;
  font-weight: 500;
  color: #444;
}
.print-form-body {
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #e1e6ef;
  margin-bottom: 16px;
  min-height: 80px;
}
.section-title {
  font-size: 15px;
  margin: 16px 0 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid #eee;
}
.print-history-table {
  width: 100%;
  border-collapse: collapse;
}
.print-history-table th {
  background: #f0f4f9;
  font-weight: 500;
  padding: 8px 10px;
  border: 1px solid #ddd;
  font-size: 13px;
}
.print-history-table td {
  padding: 8px 10px;
  border: 1px solid #ddd;
  font-size: 13px;
}
</style>
