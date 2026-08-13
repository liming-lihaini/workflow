<template>
  <div class="page-container">
    <a-card title="质控计划" :bordered="false">
      <template #extra>
        <a-radio-group v-model:value="viewMode" button-style="solid" size="small" @change="onViewChange">
          <a-radio-button value="plan">计划视图</a-radio-button>
          <a-radio-button value="task">任务视图</a-radio-button>
        </a-radio-group>
      </template>

      <template v-if="viewMode === 'plan'">
        <a-space style="margin-bottom:16px">
          <a-button v-if="hasPerm('ems:quality:create')" type="primary" @click="openPlan()">+ 新建计划</a-button>
          <a-input-search v-model:value="kw" placeholder="名称搜索" style="width:200px" @search="load" allow-clear />
        </a-space>
        <a-table
          :columns="planCols"
          :data-source="plans"
          row-key="id"
          :pagination="pg"
          :loading="loading"
          :expanded-row-keys="expandedKeys"
          :scroll="{ y: 480 }"
          @expand="onPlanExpand"
        >
        <template #expandedRowRender="{ record }">
          <div class="act-panel">
            <div class="act-toolbar">
              <span class="act-toolbar-title">计划任务</span>
              <a-input-search
                v-model:value="actState[record.id].keyword"
                placeholder="活动类型/检测项目/执行人/任务状态"
                style="width:260px"
                size="small"
                allow-clear
                @search="loadTasks(record.id)"
              />
            </div>
            <a-table
              :columns="actCols"
              :data-source="actState[record.id].list"
              :loading="actState[record.id].loading"
              :pagination="{
                current: actState[record.id].page,
                pageSize: actState[record.id].size,
                total: actState[record.id].total,
                size: 'small',
                showTotal: (t) => `共 ${t} 条`,
                onChange: (p) => { actState[record.id].page = p; loadTasks(record.id) }
              }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record: act }">
                <template v-if="column.key === 'taskNo'">
                  <a @click="$router.push(`/ems/quality/activity-detail/${act.id}`)" style="font-family: monospace">{{ act.taskNo || '-' }}</a>
                </template>
                <template v-else-if="column.key === 'operator'">
                  {{ act.operatorName || act.operatorId || '-' }}
                </template>
                <template v-else-if="column.key === 'taskStatus'">
                  <a-tag :color="taskStatusColor(act.taskStatus)">{{ act.taskStatus || '-' }}</a-tag>
                </template>
                <template v-else-if="column.key === 'op'">
                  <a-space :size="8" wrap>
                    <a v-if="hasPerm('ems:quality:update')" @click="openActivity(record, act)">编辑</a>
                    <a v-if="hasPerm('ems:quality:update')" class="danger-link" @click="removeActivity(record.id, act)">删除</a>
                    <!-- 仅活动执行人本人可修改活动状态，已完成的任务不可修改 -->
                    <a-select
                      v-if="act.operatorId && act.operatorId === userStore.username && act.taskStatus !== '已完成' && hasPerm('ems:quality:update')"
                      :value="act.taskStatus"
                      :options="taskStatusOptions"
                      size="small"
                      style="width:110px"
                      placeholder="修改状态"
                      @change="(v) => changeTaskStatus(record.id, act, v)"
                    />
                  </a-space>
                </template>
              </template>
            </a-table>
          </div>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'planNo'">
            <a @click="$router.push(`/ems/quality/plan-detail/${record.id}`)" style="font-family: monospace">{{ record.planNo }}</a>
          </template>
          <template v-else-if="column.key === 'responsibleId'">
            {{ record.responsibleName || record.responsibleId || '-' }}
          </template>
          <template v-else-if="column.key === 'progress'">
            <span class="progress-text">{{ record.taskDone || 0 }}/{{ record.taskTotal || 0 }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a v-if="hasPerm('ems:quality:update')" @click="openPlan(record)">编辑</a>
              <a v-if="record.status==='草稿' && hasPerm('ems:quality:update')" @click="submit(record)">提交</a>
              <a v-if="record.status==='审批中' && hasPerm('ems:quality:approve')" @click="approve(record)">审批通过</a>
              <a v-if="record.status==='执行中' && hasPerm('ems:quality:approve')" @click="complete(record)">完成</a>
              <a v-if="hasPerm('ems:quality:update')" @click="openActivity(record)">添加活动</a>
              <a v-if="hasPerm('ems:quality:delete')" class="danger-link" @click="removePlan(record)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
      </template>

      <!-- 任务视图：全部质控活动任务，支持条件检索与分页 -->
      <template v-else>
      <a-form layout="inline" style="margin-bottom:16px; row-gap:12px">
        <a-form-item label="名称">
          <a-input v-model:value="tvState.keyword" placeholder="任务编号/活动类型/检测项目" style="width:220px" allow-clear @press-enter="searchTaskView" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="tvState.taskStatus" :options="taskStatusOptions" placeholder="全部" style="width:130px" allow-clear />
        </a-form-item>
        <a-form-item label="时间范围">
          <a-range-picker v-model:value="tvState.dateRange" value-format="YYYY-MM-DD" style="width:240px" />
        </a-form-item>
        <a-form-item label="执行人">
          <a-select
            v-model:value="tvState.operatorId"
            :options="userOptions"
            :loading="userLoading"
            show-search
            allow-clear
            :filter-option="false"
            placeholder="输入姓名远程搜索"
            style="width:180px"
            @search="onUserSearch"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="searchTaskView">查询</a-button>
            <a-button @click="resetTaskView">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
      <a-table
        :columns="tvCols"
        :data-source="tvState.list"
        :loading="tvState.loading"
        :pagination="{
          current: tvState.page,
          pageSize: tvState.size,
          total: tvState.total,
          showTotal: (t) => `共 ${t} 条`,
          showSizeChanger: true,
          onChange: (p, s) => { tvState.page = p; tvState.size = s; loadTaskView() }
        }"
        row-key="id"
        size="small"
        :scroll="{ y: 480 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'taskNo'">
            <a @click="$router.push(`/ems/quality/activity-detail/${record.id}`)" style="font-family: monospace">{{ record.taskNo || '-' }}</a>
          </template>
          <template v-else-if="column.key === 'plan'">
            <a v-if="record.planId" @click="$router.push(`/ems/quality/plan-detail/${record.planId}`)">{{ record.planTitle || record.planId }}</a>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'operator'">
            {{ record.operatorName || record.operatorId || '-' }}
          </template>
          <template v-else-if="column.key === 'taskStatus'">
            <a-tag :color="taskStatusColor(record.taskStatus)">{{ record.taskStatus || '-' }}</a-tag>
          </template>
        </template>
      </a-table>
      </template>
    </a-card>

    <a-modal v-model:open="planVisible" :title="planForm.id?'编辑计划':'新建计划'" width="720px" @ok="savePlan">
      <a-form ref="planFormRef" :model="planForm" :rules="planRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="计划名称" name="title"><a-input v-model:value="planForm.title" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="责任人" name="responsibleId">
              <a-select
                v-model:value="planForm.responsibleId"
                :options="userOptions"
                :loading="userLoading"
                show-search
                allow-clear
                :filter-option="false"
                placeholder="输入姓名远程搜索"
                @search="onUserSearch"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="年度" name="year"><a-input-number v-model:value="planForm.year" :min="2000" style="width:100%" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="季度" name="quarter"><a-input v-model:value="planForm.quarter" placeholder="Q1/Q2/Q3/Q4/专项" /></a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期" name="startDate">
              <a-date-picker v-model:value="planForm.startDate" value-format="YYYY-MM-DD" style="width:100%" @change="() => planForm.endDate && planFormRef?.validateFields(['endDate'])" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="planForm.endDate" value-format="YYYY-MM-DD" style="width:100%" @change="() => planForm.startDate && planFormRef?.validateFields(['startDate'])" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="计划描述" name="description">
          <RichTextEditor v-model:value="planForm.description" placeholder="请输入计划描述（支持完整富文本编辑）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="actVisible" :title="(actForm.id?'编辑质控活动':'添加质控活动') + ' - ' + (curPlan?.title||'')" width="720px" @ok="saveActivity">
      <a-form ref="actFormRef" :model="actForm" :rules="actRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="活动类型" name="qcType">
              <a-select v-model:value="actForm.qcType">
                <a-select-option value="空白">空白</a-select-option>
                <a-select-option value="平行">平行</a-select-option>
                <a-select-option value="加标回收">加标回收</a-select-option>
                <a-select-option value="留样复测">留样复测</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检测项目" name="item">
              <a-select v-model:value="actForm.item" :options="itemOptions" placeholder="数据来源数据字典" show-search option-filter-prop="label" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="活动执行人" name="operatorId">
              <a-select
                v-model:value="actForm.operatorId"
                :options="userOptions"
                :loading="userLoading"
                show-search
                allow-clear
                :filter-option="false"
                placeholder="输入姓名远程搜索"
                @search="onUserSearch"
                @change="onOperatorChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="任务状态" name="taskStatus">
              <a-select
                v-model:value="actForm.taskStatus"
                :options="taskStatusOptions"
                placeholder="数据来源数据字典"
                allow-clear
                :disabled="statusLocked"
              />
              <span v-if="statusLocked" class="status-locked-tip">已完成的任务不允许修改任务状态</span>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期" name="startDate">
              <a-date-picker v-model:value="actForm.startDate" value-format="YYYY-MM-DD" style="width:100%" @change="() => actForm.endDate && actFormRef?.validateFields(['endDate'])" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="actForm.endDate" value-format="YYYY-MM-DD" style="width:100%" @change="() => actForm.startDate && actFormRef?.validateFields(['startDate'])" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="活动描述">
          <RichTextEditor v-model:value="actForm.description" placeholder="请输入活动描述（支持完整富文本编辑）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { saveQcPlan, getQcPlans, submitQcPlan, approveQcPlan, completeQcPlan, deleteQcPlan,
  saveQcActivity, getQcActivities, deleteQcActivity, getDictItems } from '../../../api/ems'
