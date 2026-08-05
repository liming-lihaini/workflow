<template>
  <div class="dashboard">
    <a-page-header title="监测数据驾驶舱" sub-title="核心业务指标与统计概览">
      <template #extra>
        <a-range-picker v-model:value="range" style="width: 260px" disabled />
        <a-button type="primary" :loading="loading" @click="load">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </template>
    </a-page-header>

    <a-spin :spinning="loading">
      <!-- 核心 KPI -->
      <a-row :gutter="16" class="mt-block">
        <a-col :span="4">
          <a-card>
            <a-statistic title="委托总数" :value="kpis.entrustTotal" />
          </a-card>
        </a-col>
        <a-col :span="4">
          <a-card>
            <a-statistic title="检测任务" :value="kpis.taskTotal" />
          </a-card>
        </a-col>
        <a-col :span="4">
          <a-card>
            <a-statistic title="检测结果数" :value="kpis.resultTotal" />
          </a-card>
        </a-col>
        <a-col :span="4">
          <a-card>
            <a-statistic title="报告总数" :value="kpis.reportTotal" />
          </a-card>
        </a-col>
        <a-col :span="4">
          <a-card>
            <a-statistic title="模板总数" :value="kpis.templateTotal" />
          </a-card>
        </a-col>
        <a-col :span="4">
          <a-card>
            <a-statistic title="累计超标项" :value="kpis.exceedTotal" :value-style="{ color: '#cf1322' }" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 待办 -->
      <a-row :gutter="16" class="mt-block">
        <a-col :span="12">
          <a-card title="待复核检测任务">
            <a-statistic :value="kpis.pendingReview" suffix="项" />
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="待审核报告">
            <a-statistic :value="kpis.pendingReport" suffix="份" />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="16" class="mt-block">
        <!-- 委托状态分布 -->
        <a-col :span="8">
          <a-card title="委托状态分布">
            <a-table
              :columns="distColumns"
              :data-source="entrustStatusDist"
              :pagination="false"
              size="small"
              row-key="name"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'value'">
                  <a-tag color="blue">{{ record.value }}</a-tag>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>

        <!-- 检测结论分布 -->
        <a-col :span="8">
          <a-card title="检测结论分布">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="达标">
                <a-progress
                  :percent="conclusionRate('达标')"
                  status="success"
                  :format="() => reachCount"
                />
              </a-descriptions-item>
              <a-descriptions-item label="超标">
                <a-progress
                  :percent="conclusionRate('超标')"
                  status="exception"
                  :format="() => exceedCount"
                />
              </a-descriptions-item>
              <a-descriptions-item label="超标率">
                <a-statistic :value="conclusionDist.exceedRate" :precision="1" suffix="%" :value-style="{ color: '#cf1322' }" />
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 报告状态分布 -->
        <a-col :span="8">
          <a-card title="报告状态分布">
            <a-table
              :columns="distColumns"
              :data-source="reportStatusDist"
              :pagination="false"
              size="small"
              row-key="name"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'value'">
                  <a-tag :color="reportTagColor(record.name)">{{ record.value }}</a-tag>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>

      <!-- 近 6 月趋势 -->
      <a-row :gutter="16" class="mt-block">
        <a-col :span="24">
          <a-card title="近 6 个月报告 / 超标趋势">
            <a-table
              :columns="trendColumns"
              :data-source="monthlyTrend"
              :pagination="false"
              size="small"
              row-key="month"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'reports'">
                  <a-progress
                    :percent="trendPercent(record.reports, maxReports)"
                    :format="() => record.reports"
                    status="active"
                  />
                </template>
                <template v-else-if="column.key === 'exceed'">
                  <a-progress
                    :percent="trendPercent(record.exceed, maxExceed)"
                    :format="() => record.exceed"
                    status="exception"
                  />
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>

      <!-- 委托单看板卡片 -->
      <a-row class="mt-block">
        <a-col :span="24">
          <a-card title="委托单看板">
            <a-empty v-if="!entrustCards.length" description="暂无委托单" />
            <a-row v-else :gutter="[16, 16]">
              <a-col v-for="c in entrustCards" :key="c.entrustId" :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
                <a-card size="small" hoverable class="entrust-card" @click="goEntrust(c.entrustId)">
                  <div class="ec-head">
                    <span class="ec-name">{{ c.entrustName || '—' }}</span>
                    <a-tag :color="entrustStatusColor(c.status)">{{ c.status || '—' }}</a-tag>
                  </div>
                  <div class="ec-no link-text" @click.stop="goEntrust(c.entrustId)">{{ c.entrustNo || '—' }}</div>
                  <a-descriptions :column="1" size="small" class="ec-meta">
                    <a-descriptions-item label="来源">{{ c.source || '—' }}</a-descriptions-item>
                    <a-descriptions-item label="点位数">{{ c.pointCount != null ? c.pointCount : 0 }}</a-descriptions-item>
                    <a-descriptions-item label="创建">{{ renderDate(c.createTime) }}</a-descriptions-item>
                  </a-descriptions>

                  <a-divider class="ec-div" orientation="left">派单信息（{{ c.dispatchCount || 0 }}）</a-divider>
                  <a-empty v-if="!c.dispatchList || !c.dispatchList.length" description="未派单" />
                  <div v-else class="ec-dispatch">
                    <div
                      v-for="o in c.dispatchList"
                      :key="o.orderId"
                      class="ec-order link-text"
                      @click.stop="goDispatch(o)"
                    >
                      <span class="ec-order-no">{{ o.orderNo }}</span>
                      <a-tag :color="orderStatusColor(o.status)">{{ o.status }}</a-tag>
                      <span v-if="o.leadName" class="ec-order-lead">{{ o.leadName }}</span>
                    </div>
                  </div>
                </a-card>
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { getDashboardOverview, getEntrustCards } from '../../../api/ems'

