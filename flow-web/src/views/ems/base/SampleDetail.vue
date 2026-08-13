<template>
  <div class="page-container">
    <div class="page-toolbar">
      <a-button @click="goBack">
        <template #icon><span class="btn-icon">←</span></template>
        返回
      </a-button>
      <span class="page-title">样品详情{{ detail.sample?.barcode ? ' · ' + detail.sample.barcode : '' }}</span>
      <a-tag v-if="detail.sample?.status" :color="statusColor(detail.sample.status)">{{ detail.sample.status }}</a-tag>
    </div>

    <template v-if="loading">
      <a-skeleton active />
    </template>
    <template v-else-if="detail.sample">
      <!-- 条形码图 -->
      <a-card class="block barcode-card" size="small" :bordered="true">
        <div class="barcode-wrap">
          <svg ref="barcodeRef" class="barcode-svg"></svg>
        </div>
        <div class="barcode-meta">
          <span class="barcode-text">{{ detail.sample?.barcode }}</span>
          <span class="barcode-name">{{ detail.sample?.name }}</span>
        </div>
      </a-card>

      <!-- 采样信息 -->
      <a-card class="block" size="small" title="采样信息">
        <a-descriptions bordered :column="2" size="small" :label-style="{ width: '120px' }">
          <a-descriptions-item label="样品条码">{{ detail.sample?.barcode }}</a-descriptions-item>
          <a-descriptions-item label="样品名称">{{ detail.sample?.name }}</a-descriptions-item>
          <a-descriptions-item label="样品类型">{{ dictText('sampleType', detail.sample?.type) }}</a-descriptions-item>
          <a-descriptions-item label="来源">{{ detail.sample?.source }}</a-descriptions-item>
          <a-descriptions-item label="容器">{{ detail.sample?.container }}</a-descriptions-item>
          <a-descriptions-item label="数量/规格">{{ detail.sample?.amount }}</a-descriptions-item>
          <a-descriptions-item label="检测类别">{{ detail.sample?.category }}</a-descriptions-item>
          <a-descriptions-item label="检测项目">{{ detail.sample?.item }}</a-descriptions-item>
          <a-descriptions-item label="保存条件">{{ detail.sample?.preserve }}</a-descriptions-item>
          <a-descriptions-item label="固定剂">{{ dictListText('preservative', detail.sample?.preservatives) }}</a-descriptions-item>
          <a-descriptions-item label="现场质控方式">{{ dictListText('qcType', detail.sample?.qcTypes) }}</a-descriptions-item>
          <a-descriptions-item label="采样天气">{{ detail.record?.weather || detail.sample?.weather || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采样人">{{ detail.sample?.sampler || detail.record?.sampler || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采样时间">{{ detail.sample?.sampleTime || detail.record?.sampleTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采样点位">{{ detail.pointName || detail.record?.pointName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(detail.sample?.status)">{{ detail.sample?.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="收样人">{{ detail.sample?.receiveBy }}</a-descriptions-item>
          <a-descriptions-item label="收样时间">{{ detail.sample?.receiveTime }}</a-descriptions-item>
          <a-descriptions-item label="核验项" :span="2">{{ dictListText('receiveCheck', detail.sample?.checkItems) || '未核验' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 采样参数 -->
      <a-card class="block" size="small" title="采样参数（监测指标）">
        <template v-if="sampleParams.length">
          <div v-for="grp in sampleParams" :key="grp.item" class="param-group">
            <div class="param-group-title">{{ grp.item }}</div>
            <a-table
              :columns="paramColumns"
              :data-source="grp.params"
              size="small"
              row-key="code"
              :pagination="false"
              bordered
            />
          </div>
        </template>
        <a-empty v-else description="无采样参数" :image="simpleImage" />
      </a-card>

      <!-- 留样信息 -->
      <a-card class="block" size="small" title="留样信息">
        <a-descriptions bordered :column="2" size="small" :label-style="{ width: '120px' }">
          <a-descriptions-item label="是否留样">
            <a-tag :color="(detail.sample?.retainFlag === 1 || detail.sample?.retainFlag === '1') ? 'green' : 'default'">
              {{ (detail.sample?.retainFlag === 1 || detail.sample?.retainFlag === '1') ? '留样' : '不留样' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="留样到期">{{ detail.sample?.retainUntil || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留样天数">{{ detail.sample?.retainDays || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留样数量">{{ detail.sample?.retainAmount || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留样人">{{ detail.sample?.retainBy || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留样日期">{{ detail.sample?.retainDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="存放位置">{{ detail.sample?.retainLocation || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 质控样 -->
      <a-card class="block" size="small" title="质控样">
        <a-table
          v-if="detail.qcList && detail.qcList.length"
          :columns="qcColumns"
          :data-source="detail.qcList"
          size="small"
          row-key="id"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-popconfirm title="确认解绑该质控样？" @confirm="unbindQc(record)">
                <a-button type="link" danger size="small">解绑</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
        <a-empty v-else description="无质控样" :image="simpleImage" />
      </a-card>

      <!-- 现场图片 -->
      <a-card class="block" size="small" title="现场图片">
        <div class="photo-grid" v-if="photoList.length">
          <a-image
            v-for="(p, i) in photoList"
            :key="i"
            :src="p"
            :width="120"
            :height="120"
            :preview="{ mask: '查看' }"
            style="border-radius:6px;overflow:hidden;"
          />
        </div>
        <a-empty v-else description="无现场图片" :image="simpleImage" />
      </a-card>

      <!-- 异常处置信息（仅异常拒收/检测异常样品且有处置记录时展示） -->
      <a-card v-if="detail.sample.disposalTime" class="block" size="small" title="异常处置信息">
        <a-descriptions bordered :column="2" size="small" :label-style="{ width: '120px' }">
          <a-descriptions-item label="处置类型">{{ dictText('disposalType', detail.sample.disposalType) }}</a-descriptions-item>
          <a-descriptions-item label="处置方式">{{ dictText('disposalMethod', detail.sample.disposalMethod) }}</a-descriptions-item>
          <a-descriptions-item label="处置人">{{ detail.sample.disposalBy }}</a-descriptions-item>
          <a-descriptions-item label="处置时间">{{ detail.sample.disposalTime }}</a-descriptions-item>
          <a-descriptions-item label="处置说明" :span="2">
            <div class="rich-content" v-html="detail.sample.disposalDesc"></div>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 操作日志 -->
      <a-card class="block" size="small" title="操作日志">
        <a-timeline v-if="detail.logs && detail.logs.length">
          <a-timeline-item v-for="log in detail.logs" :key="log.id">
            <b>{{ log.action }}</b> · {{ log.operator }} · {{ log.detail }}
            <span style="color:#999;">（{{ log.createTime }}）</span>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else description="暂无日志" :image="simpleImage" />
      </a-card>
    </template>
    <a-empty v-else description="未找到样品信息" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Empty } from 'ant-design-vue'
import JsBarcode from 'jsbarcode'
import {
  getSampleDetail,
  unbindSampleQc,
  getSampleParamConfigs,
  getDictItems
} from '../../../api/ems'

const route = useRoute()
const router = useRouter()

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const loading = ref(false)
const detail = ref({})

function statusColor(status) {
  return {
    '待收样': 'orange',
    '已收样': 'blue',
    '异常拒收': 'orange',
    '留样中': 'purple',
    '实验室检测中': 'cyan',
    '检测数据复核中': 'gold',
    '已完成': 'green',
    '检测异常': 'red',
    '已处置': 'default'
  }[status] || 'default'
}

function goBack() {
  router.back()
}

// ===== 数据字典 value → itemText 转换 =====
const DETAIL_DICT_CODES = {
  sampleType: 'moni_sample_type',
  preservative: 'sample_preservative',
  qcType: 'moni_qc_type',
  receiveCheck: 'sample_receive_check',
  disposalType: 'moni_disposal_type',
  disposalMethod: 'moni_disposal_method'
}
const dictMaps = reactive({
  sampleType: {},
  preservative: {},
  qcType: {},
  receiveCheck: {},
  disposalType: {},
  disposalMethod: {}
})

async function loadDetailDicts() {
  const entries = Object.entries(DETAIL_DICT_CODES)
  const results = await Promise.all(
    entries.map(([, code]) => getDictItems(code).catch(() => null))
  )
  entries.forEach(([key], idx) => {
    const list = results[idx]?.data || []
    const m = {}
    list.forEach(it => { m[it.itemValue] = it.itemText })
    dictMaps[key] = m
  })
}

// 单值字典字段转展示文本
function dictText(key, val) {
  if (val === null || val === undefined || val === '') return '-'
  return dictMaps[key]?.[val] ?? val
}

// 逗号分隔的字典字段转展示文本
function dictListText(key, val) {
  if (!val) return '-'
  const arr = Array.isArray(val) ? val : String(val).split(',').map(s => s.trim()).filter(Boolean)
  if (!arr.length) return '-'
  return arr.map(v => dictMaps[key]?.[v] ?? v).join('、')
}

// ===== 条形码 / 图片 / 参数 =====
const barcodeRef = ref(null)
const PHOTO_BASE = '/api/v1/ems/base/sampling/samples/photo/'

const photoList = computed(() => {
  const raw = detail.value?.sample?.samplePhoto
  if (!raw || typeof raw !== 'string') return []
  return raw.split(',').map(s => s.trim()).filter(Boolean).map(p => PHOTO_BASE + p)
})

// 采样参数配置（旧数据分组反查用）
const sampleParamConfigs = ref([])
async function loadCollectParams() {
  try {
    const res = await getSampleParamConfigs()
    sampleParamConfigs.value = res.data || []
  } catch (e) { /* ignore */ }
}

const paramColumns = [
  { title: '参数编码', dataIndex: 'code', key: 'code' },
  { title: '参数名称', dataIndex: 'name', key: 'name' },
  { title: '实测值', dataIndex: 'value', key: 'value' },
  { title: '单位', dataIndex: 'unit', key: 'unit' }
]

const sampleParams = computed(() => {
  const raw = detail.value?.sample?.sampleParams
  if (!raw) return []
  let parsed
  try {
    parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) {
    return []
  }
  const arr = Array.isArray(parsed) ? parsed : []
  // 新数据：参数项自带 item 字段，直接按 it.item 分组
  // 旧数据（无 item）：通过样品检测项目列表 + 参数配置反查所属项目
  const hasItem = arr.some(it => it.item)
  if (hasItem) {
    const map = new Map()
    arr.forEach(it => {
      const key = it.item || '其他'
      if (!map.has(key)) map.set(key, { item: key, params: [] })
      map.get(key).params.push(it)
    })
    return Array.from(map.values())
  }
  const sampleItems = (detail.value?.sample?.item || '').split(',').map(s => s.trim()).filter(Boolean)
  const itemCodes = new Map()
  ;(sampleParamConfigs.value || []).forEach(c => {
    if (!sampleItems.includes(c.item)) return
    const codes = new Set()
    ;(c.sampleParams || []).forEach(p => { if (p.code) codes.add(p.code) })
    if (codes.size) itemCodes.set(c.item, codes)
  })
  const map = new Map()
  arr.forEach(it => {
    let key = '其他'
    for (const itemName of sampleItems) {
      const codes = itemCodes.get(itemName)
      if (codes && codes.has(it.code)) { key = itemName; break }
    }
    if (!map.has(key)) map.set(key, { item: key, params: [] })
    map.get(key).params.push(it)
  })
  return Array.from(map.values())
})

// 条形码渲染：detail.sample.barcode 变化时重绘
watch(
  () => detail.value?.sample?.barcode,
  (code) => {
    if (!code) {
      if (barcodeRef.value) barcodeRef.value.innerHTML = ''
      return
    }
    nextTick(() => {
      nextTick(() => {
        if (!barcodeRef.value) return
        try {
          JsBarcode(barcodeRef.value, String(code), {
            format: 'CODE128',
            width: 2,
            height: 56,
            margin: 8,
            displayValue: true,
            fontSize: 14,
            textAlign: 'center'
          })
        } catch (e) {
          if (barcodeRef.value) barcodeRef.value.innerHTML = ''
        }
      })
    })
  },
  { immediate: true }
)

// ===== 质控样解绑 =====
const qcColumns = [
  { title: '样品编号', dataIndex: 'sampleNo', key: 'sampleNo' },
  { title: '质控类型', dataIndex: 'qcType', key: 'qcType' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 80 }
]

async function unbindQc(record) {
  await unbindSampleQc(record.id)
  message.success('已解绑')
  load()
}

function load() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  detail.value = {}
  loadCollectParams()
  loadDetailDicts()
  getSampleDetail(id).then((res) => {
    detail.value = res.data || {}
  }).catch(() => {
    detail.value = {}
  }).finally(() => { loading.value = false })
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
  background-color: #FFF;
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
.btn-icon {
  display: inline-block;
}
.block {
  margin-bottom: 16px;
}
/* 条形码卡片 */
.barcode-card { background: #fafafa; }
.barcode-wrap { display: flex; justify-content: center; padding: 8px 0; }
.barcode-svg { max-width: 100%; height: auto; }
.barcode-meta { display: flex; justify-content: center; align-items: baseline; gap: 12px; margin-top: 4px; }
.barcode-text { font-family: 'Courier New', monospace; font-size: 15px; font-weight: 600; color: #333; letter-spacing: 1px; }
.barcode-name { font-size: 13px; color: #666; }
/* 现场图片 */
.photo-grid { display: flex; flex-wrap: wrap; gap: 12px; }
/* 按监测项目分组的采样参数区 */
.param-group { margin-bottom: 16px; }
.param-group-title { font-weight: 600; color: #1677ff; margin-bottom: 8px; padding-left: 8px; border-left: 3px solid #1677ff; }
.rich-content { word-break: break-all; }
</style>
