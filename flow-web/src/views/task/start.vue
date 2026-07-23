<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">发起流程</span>
        <a-input-search v-model:value="searchText" placeholder="搜索流程名称"
          style="width: 240px" allow-clear @search="loadData" @change="filterData" />
      </div>

      <a-spin :spinning="loading">
        <a-empty v-if="!loading && groupedList.length === 0" description="暂无已部署的流程" />

        <div v-for="(group, gIdx) in groupedList" :key="group.category" class="category-group">
          <div class="category-header" :style="{ background: categoryColors[gIdx % categoryColors.length].bg, borderColor: categoryColors[gIdx % categoryColors.length].border }">
            <div class="category-bar" :style="{ background: categoryColors[gIdx % categoryColors.length].bar }"></div>
            <span class="category-label">{{ group.category || '未分类' }}</span>
            <span class="category-count">{{ group.items.length }} 个流程</span>
          </div>
          <div class="category-body" :style="{ borderColor: categoryColors[gIdx % categoryColors.length].border }">
            <a-row :gutter="[16, 16]">
              <a-col :span="6" v-for="item in group.items" :key="item.id">
                <div class="process-card" @click="handleStart(item)">
                  <div class="card-icon" :style="{ background: categoryColors[gIdx % categoryColors.length].iconBg, color: categoryColors[gIdx % categoryColors.length].bar }">
                    <ApartmentOutlined />
                  </div>
                  <div class="card-body">
                    <div class="card-title">{{ item.processName }}</div>
                    <div class="card-desc">{{ item.description || '暂无描述' }}</div>
                    <div class="card-footer">
                      <a-tag color="blue" size="small">v{{ item.version }}</a-tag>
                      <span class="card-key">{{ item.processKey }}</span>
                    </div>
                  </div>
                </div>
              </a-col>
            </a-row>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ApartmentOutlined } from '@ant-design/icons-vue'
import { getProcessDefinitions } from '../../api/process'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)
const allDefinitions = ref([])
const searchText = ref('')

// 分类配色方案（循环使用）
const categoryColors = [
  { bg: '#e6f4ff', border: '#91caff', bar: '#1677ff', iconBg: '#bae0ff' },
  { bg: '#f6ffed', border: '#b7eb8f', bar: '#52c41a', iconBg: '#d9f7be' },
  { bg: '#fff7e6', border: '#ffd591', bar: '#fa8c16', iconBg: '#ffe7ba' },
  { bg: '#f9f0ff', border: '#d3adf7', bar: '#722ed1', iconBg: '#efdbff' },
  { bg: '#fff1f0', border: '#ffa39e', bar: '#f5222d', iconBg: '#ffccc7' },
  { bg: '#e6fffb', border: '#87e8de', bar: '#13c2c2', iconBg: '#b5f5ec' },
]

// 按分类分组
const groupedList = computed(() => {
  const kw = (searchText.value || '').trim().toLowerCase()
  const filtered = allDefinitions.value.filter(d => {
    if (!kw) return true
    return (d.processName || '').toLowerCase().includes(kw) ||
           (d.processKey || '').toLowerCase().includes(kw) ||
           (d.category || '').toLowerCase().includes(kw)
  })
  const groupMap = {}
  for (const item of filtered) {
    const cat = item.category || ''
    if (!groupMap[cat]) groupMap[cat] = []
    groupMap[cat].push(item)
  }
  return Object.entries(groupMap).map(([category, items]) => ({ category, items }))
})

function filterData() {}

async function loadData() {
  loading.value = true
  try {
    const res = await getProcessDefinitions({ status: 1 })
    const data = res.data || res || []
    allDefinitions.value = Array.isArray(data) ? data : (data.list || data.records || [])
  } catch { allDefinitions.value = [] }
  loading.value = false
}

/** 点击卡片，跳转到发起流程详情页 */
function handleStart(processDef) {
  router.push({ path: '/task/start-detail', query: { processKey: processDef.processKey } })
}

onMounted(loadData)
</script>

<style scoped>
.category-group { margin-bottom: 24px; }
.category-header {
  display: flex; align-items: center; padding: 10px 16px;
  border: 1px solid; border-radius: 8px 8px 0 0;
  position: relative; overflow: hidden;
}
.category-bar {
  width: 4px; height: 20px; border-radius: 2px;
  margin-right: 10px; flex-shrink: 0;
}
.category-label {
  font-size: 15px; font-weight: 600; color: #1a1a1a;
  margin-right: 10px;
}
.category-count {
  font-size: 12px; color: #999; font-weight: normal;
}
.category-body {
  border: 1px solid; border-top: none;
  border-radius: 0 0 8px 8px;
  padding: 16px;
  background: #fafbfd;
}
.process-card {
  border: 1px solid #e8e8e8; border-radius: 8px; padding: 16px;
  cursor: pointer; display: flex; gap: 12px;
  transition: all 0.25s; background: #fff; height: 100%;
}
.process-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
.card-icon {
  font-size: 28px;
  flex-shrink: 0; width: 44px; height: 44px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px; transition: all 0.25s;
}
.card-body { flex: 1; min-width: 0; }
.card-title {
  font-weight: 600; font-size: 14px; color: #333;
  margin-bottom: 4px; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
}
.card-desc {
  font-size: 12px; color: #999; margin-bottom: 8px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.card-footer { display: flex; align-items: center; gap: 8px; }
.card-key { font-size: 11px; color: #bbb; }
</style>
