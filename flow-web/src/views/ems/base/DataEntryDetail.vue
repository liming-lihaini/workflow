<template>
  <div class="page-container">
    <div class="entry-header">
      <a-button type="link" class="entry-back" @click="goBack">
        <left-outlined />
        <span>返回</span>
      </a-button>
      <span class="entry-title">检测任务详情</span>
      <span class="entry-subtitle">{{ detail?.task?.barcode || '' }}</span>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16" v-if="detail">
        <!-- 左侧：任务基础信息 -->
        <a-col :span="9">
          <a-card size="small" title="任务基础信息" class="detail-desc">
            <a-descriptions bordered :column="1" size="small">
              <a-descriptions-item label="任务号">{{ detail.task.taskNo || '-' }}</a-descriptions-item>
              <a-descriptions-item label="样品条码">{{ detail.task.barcode || '-' }}</a-descriptions-item>
              <a-descriptions-item label="样品名称">{{ detail.task.sampleName || '-' }}</a-descriptions-item>
              <a-descriptions-item label="任务状态">
                <a-tag :color="statusColor(detail.task.status)">{{ detail.task.status || '-' }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="检测责任人">{{ realName(detail.task.entryBy) }}</a-descriptions-item>
              <a-descriptions-item label="实验室环境">
                温度：{{ detail.task.envTemp || '-' }}<span v-if="detail.task.envTemp"> ℃</span>
                ／ 湿度：{{ detail.task.envHumidity || '-' }}<span v-if="detail.task.envHumidity"> %RH</span>
              </a-descriptions-item>
              <a-descriptions-item label="综合结论">{{ conclusionText(detail.task.conclusion) }}</a-descriptions-item>
            </a-descriptions>
          </a-card>

          <!-- 复核信息面板 -->
          <a-card size="small" title="复核信息" class="mt-16">
            <a-descriptions bordered :column="1" size="small" v-if="reviewInfo">
              <a-descriptions-item label="复核人">{{ reviewInfo.reviewer }}</a-descriptions-item>
              <a-descriptions-item label="复核时间">{{ reviewInfo.time }}</a-descriptions-item>
              <a-descriptions-item label="复核意见">{{ reviewInfo.opinion || '暂无复核意见' }}</a-descriptions-item>
            </a-descriptions>
            <a-empty v-else description="暂无复核信息" />
          </a-card>

          <a-card size="small" title="整体检验备注" class="mt-16">
            <div class="remark-box">{{ detail.task.remark || '暂无备注' }}</div>
          </a-card>
        </a-col>

        <!-- 右侧：检测结果 & 复核记录 -->
        <a-col :span="15">
          <a-card size="small" title="检测结果">
            <a-table :columns="resultColumns" :data-source="detail.results" :pagination="false" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'conclusion'">
                  <a-tag :color="record.conclusion === '超标' ? 'red' : (record.conclusion === '达标' ? 'green' : 'default')">
                    {{ record.conclusion || '未判定' }}
                  </a-tag>
                </template>
              </template>
            </a-table>
          </a-card>

          <a-card size="small" title="复核记录" class="mt-16" v-if="detail.reviews && detail.reviews.length">
            <a-timeline>
              <a-timeline-item v-for="r in detail.reviews" :key="r.id" :color="r.decision === '通过' ? 'green' : 'red'">
                {{ r.reviewer }} - {{ r.decision }} - {{ r.opinion }}（{{ r.createTime }}）
              </a-timeline-item>
            </a-timeline>
          </a-card>

          <a-card size="small" title="检测录入附件" class="mt-16">
            <ul class="att-list" v-if="taskAttachments.length">
              <li v-for="(a, i) in taskAttachments" :key="i">
                <a @click="downloadAtt(a)"><paper-clip-outlined /> {{ a.name }}</a>
              </li>
            </ul>
            <a-empty v-else description="暂无附件" />
          </a-card>

          <a-card size="small" title="操作记录" class="mt-16" v-if="detail.operations && detail.operations.length">
            <a-timeline>
              <a-timeline-item
                v-for="(op, idx) in detail.operations"
                :key="idx"
                :color="op.action.includes('复核') ? (op.action.includes('退回') ? 'red' : 'green') : 'blue'"
              >
                <div>
                  <span class="op-action">{{ op.action }}</span>
                  <span class="op-meta">操作人：{{ op.operator || '-' }}</span>
                </div>
                <div class="op-detail">{{ op.detail || '' }}</div>
                <div class="op-time">{{ op.time || '' }}</div>
              </a-timeline-item>
            </a-timeline>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { LeftOutlined, PaperClipOutlined } from '@ant-design/icons-vue'
import { getDetectionTaskDetail } from '../../../api/ems'
import { downloadAttachment } from '../../../api/attachment'
import { useUserMap } from '../../../composables/useUserMap'

const router = useRouter()
const route = useRoute()
const { realName, buildUserMap } = useUserMap()

const loading = ref(false)
const taskId = route.params.taskId
const detail = ref(null)

const statusColor = (s) => ({ '录入中': 'blue', '已提交': 'orange', '已复核': 'green', '已退回': 'red' }[s] || 'default')

const conclusionMap = {
  pending: '待确认', ok: '全部项目-合格', ng: '存在不合格项', abnormal: '检测异常，需复检'
}
const conclusionText = (c) => (c ? (conclusionMap[c] || c) : '-')

// 检测录入附件列表
const taskAttachments = computed(() => {
  const raw = detail.value?.task?.attachments
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch (e) { return [] }
})
function downloadAtt(a) {
  if (a && a.path) downloadAttachment(a.path, a.name).catch(() => message.error('下载失败'))
}

// 复核信息面板：复核人、复核时间、复核意见
const reviewInfo = computed(() => {
  const reviews = detail.value?.reviews || []
  if (!detail.value?.task || (!detail.value.task.reviewBy && reviews.length === 0)) return null
  const latest = reviews.length ? reviews[reviews.length - 1] : null
  return {
    reviewer: realName(detail.value.task.reviewBy) || (latest ? realName(latest.reviewer) : '-'),
    time: (latest && latest.createTime) ? latest.createTime : '-',
    opinion: detail.value.task.reviewOpinion || (latest ? latest.opinion : '')
  }
})

const resultColumns = [
  { title: '检测项目名称', dataIndex: 'monitorItem', key: 'monitorItem' },
  { title: '检测标准', dataIndex: 'method', key: 'method' },
  { title: '合格限值', dataIndex: 'limitValue', key: 'limitValue' },
  { title: '内控限值', dataIndex: 'innerLimit', key: 'innerLimit', width: 120 },
  { title: '实测结果', dataIndex: 'value', key: 'value', width: 120 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单项判定', key: 'conclusion', width: 120 }
]

async function loadDetail() {
  loading.value = true
  try {
    const res = await getDetectionTaskDetail(taskId)
    detail.value = res.data || res
  } catch (e) {
    message.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/ems/base/data-entry')
}

onMounted(() => {
  buildUserMap()
  loadDetail()
})
</script>

<style scoped>
.mt-16 { margin-top: 16px; }
/* 详情头部 */
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
.remark-box { white-space: pre-wrap; word-break: break-all; color: rgba(0, 0, 0, 0.85); }
/* descriptions label 固定宽度 100px */
:deep(.ant-descriptions-item-label) {
  width: 100px;
  min-width: 100px;
  max-width: 100px;
  white-space: nowrap;
  text-align: right;
}
.op-action { font-weight: 600; color: rgba(0, 0, 0, 0.88); margin-right: 12px; }
.op-meta { color: rgba(0, 0, 0, 0.65); font-size: 13px; }
.op-detail { color: rgba(0, 0, 0, 0.65); font-size: 13px; margin-top: 2px; }
.op-time { color: rgba(0, 0, 0, 0.45); font-size: 12px; margin-top: 2px; }
.att-list { margin: 0; padding-left: 18px; }
.att-list li { margin-bottom: 6px; }
</style>