import { getUsersPage } from '../../../api/system'
import { useUserStore } from '../../../stores/user'
import { usePermission } from '../../../composables/usePermission'
import RichTextEditor from '../../../components/RichTextEditor.vue'

const { hasPerm } = usePermission()
const userStore = useUserStore()
// 处置人参数：后端记录处置历史使用
const opParams = () => ({ opBy: userStore.username || '', opName: userStore.realName || '' })

const loading = ref(false), kw = ref('')
const plans = ref([])
const pg = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { pg.current = p; load() } })
const planCols = [
  { title: '计划号', dataIndex: 'planNo', key: 'planNo', width: 140 },
  { title: '名称', dataIndex: 'title', key: 'title' },
  { title: '年度', dataIndex: 'year', key: 'year', width: 80 },
  { title: '季度', dataIndex: 'quarter', key: 'quarter', width: 120 },
  { title: '责任人', dataIndex: 'responsibleId', key: 'responsibleId', width: 120 },
  { title: '任务进度', key: 'progress', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 280 }
]
const statusColor = (s) => ({ '草稿':'default','审批中':'orange','执行中':'blue','已完成':'green' }[s] || 'default')

// ===================== 计划任务（展开行）=====================
const expandedKeys = ref([])
const actState = reactive({})   // { [planId]: { keyword, page, size, total, list, loading } }
const actCols = [
  { title: '任务编号', key: 'taskNo', width: 140 },
  { title: '活动类型', dataIndex: 'qcType', key: 'qcType', width: 100 },
  { title: '检测项目', dataIndex: 'item', key: 'item' },
  { title: '活动执行人', key: 'operator', width: 110 },
  { title: '任务状态', key: 'taskStatus', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110 },
  { title: '操作', key: 'op', width: 240 }
]
const taskStatusColor = (s) => ({ '未开始':'default','进行中':'blue','已完成':'green','已取消':'red' }[s] || 'default')

