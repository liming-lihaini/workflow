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
    </a-spin>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { getDashboardOverview } from '../../../api/ems'

const loading = ref(false)
const range = ref([])
const overview = ref({})

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
    const res = await getDashboardOverview()
    overview.value = res.data || {}
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
</style>
