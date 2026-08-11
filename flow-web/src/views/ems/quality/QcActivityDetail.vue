<template>
  <div class="page-container">
    <a-card :bordered="false">
      <template #title>
        <a-space>
          <a-button size="small" @click="$router.push('/ems/quality/plan')">← 返回</a-button>
          <span>质控活动详情</span>
        </a-space>
      </template>
      <a-descriptions :column="3" bordered size="small">
        <a-descriptions-item label="任务编号">
          <span style="font-family: monospace">{{ act.taskNo || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="活动类型">{{ act.qcType || '-' }}</a-descriptions-item>
        <a-descriptions-item label="检测项目">{{ act.item || '-' }}</a-descriptions-item>
        <a-descriptions-item label="任务状态">
          <!-- 仅活动执行人本人可修改任务状态 -->
          <a-select
            v-if="canEditStatus"
            :value="act.taskStatus"
            :options="taskStatusOptions"
            style="width: 130px"
            size="small"
            placeholder="修改状态"
            :loading="statusSaving"
            @change="changeStatus"
          />
          <a-tag v-else :color="taskStatusColor(act.taskStatus)">{{ act.taskStatus || '-' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="活动执行人">{{ act.operatorName || act.operatorId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="所属计划">
          <a v-if="act.planId" @click="$router.push(`/ems/quality/plan-detail/${act.planId}`)">{{ planTitle || act.planId }}</a>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="检测结果">{{ act.result || '-' }}</a-descriptions-item>
        <a-descriptions-item label="开始日期">{{ act.startDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结束日期">{{ act.endDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建人">{{ act.createdName || act.createdBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ act.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="活动描述" :span="3">
          <div v-if="act.description" class="rich-content" v-html="act.description"></div>
          <span v-else>-</span>
        </a-descriptions-item>
      </a-descriptions>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getQcActivityDetail, getQcPlanDetail, getQcHistory, saveQcActivity, getDictItems } from '../../../api/ems'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const route = useRoute()
const id = route.params.id
const act = ref({})
const planTitle = ref('')
const history = ref([])

const taskStatusColor = (s) => ({ '未开始': 'default', '进行中': 'blue', '已完成': 'green', '已取消': 'red' }[s] || 'default')
const actionColor = (a) => ({ '新建': 'green', '编辑': 'blue', '状态变更': 'orange', '删除': 'red' }[a] || 'blue')

// 任务状态修改：仅活动执行人本人可操作，且已完成的任务不可修改
const taskStatusOptions = ref([])
const statusSaving = ref(false)
const canEditStatus = computed(() =>
  act.value.operatorId && act.value.operatorId === userStore.username && act.value.taskStatus !== '已完成')
const opParams = () => ({ opBy: userStore.username || '', opName: userStore.realName || '' })
async function loadTaskStatusOptions() {
  try {
    const res = await getDictItems('moni_qc_task_status')
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data?.records || [])
    taskStatusOptions.value = list.map((i) => ({ value: i.itemText, label: i.itemText }))
  } catch { /* 字典加载失败保持空态 */ }
}
async function changeStatus(val) {
  statusSaving.value = true
  try {
    await saveQcActivity({ ...act.value, taskStatus: val }, opParams())
    message.success('任务状态已更新')
    await loadAll()
  } catch (e) {
    message.error(e.response?.data?.message || '任务状态更新失败')
  } finally {
    statusSaving.value = false
  }
}

async function loadAll() {
  try {
    const res = await getQcActivityDetail(id)
    act.value = res.data || res || {}
    if (act.value.planId) {
      getQcPlanDetail(act.value.planId).then((r) => { planTitle.value = (r.data || r)?.title || '' }).catch(() => {})
    }
  } catch { /* 详情加载失败保持空态 */ }
  try {
    const res = await getQcHistory({ bizType: 'activity', bizId: id })
    const d = res.data || res
    history.value = Array.isArray(d) ? d : (d.list || d.records || [])
  } catch { /* 历史加载失败保持空态 */ }
}

onMounted(() => {
  loadTaskStatusOptions()
  loadAll()
})
</script>

<style scoped>
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
.rich-content :deep(img) {
  max-width: 100%;
}
</style>