// ===================== 任务视图：全部质控活动任务，支持条件检索与分页 =====================
const viewMode = ref('plan')
const tvState = reactive({ keyword: '', taskStatus: undefined, dateRange: null, operatorId: undefined, page: 1, size: 10, total: 0, list: [], loading: false })
const tvCols = [
  { title: '任务编号', key: 'taskNo', width: 140 },
  { title: '活动类型', dataIndex: 'qcType', key: 'qcType', width: 100 },
  { title: '检测项目', dataIndex: 'item', key: 'item' },
  { title: '所属计划', key: 'plan' },
  { title: '活动执行人', key: 'operator', width: 110 },
  { title: '任务状态', key: 'taskStatus', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110 }
]
async function loadTaskView() {
  tvState.loading = true
  try {
    const params = { page: tvState.page, size: tvState.size }
    if (tvState.keyword) params.keyword = tvState.keyword.trim()
    if (tvState.taskStatus) params.taskStatus = tvState.taskStatus
    if (tvState.operatorId) params.operatorId = tvState.operatorId
    if (tvState.dateRange && tvState.dateRange.length === 2) {
      params.startDateFrom = tvState.dateRange[0]
      params.startDateTo = tvState.dateRange[1]
    }
    const res = await getQcActivities(params)
    const p = res.data || res
    tvState.list = p.records || p.list || []
    tvState.total = p.total || tvState.list.length
  } catch { /* 单次加载失败保持原状 */ } finally { tvState.loading = false }
}
function searchTaskView() { tvState.page = 1; loadTaskView() }
function resetTaskView() {
  tvState.keyword = ''; tvState.taskStatus = undefined; tvState.dateRange = null; tvState.operatorId = undefined
  searchTaskView()
}
function onViewChange() {
  if (viewMode.value === 'task') loadTaskView()
}

