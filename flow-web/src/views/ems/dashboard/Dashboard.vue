<template>
  <div class="exec" v-if="ready">
    <header class="exec-header">
      <div class="exec-header__left">
        <span class="exec-header__logo">LIMS</span>
        <h1 class="exec-header__title">环境监测 LIMS · 高层领导数据驾驶舱</h1>
      </div>
      <div class="exec-header__right">
        <span class="exec-clock">{{ clock }}</span>
        <a-switch v-model:checked="autoRefresh" size="small" />
        <span class="exec-refresh-label">自动刷新 {{ refreshSec }}s</span>
        <a-button size="small" type="primary" ghost @click="loadAll">立即刷新</a-button>
      </div>
    </header>

    <!-- KPI -->
    <section class="exec-kpi">
      <div class="kpi-card" v-for="k in kpis" :key="k.key" :style="{ '--kpi-color': k.color }">
        <div class="kpi-card__icon" :style="{ color: k.color }">
          <component :is="k.icon" />
        </div>
        <div class="kpi-card__body">
          <div class="kpi-card__label">{{ k.label }}</div>
          <div class="kpi-card__value" :style="{ color: k.color }">
            {{ k.value }}<small v-if="k.unit">{{ k.unit }}</small>
          </div>
          <div class="kpi-card__sub" v-html="k.sub" />
        </div>
      </div>
    </section>

    <div class="exec-ticker">
      <span class="exec-ticker__tag">实时动态</span>
      <div class="exec-ticker__viewport">
        <div class="exec-ticker__track" :style="{ animationDuration: tickerDuration }">
          <span v-for="(t, i) in tickerItems" :key="i" class="exec-ticker__item">{{ t }}</span>
        </div>
      </div>
    </div>

    <section class="exec-main">
      <!-- 左侧 -->
      <div class="exec-col exec-col--left">
        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />合同金额月度趋势</div>
            <div class="exec-legend"><span><i style="background:#36cfc9" />签约额</span><span><i style="background:#ffc53d" />回款额</span></div>
          </div>
          <div class="exec-panel__body"><div ref="chartMonthly" class="echart" /></div>
        </div>

        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />合同结构分布</div>
          </div>
          <div class="exec-panel__body"><div ref="chartStatus" class="echart" /></div>
        </div>

        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />客户合同金额 TOP10</div>
            <span class="exec-panel__unit">单位：万元</span>
          </div>
          <div class="exec-panel__body"><div ref="chartTop" class="echart" /></div>
        </div>
      </div>

      <!-- 中间 -->
      <div class="exec-col exec-col--center">
        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />业务全链路流转</div>
            <span class="exec-panel__route">合同 → 检测 → 报告</span>
          </div>
          <div class="exec-panel__body"><div ref="chartFunnel" class="echart" /></div>
        </div>

        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />检测结果与超标预警</div>
            <span class="exec-panel__tip">共 {{ detection.total }} 条结果 · {{ detection.rows.filter(r => r.conclusion === '超标').length }} 条超标</span>
          </div>
          <div class="exec-panel__body exec-panel--scroll">
            <table class="exec-table">
              <thead>
                <tr>
                  <th>样品编号/名称</th>
                  <th>监测项目</th>
                  <th>实测值</th>
                  <th>限值</th>
                  <th>结论</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(r, i) in detection.rows" :key="i">
                  <td :title="r.sampleName">{{ r.sampleName }}</td>
                  <td>{{ r.monitorItem }}</td>
                  <td>{{ r.value }} {{ r.unit }}</td>
                  <td>{{ r.limitValue }}</td>
                  <td><span class="tag" :class="conclusionClass(r.conclusion)">{{ r.conclusion }}</span></td>
                </tr>
                <tr v-if="!detection.rows.length"><td colspan="5" class="empty">暂无数据</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />系统预警消息</div>
            <span class="exec-panel__tip" style="color:#ff7875">{{ alerts.filter(a => a.status === '未处理').length }} 条未处理</span>
          </div>
          <div class="exec-panel__body exec-panel--scroll">
            <table class="exec-table">
              <thead>
                <tr><th>预警类型</th><th>级别</th><th>消息</th><th>状态</th></tr>
              </thead>
              <tbody>
                <tr v-for="(a, i) in alerts" :key="i">
                  <td>{{ a.type }}</td>
                  <td><span class="tag" :class="'lv-' + a.level">{{ a.level }}</span></td>
                  <td :title="a.message">{{ a.message }}</td>
                  <td><span class="tag" :class="a.status === '未处理' ? 'tag-red' : 'tag-green'">{{ a.status }}</span></td>
                </tr>
                <tr v-if="!alerts.length"><td colspan="4" class="empty">暂无预警</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右侧 -->
      <div class="exec-col exec-col--right">
        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />经营与质量概览</div>
            <span class="exec-panel__tip">回款率 / 质控合格率</span>
          </div>
          <div class="exec-panel__body"><div ref="chartQc" class="echart" /></div>
        </div>

        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />仪器设备状态</div>
            <span class="exec-panel__tip">共 {{ instrumentTotal }} 台</span>
          </div>
          <div class="exec-panel__body"><div ref="chartInstr" class="echart" /></div>
        </div>

        <div class="exec-panel">
          <div class="exec-panel__head">
            <div class="exec-panel__title"><i class="exec-panel__ico" />合同到期预警</div>
            <span class="exec-panel__tip">执行中合同 · 按到期顺序</span>
          </div>
          <div class="exec-panel__body exec-panel--scroll">
            <table class="exec-table">
              <thead>
                <tr><th>对方单位</th><th>金额</th><th>到期日</th><th>状态</th></tr>
              </thead>
              <tbody>
                <tr v-for="(c, i) in expiring" :key="i">
                  <td :title="c.counterparty">{{ c.counterparty }}</td>
                  <td>{{ c.amountWan }}万</td>
                  <td>{{ c.expireDate }}</td>
                  <td><span class="tag" :class="c.remainDays <= 30 ? 'tag-red' : c.remainDays <= 90 ? 'tag-orange' : 'tag-green'">{{ c.remainDays }}天</span></td>
                </tr>
                <tr v-if="!expiring.length"><td colspan="4" class="empty">无即将到期合同</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  </div>
  <div v-else class="exec-loading">驾驶舱加载中…</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import {
  FileTextOutlined, DollarOutlined, CheckCircleOutlined,
  ExperimentOutlined, FileDoneOutlined, AlertOutlined,
  TeamOutlined, ToolOutlined
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import {
  getExecutiveKpi, getExecutiveMonthlyTrend, getExecutiveStatusDist,
  getExecutiveTopCustomers, getExecutiveFunnel, getExecutiveDetectionResults,
  getExecutiveAlerts, getExecutiveQcRate, getExecutiveInstrumentStatus,
  getExecutiveContractExpiring, getExecutiveTicker
} from '../../../api/executive'

const ready = ref(false)
const autoRefresh = ref(true)
const refreshSec = ref(60)
const expiringDays = ref(180)

const clock = ref('')
const kpis = ref([])
const monthly = reactive({ months: [], signAmount: [], receiveAmount: [] })
const statusDist = ref([])
const topCustomers = ref([])
const funnel = ref([])
const detection = reactive({ rows: [], total: 0, page: 1, size: 8 })
const alerts = ref([])
const qc = reactive({ rate: 0, receiptRate: 0, qualified: 0, total: 0 })
const instrumentStatus = ref([])
const instrumentTotal = computed(() => instrumentStatus.value.reduce((s, x) => s + (x.value || 0), 0))
const expiring = ref([])
const tickerItems = ref([])

const chartMonthly = ref(null)
const chartStatus = ref(null)
const chartTop = ref(null)
const chartFunnel = ref(null)
const chartQc = ref(null)
const chartInstr = ref(null)
const charts = []

const statusColors = {
  '草稿': '#8c8c8c', '执行中': '#1890ff', '已完结': '#52c41a', '已作废': '#ff4d4f'
}
const instrColors = {
  '在用': '#52c41a', '校准到期': '#faad14', '维修中': '#ff7a45', '报废': '#ff4d4f'
}

const tickerDuration = computed(() => Math.max(20, tickerItems.value.length * 4) + 's')

function conclusionClass(c) {
  if (c === '超标') return 'tag-red'
  if (c === '达标') return 'tag-green'
  return 'tag-gray'
}

function fmtWan(yuan) { return (yuan / 10000).toFixed(1) }

const kpiIconMap = {
  contractTotal: FileTextOutlined, contractAmountWan: DollarOutlined, receiptRate: CheckCircleOutlined,
  detectionTotal: ExperimentOutlined, reportTotal: FileDoneOutlined, alertCount: AlertOutlined,
  customerTotal: TeamOutlined, instrumentTotal: ToolOutlined
}
const kpiColorMap = {
  contractTotal: '#1890ff', contractAmountWan: '#36cfc9', receiptRate: '#52c41a',
  detectionTotal: '#722ed1', reportTotal: '#13c2c2', alertCount: '#ff4d4f',
  customerTotal: '#faad14', instrumentTotal: '#1890ff'
}
const kpiLabelMap = {
  contractTotal: '合同总数', contractAmountWan: '合同总金额', receiptRate: '已回款率',
  detectionTotal: '检测任务', reportTotal: '已出报告', alertCount: '超标/预警',
  customerTotal: '服务客户', instrumentTotal: '仪器设备'
}
const kpiUnitMap = {
  contractTotal: '份', contractAmountWan: '万元', receiptRate: '%',
  detectionTotal: '个', reportTotal: '份', alertCount: '项',
  customerTotal: '家', instrumentTotal: '台'
}

async function loadKpi() {
  const d = await getExecutiveKpi()
  const keys = ['contractTotal', 'contractAmountWan', 'receiptRate', 'detectionTotal', 'reportTotal', 'alertCount', 'customerTotal', 'instrumentTotal']
  const subMap = {
    contractTotal: `执行中 ${d.contractExecuting ?? 0} <span style="color:#52c41a">已完结 ${d.contractFinished ?? 0}</span>`,
    contractAmountWan: `已回款 ${d.receivedWan ?? 0}万 <span style="color:#52c41a">回款率 ${d.receiptRate ?? 0}%</span>`,
    receiptRate: `<span style="color:#52c41a">${d.receivedWan ?? 0}万</span> / ${d.contractAmountWan ?? 0}万`,
    detectionTotal: `录入中 ${d.detectionEntering ?? 0} <span style="color:#52c41a">已提交 ${d.detectionSubmitted ?? 0}</span>`,
    reportTotal: `待发布 ${d.reportPending ?? 0} <span style="color:#52c41a">已发布 ${d.reportPublished ?? 0}</span>`,
    alertCount: `<span style="color:#ff7875">检测超标 ${d.overrunCount ?? 0}</span> <span style="color:#faad14">系统预警 ${d.alertCount ?? 0}</span>`,
    customerTotal: `覆盖 ${d.customerCities ?? 0}+ 城市 <span style="color:#52c41a">重点 ${d.customerKey ?? 0}</span>`,
    instrumentTotal: `在库设备总数`
  }
  kpis.value = keys.map(k => ({
    key: k, label: kpiLabelMap[k], value: d[k] ?? 0, unit: kpiUnitMap[k],
    sub: subMap[k] || '', color: kpiColorMap[k], icon: kpiIconMap[k]
  }))
}

async function loadMonthly() {
  const d = await getExecutiveMonthlyTrend()
  monthly.months = d.months || []
  monthly.signAmount = d.signAmount || []
  monthly.receiveAmount = d.receiveAmount || []
}
async function loadStatus() { statusDist.value = (await getExecutiveStatusDist()).data || [] }
async function loadTop() { topCustomers.value = (await getExecutiveTopCustomers(10)).data || [] }
async function loadFunnel() { funnel.value = (await getExecutiveFunnel()).data || [] }
async function loadDetection() {
  const r = await getExecutiveDetectionResults(detection.page, detection.size)
  detection.rows = r.list || []; detection.total = r.total || 0
}
async function loadAlerts() { alerts.value = (await getExecutiveAlerts()).data || [] }
async function loadQc() {
  const q = await getExecutiveQcRate()
  qc.rate = q.rate || 0
  qc.receiptRate = q.receiptRate || 0
  qc.qualified = q.qualified || 0
  qc.total = q.total || 0
}
async function loadInstrument() { instrumentStatus.value = (await getExecutiveInstrumentStatus()).data || [] }
async function loadExpiring() { expiring.value = (await getExecutiveContractExpiring(expiringDays.value)).list || [] }
async function loadTicker() { const t = await getExecutiveTicker(); tickerItems.value = (t.list || []).map(x => x.text) }

async function loadAll() {
  await Promise.all([
    loadKpi(), loadMonthly(), loadStatus(), loadTop(), loadFunnel(),
    loadDetection(), loadAlerts(), loadQc(), loadInstrument(), loadExpiring(), loadTicker()
  ])
  ready.value = true
  nextTick(() => { initCharts(); updateCharts() })
}

function initCharts() {
  charts.forEach(c => c.dispose())
  charts.length = 0
  const doms = [chartMonthly.value, chartStatus.value, chartTop.value, chartFunnel.value, chartQc.value, chartInstr.value]
  doms.forEach(dom => {
    if (!dom) return
    const c = echarts.init(dom)
    charts.push(c)
  })
}

function updateCharts() {
  if (!charts.length) return

  const textColor = '#bcd6f5'
  const axisColor = 'rgba(140,180,230,.2)'
  const grid = { left: 46, right: 20, top: 34, bottom: 22 }

  // 月度趋势：柱状+折线
  const c0 = charts.find(c => c.getDom() === chartMonthly.value)
  if (c0) {
    c0.setOption({
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,22,46,.92)', borderColor: '#1890ff', textStyle: { color: '#fff' } },
      legend: { data: ['签约额', '回款额'], textStyle: { color: textColor }, top: 0, right: 0, itemWidth: 10, itemHeight: 10 },
      grid,
      xAxis: { type: 'category', data: monthly.months, axisLine: { lineStyle: { color: axisColor } }, axisLabel: { color: textColor } },
      yAxis: { type: 'value', name: '万元', nameTextStyle: { color: '#7fa8d6' }, splitLine: { lineStyle: { color: 'rgba(140,180,230,.08)' } }, axisLabel: { color: textColor } },
      series: [
        { name: '签约额', type: 'bar', barWidth: 12, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#36cfc9' }, { offset: 1, color: '#096dd9' }]) }, data: monthly.signAmount },
        { name: '回款额', type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { color: '#ffc53d', width: 2 }, itemStyle: { color: '#ffc53d' }, data: monthly.receiveAmount }
      ]
    }, true)
  }

  // 合同结构分布：环形
  const c1 = charts.find(c => c.getDom() === chartStatus.value)
  if (c1) {
    c1.setOption({
      tooltip: { trigger: 'item', backgroundColor: 'rgba(8,22,46,.92)', borderColor: '#1890ff', textStyle: { color: '#fff' } },
      legend: { orient: 'vertical', right: 10, top: 'middle', textStyle: { color: textColor }, itemWidth: 10, itemHeight: 10 },
      series: [{
        type: 'pie', radius: ['45%', '70%'], center: ['35%', '50%'],
        label: { show: true, color: textColor, formatter: '{b}\n{d}%' },
        labelLine: { lineStyle: { color: axisColor } },
        data: statusDist.value.map(s => ({ value: s.value, name: s.name, itemStyle: { color: statusColors[s.name] || '#1890ff' } }))
      }]
    }, true)
  }

  // 客户 TOP10：横向条形
  const c2 = charts.find(c => c.getDom() === chartTop.value)
  if (c2) {
    const data = [...topCustomers.value].reverse()
    c2.setOption({
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,22,46,.92)', borderColor: '#1890ff', textStyle: { color: '#fff' }, formatter: '{b}: {c}万' },
      grid: { left: 90, right: 50, top: 8, bottom: 8 },
      xAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(140,180,230,.08)' } }, axisLabel: { color: textColor } },
      yAxis: { type: 'category', data: data.map(c => c.name), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: textColor } },
      series: [{
        type: 'bar', barWidth: 12, data: data.map(c => c.amount),
        itemStyle: { borderRadius: 6, color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [{ offset: 0, color: '#ffd666' }, { offset: 1, color: '#fa8c16' }]) },
        label: { show: true, position: 'right', color: '#fff', formatter: '{c}万' }
      }]
    }, true)
  }

  // 业务全链路漏斗
  const c3 = charts.find(c => c.getDom() === chartFunnel.value)
  if (c3) {
    c3.setOption({
      tooltip: { trigger: 'item', backgroundColor: 'rgba(8,22,46,.92)', borderColor: '#1890ff', textStyle: { color: '#fff' } },
      series: [{
        type: 'funnel', left: 60, right: 80, top: 10, bottom: 10, minSize: '20%', sort: 'none', gap: 4,
        label: { show: true, color: '#fff', position: 'inside', formatter: '{c}' },
        data: funnel.value.map((f, i) => ({
          value: f.value, name: f.name,
          itemStyle: { color: ['#1677ff', '#36cfc9', '#13c2c2', '#73d13d', '#95de64'][i % 5] }
        }))
      }]
    }, true)
  }

  // 经营与质量概览：两个仪表盘
  const c4 = charts.find(c => c.getDom() === chartQc.value)
  if (c4) {
    c4.setOption({
      series: [
        {
          type: 'gauge', center: ['28%', '55%'], radius: '78%', startAngle: 220, endAngle: -40,
          min: 0, max: 100, splitNumber: 5,
          axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(140,180,230,.15)']] } },
          progress: { show: true, width: 10, itemStyle: { color: '#36cfc9' } },
          axisTick: { show: false }, splitLine: { length: 8, lineStyle: { color: 'auto' } }, axisLabel: { color: textColor, fontSize: 10, distance: 16 }, pointer: { length: '55%', width: 3 }, detail: { valueAnimation: true, fontSize: 18, color: '#36cfc9', offsetCenter: [0, '70%'], formatter: '{value}%' }, title: { offsetCenter: [0, '105%'], color: textColor, fontSize: 12 }, data: [{ value: qc.receiptRate || 0, name: '合同回款率' }]
        },
        {
          type: 'gauge', center: ['74%', '55%'], radius: '78%', startAngle: 220, endAngle: -40,
          min: 0, max: 100, splitNumber: 5,
          axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(140,180,230,.15)']] } },
          progress: { show: true, width: 10, itemStyle: { color: '#52c41a' } },
          axisTick: { show: false }, splitLine: { length: 8, lineStyle: { color: 'auto' } }, axisLabel: { color: textColor, fontSize: 10, distance: 16 }, pointer: { length: '55%', width: 3 }, detail: { valueAnimation: true, fontSize: 18, color: '#52c41a', offsetCenter: [0, '70%'], formatter: '{value}%' }, title: { offsetCenter: [0, '105%'], color: textColor, fontSize: 12 }, data: [{ value: qc.rate || 0, name: '质控合格率' }]
        }
      ]
    }, true)
  }

  // 仪器设备状态：环形
  const c5 = charts.find(c => c.getDom() === chartInstr.value)
  if (c5) {
    c5.setOption({
      tooltip: { trigger: 'item', backgroundColor: 'rgba(8,22,46,.92)', borderColor: '#1890ff', textStyle: { color: '#fff' } },
      legend: { orient: 'vertical', right: 10, top: 'middle', textStyle: { color: textColor }, itemWidth: 10, itemHeight: 10 },
      series: [{
        type: 'pie', radius: ['50%', '72%'], center: ['36%', '50%'],
        label: { show: true, color: textColor, formatter: '{b}\n{c}' },
        labelLine: { lineStyle: { color: axisColor } },
        data: instrumentStatus.value.map(s => ({ value: s.value, name: s.name, itemStyle: { color: instrColors[s.name] || '#1890ff' } }))
      }]
    }, true)
  }
}

