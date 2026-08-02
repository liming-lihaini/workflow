<template>
  <div class="page-container">
    <a-card title="质控计划" :bordered="false">
      <a-space style="margin-bottom:16px">
        <a-button type="primary" @click="openPlan()">+ 新建计划</a-button>
        <a-input-search v-model:value="kw" placeholder="名称搜索" style="width:200px" @search="load" allow-clear />
      </a-space>
      <a-table :columns="planCols" :data-source="plans" row-key="id" :pagination="pg" :loading="loading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a v-if="record.status==='草稿'" @click="submit(record)">提交</a>
              <a v-if="record.status==='审批中'" @click="approve(record)">审批通过</a>
              <a v-if="record.status==='执行中'" @click="complete(record)">完成</a>
              <a @click="openActivity(record)">添加活动</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="planVisible" :title="planForm.id?'编辑计划':'新建计划'" @ok="savePlan">
      <a-form :model="planForm" layout="vertical">
        <a-form-item label="计划名称"><a-input v-model:value="planForm.title" /></a-form-item>
        <a-form-item label="年度"><a-input-number v-model:value="planForm.year" :min="2000" style="width:100%" /></a-form-item>
        <a-form-item label="季度"><a-input v-model:value="planForm.quarter" placeholder="Q1/Q2/Q3/Q4/专项" /></a-form-item>
        <a-form-item label="类型"><a-select v-model:value="planForm.type"><a-select-option value="年度">年度</a-select-option><a-select-option value="季度">季度</a-select-option><a-select-option value="专项">专项</a-select-option></a-select></a-form-item>
        <a-form-item label="责任人"><a-input v-model:value="planForm.responsibleId" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="actVisible" :title="'添加监控活动 - ' + (curPlan?.title||'')" @ok="saveActivity">
      <a-form :model="actForm" layout="vertical">
        <a-form-item label="活动类型">
          <a-select v-model:value="actForm.qcType">
            <a-select-option value="空白">空白</a-select-option>
            <a-select-option value="平行">平行</a-select-option>
            <a-select-option value="加标回收">加标回收</a-select-option>
            <a-select-option value="留样复测">留样复测</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="监测项目"><a-input v-model:value="actForm.item" /></a-form-item>
        <a-form-item label="检测结果"><a-input v-model:value="actForm.result" /></a-form-item>
        <a-form-item label="是否合格">
          <a-radio-group v-model:value="actForm.passFlag">
            <a-radio value="合格">合格</a-radio>
            <a-radio value="不合格">不合格</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="操作人"><a-input v-model:value="actForm.operatorId" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { saveQcPlan, getQcPlans, submitQcPlan, approveQcPlan, completeQcPlan,
  saveQcActivity, getQcActivities } from '../../../api/ems'

const loading = ref(false), kw = ref('')
const plans = ref([])
const pg = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { pg.current = p; load() } })
const planCols = [
  { title: '计划号', dataIndex: 'planNo', key: 'planNo' },
  { title: '名称', dataIndex: 'title', key: 'title' },
  { title: '年度', dataIndex: 'year', key: 'year' },
  { title: '季度', dataIndex: 'quarter', key: 'quarter' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '责任人', dataIndex: 'responsibleId', key: 'responsibleId' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]
const statusColor = (s) => ({ '草稿':'default','审批中':'orange','执行中':'blue','已完成':'green' }[s] || 'default')

const planVisible = ref(false), curPlan = ref(null)
const planForm = reactive({ id: null, title: '', year: new Date().getFullYear(), quarter: 'Q1', type: '年度', responsibleId: '' })
function openPlan(r) {
  if (r) Object.assign(planForm, r)
  else Object.keys(planForm).forEach(k => { if (k!=='year') planForm[k]=null }); planForm.year = new Date().getFullYear()
  planVisible.value = true
}
async function savePlan() {
  await saveQcPlan({ ...planForm }); message.success('已保存'); planVisible.value = false; load()
}
async function submit(r) { await submitQcPlan(r.id); message.success('已提交审批'); load() }
async function approve(r) { await approveQcPlan(r.id); message.success('审批通过，进入执行中'); load() }
async function complete(r) { await completeQcPlan(r.id); message.success('已完成'); load() }

const actVisible = ref(false)
const actForm = reactive({ qcType: '空白', item: '', result: '', passFlag: '合格', operatorId: '' })
function openActivity(plan) {
  curPlan.value = plan
  Object.keys(actForm).forEach(k => actForm[k] = k==='qcType'?'空白':(k==='passFlag'?'合格':''))
  actVisible.value = true
}
async function saveActivity() {
  await saveQcActivity({ ...actForm, planId: curPlan.value.id })
  message.success('活动已记录'); actVisible.value = false
}

async function load() {
  loading.value = true
  try {
    const res = await getQcPlans({ keyword: kw.value, page: pg.current, size: pg.pageSize })
    const p = res.data || res; plans.value = p.records || p.list || []; pg.total = p.total || plans.value.length
  } finally { loading.value = false }
}
onMounted(load)
</script>
