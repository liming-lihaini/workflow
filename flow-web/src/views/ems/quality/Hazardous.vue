<template>
  <div class="page-container">
    <a-card title="危化品台账" :bordered="false">
      <a-space style="margin-bottom:16px">
        <a-button type="primary" @click="openForm()">+ 新增危化品</a-button>
        <a-input-search v-model:value="kw" placeholder="名称/CAS搜索" style="width:220px" @search="load" allow-clear />
        <a-select v-model:value="status" style="width:140px" @change="load">
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="在库">在库</a-select-option>
          <a-select-option value="待审批">待审批</a-select-option>
          <a-select-option value="已领用">已领用</a-select-option>
          <a-select-option value="已报废">已报废</a-select-option>
        </a-select>
      </a-space>
      <a-table :columns="cols" :data-source="rows" row-key="id" :pagination="pg" :loading="loading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status==='已报废'?'red':(record.status==='待审批'?'orange':(record.status==='已领用'?'blue':'green'))">
              {{ record.status }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a v-if="record.status==='在库'" @click="apply(record)">申请</a>
              <a v-if="record.status==='待审批'" @click="approve(record)">审批</a>
              <a v-if="record.status==='在库'" style="color:#ff4d4f" @click="startScrapProcess(record)">报废</a>
              <a @click="openForm(record)">编辑</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="visible" :title="form.id?'编辑危化品':'新增危化品'" @ok="save">
      <a-form :model="form" layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="CAS号"><a-input v-model:value="form.casNo" /></a-form-item>
        <a-form-item label="类别"><a-input v-model:value="form.category" placeholder="易燃/腐蚀/有毒/易爆" /></a-form-item>
        <a-form-item label="数量"><a-input v-model:value="form.qty" /></a-form-item>
        <a-form-item label="单位"><a-input v-model:value="form.unit" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="applyVisible" title="申请领用/报废" @ok="doApply">
      <a-form layout="vertical">
        <a-form-item label="申请人"><a-input v-model:value="applyBy" /></a-form-item>
        <a-form-item label="用途/原因"><a-textarea v-model:value="applyReason" :rows="3" /></a-form-item>
        <a-form-item label="目标状态">
          <a-radio-group v-model:value="targetStatus">
            <a-radio value="已领用">领用</a-radio>
            <a-radio value="已报废">报废</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="approveVisible" title="审批" @ok="doApprove">
      <a-form layout="vertical">
        <a-form-item label="审批人"><a-input v-model:value="approveBy" /></a-form-item>
        <a-form-item label="意见"><a-textarea v-model:value="approveOpinion" :rows="3" /></a-form-item>
        <a-form-item label="结果">
          <a-radio-group v-model:value="approveOk">
            <a-radio :value="true">通过</a-radio>
            <a-radio :value="false">退回</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { saveHazardous, getHazardous, applyHazardous, approveHazardous } from '../../../api/ems'

const router = useRouter()

const loading = ref(false)
const kw = ref(''), status = ref('')
const rows = ref([])
const pg = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { pg.current = p; load() } })
const cols = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: 'CAS号', dataIndex: 'casNo', key: 'casNo' },
  { title: '类别', dataIndex: 'category', key: 'category' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '申请人', dataIndex: 'applyBy', key: 'applyBy' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

const visible = ref(false)
const form = reactive({ id: null, name: '', casNo: '', category: '', qty: '', unit: '' })
function openForm(r) {
  if (r) Object.assign(form, r)
  else Object.keys(form).forEach(k => form[k] = null)
  visible.value = true
}
async function save() {
  await saveHazardous({ ...form }); message.success('已保存'); visible.value = false; load()
}

const applyVisible = ref(false), approveVisible = ref(false)
const cur = ref(null)
const applyBy = ref(''), applyReason = ref(''), targetStatus = ref('已领用')
const approveBy = ref(''), approveOpinion = ref(''), approveOk = ref(true)
function apply(r) { cur.value = r; applyVisible.value = true }
function approve(r) { cur.value = r; approveVisible.value = true }
async function doApply() {
  await applyHazardous(cur.value.id, { applyBy: applyBy.value, applyReason: applyReason.value, targetStatus: targetStatus.value })
  message.success('已提交审批'); applyVisible.value = false; load()
}
async function doApprove() {
  await approveHazardous(cur.value.id, { approveBy: approveBy.value, approveOpinion: approveOpinion.value, approve: approveOk.value })
  message.success('审批完成'); approveVisible.value = false; load()
}

// 发起资产报废申请(ZCBFSQ)：跳转流程发起详情页，预填资产类型/资产ID/名称/CAS号；
// 申请人需在表单中说明报废原因与处置方式，审批通过后由 Webhook 更新台账状态为已报废
function startScrapProcess(record) {
  const query = { processKey: 'ZCBFSQ', assetType: '危化品', assetId: record.id }
  if (record.name) query.name = record.name
  if (record.casNo) query.spec = record.casNo
  router.push({ path: '/task/start-detail', query })
}

async function load() {
  loading.value = true
  try {
    const res = await getHazardous({ keyword: kw.value, status: status.value, page: pg.current, size: pg.pageSize })
    const p = res.data || res; rows.value = p.records || p.list || []; pg.total = p.total || rows.value.length
  } finally { loading.value = false }
}
onMounted(load)
</script>
