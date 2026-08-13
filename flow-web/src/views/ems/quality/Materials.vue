<template>
  <div class="page-container">
    <a-tabs v-model:activeKey="tab">
      <!-- 标准物质 -->
      <a-tab-pane key="material" tab="标准物质">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button v-if="hasPerm('ems:quality:create')" type="primary" @click="openMaterial()">+ 新增标物</a-button>
            <a-input-search v-model:value="mk" placeholder="名称搜索" style="width:200px" @search="loadMaterials" allow-clear />
            <a-select v-model:value="mStatus" style="width:140px" @change="loadMaterials">
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="在库">在库</a-select-option>
              <a-select-option value="临期">临期</a-select-option>
              <a-select-option value="过期">过期</a-select-option>
            </a-select>
            <a-button @click="checkGate">效期闸门校验</a-button>
          </a-space>
          <a-table :columns="materialCols" :data-source="materials" row-key="id" :pagination="mp" :loading="ml">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status==='过期'?'red':(record.status==='临期'?'orange':'green')">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a v-if="hasPerm('ems:quality:update')" @click="openMaterial(record)">编辑</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- 耗材 -->
      <a-tab-pane key="consumable" tab="耗材">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button v-if="hasPerm('ems:quality:create')" type="primary" @click="openConsumable()">+ 新增耗材</a-button>
            <a-input-search v-model:value="ck" placeholder="名称搜索" style="width:200px" @search="loadConsumables" allow-clear />
            <a-select v-model:value="cStatus" style="width:140px" @change="loadConsumables">
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="在库">在库</a-select-option>
              <a-select-option value="临期">临期</a-select-option>
              <a-select-option value="过期">过期</a-select-option>
            </a-select>
          </a-space>
          <a-table :columns="consumableCols" :data-source="consumables" row-key="id" :pagination="cp" :loading="cl">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status==='过期'?'red':(record.status==='临期'?'orange':'green')">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a v-if="hasPerm('ems:quality:update')" @click="openConsumable(record)">编辑</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- 标物表单 -->
    <a-modal v-model:open="mVisible" :title="mForm.id?'编辑标准物质':'新增标准物质'" @ok="submitMaterial">
      <a-form :model="mForm" layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="mForm.name" /></a-form-item>
        <a-form-item label="批号"><a-input v-model:value="mForm.lotNo" /></a-form-item>
        <a-form-item label="规格"><a-input v-model:value="mForm.spec" /></a-form-item>
        <a-form-item label="效期"><a-date-picker v-model:value="mExpire" style="width:100%" /></a-form-item>
        <a-form-item label="库存"><a-input-number v-model:value="mForm.stock" :min="0" style="width:100%" /></a-form-item>
        <a-form-item label="证书编号"><a-input v-model:value="mForm.certNo" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 耗材表单 -->
    <a-modal v-model:open="cVisible" :title="cForm.id?'编辑耗材':'新增耗材'" @ok="submitConsumable">
      <a-form :model="cForm" layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="cForm.name" /></a-form-item>
        <a-form-item label="规格"><a-input v-model:value="cForm.spec" /></a-form-item>
        <a-form-item label="数量"><a-input-number v-model:value="cForm.qty" :min="0" style="width:100%" /></a-form-item>
        <a-form-item label="效期"><a-date-picker v-model:value="cExpire" style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { saveMaterial, getMaterials, saveConsumable, getConsumables, checkMaterialGate } from '../../../api/ems'
import { usePermission } from '../../../composables/usePermission'

const { hasPerm } = usePermission()
const tab = ref('material')
const ml = ref(false), cl = ref(false)
const mk = ref(''), mStatus = ref('')
const ck = ref(''), cStatus = ref('')
const materials = ref([]), consumables = ref([])
const mp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { mp.current = p; loadMaterials() } })
const cp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { cp.current = p; loadConsumables() } })

const materialCols = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '批号', dataIndex: 'lotNo', key: 'lotNo' },
  { title: '规格', dataIndex: 'spec', key: 'spec' },
  { title: '效期', dataIndex: 'expireDate', key: 'expireDate' },
  { title: '库存', dataIndex: 'stock', key: 'stock' },
  { title: '状态', key: 'status' },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo' },
  { title: '操作', key: 'action' }
]
const consumableCols = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '规格', dataIndex: 'spec', key: 'spec' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '效期', dataIndex: 'expireDate', key: 'expireDate' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

const mVisible = ref(false), cVisible = ref(false)
const mForm = reactive({ id: null, name: '', lotNo: '', spec: '', expireDate: '', stock: 0, certNo: '' })
const cForm = reactive({ id: null, name: '', spec: '', qty: 0, expireDate: '' })
const mExpire = ref(null), cExpire = ref(null)

function toStr(d) { return d ? (d.$d ? d.format('YYYY-MM-DD') : d) : null }
function openMaterial(r) {
  if (r) { Object.assign(mForm, r); mExpire.value = r.expireDate ? dayjs(r.expireDate) : null }
  else { Object.keys(mForm).forEach(k => mForm[k] = k==='stock'?0:null); mExpire.value = null }
  mVisible.value = true
}
function openConsumable(r) {
  if (r) { Object.assign(cForm, r); cExpire.value = r.expireDate ? dayjs(r.expireDate) : null }
  else { Object.keys(cForm).forEach(k => cForm[k] = k==='qty'?0:null); cExpire.value = null }
  cVisible.value = true
}
async function submitMaterial() {
  mForm.expireDate = toStr(mExpire.value)
  await saveMaterial({ ...mForm }); message.success('已保存'); mVisible.value = false; loadMaterials()
}
async function submitConsumable() {
  cForm.expireDate = toStr(cExpire.value)
  await saveConsumable({ ...cForm }); message.success('已保存'); cVisible.value = false; loadConsumables()
}
async function loadMaterials() {
  ml.value = true
  try {
    const res = await getMaterials({ keyword: mk.value, status: mStatus.value, page: mp.current, size: mp.pageSize })
    const p = res.data || res; materials.value = p.records || p.list || []; mp.total = p.total || materials.value.length
  } finally { ml.value = false }
}
async function loadConsumables() {
  cl.value = true
  try {
    const res = await getConsumables({ keyword: ck.value, status: cStatus.value, page: cp.current, size: cp.pageSize })
    const p = res.data || res; consumables.value = p.records || p.list || []; cp.total = p.total || consumables.value.length
  } finally { cl.value = false }
}
async function checkGate() {
  const res = await checkMaterialGate()
  const g = res.data || res
  if (g.pass) message.success('物资效期闸门通过')
  else message.warning(`有 ${g.blocked.length} 项标物临近/已过期`)
}

onMounted(() => { loadMaterials(); loadConsumables() })
</script>