function ensureActState(planId) {
  if (!actState[planId]) {
    actState[planId] = { keyword: '', page: 1, size: 10, total: 0, list: [], loading: false }
  }
  return actState[planId]
}
function onPlanExpand(expanded, record) {
  if (expanded) {
    ensureActState(record.id)
    if (!expandedKeys.value.includes(record.id)) expandedKeys.value.push(record.id)
    loadTasks(record.id)
  } else {
    expandedKeys.value = expandedKeys.value.filter(k => k !== record.id)
  }
}
async function loadTasks(planId) {
  const st = ensureActState(planId)
  st.loading = true
  try {
    const res = await getQcActivities({ planId, keyword: st.keyword, page: st.page, size: st.size })
    const p = res.data || res
    st.list = p.records || p.list || []
    st.total = p.total || st.list.length
  } catch { /* 单次加载失败不影响页面 */ } finally { st.loading = false }
}

const planVisible = ref(false), curPlan = ref(null)
const planForm = reactive({ id: null, title: '', year: new Date().getFullYear(), quarter: 'Q1', type: '年度', responsibleId: '', startDate: null, endDate: null, description: '' })

// 新建/编辑质控计划必填项校验
const planFormRef = ref(null)
const planRules = {
  title: [{ required: true, message: '请输入计划名称' }],
  responsibleId: [{ required: true, message: '请选择责任人' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [
    { required: true, message: '请选择结束日期' },
    { validator: (rule, value) => {
        if (value && planForm.startDate && value < planForm.startDate) {
          return Promise.reject('结束日期必须大于等于开始日期')
        }
        return Promise.resolve()
      } }
  ],
  description: [{ required: true, message: '请输入计划描述' }]
}
function openPlan(r) {
  if (r) Object.assign(planForm, r)
  else Object.keys(planForm).forEach(k => { if (k!=='year') planForm[k]=null }); planForm.year = new Date().getFullYear()
  // 回填责任人下拉选项，保证已选值可回显
  if (planForm.responsibleId && !userOptions.value.some(o => o.value === planForm.responsibleId)) {
    userOptions.value = [{ value: planForm.responsibleId, label: planForm.responsibleId, realName: planForm.responsibleId }, ...userOptions.value]
  }
  if (!userOptions.value.length) fetchUsers('')  // 打开即预载候选人员
  planVisible.value = true
}
async function savePlan() {
  // 必填项校验不通过则阻止提交
  try { await planFormRef.value.validate() } catch { return }
  await saveQcPlan({ ...planForm }, opParams()); message.success('已保存'); planVisible.value = false; load()
}
async function submit(r) { await submitQcPlan(r.id, null, opParams()); message.success('已提交审批'); load() }
async function approve(r) { await approveQcPlan(r.id, null, opParams()); message.success('审批通过，进入执行中'); load() }
async function complete(r) { await completeQcPlan(r.id, opParams()); message.success('已完成'); load() }
function removePlan(r) {
  Modal.confirm({
    title: '删除计划',
    content: `确认删除计划【${r.title}】及其下全部监控活动？删除后不可恢复。`,
    okText: '删除', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      await deleteQcPlan(r.id, opParams())
      message.success('已删除'); load()
    }
  })
}

const actVisible = ref(false)
const actForm = reactive({ id: null, qcType: '空白', item: undefined, result: undefined, passFlag: '合格', operatorId: undefined, operatorName: '', taskStatus: undefined, startDate: null, endDate: null, description: '' })

// 新建/编辑质控活动必填项校验
const actFormRef = ref(null)
// 活动日期校验：结束日期 >= 开始日期；活动日期不能超出所属计划的日期范围
function validateActStartDate(rule, value) {
  const plan = curPlan.value || {}
  if (value && plan.startDate && value < plan.startDate) {
    return Promise.reject(`开始日期不能早于计划开始日期（${plan.startDate}）`)
  }
  return Promise.resolve()
}
function validateActEndDate(rule, value) {
  if (value && actForm.startDate && value < actForm.startDate) {
    return Promise.reject('结束日期必须大于等于开始日期')
  }
  const plan = curPlan.value || {}
  if (value && plan.endDate && value > plan.endDate) {
    return Promise.reject(`结束日期不能晚于计划结束日期（${plan.endDate}）`)
  }
  return Promise.resolve()
}
const actRules = {
  qcType: [{ required: true, message: '请选择活动类型' }],
  item: [{ required: true, message: '请选择检测项目' }],
  operatorId: [{ required: true, message: '请选择活动执行人' }],
  taskStatus: [{ required: true, message: '请选择任务状态' }],
  startDate: [{ required: true, message: '请选择开始日期' }, { validator: validateActStartDate }],
  endDate: [{ required: true, message: '请选择结束日期' }, { validator: validateActEndDate }]
}

// 活动执行人：人员远程选择（/system/users/page，防抖300ms）
const userOptions = ref([])
const userLoading = ref(false)
let userSearchTimer = null
async function fetchUsers(keyword) {
  userLoading.value = true
  try {
    const params = { page: 1, size: keyword ? 10 : 20 }
    if (keyword) params.keyword = keyword
    const res = await getUsersPage(params)
    const d = res.data || res
    const records = Array.isArray(d) ? d : (d.records || d.list || [])
    userOptions.value = records.map(u => ({
      value: u.username || String(u.id),
      label: `${u.realName || u.username} (${u.username || u.id})`,
      realName: u.realName || u.username
    }))
  } catch { userOptions.value = [] } finally { userLoading.value = false }
}
function onUserSearch(keyword) {
  clearTimeout(userSearchTimer)
  userSearchTimer = setTimeout(() => fetchUsers(keyword), 300)
}
function onOperatorChange(val, option) {
  actForm.operatorName = option?.realName || ''
}

// 监控活动字典下拉：检测项目（moni_monitor_factor）/任务状态（moni_qc_task_status）
const itemOptions = ref([])
const taskStatusOptions = ref([])
function loadDictOptions(code, target, valueField = 'itemText') {
  getDictItems(code).then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) target.value = list.map((i) => ({ label: i.itemText, value: i[valueField] }))
  }).catch(() => {})
}
function loadActDicts() {
  loadDictOptions('moni_monitor_factor', itemOptions)
  loadDictOptions('moni_qc_task_status', taskStatusOptions)
}

