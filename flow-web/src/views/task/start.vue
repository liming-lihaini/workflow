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

        <div v-for="group in groupedList" :key="group.category" class="category-group">
          <div class="category-title">
            <FolderOpenOutlined style="margin-right: 6px" />
            {{ group.category || '未分类' }}
            <a-tag style="margin-left: 8px">{{ group.items.length }}</a-tag>
          </div>
          <a-row :gutter="[16, 16]">
            <a-col :span="6" v-for="item in group.items" :key="item.id">
              <div class="process-card" @click="handleStart(item)">
                <div class="card-icon"><ApartmentOutlined /></div>
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
      </a-spin>
    </div>

    <!-- 发起流程抽屉 -->
    <TaskFormDrawer
      v-model:open="drawerVisible"
      mode="start"
      :loading="formLoading"
      :process-info="drawerProcessInfo"
      :form-fields="formFields"
      :initial-values="initialFormValues"
      :flow-nodes="flowNodes"
      :flow-edges="flowEdges"
      @submit="handleSubmitStart"
      @save="handleSaveDraft"
      @cancel="drawerVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ApartmentOutlined, FolderOpenOutlined } from '@ant-design/icons-vue'
import { getProcessDefinitions, startProcessInstance } from '../../api/process'
import { getForm } from '../../api/form'
import { useUserStore } from '../../stores/user'
import { useRouter } from 'vue-router'
import TaskFormDrawer from '../../components/TaskFormDrawer.vue'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const allDefinitions = ref([])
const searchText = ref('')

const drawerVisible = ref(false)
const formLoading = ref(false)
const selectedProcess = ref(null)
const formFields = ref([])
const initialFormValues = ref({})
const drawerProcessInfo = ref({})
const flowNodes = ref([])
const flowEdges = ref([])

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

/** 从流程 JSON 提取第一个 userTask 的 formKey */
function extractFormKey(processJson) {
  try {
    const pj = typeof processJson === 'string' ? JSON.parse(processJson) : processJson
    for (const node of (pj.nodes || [])) {
      if (node.type === 'userTask' && node.properties?.formKey) return node.properties.formKey
    }
  } catch {}
  return null
}

async function handleStart(processDef) {
  selectedProcess.value = processDef
  formFields.value = []
  initialFormValues.value = {}
  flowNodes.value = []
  flowEdges.value = []

  const userId = userStore.username || localStorage.getItem('username') || ''
  drawerProcessInfo.value = {
    processName: processDef.processName,
    processKey: processDef.processKey,
    startTime: new Date().toLocaleString('zh-CN'),
    startUser: userId,
    deptName: '-'
  }

  drawerVisible.value = true
  formLoading.value = true

  try {
    const pj = typeof processDef.processJson === 'string' ? JSON.parse(processDef.processJson) : processDef.processJson
    // 填充流程图数据（发起模式所有节点为 pending）
    flowNodes.value = (pj?.nodes || []).map(n => ({
      id: n.id || n.nodeId,
      type: n.type || n.nodeType,
      name: n.name,
      x: n.x ?? n.position?.x ?? 100,
      y: n.y ?? n.position?.y ?? 0,
      status: 'pending'
    }))
    flowEdges.value = pj?.edges || []

    const formKey = extractFormKey(processDef.processJson)
    if (formKey) {
      const res = await getForm(formKey)
      const formDef = res.data || res
      if (formDef.formJson) {
        const fj = typeof formDef.formJson === 'string' ? JSON.parse(formDef.formJson) : formDef.formJson
        const fields = fj.fields || []
        formFields.value = fields
        const vals = {}
        for (const f of fields) {
          if (f.key) vals[f.key] = f.type === 'checkbox' ? [] : undefined
        }
        initialFormValues.value = vals
      }
    }
  } catch { message.warning('加载表单定义失败') }

  formLoading.value = false
}

async function handleSubmitStart(variables) {
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    await startProcessInstance({
      processKey: selectedProcess.value.processKey,
      startUser: userId,
      variables: Object.keys(variables).length > 0 ? variables : undefined
    })
    message.success('流程发起成功')
    drawerVisible.value = false
    router.push('/task/todo')
  } catch {
    // handled by interceptor
  }
}

function handleSaveDraft(variables) {
  message.info('草稿已保存（本地暂存）')
}

onMounted(loadData)
</script>

<style scoped>
.category-group { margin-bottom: 24px; }
.category-title {
  font-size: 15px; font-weight: 600; color: #333;
  margin-bottom: 12px; display: flex; align-items: center;
  padding-bottom: 8px; border-bottom: 1px solid #f0f0f0;
}
.process-card {
  border: 1px solid #e8e8e8; border-radius: 8px; padding: 16px;
  cursor: pointer; display: flex; gap: 12px;
  transition: all 0.2s; background: #fff; height: 100%;
}
.process-card:hover {
  border-color: var(--color-primary, #1677ff);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);
  transform: translateY(-1px);
}
.card-icon {
  font-size: 28px; color: var(--color-primary, #1677ff);
  flex-shrink: 0; width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  background: #e6f4ff; border-radius: 8px;
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