const router = useRouter()
const loading = ref(false)
const range = ref([])
const overview = ref({})
const entrustCards = ref([])

function goEntrust(id) {
  if (id) router.push({ path: '/ems/base/entrust', query: { detailId: id, tab: 'base' } })
}
function goDispatch(order) {
  if (order && order.orderId) router.push({ path: '/ems/base/dispatch-detail', query: { id: order.orderId } })
}
function entrustStatusColor(s) {
  return { '草稿': 'default', '待技术确认': 'orange', '已确认': 'green', '已退回': 'red' }[s] || 'default'
}
function orderStatusColor(s) {
  return { '待派单': 'default', '已派单': 'blue', '采样中': 'processing', '已完成': 'green' }[s] || 'default'
}
function renderDate(v) {
  if (!v) return '-'
  const d = new Date(String(v).replace(' ', 'T'))
  if (isNaN(d.getTime())) return v
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const kpis = computed(() => overview.value.kpis || {})
const entrustStatusDist = computed(() => overview.value.entrustStatusDist || [])
const conclusionDist = computed(() => overview.value.conclusionDist || { series: [] })
const reportStatusDist = computed(() => overview.value.reportStatusDist || [])
const monthlyTrend = computed(() => overview.value.monthlyTrend || [])

const reachCount = computed(() => {
  const s = conclusionDist.value.series.find((x) => x.name === '达标')
  return s ? s.value : 0
})
const exceedCount = computed(() => {
  const s = conclusionDist.value.series.find((x) => x.name === '超标')
  return s ? s.value : 0
})

const maxReports = computed(() =>
  Math.max(1, ...monthlyTrend.value.map((m) => m.reports))
)
const maxExceed = computed(() =>
  Math.max(1, ...monthlyTrend.value.map((m) => m.exceed))
)

const distColumns = [
  { title: '状态', dataIndex: 'name', key: 'name' },
  { title: '数量', dataIndex: 'value', key: 'value' }
]
const trendColumns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: '报告数', dataIndex: 'reports', key: 'reports' },
  { title: '超标项', dataIndex: 'exceed', key: 'exceed' }
]

function conclusionRate(name) {
  const total = reachCount.value + exceedCount.value
  const s = conclusionDist.value.series.find((x) => x.name === name)
  const v = s ? s.value : 0
  return total === 0 ? 0 : Math.round((v * 100) / total)
}

function trendPercent(v, max) {
  if (!max) return 0
  return Math.round((v * 100) / max)
}

function reportTagColor(name) {
  if (name === '已发布') return 'green'
  if (name === '已退回') return 'red'
  if (name === '待审核') return 'orange'
  return 'default'
}

async function load() {
  loading.value = true
  try {
    const [ov, cards] = await Promise.all([getDashboardOverview(), getEntrustCards({ limit: 12 })])
    overview.value = ov.data || {}
    entrustCards.value = cards.data || cards || []
  } catch (e) {
    message.error('加载驾驶舱数据失败：' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dashboard {
  padding: 0 16px 24px;
}
.mt-block {
  margin-top: 16px;
}
.entrust-card {
  height: 100%;
}
.ec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.ec-name {
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ec-no {
  margin: 6px 0 4px;
  font-size: 13px;
  color: #2563EB;
}
.ec-meta {
  margin-top: 4px;
}
.ec-div {
  margin: 8px 0 6px;
  font-size: 12px;
}
.ec-dispatch {
  max-height: 120px;
  overflow-y: auto;
}
.ec-order {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 0;
  font-size: 13px;
  color: #2563EB;
}
.ec-order-no {
  font-weight: 500;
}
.ec-order-lead {
  color: #8c8c8c;
  font-size: 12px;
  margin-left: auto;
}
.link-text {
  cursor: pointer;
}
.link-text:hover {
  text-decoration: underline;
}
</style>
