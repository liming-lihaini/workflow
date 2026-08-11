<template>
  <div class="page-container">
    <a-card :bordered="false">
      <template #title>
        <a-space>
          <a-button size="small" @click="$router.push('/ems/quality/plan')">← 返回</a-button>
          <span>质控计划详情</span>
        </a-space>
      </template>
      <a-descriptions :column="3" bordered size="small">
        <a-descriptions-item label="计划号">{{ plan.planNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="计划名称">{{ plan.title || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(plan.status)">{{ plan.status || '-' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="年度">{{ plan.year || '-' }}</a-descriptions-item>
        <a-descriptions-item label="季度">{{ plan.quarter || '-' }}</a-descriptions-item>
        <a-descriptions-item label="类型">{{ plan.type || '-' }}</a-descriptions-item>
        <a-descriptions-item label="责任人">{{ plan.responsibleId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="任务进度">{{ plan.taskDone ?? 0 }}/{{ plan.taskTotal ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="审批人">{{ plan.approvedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建人">{{ plan.createdName || plan.createdBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ plan.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ plan.updateTime || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card title="质控活动清单" :bordered="false" style="margin-top: 16px">
      <a-table
        :columns="actCols"
        :data-source="acts"
        :loading="actLoading"
        :pagination="{
          current: actPg.current,
          pageSize: actPg.pageSize,
          total: actPg.total,
          size: 'small',
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => { actPg.current = p; loadActs() }
        }"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'taskNo'">
            <a @click="$router.push(`/ems/quality/activity-detail/${record.id}`)" style="font-family: monospace">{{ record.taskNo || '-' }}</a>
          </template>
          <template v-else-if="column.key === 'operator'">
            {{ record.operatorName || record.operatorId || '-' }}
          </template>
          <template v-else-if="column.key === 'taskStatus'">
            <a-tag :color="taskStatusColor(record.taskStatus)">{{ record.taskStatus || '-' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'description'">
            <a-tooltip placement="topLeft" overlay-class-name="desc-tip-overlay">
              <template #title>
                <div class="desc-tip-content">{{ plainDesc(record.description) }}</div>
              </template>
              <span class="desc-cell">{{ plainDesc(record.description) || '-' }}</span>
            </a-tooltip>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card title="处置历史" :bordered="false" style="margin-top: 16px">
      <a-timeline v-if="history.length">
        <a-timeline-item v-for="h in history" :key="h.id" :color="actionColor(h.action)">
          <div class="h-line">
            <span class="h-time">{{ h.createTime || '-' }}</span>
            <a-tag :color="actionColor(h.action)">{{ h.action }}</a-tag>
            <span class="h-op">{{ h.operatorName || h.operatorId || '系统' }}</span>
          </div>
          <div class="h-content">{{ h.content || '-' }}</div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无处置历史" />
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getQcPlanDetail, getQcActivities, getQcHistory } from '../../../api/ems'

const route = useRoute()
const id = route.params.id
const plan = ref({})
const history = ref([])

// 质控活动清单
const acts = ref([])
const actLoading = ref(false)
const actPg = reactive({ current: 1, pageSize: 10, total: 0 })
const actCols = [
  { title: '任务编号', key: 'taskNo', width: 140 },
  { title: '活动类型', dataIndex: 'qcType', key: 'qcType', width: 100 },
  { title: '检测项目', dataIndex: 'item', key: 'item' },
  { title: '活动执行人', key: 'operator', width: 110 },
  { title: '任务状态', key: 'taskStatus', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110 },
  { title: '活动描述', key: 'description', ellipsis: { showTitle: false } }
]
const taskStatusColor = (s) => ({ '未开始': 'default', '进行中': 'blue', '已完成': 'green', '已取消': 'red' }[s] || 'default')
// 活动描述为富文本 HTML：剥离标签后用于单元格截断展示与悬浮提示
const plainDesc = (html) => (html || '').replace(/<[^>]+>/g, ' ').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim()
async function loadActs() {
  actLoading.value = true
  try {
    const res = await getQcActivities({ planId: id, page: actPg.current, size: actPg.pageSize })
    const p = res.data || res
    acts.value = p.records || p.list || []
    actPg.total = p.total || acts.value.length
  } catch { /* 加载失败保持空态 */ } finally { actLoading.value = false }
}

const statusColor = (s) => ({ '草稿': 'default', '审批中': 'orange', '执行中': 'blue', '已完成': 'green' }[s] || 'default')
const actionColor = (a) => ({ '新建': 'green', '编辑': 'blue', '状态变更': 'orange', '删除': 'red' }[a] || 'blue')

onMounted(async () => {
  loadActs()
  try {
    const res = await getQcPlanDetail(id)
    plan.value = res.data || res || {}
  } catch { /* 详情加载失败保持空态 */ }
  try {
    const res = await getQcHistory({ bizType: 'plan', bizId: id })
    const d = res.data || res
    history.value = Array.isArray(d) ? d : (d.list || d.records || [])
  } catch { /* 历史加载失败保持空态 */ }
})
</script>

<style scoped>
.desc-cell {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.h-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.h-time {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.h-op {
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
}
.h-content {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.85);
}
</style>

<style>
/* 活动描述悬浮提示：限制宽度并支持换行（浮层 teleport 到 body，需非 scoped） */
.desc-tip-overlay {
  max-width: 420px;
}
.desc-tip-overlay .desc-tip-content {
  white-space: normal;
  word-break: break-all;
}
</style>