function handleResize() { charts.forEach(c => c && c.resize()) }

let timer = null
function tickClock() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  clock.value = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
function startTimer() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    tickClock()
    if (autoRefresh.value) loadAll()
  }, refreshSec.value * 1000)
}

onMounted(() => { tickClock(); loadAll(); window.addEventListener('resize', handleResize); startTimer() })
onUnmounted(() => { if (timer) clearInterval(timer); window.removeEventListener('resize', handleResize); charts.forEach(c => c && c.dispose()) })
</script>

<style scoped>
.exec { min-height: 100vh; background: radial-gradient(circle at 20% 0%, #0b2545 0%, #061326 60%, #03060f 100%); color: #cfe3ff; padding: 8px 12px 16px; font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; }
.exec-loading { min-height: 100vh; display: flex; align-items: center; justify-content: center; color: #8fb4dd; background: #061326; }

.exec-header { display: flex; justify-content: space-between; align-items: center; padding: 4px 4px 10px; }
.exec-header__left { display: flex; align-items: center; gap: 12px; }
.exec-header__logo { background: linear-gradient(135deg, #1890ff, #36cfc9); color: #04122b; font-weight: 800; padding: 4px 10px; border-radius: 6px; letter-spacing: 1px; box-shadow: 0 0 10px rgba(54,207,201,.35); }
.exec-header__title { font-size: 20px; margin: 0; color: #e6f4ff; letter-spacing: 2px; text-shadow: 0 0 14px rgba(24,144,255,.55); }
.exec-header__right { display: flex; align-items: center; gap: 10px; color: #9fc1e8; font-size: 13px; }
.exec-clock { font-variant-numeric: tabular-nums; color: #7ec1ff; }
.exec-refresh-label { color: #7fa8d6; }

.exec-kpi { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; margin-bottom: 10px; }
.kpi-card { position: relative; background: linear-gradient(160deg, rgba(18,60,120,.55), rgba(8,22,46,.7)); border: 1px solid rgba(58,128,196,.32); border-radius: 10px; padding: 10px 14px; display: flex; gap: 12px; align-items: center; overflow: hidden; box-shadow: inset 0 0 18px rgba(16,58,120,.25); }
.kpi-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--kpi-color, #1890ff); }
.kpi-card__icon { font-size: 28px; }
.kpi-card__body { flex: 1; min-width: 0; }
.kpi-card__label { font-size: 12px; color: #8fb4dd; }
.kpi-card__value { font-size: 22px; font-weight: 800; line-height: 1.1; }
.kpi-card__value small { font-size: 12px; margin-left: 2px; opacity: .7; font-weight: 400; }
.kpi-card__sub { font-size: 11px; color: #6f97c4; margin-top: 3px; }

.exec-ticker { display: flex; align-items: center; gap: 10px; background: rgba(10,28,56,.7); border: 1px solid rgba(58,128,196,.25); border-radius: 8px; padding: 5px 10px; margin-bottom: 10px; overflow: hidden; }
.exec-ticker__tag { flex: none; background: #ff4d4f; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.exec-ticker__viewport { flex: 1; overflow: hidden; white-space: nowrap; }
.exec-ticker__track { display: inline-block; padding-left: 100%; animation: ticker linear infinite; }
.exec-ticker__item { margin: 0 32px; font-size: 13px; color: #bcd6f5; }
@keyframes ticker { from { transform: translateX(0); } to { transform: translateX(-100%); } }

.exec-main { display: grid; grid-template-columns: 1fr 1.35fr 1fr; gap: 10px; min-height: 0; height: calc(100vh - 210px); }
.exec-col { display: flex; flex-direction: column; gap: 10px; min-height: 0; }
.exec-col--left .exec-panel:nth-child(1) { flex: 1.2; }
.exec-col--left .exec-panel:nth-child(2) { flex: 0.9; }
.exec-col--left .exec-panel:nth-child(3) { flex: 1.1; }
.exec-col--center .exec-panel:nth-child(1) { flex: 1; }
.exec-col--center .exec-panel:nth-child(2) { flex: 1.2; }
.exec-col--center .exec-panel:nth-child(3) { flex: 1; }
.exec-col--right .exec-panel:nth-child(1) { flex: 1; }
.exec-col--right .exec-panel:nth-child(2) { flex: 0.9; }
.exec-col--right .exec-panel:nth-child(3) { flex: 1.1; }

.exec-panel { position: relative; background: rgba(9,24,50,.72); border: 1px solid rgba(58,128,196,.22); border-radius: 10px; padding: 10px 12px; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.exec-panel::after { content: ''; position: absolute; top: 0; right: 0; width: 16px; height: 16px; border-top: 2px solid #36cfc9; border-right: 2px solid #36cfc9; opacity: .7; }
.exec-panel::before { content: ''; position: absolute; bottom: 0; left: 0; width: 16px; height: 16px; border-bottom: 2px solid #36cfc9; border-left: 2px solid #36cfc9; opacity: .7; }

.exec-panel__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; flex: none; }
.exec-panel__title { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #e6f4ff; font-weight: 600; }
.exec-panel__ico { width: 4px; height: 14px; background: linear-gradient(180deg, #36cfc9, #1890ff); border-radius: 2px; }
.exec-panel__unit, .exec-panel__route, .exec-panel__tip { font-size: 11px; color: #7fa8d6; font-weight: 400; }
.exec-panel__body { flex: 1; min-height: 0; }
.exec-panel--scroll { overflow-y: auto; }
.exec-panel--scroll::-webkit-scrollbar { width: 4px; }
.exec-panel--scroll::-webkit-scrollbar-thumb { background: rgba(58,128,196,.45); border-radius: 2px; }

.exec-legend { font-size: 11px; color: #8fb4dd; }
.exec-legend span { margin-left: 12px; }
.exec-legend i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; vertical-align: middle; }

.echart { width: 100%; height: 100%; min-height: 0; }

.exec-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.exec-table th, .exec-table td { padding: 6px 7px; border-bottom: 1px solid rgba(58,128,196,.14); text-align: left; color: #bcd6f5; }
.exec-table th { position: sticky; top: 0; background: rgba(9,24,50,.95); color: #7fa8d6; font-weight: 600; z-index: 1; }
.exec-table td { max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty { text-align: center; color: #6f97c4; padding: 16px 0; }

.tag { font-size: 11px; padding: 1px 7px; border-radius: 3px; }
.tag-red { background: rgba(255,77,79,.18); color: #ff7875; }
.tag-orange { background: rgba(250,173,21,.18); color: #ffc53d; }
.tag-green { background: rgba(82,196,26,.18); color: #95de64; }
.tag-gray { background: rgba(140,140,140,.18); color: #bfbfbf; }
.lv-高 { background: rgba(255,77,79,.25); color: #ff7875; }
.lv-中 { background: rgba(250,173,21,.25); color: #ffc53d; }
.lv-低 { background: rgba(82,196,26,.25); color: #95de64; }

::deep(.ant-switch) { background: rgba(58,128,196,.3); }
::deep(.ant-switch-checked) { background: #1890ff; }

@media (max-width: 1400px) {
  .exec-main { grid-template-columns: 1fr 1.2fr 1fr; }
  .exec-kpi { grid-template-columns: repeat(4, 1fr); }
}
</style>