// 已完成的任务锁定任务状态修改
const statusLocked = ref(false)

function openActivity(plan, act) {
  curPlan.value = plan
  statusLocked.value = !!(act && act.taskStatus === '已完成')
  Object.keys(actForm).forEach(k => actForm[k] = k==='qcType'?'空白':(k==='passFlag'?'合格':null))
  actForm.operatorId = undefined
  actForm.operatorName = ''
  actForm.description = ''
  if (act) {
    // 编辑：回填活动数据
    Object.assign(actForm, {
      id: act.id, qcType: act.qcType, item: act.item, result: act.result, passFlag: act.passFlag,
      operatorId: act.operatorId, operatorName: act.operatorName, taskStatus: act.taskStatus,
      startDate: act.startDate, endDate: act.endDate, description: act.description
    })
    // 回填执行人下拉选项，保证已选值可回显
    if (act.operatorId && !userOptions.value.some(o => o.value === act.operatorId)) {
      userOptions.value = [{ value: act.operatorId, label: `${act.operatorName || act.operatorId} (${act.operatorId})`, realName: act.operatorName }, ...userOptions.value]
    }
  }
  if (!userOptions.value.length) fetchUsers('')  // 打开即预载候选人员
  actVisible.value = true
}
async function saveActivity() {
  // 必填项校验不通过则阻止提交
  try { await actFormRef.value.validate() } catch { return }
  const planId = curPlan.value.id
  await saveQcActivity({ ...actForm, planId }, opParams())
  message.success(actForm.id ? '活动已更新' : '活动已记录'); actVisible.value = false
  // 刷新计划任务进度；若该计划已展开则同步刷新任务表格，未展开则自动展开便于查看
  load()
  if (!expandedKeys.value.includes(planId)) expandedKeys.value.push(planId)
  ensureActState(planId)
  loadTasks(planId)
}
function removeActivity(planId, act) {
  Modal.confirm({
    title: '删除监控活动',
    content: `确认删除监控活动【${act.qcType || ''}-${act.item || ''}】？删除后不可恢复。`,
    okText: '删除', okType: 'danger', cancelText: '取消',
    onOk: async () => {
      await deleteQcActivity(act.id, opParams())
      message.success('已删除')
      loadTasks(planId)
      load() // 同步刷新计划任务进度
    }
  })
}
// 活动执行人本人修改活动状态
async function changeTaskStatus(planId, act, val) {
  try {
    await saveQcActivity({ ...act, taskStatus: val }, opParams())
    message.success('活动状态已更新')
    loadTasks(planId)
    load() // 同步刷新计划任务进度
  } catch { /* 更新失败保持原状 */ }
}

async function load() {
  loading.value = true
  try {
    const res = await getQcPlans({ keyword: kw.value, page: pg.current, size: pg.pageSize })
    const p = res.data || res; plans.value = p.records || p.list || []; pg.total = p.total || plans.value.length
  } finally { loading.value = false }
}
onMounted(() => { load(); loadActDicts() })
</script>

<style scoped>
.status-locked-tip {
  display: block;
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.act-panel {
  background: #fafafa;
  border-radius: 4px;
  padding: 8px 12px;
}
.act-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.act-toolbar-title {
  font-weight: 600;
  color: rgba(0, 0, 0, 0.75);
}
.progress-text {
  font-weight: 600;
}
.danger-link {
  color: #f53f3f;
}
</style>
