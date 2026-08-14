<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <!-- 台账统计卡片（PRD-02 §5.1） -->
      <div class="stat-row">
        <div class="stat-card stat-blue">
          <div class="stat-icon icon-blue"><FileTextOutlined /></div>
          <div class="stat-body">
            <div class="stat-label">合同总数 / 执行中</div>
            <div class="stat-value">{{ stats.totalCount || 0 }} <span class="stat-sub">/ {{ stats.runningCount || 0 }}</span></div>
          </div>
        </div>
        <div class="stat-card stat-green">
          <div class="stat-icon icon-green"><DollarOutlined /></div>
          <div class="stat-body">
            <div class="stat-label">应收总额 / 已收（万元）</div>
            <div class="stat-value">{{ wan(stats.receivable) }} <span class="stat-sub">/ {{ wan(stats.received) }}</span></div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-icon icon-orange"><ClockCircleOutlined /></div>
          <div class="stat-body">
            <div class="stat-label">应收未收（万元）</div>
            <div class="stat-value warn">{{ wan(stats.receivableUnsettled) }}</div>
          </div>
        </div>
        <div class="stat-card stat-blue">
          <div class="stat-icon icon-blue"><CreditCardOutlined /></div>
          <div class="stat-body">
            <div class="stat-label">应付总额 / 已付（万元）</div>
            <div class="stat-value">{{ wan(stats.payable) }} <span class="stat-sub">/ {{ wan(stats.paid) }}</span></div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-icon icon-orange"><MoneyCollectOutlined /></div>
          <div class="stat-body">
            <div class="stat-label">应付未付（万元）</div>
            <div class="stat-value warn">{{ wan(stats.payableUnsettled) }}</div>
          </div>
        </div>
        <div class="stat-card stat-red">
          <div class="stat-icon icon-red"><ExclamationCircleOutlined /></div>
          <div class="stat-body">
            <div class="stat-label">逾期节点数</div>
            <div class="stat-value" :class="{ danger: stats.overdueNodeCount > 0 }">{{ stats.overdueNodeCount || 0 }}</div>
          </div>
        </div>
      </div>

      <!-- 查询条件：快捷查询（编号/名称/状态）+ 点击「更多」展开其余条件 -->
      <a-form layout="inline" class="search-form">
        <a-form-item label="合同编号"><a-input v-model:value="query.contractNo" allow-clear placeholder="请输入" style="width: 140px" @press-enter="loadList" /></a-form-item>
        <a-form-item label="合同名称"><a-input v-model:value="query.contractName" allow-clear placeholder="请输入" style="width: 160px" @press-enter="loadList" /></a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear placeholder="全部" style="width: 110px">
            <a-select-option v-for="s in STATUS_LIST" :key="s" :value="s">{{ s }}</a-select-option>
          </a-select>
        </a-form-item>
        <template v-if="showMore">
          <a-form-item label="合同类型">
            <a-select v-model:value="query.contractType" allow-clear placeholder="全部" style="width: 120px">
              <a-select-option v-for="i in typeOptions" :key="i.itemText" :value="i.itemText">{{ i.itemText }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="相对方"><a-input v-model:value="query.counterparty" allow-clear placeholder="请输入" style="width: 140px" @press-enter="loadList" /></a-form-item>
          <a-form-item label="签订日期">
            <a-range-picker v-model:value="signRange" style="width: 230px" value-format="YYYY-MM-DD" />
          </a-form-item>
        </template>
        <a-form-item :class="showMore ? 'btn-row-center' : ''">
          <a-space>
            <a-button type="primary" @click="searchList">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
            <a class="more-link" @click="showMore = !showMore">
              {{ showMore ? '收起' : '更多' }}
              <component :is="showMore ? UpOutlined : DownOutlined" />
            </a>
          </a-space>
        </a-form-item>
      </a-form>

      <!-- 工具栏 -->
      <div class="toolbar">
        <a-button v-if="hasPerm('ems:contract:edit')" type="primary" @click="openDrawer()">新建合同</a-button>
      </div>

      <!-- 台账列表（高度固定，内容区滚动） -->
      <div class="table-host">
        <a-table
          row-key="id"
          size="small"
          :columns="columns"
          :data-source="list"
          :loading="loading"
          :pagination="pagination"
          :scroll="{ x: 1500, y: scrollY }"
        >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'contractNo'">
            <span class="link-text" @click="goDetail(record)">{{ record.contractNo }}</span>
          </template>
          <template v-else-if="column.key === 'contractType'">
            <a-tag :color="record.contractType === '收入合同' ? 'blue' : 'purple'">{{ record.contractType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
          <template v-else-if="column.key === 'settled'">
            {{ money(record.settledAmount) }}
            <a-badge v-if="record.overdueNodeCount > 0" :count="record.overdueNodeCount" :number-style="{ backgroundColor: '#f5222d' }" :title="`逾期节点 ${record.overdueNodeCount} 个`" style="margin-left: 6px" />
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Number(record.progress || 0)" size="small" :style="{ width: '110px' }" />
          </template>
          <template v-else-if="column.key === 'expireDate'">
            <span :class="{ 'warn-text': isExpiring(record) }">{{ record.expireDate || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space :size="4" wrap>
              <span class="action-link" @click="goDetail(record)">详情</span>
              <span v-if="hasPerm('ems:contract:edit') && canEdit(record)" class="action-link" @click="openDrawer(record)">编辑</span>
              <span v-if="hasPerm('ems:contract:edit') && record.status === '草稿'" class="action-link" @click="handleSubmit(record)">提交</span>
              <span v-if="hasPerm('ems:contract:finance') && record.status === '执行中'" class="action-link" @click="goDetail(record, 'txn')">登记</span>
              <span v-if="hasPerm('ems:contract:edit') && record.status === '执行中'" class="action-link" @click="handleSuspend(record)">中止</span>
              <span v-if="hasPerm('ems:contract:edit') && record.status === '已中止'" class="action-link" @click="handleResume(record)">恢复</span>
              <span v-if="hasPerm('ems:contract:edit') && ['草稿', '执行中'].includes(record.status)" class="action-link danger" @click="handleCancel(record)">作废</span>
              <span v-if="hasPerm('ems:contract:delete') && record.status === '草稿'" class="action-link danger" @click="handleDelete(record)">删除</span>
            </a-space>
          </template>
        </template>
        </a-table>
      </div>
    </div>

    <!-- 新建/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="formState.id ? '编辑合同' : '新建合同'"
      width="860"
      :body-style="{ paddingBottom: '80px' }"
      @close="drawerVisible = false"
    >
      <a-form :label-col="{ style: { width: '100px' } }">
        <a-divider class="title-divider" orientation="left">基本信息</a-divider>
        <a-row :gutter="[16, 4]">
          <a-col :span="12">
            <a-form-item label="合同编号">
              <a-input v-model:value="formState.contractNo" placeholder="留空自动生成（HT+日期+流水）" :disabled="lockCore" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同名称" required>
              <a-input v-model:value="formState.contractName" :maxlength="100" :disabled="lockCore" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同类型" required>
              <a-select v-model:value="formState.contractType" placeholder="请选择" :disabled="lockCore" @change="onTypeChange">
                <a-select-option v-for="i in typeOptions" :key="i.itemText" :value="i.itemText">{{ i.itemText }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item v-if="formState.contractType === '收入合同'" label="客户" required>
              <a-select
                v-model:value="formState.counterpartyId"
                show-search
                :filter-option="false"
                placeholder="选择客户"
                :disabled="lockCore"
                :options="customerOptions"
                @search="loadCustomers"
                @change="onCustomerChange"
              />
            </a-form-item>
            <a-form-item v-else label="供应商" required>
              <a-input v-model:value="formState.counterpartyName" placeholder="请输入供应商名称" :disabled="lockCore" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同金额" required>
              <a-input-number v-model:value="formState.amount" :min="0" :precision="2" style="width: 100%" placeholder="元" :disabled="lockCore" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="付款方式">
              <a-select v-model:value="formState.payMode" allow-clear placeholder="请选择" :disabled="lockCore">
                <a-select-option v-for="i in payModeOptions" :key="i.itemText" :value="i.itemText">{{ i.itemText }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="签订日期" required :label-col="{ style: { width: '80px' } }">
              <a-date-picker v-model:value="formState.signDate" value-format="YYYY-MM-DD" style="width: 100%" :disabled="lockCore" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="生效日期" :label-col="{ style: { width: '80px' } }">
              <a-date-picker v-model:value="formState.effectDate" value-format="YYYY-MM-DD" style="width: 100%" :disabled="lockCore" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="到期日期" :label-col="{ style: { width: '80px' } }">
              <a-date-picker v-model:value="formState.expireDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="负责人" required>
              <a-select
                v-model:value="formState.leadId"
                show-search
                :filter-option="false"
                placeholder="搜索选择负责人"
                :disabled="lockCore"
                :options="userOptions"
                @search="loadUsers"
                @change="onLeadChange"
              />
            </a-form-item>
          </a-col>
          <a-col v-if="formState.contractType === '收入合同'" :span="12">
            <a-form-item label="关联委托">
              <a-select
                v-model:value="formState.entrustIds"
                mode="multiple"
                show-search
                :filter-option="false"
                placeholder="可关联多个检测委托"
                :disabled="lockCore"
                :options="entrustOptions"
                @search="loadEntrusts"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="合同说明" :label-col="{ style: { width: '100px' } }">
              <a-textarea v-model:value="formState.description" :rows="2" placeholder="合同说明（选填）" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" :label-col="{ style: { width: '100px' } }">
              <a-textarea v-model:value="formState.remark" :rows="2" placeholder="备注（选填）" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="合同附件" :label-col="{ style: { width: '100px' } }">
              <a-upload
                multiple
                list-type="text"
                :file-list="attachments"
                :custom-request="handleAttachmentUpload"
                @remove="onAttachmentRemove"
              >
                <a-button size="small"><UploadOutlined /> 上传附件（可多选）</a-button>
              </a-upload>
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider class="title-divider" orientation="left">{{ formState.contractType === '支出合同' ? '付款节点' : '收款节点' }}</a-divider>
        <div class="node-toolbar">
          <a-button size="small" type="dashed" :disabled="lockNodes" @click="addNode">+ 添加节点</a-button>
          <span class="node-sum" :class="{ 'sum-error': nodeSumError }">
            节点合计：{{ money(nodeSum) }} / 合同金额：{{ money(formState.amount || 0) }}
            <span v-if="nodeSumError">（差额 {{ money((formState.amount || 0) - nodeSum) }}）</span>
          </span>
        </div>
        <a-table row-key="_uid" size="small" :columns="nodeColumns" :data-source="nodeRows" :pagination="false">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
            <template v-else-if="column.key === 'nodeName'">
              <a-input v-model:value="record.nodeName" placeholder="如：预付款/验收款" :disabled="lockNodes" />
            </template>
            <template v-else-if="column.key === 'planAmount'">
              <a-input-number v-model:value="record.planAmount" :min="0.01" :precision="2" style="width: 100%" :disabled="lockNodes" />
            </template>
            <template v-else-if="column.key === 'planDate'">
              <a-date-picker v-model:value="record.planDate" value-format="YYYY-MM-DD" style="width: 100%" :disabled="lockNodes" />
            </template>
            <template v-else-if="column.key === 'nodeDesc'">
              <a-input v-model:value="record.nodeDesc" placeholder="付款条件说明" :disabled="lockNodes" />
            </template>
            <template v-else-if="column.key === 'nodeStatus'" >
              <a-tag v-if="record.status" :color="nodeStatusColor(record.status)">{{ record.status }}</a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'op'">
              <span class="action-link danger" :class="{ 'op-disabled': lockNodes }" @click="removeNode(record)">删除</span>
            </template>
          </template>
        </a-table>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
        </a-space>
      </div>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { UploadOutlined, DownOutlined, UpOutlined, FileTextOutlined, DollarOutlined, ClockCircleOutlined, CreditCardOutlined, MoneyCollectOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  getContracts, getContractStatistics, saveContract, deleteContract,
  submitContract, suspendContract, resumeContract, cancelContract,
  getDictItems, getCustomers, getEntrusts, archiveFile, getFiles
} from '../../../api/ems'
import { getUsers } from '../../../api/system'
import { searchUsers } from '../../../api/ems'
import { uploadAttachment } from '../../../api/attachment'
import { usePermission } from '../../../composables/usePermission'

const { hasPerm } = usePermission()
const router = useRouter()

const STATUS_LIST = ['草稿', '执行中', '已完结', '已中止', '已作废']

// ---------- 字典 ----------
const typeOptions = ref([])
const payModeOptions = ref([])
function loadDicts() {
  getDictItems('contract_type').then((res) => { typeOptions.value = res.data || res || [] }).catch(() => {})
  getDictItems('contract_pay_mode').then((res) => { payModeOptions.value = res.data || res || [] }).catch(() => {})
}

// ---------- 列表与统计 ----------
const list = ref([])
const loading = ref(false)
const stats = ref({})
const query = reactive({ contractNo: '', contractName: '', contractType: undefined, status: undefined, counterparty: '' })
const signRange = ref(null)
// 服务端分页：默认每页10条
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: (t) => `共 ${t} 条`, onChange: (p) => { pagination.current = p; loadList() } })
// 查询面板展开状态：默认仅显示快捷条件（编号/名称/状态），点击「更多」展开其余条件
const showMore = ref(false)

const columns = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 150, fixed: 'left' },
  { title: '合同名称', dataIndex: 'contractName', key: 'contractName', width: 200, ellipsis: true },
  { title: '合同类型', key: 'contractType', width: 100 },
  { title: '相对方', dataIndex: 'counterpartyName', key: 'counterpartyName', width: 160, ellipsis: true },
  { title: '合同金额(元)', key: 'amount', width: 120, align: 'right' },
  { title: '已收/已付', key: 'settled', width: 130, align: 'right' },
  { title: '收付进度', key: 'progress', width: 140 },
  { title: '签订日期', dataIndex: 'signDate', key: 'signDate', width: 110 },
  { title: '到期日期', key: 'expireDate', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '负责人', dataIndex: 'leadName', key: 'leadName', width: 90 },
  { title: '操作', key: 'actions', width: 240, fixed: 'right' }
]

function loadList() {
  loading.value = true
  const params = { ...query, page: pagination.current, size: pagination.pageSize }
  if (signRange.value && signRange.value.length === 2) {
    params.signStart = signRange.value[0]
    params.signEnd = signRange.value[1]
  }
  getContracts(params).then((res) => {
    const p = res.data || res
    if (Array.isArray(p)) {
      list.value = p
      pagination.total = p.length
    } else {
      list.value = p.records || []
      pagination.total = p.total || 0
    }
  }).catch(() => {}).finally(() => { loading.value = false; nextTick(syncTableHeight) })
}

/** 条件查询：回到第一页 */
function searchList() {
  pagination.current = 1
  loadList()
}

function loadStats() {
  getContractStatistics().then((res) => { stats.value = res.data || res || {} }).catch(() => {})
}

function resetQuery() {
  Object.assign(query, { contractNo: '', contractName: '', contractType: undefined, status: undefined, counterparty: '' })
  signRange.value = null
  pagination.current = 1
  loadList()
}

function refreshAll() {
  loadList()
  loadStats()
}

// ---------- 展示辅助 ----------
function money(v) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
/** 金额按万元展示（统计卡片） */
function wan(v) {
  if (v === null || v === undefined || v === '') return '0.00'
  return (Number(v) / 10000).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ---------- 表格固定高度：动态计算使页面布满一屏，表体内部滚动 ----------
const scrollY = ref(420)
let hostObserver = null
function syncTableHeight() {
  const box = document.querySelector('.page-wrap .table-host')
  if (!box) return
  const headerEl = box.querySelector('.ant-table-thead')
  const theadTop = (headerEl || box).getBoundingClientRect().top
  const pagEl = box.querySelector('.ant-table-pagination')
  const pagH = pagEl ? pagEl.getBoundingClientRect().height : 32
  // 视口底 - 表头顶部 - 分页区 - 底部留白（content 外边距12 + 页内边距16 + 卡片内边距16 + 分页上边距16）
  const h = window.innerHeight - theadTop - pagH - 60
  scrollY.value = h > 200 ? Math.floor(h) : 200
}
watch(showMore, () => nextTick(syncTableHeight))
function statusColor(s) {
  return { '草稿': 'default', '执行中': 'blue', '已完结': 'green', '已中止': 'orange', '已作废': 'red' }[s] || 'default'
}
function nodeStatusColor(s) {
  if (s === '已收讫' || s === '已付讫') return 'green'
  if (s === '部分收' || s === '部分付') return 'orange'
  return 'default'
}
function canEdit(record) {
  return ['草稿', '执行中'].includes(record.status)
}
function isExpiring(record) {
  if (!record.expireDate || record.status === '已完结' || record.status === '已作废') return false
  return dayjs(record.expireDate).diff(dayjs(), 'day') <= 30
}
function goDetail(record, tab) {
  router.push({ path: `/ems/contract/${record.id}`, query: tab ? { tab } : {} })
}

// ---------- 状态操作 ----------
function handleSubmit(record) {
  Modal.confirm({
    title: '提交合同',
    content: `确认提交合同【${record.contractName}】？提交后进入执行中状态。`,
    onOk: () => submitContract(record.id).then(() => {
      message.success('提交成功')
      refreshAll()
    }).catch(() => {})
  })
}
function handleSuspend(record) {
  let reason = ''
  Modal.confirm({
    title: '中止合同',
    content: () => h('div', [
      h('p', { style: 'margin-bottom:8px' }, `确认中止合同【${record.contractName}】？中止后禁止新增收付款登记。`),
      h('textarea', {
        class: 'ant-input', rows: 3, placeholder: '请填写中止原因',
        onChange: (e) => { reason = e.target.value }
      })
    ]),
    onOk: () => suspendContract(record.id, reason).then(() => {
      message.success('已中止')
      refreshAll()
    }).catch(() => {})
  })
}
function handleResume(record) {
  Modal.confirm({
    title: '恢复合同',
    content: `确认恢复合同【${record.contractName}】执行？`,
    onOk: () => resumeContract(record.id).then(() => {
      message.success('已恢复')
      refreshAll()
    }).catch(() => {})
  })
}
function handleCancel(record) {
  Modal.confirm({
    title: '作废合同',
    content: `确认作废合同【${record.contractName}】？作废后不可恢复；已发生收付款登记的合同仅可中止。`,
    okType: 'danger',
    onOk: () => cancelContract(record.id).then(() => {
      message.success('已作废')
      refreshAll()
    }).catch(() => {})
  })
}
function handleDelete(record) {
  Modal.confirm({
    title: '删除合同',
    content: `确认删除草稿合同【${record.contractName}】？节点与关联数据将一并删除。`,
    okType: 'danger',
    onOk: () => deleteContract(record.id).then(() => {
      message.success('已删除')
      refreshAll()
    }).catch(() => {})
  })
}

// ---------- 新建/编辑抽屉 ----------
const drawerVisible = ref(false)
const saving = ref(false)
const formState = reactive({
  id: null, contractNo: '', contractName: '', contractType: undefined,
  counterpartyId: undefined, counterpartyName: '', amount: null,
  signDate: null, effectDate: null, expireDate: null, payMode: undefined,
  leadId: undefined, leadName: '', description: '', remark: '', entrustIds: []
})
// 执行中合同仅允许编辑非核心字段
const lockCore = computed(() => !!formState.id && formState.status === '执行中')
const lockNodes = computed(() => !!formState.id && !['草稿', '执行中'].includes(formState.status))

const customerOptions = ref([])
const userOptions = ref([])
const entrustOptions = ref([])

function loadCustomers(kw) {
  getCustomers().then((res) => {
    const all = res.data || res || []
    const k = (kw || '').trim()
    customerOptions.value = all
      .filter((c) => c.status === 1 || c.status === '1' || c.status === '启用')
      .filter((c) => !k || (c.custName || '').includes(k))
      .map((c) => ({ label: c.custName, value: c.id }))
  }).catch(() => {})
}
function loadUsers(kw) {
  searchUsers({ keyword: kw || '', page: 1, size: 20 }).then((res) => {
    const data = res.data || res || {}
    const records = data.records || data.list || data || []
    userOptions.value = records.map((u) => ({
      label: `${u.realName || u.username}（${u.username}）`,
      value: u.id,
      realName: u.realName
    }))
  }).catch(() => {})
}
function loadEntrusts(kw) {
  getEntrusts({ keyword: kw || '' }).then((res) => {
    const all = res.data || res || []
    entrustOptions.value = all.map((e) => ({
      label: `${e.entrustNo || ''} ${e.entrustName || ''}`,
      value: e.id
    }))
  }).catch(() => {})
}
function onCustomerChange(id) {
  const hit = customerOptions.value.find((c) => c.value === id)
  formState.counterpartyName = hit ? hit.label : ''
}
function onLeadChange(id) {
  const hit = userOptions.value.find((u) => u.value === id)
  formState.leadName = hit ? hit.realName : ''
}
function onTypeChange() {
  formState.counterpartyId = undefined
  formState.counterpartyName = ''
  formState.entrustIds = []
}

// ---------- 节点编辑 ----------
let nodeUid = 0
const nodeRows = ref([])
const nodeColumns = [
  { title: '#', key: 'seq', width: 46 },
  { title: '节点名称', key: 'nodeName', width: 160 },
  { title: '计划金额(元)', key: 'planAmount', width: 130 },
  { title: '计划日期', key: 'planDate', width: 150 },
  { title: '节点说明', key: 'nodeDesc' },
  { title: '状态', key: 'nodeStatus', width: 90 },
  { title: '操作', key: 'op', width: 60 }
]
const nodeSum = computed(() =>
  nodeRows.value.reduce((s, n) => s + Number(n.planAmount || 0), 0))
const nodeSumError = computed(() => {
  if (!formState.amount && formState.amount !== 0) return false
  return nodeRows.value.length > 0 && Math.abs(nodeSum.value - Number(formState.amount || 0)) > 0.001
})
function addNode() {
  nodeRows.value.push({ _uid: ++nodeUid, id: null, nodeName: '', planAmount: null, planDate: null, nodeDesc: '', status: null })
}
function removeNode(record) {
  if (lockNodes.value) return
  if (record.hasAlloc) {
    message.warning('该节点已产生收付款登记，不允许删除，请先撤销对应登记')
    return
  }
  nodeRows.value = nodeRows.value.filter((n) => n._uid !== record._uid)
}

// ---------- 附件 ----------
const attachments = ref([])
const archivedPaths = ref(new Set())
async function handleAttachmentUpload({ file, onSuccess, onError }) {
  try {
    const res = await uploadAttachment(file)
    const data = res.data || res
    const item = { uid: file.uid, name: data.name || file.name, path: data.path, status: 'done' }
    attachments.value.push(item)
    onSuccess && onSuccess(res, file)
  } catch (e) {
    onError && onError(e)
    message.error(`附件「${file.name}」上传失败`)
  }
}
function onAttachmentRemove(file) {
  attachments.value = attachments.value.filter((a) => a.uid !== file.uid)
}
function loadAttachments(contractId) {
  if (!contractId) return
  getFiles({ bizType: 'contract', bizId: contractId }).then((res) => {
    const files = res.data || res || []
    attachments.value = files.map((f, idx) => ({
      uid: 'exist-' + idx, name: f.fileName, path: f.filePath, status: 'done'
    }))
    files.forEach((f) => archivedPaths.value.add(f.filePath))
  }).catch(() => {})
}
function archivePending(contractId) {
  const pending = attachments.value.filter((a) => a.path && !archivedPaths.value.has(a.path))
  if (!pending.length || !contractId) return Promise.resolve()
  return Promise.all(pending.map((a) =>
    archiveFile({ bizType: 'contract', bizId: contractId, fileName: a.name, filePath: a.path })
      .then(() => { archivedPaths.value.add(a.path) })
      .catch(() => {})
  ))
}

// ---------- 打开抽屉 ----------
function openDrawer(record) {
  Object.assign(formState, {
    id: null, contractNo: '', contractName: '', contractType: undefined,
    counterpartyId: undefined, counterpartyName: '', amount: null,
    signDate: dayjs().format('YYYY-MM-DD'), effectDate: null, expireDate: null,
    payMode: undefined, leadId: undefined, leadName: '',
    description: '', remark: '', entrustIds: [], status: '草稿'
  })
  nodeRows.value = []
  attachments.value = []
  archivedPaths.value = new Set()
  loadCustomers('')
  loadUsers('')
  loadEntrusts('')
  if (record) {
    // 拉取详情回填（含节点与关联委托）
    import('../../../api/ems').then(({ getContract }) => {
      getContract(record.id).then((res) => {
        const d = res.data || res
        Object.assign(formState, {
          id: d.id, contractNo: d.contractNo, contractName: d.contractName,
          contractType: d.contractType, counterpartyId: d.counterpartyId,
          counterpartyName: d.counterpartyName, amount: Number(d.amount || 0),
          signDate: d.signDate, effectDate: d.effectDate, expireDate: d.expireDate,
          payMode: d.payMode, leadId: d.leadId, leadName: d.leadName,
          description: d.description, remark: d.remark, status: d.status,
          entrustIds: (d.entrusts || []).map((e) => e.entrustId)
        })
        if (d.leadId) {
          userOptions.value = [{ label: d.leadName || '', value: d.leadId, realName: d.leadName }]
        }
        if (d.counterpartyId) {
          customerOptions.value = [{ label: d.counterpartyName, value: d.counterpartyId }]
        }
        nodeRows.value = (d.nodes || []).map((n) => ({
          _uid: ++nodeUid, id: n.id, nodeName: n.nodeName,
          planAmount: Number(n.planAmount || 0), planDate: n.planDate,
          nodeDesc: n.nodeDesc, status: n.status,
          hasAlloc: Number(n.allocatedAmount || 0) > 0
        }))
        loadAttachments(d.id)
      }).catch(() => {})
    })
  }
  drawerVisible.value = true
}

function handleSave() {
  if (!formState.contractName) return message.warning('请输入合同名称')
  if (!formState.contractType) return message.warning('请选择合同类型')
  if (!formState.counterpartyName && !formState.counterpartyId) return message.warning('请选择或输入相对方')
  if (formState.amount === null || formState.amount === undefined) return message.warning('请输入合同金额')
  if (!formState.signDate) return message.warning('请选择签订日期')
  if (!formState.leadId) return message.warning('请选择负责人')
  if (formState.payMode === '分期' && nodeRows.value.length < 2) return message.warning('付款方式为分期时，节点至少 2 期')
  if (nodeRows.value.length && nodeSumError.value) return message.warning('节点计划金额合计必须等于合同金额')
  for (const n of nodeRows.value) {
    if (!n.nodeName) return message.warning('请填写全部节点名称')
    if (!n.planAmount || n.planAmount <= 0) return message.warning('节点计划金额必须大于0')
    if (!n.planDate) return message.warning('请选择全部节点的计划日期')
  }

  saving.value = true
  const contract = {
    id: formState.id || null,
    contractNo: formState.contractNo || null,
    contractName: formState.contractName,
    contractType: formState.contractType,
    counterpartyId: formState.contractType === '收入合同' ? formState.counterpartyId : null,
    counterpartyName: formState.counterpartyName,
    amount: formState.amount,
    signDate: formState.signDate,
    effectDate: formState.effectDate || formState.signDate,
    expireDate: formState.expireDate,
    payMode: formState.payMode,
    leadId: formState.leadId,
    leadName: formState.leadName,
    description: formState.description,
    remark: formState.remark
  }
  const nodes = nodeRows.value.map((n) => ({
    id: n.id || null, nodeName: n.nodeName, planAmount: n.planAmount,
    planDate: n.planDate, nodeDesc: n.nodeDesc
  }))
  saveContract({ contract, nodes, entrustIds: formState.entrustIds || [] }).then((res) => {
    const saved = res.data || res
    return archivePending(saved.id).then(() => saved)
  }).then(() => {
    message.success('保存成功')
    drawerVisible.value = false
    refreshAll()
  }).catch(() => {}).finally(() => { saving.value = false })
}

onMounted(() => {
  loadDicts()
  refreshAll()
  nextTick(() => {
    syncTableHeight()
    const box = document.querySelector('.page-wrap .table-host')
    if (box && 'ResizeObserver' in window) {
      hostObserver = new ResizeObserver(() => syncTableHeight())
      hostObserver.observe(box)
    }
    window.addEventListener('resize', syncTableHeight)
  })
})

onUnmounted(() => {
  if (hostObserver) hostObserver.disconnect()
  window.removeEventListener('resize', syncTableHeight)
})
</script>

<style scoped>
.page-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
  box-sizing: border-box;
}
.card-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-sizing: border-box;
  overflow: hidden;
}
/* 台账统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
/* 台账表格容器：填满剩余高度，表体内部滚动 */
.table-host {
  flex: 1;
  min-height: 0;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: box-shadow 0.2s;
}
.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.stat-blue { background: #eff6ff; border-color: #dbeafe; }
.stat-green { background: #f0fdf4; border-color: #dcfce7; }
.stat-orange { background: #fff7ed; border-color: #ffedd5; }
.stat-red { background: #fef2f2; border-color: #fee2e2; }
.stat-icon {
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.icon-blue { background: rgba(22, 119, 255, 0.1); color: #1677ff; }
.icon-green { background: rgba(0, 180, 42, 0.1); color: #00b42a; }
.icon-orange { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
.icon-red { background: rgba(245, 63, 63, 0.1); color: #f53f3f; }
.stat-body {
  min-width: 0;
}
.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 2px;
  white-space: nowrap;
}
.stat-value {
  font-size: 19px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
}
.stat-value.warn { color: #ff7d00; }
.stat-value.danger { color: #f53f3f; }
.stat-sub {
  font-size: 13px;
  font-weight: 400;
  color: #8c8c8c;
}
.search-form {
  margin-bottom: 8px;
}
/* 更多条件展开/收起链接 */
.more-link {
  color: #2563EB;
  font-size: 13px;
  white-space: nowrap;
}
.more-link:hover {
  opacity: 0.8;
}
/* 展开更多后：按钮区独占一行并水平居中（折叠时自动恢复同行排列） */
.search-form .btn-row-center {
  width: 100%;
  margin-top: 4px;
}
.search-form .btn-row-center :deep(.ant-form-item-control-input-content) {
  display: flex;
  justify-content: center;
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
.link-text {
  color: #2563EB;
  cursor: pointer;
}
.link-text:hover { text-decoration: underline; }
.action-link {
  color: #2563EB;
  cursor: pointer;
}
.action-link:hover { text-decoration: underline; }
.action-link.danger { color: #f5222d; }
.warn-text { color: #fa8c16; }
/* 节点编辑 */
.node-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.node-sum {
  font-size: 13px;
  color: #52c41a;
}
.node-sum.sum-error { color: #f5222d; }
.op-disabled {
  color: #bfbfbf !important;
  cursor: not-allowed;
}
.drawer-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 100%;
  padding: 10px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}
</style>
