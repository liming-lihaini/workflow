<template>
  <div class="page">
    <a-card :bordered="false">
      <template #title>收样工作台</template>
      <template #extra>
        <a-badge :count="stats.pending" :number-style="{ backgroundColor: '#fa8c16' }" />
        <span style="margin-left:8px;color:#888;">待收样样品</span>
        <a-button type="primary" style="margin-left:16px;" @click="openCollect">手动收集样品</a-button>
        <a-button type="default" style="margin-left:8px;" @click="loadData">刷新</a-button>
      </template>

      <a-alert
        v-if="stats.pending > 0"
        type="warning"
        show-icon
        style="margin-bottom:16px;"
        :message="`当前有 ${stats.pending} 个样品待收样，请核对保存条件后登记收样`"
      />

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="false"></template>
          <template v-else-if="column.key === 'barcode'">
            <a-button type="link" style="padding:0;" @click="openDetail(record)">{{ record.barcode }}</a-button>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag color="orange">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openReceive(record)">登记收样</a-button>
            <a-button type="link" @click="openDetail(record)">详情</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 收样弹窗 -->
    <a-modal
      v-model:open="receiveOpen"
      title="登记收样"
      @ok="submitReceive"
      @cancel="receiveOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="样品条码">
          <span>{{ current?.barcode }}</span>
        </a-form-item>
        <a-form-item label="样品名称">
          <span>{{ current?.name }}</span>
        </a-form-item>
        <a-form-item label="数量规格">
          <a-input v-model:value="receiveForm.amount" placeholder="如 500mL / 1kg" />
        </a-form-item>
        <a-form-item label="保存容器">
          <a-input v-model:value="receiveForm.container" placeholder="如 聚乙烯瓶 / 玻璃瓶" />
        </a-form-item>
        <a-form-item label="保存条件">
          <a-input v-model:value="receiveForm.preserve" :placeholder="current?.preserve || '如 冷藏/避光/4℃'" />
        </a-form-item>
        <a-form-item label="收样人" required>
          <a-input v-model:value="receiveForm.receiveBy" placeholder="请输入收样人" />
        </a-form-item>
        <a-form-item label="收样时间">
          <a-date-picker
            v-model:value="receiveDate"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="receiveForm.remark" rows="2" />
        </a-form-item>

        <!-- 留样面板 -->
        <a-divider class="title-divider" orientation="left">留样信息</a-divider>
        <a-card size="small" :bordered="true" style="margin-bottom:8px;">
          <a-form-item label="是否留样" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-switch v-model:checked="retainChecked" checked-children="留样" un-checked-children="不留样" />
          </a-form-item>
          <template v-if="retainChecked">
            <a-form-item label="留样保存天数" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-input-number v-model:value="receiveForm.retainDays" :min="1" :precision="0" style="width:100%" placeholder="请输入留样天数" />
            </a-form-item>
            <a-form-item label="留样人" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-select
                v-model:value="receiveForm.retainBy"
                show-search
                placeholder="选择留样人"
                :options="userOptions"
                :filter-option="filterUser"
                style="width:100%"
              />
            </a-form-item>
            <a-form-item label="留样日期" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-date-picker
                v-model:value="retainDate"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </a-form-item>
            <a-form-item label="存放位置" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="receiveForm.retainLocation" placeholder="如 留样室A区3层货架2号" style="width:100%" />
            </a-form-item>
          </template>
        </a-card>
      </a-form>
    </a-modal>

    <!-- 手动收集样品（右侧抽屉，宽 1000px） -->
    <a-drawer
      v-model:open="collectOpen"
      title="手动收集样品"
      width="1000"
      :mask-closable="false"
      @close="collectOpen = false"
      @after-visible-change="onCollectVisibleChange"
    >
      <a-steps :current="stepCurrent" size="small" style="margin-bottom:16px;">
        <a-step title="基本信息" />
        <a-step title="采样信息" />
        <a-step title="固定剂/质控" />
        <a-step title="现场照片" />
      </a-steps>

      <a-form layout="horizontal" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" :model="form">
        <!-- 基本信息 -->
        <a-divider class="title-divider" orientation="left">基本信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="采样派单" required>
              <a-select
                v-model:value="form.dispatchId"
                show-search
                placeholder="选择已派单状态的采样派单"
                :options="dispatchOptions"
                :filter-option="filterDispatch"
                style="width:100%"
                @change="onDispatchChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="委托单">
              <span class="readonly-text">{{ form.entrustName || '—' }}</span>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位信息" required>
              <a-select
                v-model:value="form.custName"
                show-search
                placeholder="选择派单中委托单所属单位"
                :options="custOptions"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="监测点位" required>
              <a-select
                v-model:value="form.pointId"
                placeholder="选择监测点位"
                :options="pointOptions"
                :disabled="!form.dispatchId"
                style="width:100%"
                @change="onPointChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检测类别">
              <a-input v-model:value="form.category" placeholder="如 水质/大气/土壤（可手动补充）" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检测项目" required>
              <a-select
                v-model:value="form.item"
                placeholder="选择检测项目（监测因子）"
                :options="itemOptions"
                mode="multiple"
                :disabled="!form.pointId"
                style="width:100%"
                @change="onItemChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="样品类型">
              <a-select
                v-model:value="form.sampleType"
                placeholder="选择样品类型"
                :options="sampleTypeOptions"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采样天气状况">
              <a-input v-model:value="form.weather" placeholder="如 晴/多云/小雨/风力3级" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数量规格">
              <a-input v-model:value="form.amount" placeholder="如 500mL / 1kg" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="保存容器">
              <a-input v-model:value="form.container" placeholder="如 聚乙烯瓶 / 玻璃瓶" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="保存条件">
              <a-input v-model:value="form.preserve" placeholder="如 冷藏/避光/4℃" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 采样信息 -->
        <a-divider class="title-divider" orientation="left">采样信息</a-divider>
        <a-alert
          v-if="!form.item || form.item.length === 0"
          type="info"
          show-icon
          style="margin-bottom:12px;"
          message="选择检测项目后，系统自动加载该项目的采样参数配置，并渲染为采样表单。"
        />
        <a-row :gutter="16">
          <template v-for="p in selectedParams" :key="p.code + (p.item || '')">
            <a-col :span="12">
              <a-form-item :label="p.name">
                <a-input
                  v-model:value="p.value"
                  :placeholder="p.item ? p.item + ' · ' + p.name : p.name"
                  style="width:100%"
                >
                  <template v-if="p.unit" #addonAfter>{{ p.unit }}</template>
                </a-input>
              </a-form-item>
            </a-col>
          </template>
        </a-row>

        <!-- 固定剂 / 现场质控 -->
        <a-divider class="title-divider" orientation="left">固定剂</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="固定剂">
              <a-select
                v-model:value="form.preservatives"
                mode="multiple"
                placeholder="多选，数据来源数据字典"
                :options="preservativeOptions"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="质控方式">
              <a-select
                v-model:value="form.qcTypes"
                mode="multiple"
                placeholder="多选，数据来源数据字典"
                :options="qcTypeOptions"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 留样面板 -->
        <a-divider class="title-divider" orientation="left">留样信息</a-divider>
        <a-card size="small" :bordered="true" style="margin-bottom:8px;">
          <a-form-item label="是否留样" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-switch v-model:checked="retainChecked" checked-children="留样" un-checked-children="不留样" />
          </a-form-item>
          <template v-if="retainChecked">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="留样保存天数" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
                  <a-input-number v-model:value="form.retainDays" :min="1" :precision="0" style="width:100%" placeholder="请输入留样天数" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="留样人" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
                  <a-select
                    v-model:value="form.retainBy"
                    show-search
                    placeholder="选择留样人"
                    :options="userOptions"
                    :filter-option="filterUser"
                    style="width:100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="留样日期" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
                  <a-date-picker
                    v-model:value="retainDate"
                    value-format="YYYY-MM-DD"
                    style="width:100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="存放位置" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
                  <a-input v-model:value="form.retainLocation" placeholder="如 留样室A区3层货架2号" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
          </template>
        </a-card>

        <!-- 现场照片 -->
        <a-divider class="title-divider" orientation="left">现场采样照片</a-divider>
        <div class="photo-block">
          <a-upload
            list-type="picture-card"
            :file-list="fileList"
            :before-upload="beforeUpload"
            :custom-request="customUpload"
            @preview="handlePreview"
            @remove="handleRemove"
            multiple
            accept="image/*"
          >
            <div v-if="fileList.length < 20">
              <plus-outlined />
              <div style="margin-top:8px;">上传</div>
            </div>
          </a-upload>
          <a-modal :open="previewOpen" :footer="null" @cancel="previewOpen = false">
            <img alt="preview" style="width:100%" :src="previewImage" />
          </a-modal>
          <div class="hint">支持批量选择图片，上传后显示缩略图，点击可放大预览。</div>
        </div>
      </a-form>

      <template #footer>
        <a-button style="margin-right:8px;" @click="collectOpen = false">取消</a-button>
        <a-button type="primary" :loading="collecting" @click="submitCollect">确定保存</a-button>
      </template>
    </a-drawer>

    <!-- 样品详情抽屉 -->
    <a-drawer
      v-model:open="detailOpen"
      title="样品详情"
      width="720"
      @close="detailOpen = false"
    >
      <a-descriptions bordered :column="2" size="small" v-if="detail">
        <a-descriptions-item label="样品条码">{{ detail.sample?.barcode }}</a-descriptions-item>
        <a-descriptions-item label="样品名称">{{ detail.sample?.name }}</a-descriptions-item>
        <a-descriptions-item label="样品类型">{{ detail.sample?.type }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ detail.sample?.source }}</a-descriptions-item>
        <a-descriptions-item label="数量/规格">{{ detail.sample?.amount }}</a-descriptions-item>
        <a-descriptions-item label="保存容器">{{ detail.sample?.container }}</a-descriptions-item>
        <a-descriptions-item label="保存条件">{{ detail.sample?.preserve }}</a-descriptions-item>
        <a-descriptions-item label="采样天气状况">{{ detail.sample?.weather }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag color="orange">{{ detail.sample?.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="收样人">{{ detail.sample?.receiveBy }}</a-descriptions-item>
        <a-descriptions-item label="收样时间">{{ detail.sample?.receiveTime }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="title-divider" orientation="left">留样信息</a-divider>
      <a-descriptions bordered :column="2" size="small" v-if="detail">
        <a-descriptions-item label="是否留样">
          <a-tag :color="detail.sample?.retainFlag === 1 ? 'green' : 'default'">
            {{ detail.sample?.retainFlag === 1 ? '留样' : '不留样' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="留样保存天数">{{ detail.sample?.retainDays }}</a-descriptions-item>
        <a-descriptions-item label="留样人">{{ detail.sample?.retainBy }}</a-descriptions-item>
        <a-descriptions-item label="留样日期">{{ detail.sample?.retainDate }}</a-descriptions-item>
        <a-descriptions-item label="留样到期日">{{ detail.sample?.retainUntil }}</a-descriptions-item>
        <a-descriptions-item label="存放位置">{{ detail.sample?.retainLocation }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="title-divider" orientation="left">质控样</a-divider>
      <a-table
        v-if="detail.qcList"
        :columns="qcColumns"
        :data-source="detail.qcList"
        size="small"
        row-key="id"
        :pagination="false"
      />
      <a-empty v-else description="无质控样" />

      <a-divider class="title-divider" orientation="left">操作日志</a-divider>
      <a-timeline v-if="detail.logs && detail.logs.length">
        <a-timeline-item v-for="log in detail.logs" :key="log.id">
          <b>{{ log.action }}</b> · {{ log.operator }} · {{ log.detail }}
          <span style="color:#999;">（{{ log.createTime }}）</span>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无日志" />
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message, Upload } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  getReceiveWorkbench,
  receiveSample,
  getSampleDetail,
  getCollectDispatchList,
  getEntrust,
  getSampleParamConfigs,
  getDictItems,
  collectSample,
  uploadSamplePhoto
} from '../../../api/ems'
import { getUsers } from '../../../api/system'

const loading = ref(false)
const list = ref([])
const stats = reactive({ pending: 0 })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '样品条码', dataIndex: 'barcode', key: 'barcode' },
  { title: '样品名称', dataIndex: 'name', key: 'name' },
  { title: '样品类型', dataIndex: 'type', key: 'type' },
  { title: '数量规格', dataIndex: 'amount', key: 'amount' },
  { title: '保存容器', dataIndex: 'container', key: 'container' },
  { title: '保存条件', dataIndex: 'preserve', key: 'preserve', ellipsis: true },
  { title: '采样天气', dataIndex: 'weather', key: 'weather' },
  { title: '留样', key: 'retain', width: 70,
    customRender: ({ text, record }) => (record.retainFlag === 1 ? '是' : '否') },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 160 }
]

const qcColumns = [
  { title: '样品编号', dataIndex: 'sampleNo', key: 'sampleNo' },
  { title: '质控类型', dataIndex: 'qcType', key: 'qcType' },
  { title: '备注', dataIndex: 'remark', key: 'remark' }
]

const receiveOpen = ref(false)
const submitting = ref(false)
const current = ref(null)
const receiveForm = reactive({
  receiveBy: '',
  remark: '',
  amount: '',
  container: '',
  preserve: '',
  retainDays: 1,
  retainBy: undefined,
  retainLocation: ''
})
const receiveDate = ref(null)
const receiveRetainChecked = ref(false)
const retainDate = ref(null)
const userOptions = ref([])
const filterUser = (input, option) =>
  (option.label || '').toLowerCase().includes(input.toLowerCase())

const detailOpen = ref(false)
const detail = ref(null)

// ===================== 手动收集样品（采集版） =====================
const collectOpen = ref(false)
const collecting = ref(false)
const stepCurrent = ref(0)
const retainChecked = ref(false)

const form = reactive({
  dispatchId: undefined,
  dispatchNo: '',
  entrustId: undefined,
  entrustName: '',
  custName: undefined,
  pointId: undefined,
  category: '',
  item: [],
  sampleType: undefined,
  weather: '',
  amount: '',
  container: '',
  preserve: '',
  preservatives: [],
  qcTypes: [],
  retainDays: 1,
  retainBy: undefined,
  retainLocation: '',
  receiveBy: ''
})

// 下拉数据
const dispatchOptions = ref([])
const custOptions = ref([])
const pointOptions = ref([])
const itemOptions = ref([])
const preservativeOptions = ref([])
const qcTypeOptions = ref([])
const sampleTypeOptions = ref([])

// 采样参数动态表单（按检测项目+类型从配置加载）
const selectedParams = ref([])
// 照片
const fileList = ref([])
const previewOpen = ref(false)
const previewImage = ref('')

const stepCurrentVal = computed(() => {
  if (!form.dispatchId || !form.pointId) return 0
  if (form.item.length === 0) return 1
  return 2
})
// 监听步骤高亮
watch(stepCurrentVal, (v) => { stepCurrent.value = v })

function filterDispatch(input, option) {
  return option.label && option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

async function openCollect() {
  collectOpen.value = true
  retainDate.value = todayStr()
  await loadDispatchOptions()
  await loadDicts()
  await loadUsers()
}

async function loadDispatchOptions() {
  try {
    const res = await getCollectDispatchList({ status: '已派单', size: 200 })
    const rows = res.data?.records || res.data || []
    dispatchOptions.value = rows.map(r => ({
      value: r.id,
      label: `${r.orderNo} · ${r.entrustName} · ${r.custName || ''}`,
      raw: r
    }))
  } catch (e) {
    message.error('加载采样派单失败')
  }
}

async function loadDicts() {
  try {
    const p = await getDictItems('sample_preservative')
    preservativeOptions.value = (p.data || []).map(d => ({ value: d.itemValue, label: d.itemText }))
  } catch (e) {
    console.error('[loadDicts] 固定剂字典加载失败:', e?.message || e)
    if (e?.code === 1039) {
      message.error('固定剂字典类型不存在，请联系管理员初始化「sample_preservative」')
    } else {
      message.error('固定剂字典加载失败')
    }
  }
  try {
    const q = await getDictItems('moni_qc_type')
    qcTypeOptions.value = (q.data || []).map(d => ({ value: d.itemValue, label: d.itemText }))
  } catch (e) {
    console.error('[loadDicts] 质控方式字典加载失败:', e?.message || e)
    message.error('质控方式字典加载失败')
  }
  try {
    const s = await getDictItems('moni_sample_type')
    sampleTypeOptions.value = (s.data || []).map(d => ({ value: d.itemValue, label: d.itemText }))
  } catch (e) {
    console.error('[loadDicts] 样品类型字典加载失败:', e?.message || e)
    message.error('样品类型字典加载失败')
  }
}

async function onDispatchChange(val) {
  const opt = dispatchOptions.value.find(o => o.value === val)
  const raw = opt?.raw
  pointOptions.value = []
  itemOptions.value = []
  form.pointId = undefined
  form.item = []
  form.category = ''
  selectedParams.value = []
  form.entrustName = ''
  form.custName = undefined
  custOptions.value = []
  if (!raw) return
  form.dispatchNo = raw.orderNo
  form.entrustId = raw.entrustId
  form.entrustName = raw.entrustName
  // 单位：默认带出派单所属委托单单位，并允许在下拉重选
  const cust = raw.custName && raw.custName !== '—' ? raw.custName : ''
  custOptions.value = cust ? [{ value: cust, label: cust }] : []
  form.custName = cust || undefined
  // 加载委托单点位
  if (raw.entrustId) {
    try {
      const ent = await getEntrust(raw.entrustId)
      const e = ent.data || {}
      const points = e.points || []
      pointOptions.value = points.map(pt => ({
        value: pt.id,
        label: pt.pointName,
        raw: pt
      }))
      // 若委托单单位信息更完整，补充
      if (e.custName && !custOptions.value.some(c => c.value === e.custName)) {
        custOptions.value.push({ value: e.custName, label: e.custName })
      }
    } catch (err) {
      // 忽略点位加载失败
    }
  }
}

function onPointChange() {
  form.item = []
  selectedParams.value = []
  const opt = pointOptions.value.find(o => o.value === form.pointId)
  // 检测项目（监测因子）来自点位 factors
  const factors = opt?.raw?.factors
  const arr = factors ? String(factors).split(',').map(s => s.trim()).filter(Boolean) : []
  itemOptions.value = arr.map(f => ({ value: f, label: f }))
}

async function onItemChange() {
  selectedParams.value = []
  if (!form.item || form.item.length === 0) return
  await loadSampleParams()
}

async function loadSampleParams() {
  // 按 检测类别(category) + 检测项目(item) 查询采样参数配置，渲染为采样表单
  const items = form.item
  const paramMap = new Map()
  for (const it of items) {
    try {
      const res = await getSampleParamConfigs({
        type: form.category || undefined,
        keyword: it,
        size: 100
      })
      const rows = res.data?.records || res.data || []
      rows.forEach(c => {
        const params = c.sampleParams || []
        params.forEach(p => {
          const key = it + '::' + p.code
          if (!paramMap.has(key)) {
            paramMap.set(key, {
              code: p.code,
              name: p.name,
              unit: p.unit,
              value: '',
              item: it
            })
          }
        })
      })
    } catch (e) { /* 忽略单项失败 */ }
  }
  selectedParams.value = Array.from(paramMap.values())
}

// 照片上传
function beforeUpload(file) {
  const isImg = file.type.startsWith('image/')
  if (!isImg) {
    message.error('只能上传图片文件')
    return Upload.LIST_IGNORE
  }
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    message.error('图片大小不能超过 20MB')
    return Upload.LIST_IGNORE
  }
  return true
}

async function customUpload({ file, onSuccess, onError }) {
  try {
    const res = await uploadSamplePhoto(file)
    const data = res.data
    // 更新列表项中对应文件，确保缩略图响应式渲染
    const target = fileList.value.find(f => f.uid === file.uid) || file
    const photoUrl = data.url
    const photoPath = data.path
    target.photoPath = photoPath
    target.url = photoUrl
    target.thumbUrl = photoUrl
    target.status = 'done'
    target.response = res
    // a-upload 内部对象同步（保证缩略图）
    file.url = photoUrl
    file.thumbUrl = photoUrl
    file.status = 'done'
    file.response = res
    file.photoPath = photoPath
    onSuccess(res, file)
    message.success(`${file.name} 上传成功`)
  } catch (e) {
    const target = fileList.value.find(f => f.uid === file.uid) || file
    target.status = 'error'
    file.status = 'error'
    onError(e)
    message.error(`${file.name} 上传失败`)
  }
}

function handleRemove(file) {
  const idx = fileList.value.indexOf(file)
  if (idx >= 0) fileList.value.splice(idx, 1)
}

function handlePreview(file) {
  previewImage.value = file.url || file.thumbUrl ||
    (file.response && file.response.data && file.response.data.url)
  previewOpen.value = true
}

function onCollectVisibleChange(v) {
  if (!v) resetCollect()
}

function resetCollect() {
  Object.assign(form, {
    dispatchId: undefined,
    dispatchNo: '',
    entrustId: undefined,
    entrustName: '',
    custName: undefined,
    pointId: undefined,
    category: '',
    item: [],
    sampleType: undefined,
    weather: '',
    amount: '',
    container: '',
    preserve: '',
    preservatives: [],
    qcTypes: [],
    retainDays: 1,
    retainBy: undefined,
    retainLocation: '',
    receiveBy: ''
  })
  pointOptions.value = []
  itemOptions.value = []
  selectedParams.value = []
  fileList.value = []
  retainChecked.value = false
  previewOpen.value = false
  previewImage.value = ''
}

async function submitCollect() {
  if (!form.dispatchId) return message.warning('请选择采样派单')
  if (!form.custName) return message.warning('请选择单位信息')
  if (!form.pointId) return message.warning('请选择监测点位')
  if (!form.item || form.item.length === 0) return message.warning('请选择检测项目')
  collecting.value = true
  try {
    await collectSample({
      dispatchId: form.dispatchId,
      dispatchNo: form.dispatchNo,
      entrustId: form.entrustId,
      custName: form.custName,
      pointId: form.pointId,
      category: form.category,
      item: form.item.join(','),
      type: form.sampleType,
      weather: form.weather,
      amount: form.amount,
      container: form.container,
      preserve: form.preserve,
      name: `${form.custName}-${form.item.join('/')}`,
      source: pointOptions.value.find(o => o.value === form.pointId)?.label || '',
      sampleParams: selectedParams.value.map(p => ({
        code: p.code, name: p.name, value: p.value, unit: p.unit
      })),
      preservatives: form.preservatives,
      qcTypes: form.qcTypes,
      retainSample: retainChecked.value ? 1 : 0,
      retainDays: retainChecked.value ? form.retainDays : null,
      retainBy: retainChecked.value ? form.retainBy : null,
      retainDate: retainChecked.value ? retainDate.value : null,
      retainLocation: retainChecked.value ? form.retainLocation : null,
      photos: fileList.value.map(f => f.photoPath || (f.response && f.response.data && f.response.data.path)).filter(Boolean),
      receiveBy: form.receiveBy || '收样员'
    })
    message.success('采样信息已保存')
    collectOpen.value = false
    loadData()
  } finally {
    collecting.value = false
  }
}

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getReceiveWorkbench({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || {}
    list.value = data.pendingSamples || []
    stats.pending = data.pendingStatusCount || 0
    pagination.total = data.pendingTotal || 0
  } finally {
    loading.value = false
  }
}

function todayStr() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

async function loadUsers() {
  try {
    const res = await getUsers()
    userOptions.value = (res.data || []).map(u => ({
      value: u.realName || u.username,
      label: u.realName ? `${u.realName}（${u.username}）` : u.username
    }))
  } catch (e) {
    console.error('[loadUsers] 人员列表加载失败:', e?.message || e)
  }
}

function openReceive(record) {
  current.value = record
  receiveForm.receiveBy = ''
  receiveForm.remark = ''
  receiveForm.amount = ''
  receiveForm.container = ''
  receiveForm.preserve = record?.preserve || ''
  receiveForm.retainDays = 1
  receiveForm.retainBy = undefined
  receiveForm.retainLocation = ''
  receiveDate.value = todayStr()
  retainDate.value = todayStr()
  receiveRetainChecked.value = false
  receiveOpen.value = true
  loadUsers()
}

async function submitReceive() {
  if (!receiveForm.receiveBy) {
    message.warning('请填写收样人')
    return
  }
  if (receiveRetainChecked.value && !receiveForm.retainDays) {
    message.warning('请填写留样保存天数')
    return
  }
  submitting.value = true
  try {
    await receiveSample(current.value.id, {
      receiveBy: receiveForm.receiveBy,
      receiveTime: receiveDate.value,
      remark: receiveForm.remark,
      amount: receiveForm.amount,
      container: receiveForm.container,
      preserve: receiveForm.preserve,
      retainFlag: receiveRetainChecked.value ? 1 : 0,
      retainDays: receiveRetainChecked.value ? receiveForm.retainDays : null,
      retainBy: receiveRetainChecked.value ? receiveForm.retainBy : null,
      retainDate: receiveRetainChecked.value ? retainDate.value : null,
      retainLocation: receiveRetainChecked.value ? receiveForm.retainLocation : null
    })
    message.success('收样登记成功')
    receiveOpen.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function openDetail(record) {
  const res = await getSampleDetail(record.id)
  detail.value = res.data
  detailOpen.value = true
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 4px; }
.readonly-text { color: #333; }
.title-divider { margin: 16px 0 !important; font-weight: 600; }
.hint { color: #999; font-size: 12px; margin-top: 6px; }
.photo-block { text-align: left; }
.photo-block :deep(.ant-upload-list-picture-card) { text-align: left; }
</style>
