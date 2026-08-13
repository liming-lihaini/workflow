<template>
  <div class="report-view">
    <div class="toolbar no-print">
      <a-space>
        <a-button @click="router.back()"><left-outlined /> 返回</a-button>
        <a-button type="primary" :loading="pdfLoading" @click="handleDownloadPdf">
          <download-outlined /> PDF 下载
        </a-button>
      </a-space>
    </div>

    <div v-if="view" ref="paperRef" class="paper">
      <h1 class="report-title">监 测 报 告</h1>
      <div class="report-subtitle">
        <span>报告编号：{{ report.reportNo }}</span>
        <span style="margin-left: 24px">CMA 资质认定证书号：{{ report.cmaCertNo || '—' }}</span>
      </div>
      <div class="heavy-divider"></div>

      <table class="info-table">
        <tbody>
          <tr>
            <td class="label">委托单位</td>
            <td class="value">{{ view.clientName || '—' }}</td>
            <td class="label">监测类别</td>
            <td class="value">{{ categoryText }}</td>
          </tr>
          <tr>
            <td class="label">采样日期</td>
            <td class="value">{{ view.sampleDate || '—' }}</td>
            <td class="label">分析日期</td>
            <td class="value">{{ view.analysisDate || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <table class="result-table">
        <thead>
          <tr>
            <th style="width: 26%">点位</th>
            <th style="width: 18%">项目</th>
            <th style="width: 22%">结果{{ commonUnit ? ' ' + commonUnit : '' }}</th>
            <th style="width: 16%">限值</th>
            <th style="width: 18%">判定</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in view.rows" :key="i">
            <td>{{ row.point || '—' }}</td>
            <td>{{ row.item }}</td>
            <td>
              <span v-if="row.conclusion === '超标'" class="exceed">{{ row.value }} ▲</span>
              <span v-else>{{ row.value }}</span>
            </td>
            <td>{{ row.limit || '—' }}</td>
            <td>
              <span class="tag" :class="row.conclusion === '超标' ? 'tag-ng' : 'tag-ok'">
                {{ row.conclusion === '超标' ? '超标' : '达标' }}
              </span>
            </td>
          </tr>
          <tr v-if="!view.rows || !view.rows.length">
            <td colspan="5" style="text-align: center; color: #8c8c8c">暂无检测明细</td>
          </tr>
        </tbody>
      </table>

      <div class="sign-row">
        <span>编制：{{ realName(report.generator) }} <span class="sign-icon">✍</span>(已签) {{ fmtTime(report.createTime) }}</span>
        <span>审核：{{ realName(report.reviewer) }} <span class="sign-icon">✍</span>({{ published ? '已签' : '待签' }})</span>
        <span>批准：{{ realName(report.approver) }} <span class="sign-icon">✍</span>({{ published ? '已签' : '待签' }})</span>
      </div>
    </div>
    <a-empty v-else-if="!loading" description="报告不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LeftOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { getReportView } from '../../../api/ems'
import { getDictItemsByCode } from '../../../api/dict'
import { useUserMap } from '../../../composables/useUserMap'

const route = useRoute()
const router = useRouter()
const { realName, buildUserMap } = useUserMap()

const loading = ref(true)
const pdfLoading = ref(false)
const view = ref(null)
const paperRef = ref(null)
const categoryMap = ref({})

const report = computed(() => (view.value && view.value.report) || {})
const published = computed(() => report.value.status === '已发布')

const categoryText = computed(() => {
  const c = view.value && view.value.category
  if (!c) return '—'
  return categoryMap.value[c] || c
})

const commonUnit = computed(() => {
  const rows = (view.value && view.value.rows) || []
  if (!rows.length) return ''
  const u = rows[0].unit
  if (!u) return ''
  return rows.every(r => r.unit === u) ? u : ''
})

function fmtTime(t) {
  if (!t) return ''
  const s = String(t).replace('T', ' ')
  // yyyy-MM-dd HH:mm:ss -> MM-dd HH:mm
  const m = s.match(/^\d{4}-(\d{2}-\d{2})[ T](\d{2}:\d{2})/)
  return m ? `${m[1]} ${m[2]}` : s
}

async function load() {
  loading.value = true
  try {
    const res = await getReportView(route.params.id)
    view.value = res.data || null
  } catch (e) {
    message.error('加载报告失败：' + (e.response?.data?.message || e.message))
    view.value = null
  } finally {
    loading.value = false
  }
}

async function handleDownloadPdf() {
  pdfLoading.value = true
  await nextTick()
  try {
    const el = paperRef.value
    if (!el) return
    const canvas = await html2canvas(el, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#f5f7fa',
      logging: false
    })
    const imgWidth = 210 // A4 宽 mm
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    const pdf = new jsPDF('p', 'mm', 'a4')
    let position = 0
    const pageHeight = 297
    if (imgHeight <= pageHeight) {
      pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, 0, imgWidth, imgHeight)
    } else {
      while (position < imgHeight) {
        pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', 0, -position, imgWidth, imgHeight)
        position += pageHeight
        if (position < imgHeight) pdf.addPage()
      }
    }
    pdf.save(`${report.value.reportNo || '监测报告'}.pdf`)
    message.success('PDF 下载成功')
  } catch (e) {
    message.error('PDF 生成失败: ' + (e.message || ''))
  } finally {
    pdfLoading.value = false
  }
}

onMounted(async () => {
  buildUserMap()
  try {
    const res = await getDictItemsByCode('moni_sample_type')
    const items = res.data || res || []
    const map = {}
    ;(Array.isArray(items) ? items : []).forEach(i => { map[i.itemValue || i.value] = i.itemName || i.label })
    categoryMap.value = map
  } catch { /* 字典加载失败降级为原值 */ }
  await load()
})
</script>

<style scoped>
.report-view { padding: 8px; }
.toolbar { margin-bottom: 12px; }
.paper {
  max-width: 860px;
  margin: 0 auto;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 32px 40px 24px;
}
.report-title {
  text-align: center;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 8px;
  color: #1f2d3d;
  margin: 0 0 14px;
}
.report-subtitle {
  text-align: center;
  color: #5a6b7b;
  font-size: 13px;
  margin-bottom: 14px;
}
.heavy-divider {
  height: 3px;
  background: #1f2d3d;
  margin-bottom: 20px;
}
.info-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  margin-bottom: 16px;
}
.info-table td {
  border: 1px solid #e3e8ee;
  padding: 10px 12px;
  font-size: 13px;
}
.info-table td.label {
  width: 12%;
  color: #5a6b7b;
  background: #fafbfc;
}
.info-table td.value {
  width: 38%;
  color: #1f2d3d;
  font-weight: 500;
}
.result-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  margin-bottom: 18px;
}
.result-table th,
.result-table td {
  border: 1px solid #e3e8ee;
  padding: 10px 12px;
  font-size: 13px;
  text-align: left;
}
.result-table th {
  background: #eef2f6;
  color: #1f2d3d;
  font-weight: 600;
}
.exceed { color: #cf1322; font-weight: 700; }
.tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.tag-ok { background: #e6f7ec; color: #389e0d; }
.tag-ng { background: #fde8ea; color: #cf1322; }
.sign-row {
  display: flex;
  justify-content: space-between;
  color: #5a6b7b;
  font-size: 13px;
  padding: 4px 2px;
}
.sign-icon { color: #d48806; }
</style>
