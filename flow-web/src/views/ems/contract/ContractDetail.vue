<template>
  <div class="page-container">
    <div class="page-toolbar">
      <a-button size="small" @click="goBack">
        <template #icon><span class="btn-icon">←</span></template>
        返回
      </a-button>
      <span class="page-title">{{ vo.contractName || '合同详情' }}<span class="page-sub">{{ vo.contractNo }}</span></span>
      <a-tag :color="statusColor(vo.status)">{{ vo.status || '-' }}</a-tag>
      <div class="toolbar-actions">
        <a-space>
          <a-button v-if="hasPerm('ems:contract:finance') && vo.status === '执行中'" size="small" type="primary" @click="openTxnModal">
            {{ vo.contractType === '支出合同' ? '支付登记' : '收款登记' }}
          </a-button>
          <a-button v-if="hasPerm('ems:contract:edit') && ['草稿', '执行中'].includes(vo.status)" size="small" @click="openNodeModal">编辑节点计划</a-button>
          <a-button v-if="hasPerm('ems:contract:edit') && vo.status === '草稿'" size="small" type="primary" @click="handleSubmit">提交</a-button>
          <a-button v-if="hasPerm('ems:contract:edit') && vo.status === '执行中'" size="small" @click="handleSuspend">中止</a-button>
          <a-button v-if="hasPerm('ems:contract:edit') && vo.status === '已中止'" size="small" @click="handleResume">恢复</a-button>
          <a-button v-if="hasPerm('ems:contract:edit') && ['草稿', '执行中'].includes(vo.status)" size="small" danger @click="handleCancel">作废</a-button>
        </a-space>
      </div>
    </div>

    <a-skeleton v-if="loading" active />
    <template v-else>
      <!-- 基本信息 -->
      <a-card title="基本信息" size="small" class="block">
        <a-descriptions :column="2" bordered size="small" :label-style="{ width: '120px' }">
          <a-descriptions-item label="合同编号">{{ vo.contractNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="合同类型">
            <a-tag :color="vo.contractType === '收入合同' ? 'blue' : 'purple'">{{ vo.contractType || '-' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="相对方">{{ vo.counterpartyName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="合同金额(元)">{{ money(vo.amount) }}</a-descriptions-item>
          <a-descriptions-item label="付款方式">{{ vo.payMode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="签订日期">{{ vo.signDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="生效日期">{{ vo.effectDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="到期日期">
            <span :class="{ 'warn-text': isExpiring }">{{ vo.expireDate || '-' }}</span>
            <span v-if="isExpiring" class="warn-text">（30 天内到期）</span>
          </a-descriptions-item>
          <a-descriptions-item label="负责人">{{ vo.leadName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收付进度">
            <a-progress :percent="Number(vo.progress || 0)" size="small" :style="{ width: '160px' }" />
            <span class="progress-text">{{ money(vo.settledAmount) }} / {{ money(vo.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="创建人">{{ personText(vo.createName, vo.createBy) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ renderDate(vo.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="更新人">{{ personText(vo.updateName, vo.updateBy) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ renderDate(vo.updateTime) }}</a-descriptions-item>
          <a-descriptions-item v-if="vo.remark" label="备注" :span="2">{{ vo.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-divider class="title-divider" orientation="left">合同说明</a-divider>
        <div class="desc-text">{{ vo.description || '暂无说明' }}</div>

        <a-divider class="title-divider" orientation="left">合同附件</a-divider>
        <div v-if="attachments.length" class="attach-grid">
          <div v-for="item in attachments" :key="item.filePath" class="attach-card">
            <FileOutlined class="attach-icon" />
            <div class="attach-name" :title="item.fileName">{{ item.fileName }}</div>
            <a class="attach-download" :href="attachmentUrl(item)" target="_blank" rel="noopener" title="下载">
              <DownloadOutlined />
            </a>
          </div>
        </div>
        <div v-else class="empty-tip">暂无附件</div>
      </a-card>

      <!-- 收付款节点 -->
      <a-card :title="vo.contractType === '支出合同' ? '付款节点' : '收款节点'" size="small" class="block">
        <a-table
          row-key="id"
          size="small"
          :columns="nodeColumns"
          :data-source="vo.nodes || []"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'planAmount'">{{ money(record.planAmount) }}</template>
            <template v-else-if="column.key === 'planDate'">
              <span :class="{ 'danger-text': record.overdue }">{{ record.planDate || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'allocated'">{{ money(record.allocatedAmount) }}</template>
            <template v-else-if="column.key === 'progress'">
              <a-progress :percent="nodePercent(record)" size="small" :style="{ width: '110px' }" />
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="nodeStatusColor(record.status)">{{ record.status || '-' }}</a-tag>
              <a-tag v-if="record.overdue" color="red">逾期</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 收付款记录 -->
      <a-card ref="txnCardRef" :title="vo.contractType === '支出合同' ? '付款记录' : '收款记录'" size="small" class="block">
        <a-table
          v-if="(vo.txns || []).length"
          row-key="id"
          size="small"
          :columns="txnColumns"
          :data-source="vo.txns || []"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            <template v-else-if="column.key === 'allocations'">
              <a-tag v-for="a in record.allocations || []" :key="a.nodeId" color="geekblue">
                第{{ a.nodeSeq }}期 {{ a.nodeName }}：{{ money(a.allocateAmount) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'createTime'">{{ renderDate(record.createTime) }}</template>
            <template v-else-if="column.key === 'op'">
              <span
                v-if="canDeleteTxn"
                class="action-link danger"
                @click="handleDeleteTxn(record)"
              >撤销</span>
            </template>
          </template>
        </a-table>
        <a-empty v-else description="暂无收付款记录" :image="simpleImage" />
      </a-card>

      <!-- 关联检测委托 -->
      <a-card v-if="(vo.entrusts || []).length" title="关联检测委托" size="small" class="block">
        <a-table
          row-key="entrustId"
          size="small"
          :columns="entrustColumns"
          :data-source="vo.entrusts || []"
          :pagination="false"
          :scroll="{ x: 900 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
            <template v-else-if="column.key === 'entrustNo'">
              <span class="link-text" @click="goEntrust(record)">{{ record.entrustNo || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'entrustName'">
              <a-tag v-if="record.urgent" color="red" style="margin-right: 4px">紧急</a-tag>
              <span class="link-text" @click="goEntrust(record)">{{ record.entrustName || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="entrustStatusColor(record.status)">{{ record.status || '-' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'createName'">{{ record.createName || '-' }}</template>
            <template v-else-if="column.key === 'createTime'">{{ renderDate(record.createTime) }}</template>
          </template>
        </a-table>
      </a-card>

      <!-- 操作记录 -->
      <a-card title="操作记录" size="small" class="block">
        <a-timeline v-if="(vo.histories || []).length">
          <a-timeline-item v-for="h in vo.histories" :key="h.id" :color="historyColor(h.action)">
            <div class="hist-line">
              <a-tag :color="historyColor(h.action)">{{ h.action }}</a-tag>
              <span class="hist-content">{{ h.content }}</span>
            </div>
            <div class="hist-meta">{{ h.operatorName || h.operatorId || '-' }} · {{ renderDate(h.createTime) }}</div>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else description="暂无操作记录" :image="simpleImage" />
      </a-card>
    </template>

    <!-- 收款/支付登记弹窗 -->
    <a-modal
      v-model:open="txnModalVisible"
      :title="vo.contractType === '支出合同' ? '支付登记' : '收款登记'"
      :width="1000"
      :confirm-loading="txnSaving"
      @ok="handleTxnSave"
    >
      <a-form :label-col="{ style: { width: '100px' } }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="vo.contractType === '支出合同' ? '支付日期' : '收款日期'" required>
              <a-date-picker v-model:value="txnForm.txnDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="vo.contractType === '支出合同' ? '支付金额(元)' : '收款金额(元)'" required>
              <a-input-number v-model:value="txnForm.amount" :min="0.01" :precision="2" style="width: 100%" @change="autoAllocate" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="vo.contractType === '支出合同' ? '支付方式' : '收款方式'" required>
              <a-select v-model:value="txnForm.payMethod" placeholder="请选择">
                <a-select-option v-for="i in payMethodOptions" :key="i.itemText" :value="i.itemText">{{ i.itemText }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="交易流水号">
              <a-input v-model:value="txnForm.txnNo" placeholder="银行回单号（选填）" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" :label-col="{ style: { width: '100px' } }">
              <a-textarea v-model:value="txnForm.remark" :rows="2" placeholder="备注（选填）" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider class="title-divider" orientation="left">核销节点分摊</a-divider>
        <div class="alloc-toolbar">
          <a-button size="small" type="dashed" @click="autoAllocate">按节点顺序自动分摊</a-button>
          <span class="alloc-sum" :class="{ 'sum-error': allocSumError }">
            分摊合计：{{ money(allocSum) }} / 登记金额：{{ money(txnForm.amount || 0) }}
          </span>
        </div>
        <a-table row-key="id" size="small" :columns="allocColumns" :data-source="allocRows" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'planAmount'">{{ money(record.planAmount) }}</template>
            <template v-else-if="column.key === 'remaining'">{{ money(record.remaining) }}</template>
            <template v-else-if="column.key === 'allocAmount'">
              <a-input-number v-model:value="record.allocAmount" :min="0" :max="Number(record.remaining)" :precision="2" style="width: 100%" />
            </template>
          </template>
        </a-table>
      </a-form>
    </a-modal>

    <!-- 节点计划编辑弹窗 -->
    <a-modal
      v-model:open="nodeModalVisible"
      :title="vo.contractType === '支出合同' ? '编辑付款节点' : '编辑收款节点'"
      :width="1000"
      :confirm-loading="nodeSaving"
      @ok="handleNodeSave"
    >
      <div class="node-toolbar">
        <a-button size="small" type="dashed" @click="addNodeRow">+ 添加节点</a-button>
        <span class="node-sum" :class="{ 'sum-error': nodeSumError }">
          节点合计：{{ money(nodeSum) }} / 合同金额：{{ money(vo.amount || 0) }}
        </span>
      </div>
      <a-table row-key="_uid" size="small" :columns="nodeEditColumns" :data-source="nodeEditRows" :pagination="false">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
          <template v-else-if="column.key === 'nodeName'">
            <a-input v-model:value="record.nodeName" placeholder="如：预付款/验收款" />
          </template>
          <template v-else-if="column.key === 'planAmount'">
            <a-input-number v-model:value="record.planAmount" :min="0.01" :precision="2" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'planDate'">
            <a-date-picker v-model:value="record.planDate" value-format="YYYY-MM-DD" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'nodeDesc'">
            <a-input v-model:value="record.nodeDesc" placeholder="付款条件说明" />
          </template>
          <template v-else-if="column.key === 'op'">
            <span class="action-link danger" @click="removeNodeRow(record)">删除</span>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal, Empty } from 'ant-design-vue'
import { FileOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  getContract, submitContract, suspendContract, resumeContract, cancelContract,
  saveContractNodes, addContractTxn, deleteContractTxn,
  getDictItems, getFiles
} from '../../../api/ems'
import { usePermission } from '../../../composables/usePermission'

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const { hasPerm } = usePermission()
const route = useRoute()
const router = useRouter()

const contractId = Number(route.params.id)
const vo = ref({})
const loading = ref(false)
const attachments = ref([])
const payMethodOptions = ref([])
const txnCardRef = ref(null)

function load() {
  loading.value = true
  getContract(contractId).then((res) => {
    vo.value = res.data || res || {}
    loadAttachments()
    if (route.query.tab === 'txn') {
      nextTick(() => {
        const el = txnCardRef.value?.$el
        el && el.scrollIntoView({ behavior: 'smooth' })
      })
    }
  }).catch(() => {}).finally(() => { loading.value = false })
}

function loadAttachments() {
  getFiles({ bizType: 'contract', bizId: contractId }).then((res) => {
    attachments.value = res.data || res || []
  }).catch(() => {})
}

// ---------- 展示辅助 ----------
function money(v) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function renderDate(v) {
  if (!v) return '-'
  return dayjs(v).isValid() ? dayjs(v).format('YYYY-MM-DD HH:mm') : String(v)
}
function personText(name, account) {
  if (!name && !account) return '-'
  return (name || '') + (account ? `(${account})` : '')
}
function statusColor(s) {
  return { '草稿': 'default', '执行中': 'blue', '已完结': 'green', '已中止': 'orange', '已作废': 'red' }[s] || 'default'
}
function nodeStatusColor(s) {
  if (s === '已收讫' || s === '已付讫') return 'green'
  if (s === '部分收' || s === '部分付') return 'orange'
  return 'default'
}
function entrustStatusColor(s) {
  return { '草稿': 'default', '待技术确认': 'orange', '已确认': 'green', '已退回': 'red' }[s] || 'default'
}
function nodePercent(n) {
  const plan = Number(n.planAmount || 0)
  if (!plan) return 0
  return Math.min(100, Math.round(Number(n.allocatedAmount || 0) / plan * 100))
}
const isExpiring = computed(() => {
  if (!vo.value.expireDate || ['已完结', '已作废'].includes(vo.value.status)) return false
  return dayjs(vo.value.expireDate).diff(dayjs(), 'day') <= 30
})
function historyColor(action) {
  if (['提交'].includes(action)) return 'green'
  if (['中止', '作废', '撤销登记'].includes(action)) return 'red'
  if (['恢复'].includes(action)) return 'blue'
  return 'gray'
}
function attachmentUrl(file) {
  return `/api/v1/attachments/download?path=${encodeURIComponent(file.filePath)}&name=${encodeURIComponent(file.fileName)}`
}

// ---------- 节点 / 流水表格列 ----------
const nodeColumns = [
  { title: '期数', dataIndex: 'seq', key: 'seq', width: 60 },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 140 },
  { title: '计划金额(元)', key: 'planAmount', width: 120, align: 'right' },
  { title: '计划日期', key: 'planDate', width: 110 },
  { title: '节点说明', dataIndex: 'nodeDesc', key: 'nodeDesc', ellipsis: true },
  { title: '已核销(元)', key: 'allocated', width: 120, align: 'right' },
  { title: '核销进度', key: 'progress', width: 140 },
  { title: '状态', key: 'status', width: 130 }
]
const txnColumns = [
  { title: '日期', dataIndex: 'txnDate', key: 'txnDate', width: 100 },
  { title: '金额(元)', key: 'amount', width: 110, align: 'right' },
  { title: '方式', dataIndex: 'payMethod', key: 'payMethod', width: 90 },
  { title: '流水号', dataIndex: 'txnNo', key: 'txnNo', width: 130, ellipsis: true },
  { title: '核销节点', key: 'allocations' },
  { title: '经办人', dataIndex: 'operatorName', key: 'operatorName', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 120, ellipsis: true },
  { title: '登记时间', key: 'createTime', width: 130 },
  { title: '操作', key: 'op', width: 60 }
]
const entrustColumns = [
  { title: '序号', key: 'seq', width: 60 },
  { title: '委托单号', key: 'entrustNo', width: 150 },
  { title: '委托名称', key: 'entrustName', width: 240, ellipsis: true },
  { title: '客户', dataIndex: 'custName', key: 'custName', width: 180, ellipsis: true },
  { title: '来源', dataIndex: 'sourceName', key: 'sourceName', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 100 },
  { title: '状态', key: 'status', width: 110 },
  { title: '创建人', key: 'createName', width: 90 },
  { title: '创建时间', key: 'createTime', width: 130 }
]
const canDeleteTxn = computed(() => hasPerm('ems:contract:finance') || hasPerm('ems:contract:delete'))

// ---------- 状态操作 ----------
function handleSubmit() {
  Modal.confirm({
    title: '提交合同',
    content: `确认提交合同【${vo.value.contractName}】？提交后进入执行中状态。`,
    onOk: () => submitContract(contractId).then(() => { message.success('提交成功'); load() }).catch(() => {})
  })
}
function handleSuspend() {
  let reason = ''
  Modal.confirm({
    title: '中止合同',
    content: () => h('div', [
      h('p', { style: 'margin-bottom:8px' }, '确认中止该合同？中止后禁止新增收付款登记。'),
      h('textarea', {
        class: 'ant-input', rows: 3, placeholder: '请填写中止原因',
        onChange: (e) => { reason = e.target.value }
      })
    ]),
    onOk: () => suspendContract(contractId, reason).then(() => { message.success('已中止'); load() }).catch(() => {})
  })
}
function handleResume() {
  Modal.confirm({
    title: '恢复合同',
    content: '确认恢复该合同执行？',
    onOk: () => resumeContract(contractId).then(() => { message.success('已恢复'); load() }).catch(() => {})
  })
}
function handleCancel() {
  Modal.confirm({
    title: '作废合同',
    content: '确认作废该合同？作废后不可恢复；已发生收付款登记的合同仅可中止。',
    okType: 'danger',
    onOk: () => cancelContract(contractId).then(() => { message.success('已作废'); load() }).catch(() => {})
  })
}
function handleDeleteTxn(txn) {
  Modal.confirm({
    title: '撤销登记',
    content: `确认撤销 ${txn.txnDate} 的 ${money(txn.amount)} 元登记？撤销后节点核销进度与已收付金额将自动回退。`,
    okType: 'danger',
    onOk: () => deleteContractTxn(txn.id).then(() => { message.success('已撤销'); load() }).catch(() => {})
  })
}

// ---------- 收款/支付登记 ----------
const txnModalVisible = ref(false)
const txnSaving = ref(false)
const txnForm = reactive({ txnDate: null, amount: null, payMethod: undefined, txnNo: '', remark: '' })
const allocRows = ref([])
const allocColumns = [
  { title: '期数', dataIndex: 'seq', key: 'seq', width: 60 },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 130 },
  { title: '计划金额(元)', key: 'planAmount', width: 110, align: 'right' },
  { title: '待核销(元)', key: 'remaining', width: 110, align: 'right' },
  { title: '本次分摊(元)', key: 'allocAmount', width: 140 }
]
const allocSum = computed(() => allocRows.value.reduce((s, r) => s + Number(r.allocAmount || 0), 0))
const allocSumError = computed(() => {
  if (!txnForm.amount) return false
  return Math.abs(allocSum.value - Number(txnForm.amount || 0)) > 0.001
})

function openTxnModal() {
  Object.assign(txnForm, { txnDate: dayjs().format('YYYY-MM-DD'), amount: null, payMethod: undefined, txnNo: '', remark: '' })
  // 可核销节点：未收讫/付讫的节点，按计划金额-已核销计算剩余
  allocRows.value = (vo.value.nodes || [])
    .filter((n) => n.status !== '已收讫' && n.status !== '已付讫')
    .map((n) => ({
      id: n.id, seq: n.seq, nodeName: n.nodeName,
      planAmount: Number(n.planAmount || 0),
      remaining: Math.max(0, Number(n.planAmount || 0) - Number(n.allocatedAmount || 0)),
      allocAmount: 0
    }))
  getDictItems('pay_method').then((res) => { payMethodOptions.value = res.data || res || [] }).catch(() => {})
  txnModalVisible.value = true
}

/** 按节点顺序自动分摊：依次填满各节点剩余金额 */
function autoAllocate() {
  let rest = Number(txnForm.amount || 0)
  for (const row of allocRows.value) {
    const fill = Math.min(rest, row.remaining)
    row.allocAmount = fill
    rest -= fill
    if (rest <= 0.001) break
  }
}

function handleTxnSave() {
  if (!txnForm.txnDate) return message.warning('请选择日期')
  if (!txnForm.amount || txnForm.amount <= 0) return message.warning('请输入登记金额')
  if (!txnForm.payMethod) return message.warning('请选择收付款方式')
  const rows = allocRows.value.filter((r) => Number(r.allocAmount || 0) > 0)
  if (!rows.length) return message.warning('请至少为一个节点分摊金额')
  if (allocSumError.value) return message.warning('分摊合计必须等于登记金额')
  txnSaving.value = true
  addContractTxn(contractId, {
    txnDate: txnForm.txnDate,
    amount: txnForm.amount,
    payMethod: txnForm.payMethod,
    txnNo: txnForm.txnNo,
    remark: txnForm.remark,
    allocations: rows.map((r) => ({ nodeId: r.id, amount: r.allocAmount }))
  }).then(() => {
    message.success('登记成功')
    txnModalVisible.value = false
    load()
  }).catch(() => {}).finally(() => { txnSaving.value = false })
}

// ---------- 节点计划编辑 ----------
const nodeModalVisible = ref(false)
const nodeSaving = ref(false)
let nodeUid = 0
const nodeEditRows = ref([])
const nodeEditColumns = [
  { title: '#', key: 'seq', width: 46 },
  { title: '节点名称', key: 'nodeName', width: 150 },
  { title: '计划金额(元)', key: 'planAmount', width: 120 },
  { title: '计划日期', key: 'planDate', width: 150 },
  { title: '节点说明', key: 'nodeDesc' },
  { title: '操作', key: 'op', width: 60 }
]
const nodeSum = computed(() => nodeEditRows.value.reduce((s, n) => s + Number(n.planAmount || 0), 0))
const nodeSumError = computed(() =>
  nodeEditRows.value.length > 0 && Math.abs(nodeSum.value - Number(vo.value.amount || 0)) > 0.001)

function openNodeModal() {
  nodeEditRows.value = (vo.value.nodes || []).map((n) => ({
    _uid: ++nodeUid, id: n.id, nodeName: n.nodeName,
    planAmount: Number(n.planAmount || 0), planDate: n.planDate, nodeDesc: n.nodeDesc,
    hasAlloc: Number(n.allocatedAmount || 0) > 0
  }))
  nodeModalVisible.value = true
}
function addNodeRow() {
  nodeEditRows.value.push({ _uid: ++nodeUid, id: null, nodeName: '', planAmount: null, planDate: null, nodeDesc: '' })
}
function removeNodeRow(record) {
  if (record.hasAlloc) {
    message.warning('该节点已产生收付款登记，不允许删除，请先撤销对应登记')
    return
  }
  nodeEditRows.value = nodeEditRows.value.filter((n) => n._uid !== record._uid)
}
function handleNodeSave() {
  if (!nodeEditRows.value.length) return message.warning('请至少维护 1 个节点')
  if (nodeSumError.value) return message.warning('节点计划金额合计必须等于合同金额')
  for (const n of nodeEditRows.value) {
    if (!n.nodeName) return message.warning('请填写全部节点名称')
    if (!n.planAmount || n.planAmount <= 0) return message.warning('节点计划金额必须大于0')
    if (!n.planDate) return message.warning('请选择全部节点的计划日期')
  }
  nodeSaving.value = true
  saveContractNodes(contractId, nodeEditRows.value.map((n) => ({
    id: n.id || null, nodeName: n.nodeName, planAmount: n.planAmount,
    planDate: n.planDate, nodeDesc: n.nodeDesc
  }))).then(() => {
    message.success('节点已更新')
    nodeModalVisible.value = false
    load()
  }).catch(() => {}).finally(() => { nodeSaving.value = false })
}

// ---------- 导航 ----------
function goBack() {
  router.back()
}
function goEntrust(record) {
  router.push({ path: '/ems/base/entrust', query: { detailId: record.entrustId, tab: 'base' } })
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 0 4px;
}
.page-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 10px;
  /* 信息头固定：吸附在滚动视口顶部，其余内容超长时由其下方滚动 */
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f0f4f9;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.page-sub {
  margin-left: 10px;
  font-size: 13px;
  font-weight: 400;
  color: #8c8c8c;
}
.btn-icon {
  font-weight: 700;
}
.toolbar-actions {
  margin-left: auto;
}
.block {
  margin-bottom: 16px;
}
.warn-text { color: #fa8c16; }
.danger-text { color: #f5222d; }
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
.desc-text {
  white-space: pre-wrap;
  color: #434343;
}
.progress-text {
  margin-left: 8px;
  font-size: 12px;
  color: #8c8c8c;
}
/* 附件网格 */
.attach-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}
.attach-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fafcff;
}
.attach-icon {
  font-size: 20px;
  color: #2563EB;
}
.attach-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.attach-download {
  color: #2563EB;
}
.empty-tip {
  color: #bfbfbf;
  padding: 4px 0;
}
/* 历史记录 */
.hist-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hist-content {
  color: #434343;
}
.hist-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #8c8c8c;
}
/* 分摊与节点编辑 */
.alloc-toolbar,
.node-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.alloc-sum,
.node-sum {
  font-size: 13px;
  color: #52c41a;
}
.alloc-sum.sum-error,
.node-sum.sum-error { color: #f5222d; }
</style>
