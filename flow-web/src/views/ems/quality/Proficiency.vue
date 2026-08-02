<template>
  <div class="page-container">
    <a-tabs v-model:activeKey="tab">
      <a-tab-pane key="proficiency" tab="能力验证">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openProf()">+ 新增</a-button>
          </a-space>
          <a-table :columns="profCols" :data-source="prof" row-key="id" :pagination="pp" />
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="interlab" tab="实验室间比对">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openInter()">+ 新增</a-button>
          </a-space>
          <a-table :columns="interCols" :data-source="inter" row-key="id" :pagination="ip" />
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="repeat" tab="重复性试验">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openRep()">+ 新增</a-button>
          </a-space>
          <a-table :columns="repCols" :data-source="rep" row-key="id" :pagination="rp" />
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal v-model:open="profVisible" title="新增能力验证" @ok="saveProf">
      <a-form :model="profForm" layout="vertical">
        <a-form-item label="外部机构"><a-input v-model:value="profForm.org" /></a-form-item>
        <a-form-item label="项目"><a-input v-model:value="profForm.item" /></a-form-item>
        <a-form-item label="结果"><a-input v-model:value="profForm.result" /></a-form-item>
        <a-form-item label="结论"><a-select v-model:value="profForm.conclusion"><a-select-option value="合格">合格</a-select-option><a-select-option value="不合格">不合格</a-select-option></a-select></a-form-item>
        <a-form-item label="验证日期"><a-date-picker v-model:value="profDate" style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="interVisible" title="新增实验室间比对" @ok="saveInter">
      <a-form :model="interForm" layout="vertical">
        <a-form-item label="合作实验室"><a-input v-model:value="interForm.partnerLab" /></a-form-item>
        <a-form-item label="项目"><a-input v-model:value="interForm.item" /></a-form-item>
        <a-form-item label="我方值"><a-input v-model:value="interForm.ourValue" /></a-form-item>
        <a-form-item label="参考值"><a-input v-model:value="interForm.refValue" /></a-form-item>
        <a-form-item label="结论"><a-select v-model:value="interForm.conclusion"><a-select-option value="合格">合格</a-select-option><a-select-option value="不合格">不合格</a-select-option></a-select></a-form-item>
        <a-form-item label="比对日期"><a-date-picker v-model:value="interDate" style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="repVisible" title="新增重复性试验" @ok="saveRep">
      <a-form :model="repForm" layout="vertical">
        <a-form-item label="项目"><a-input v-model:value="repForm.item" /></a-form-item>
        <a-form-item label="首次值"><a-input v-model:value="repForm.firstValue" /></a-form-item>
        <a-form-item label="重复值"><a-input v-model:value="repForm.repeatValue" /></a-form-item>
        <a-form-item label="结论"><a-select v-model:value="repForm.conclusion"><a-select-option value="合格">合格</a-select-option><a-select-option value="不合格">不合格</a-select-option></a-select></a-form-item>
        <a-form-item label="试验日期"><a-date-picker v-model:value="repDate" style="width:100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { saveProficiency, getProficiency, saveInterlab, getInterlab, saveRepeat, getRepeat } from '../../../api/ems'

const tab = ref('proficiency')
const prof = ref([]), inter = ref([]), rep = ref([])
const pp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { pp.current = p; loadProf() } })
const ip = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { ip.current = p; loadInter() } })
const rp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { rp.current = p; loadRep() } })

const profCols = [
  { title: '机构', dataIndex: 'org', key: 'org' },
  { title: '项目', dataIndex: 'item', key: 'item' },
  { title: '结果', dataIndex: 'result', key: 'result' },
  { title: '结论', dataIndex: 'conclusion', key: 'conclusion' },
  { title: '日期', dataIndex: 'testDate', key: 'testDate' }
]
const interCols = [
  { title: '实验室', dataIndex: 'partnerLab', key: 'partnerLab' },
  { title: '项目', dataIndex: 'item', key: 'item' },
  { title: '我方值', dataIndex: 'ourValue', key: 'ourValue' },
  { title: '参考值', dataIndex: 'refValue', key: 'refValue' },
  { title: '结论', dataIndex: 'conclusion', key: 'conclusion' },
  { title: '日期', dataIndex: 'compareDate', key: 'compareDate' }
]
const repCols = [
  { title: '项目', dataIndex: 'item', key: 'item' },
  { title: '首次值', dataIndex: 'firstValue', key: 'firstValue' },
  { title: '重复值', dataIndex: 'repeatValue', key: 'repeatValue' },
  { title: '结论', dataIndex: 'conclusion', key: 'conclusion' },
  { title: '日期', dataIndex: 'testDate', key: 'testDate' }
]

const profVisible = ref(false), interVisible = ref(false), repVisible = ref(false)
const profForm = reactive({ id: null, org: '', item: '', result: '', conclusion: '合格' })
const interForm = reactive({ id: null, partnerLab: '', item: '', ourValue: '', refValue: '', conclusion: '合格' })
const repForm = reactive({ id: null, item: '', firstValue: '', repeatValue: '', conclusion: '合格' })
const profDate = ref(null), interDate = ref(null), repDate = ref(null)
const toStr = (d) => d ? d.format('YYYY-MM-DD') : null

function openProf() { Object.keys(profForm).forEach(k => profForm[k] = k==='conclusion'?'合格':null); profDate.value=null; profVisible.value=true }
function openInter() { Object.keys(interForm).forEach(k => interForm[k] = k==='conclusion'?'合格':null); interDate.value=null; interVisible.value=true }
function openRep() { Object.keys(repForm).forEach(k => repForm[k] = k==='conclusion'?'合格':null); repDate.value=null; repVisible.value=true }

async function saveProf() {
  await saveProficiency({ ...profForm, testDate: toStr(profDate.value) }); message.success('已保存'); profVisible.value=false; loadProf()
}
async function saveInter() {
  await saveInterlab({ ...interForm, compareDate: toStr(interDate.value) }); message.success('已保存'); interVisible.value=false; loadInter()
}
async function saveRep() {
  await saveRepeat({ ...repForm, testDate: toStr(repDate.value) }); message.success('已保存'); repVisible.value=false; loadRep()
}

async function loadProf() { const r = await getProficiency({ page: pp.current, size: pp.pageSize }); const p=r.data||r; prof.value=p.records||p.list||[]; pp.total=p.total||prof.value.length }
async function loadInter() { const r = await getInterlab({ page: ip.current, size: ip.pageSize }); const p=r.data||r; inter.value=p.records||p.list||[]; ip.total=p.total||inter.value.length }
async function loadRep() { const r = await getRepeat({ page: rp.current, size: rp.pageSize }); const p=r.data||r; rep.value=p.records||p.list||[]; rp.total=p.total||rep.value.length }

onMounted(() => { loadProf(); loadInter(); loadRep() })
</script>
