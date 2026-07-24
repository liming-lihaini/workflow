<template>
  <div class="start-page">
    <!-- 顶部深蓝标题栏 -->
    <div class="top-bar">
      <div class="top-title">
        {{ processInfo.processName || '发起流程' }} - 流程发起
        <a-tag color="blue" style="margin-left: 8px">v{{ processInfo.version || 1 }}</a-tag>
      </div>
      <div class="top-opt">
        <a @click="goBack">返回列表</a>
      </div>
    </div>

    <!-- Tab 切换栏 -->
    <div class="tab-wrap">
      <div class="tab-list">
        <div :class="['tab-item', activeTab === 'form' ? 'active' : '']" @click="activeTab = 'form'">表单详情</div>
        <div :class="['tab-item', activeTab === 'flow' ? 'active' : '']" @click="activeTab = 'flow'">流程图</div>
      </div>
    </div>

    <!-- Tab 内容区 -->
    <div class="tab-content">
      <!-- Tab1: 表单详情 - 左右分栏 -->
      <div v-show="activeTab === 'form'" class="tab-panel">
        <div class="form-container">
          <!-- 左侧：表单 + 操作按钮 -->
          <div class="left-form">
            <!-- 基本信息表格 -->
            <div class="form-base">
              <table class="form-table">
                <tr>
                  <td class="label">流程标识</td>
                  <td>{{ processInfo.processKey || '-' }}</td>
                  <td class="label">流程名称</td>
                  <td>{{ processInfo.processName || '-' }}</td>
                </tr>
                <tr>
                  <td class="label">发起人</td>
                  <td>{{ startUser }}</td>
                  <td class="label">发起时间</td>
                  <td>{{ startTimeStr }}</td>
                </tr>
              </table>
            </div>

            <!-- 表单内容 -->
            <div class="panel-title">表单内容</div>
            <div class="form-body">
              <a-spin :spinning="formLoading">
                <FormRenderer
                  :form-json="formJson"
                  :fields="formFields"
                  v-model="formValues"
                  mode="editable"
                />
              </a-spin>
            </div>

            <!-- 底部操作按钮 -->
            <div class="opt-bottom">
              <div class="btn-group">
                <a-button class="btn-submit" type="primary" @click="handleSubmit" :loading="submitLoading">
                  提交流程
                </a-button>
                <a-button class="btn-back" @click="goBack">返回列表</a-button>
              </div>
            </div>
          </div>

          <!-- 右侧：流程说明 -->
          <div class="right-history">
            <div class="panel-title">流程说明</div>
            <div class="history-inner">
              <div class="info-item">
                <div class="info-label">流程分类</div>
                <div class="info-value">{{ processInfo.category || '-' }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">流程版本</div>
                <div class="info-value">v{{ processInfo.version || 1 }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">流程描述</div>
                <div class="info-value">{{ processInfo.description || '暂无描述' }}</div>
              </div>
              <div class="info-item" v-if="flowSequence.length > 0">
                <div class="info-label">流程节点</div>
                <div class="info-value">共 {{ flowSequence.length }} 个节点</div>
              </div>
              <div style="margin-top: 16px">
                <div class="info-label" style="margin-bottom: 8px">节点列表</div>
                <div class="node-list" v-if="flowSequence.length > 0">
                  <div class="node-item" v-for="(item, idx) in flowSequence" :key="idx">
                    <span class="node-idx">{{ idx + 1 }}</span>
                    <span class="node-name">{{ item.name }}</span>
                  </div>
                </div>
                <div v-else style="color: #999; font-size: 12px">暂无节点</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab2: 流程图 -->
      <div v-show="activeTab === 'flow'" class="tab-panel">
        <div class="panel-title">流程轨迹拓扑图</div>
        <div class="flow-chart-box" v-if="flowSequence.length > 0">
          <template v-for="(item, idx) in flowSequence" :key="idx">
            <div class="flow-node">
              <div class="flow-node-name">{{ item.name }}</div>
              <div class="flow-node-user">{{ item.ntLabel }}</div>
            </div>
            <div v-if="idx < flowSequence.length - 1" class="flow-line"></div>
          </template>
        </div>
        <a-empty v-else description="暂无流程图数据" style="margin-top: 40px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getProcessDefinitionByKey, startProcessInstance } from '../../api/process'
import { getForm } from '../../api/form'
import { useUserStore } from '../../stores/user'
import FormRenderer from '../../components/FormRenderer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const processKey = computed(() => route.query.processKey)
const activeTab = ref('form')

// 流程信息
const processInfo = ref({})
const formLoading = ref(false)
const submitLoading = ref(false)
const formJson = ref(null)
const formFields = ref([])
const formValues = ref({})

// 流程图
const flowNodesRaw = ref([])
const flowEdgesRaw = ref([])

const startUser = computed(() => userStore.username || localStorage.getItem('username') || '')
const startTimeStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

function goBack() { router.push('/task/start') }

// ====== 流程图：横向节点序列 ======
const NT_LABELS = {
  start: '开始', end: '结束', userTask: '用户任务', serviceTask: '服务任务',
  scriptTask: '脚本任务', exclusiveGateway: '排他网关', parallelGateway: '并行网关',
  inclusiveGateway: '包容网关'
}

const flowSequence = computed(() => {
  if (!flowNodesRaw.value.length) return []
  const nodes = flowNodesRaw.value
  const edges = flowEdgesRaw.value || []

  // 构建邻接表和入度表
  const adj = {}
  const inDeg = {}
  const nodeMap = {}
  nodes.forEach(n => {
    const id = n.id || n.nodeId
    adj[id] = []
    inDeg[id] = 0
    nodeMap[id] = n
  })
  edges.forEach(e => {
    const src = e.source || e.from
    const tgt = e.target || e.to
    if (src && tgt && adj[src]) {
      adj[src].push(tgt)
      inDeg[tgt] = (inDeg[tgt] || 0) + 1
    }
  })

  // BFS 拓扑排序
  const queue = []
  Object.keys(inDeg).forEach(id => { if (inDeg[id] === 0) queue.push(id) })
  const sorted = []
  while (queue.length) {
    const id = queue.shift()
    sorted.push(id)
    (adj[id] || []).forEach(next => {
      inDeg[next]--
      if (inDeg[next] === 0) queue.push(next)
    })
  }
  // 未连线的节点补充到末尾
  nodes.forEach(n => {
    const id = n.id || n.nodeId
    if (!sorted.includes(id)) sorted.push(id)
  })

  // 过滤网关并映射
  return sorted
    .map(id => nodeMap[id])
    .filter(n => {
      const t = n.type || n.nodeType
      return t !== 'exclusiveGateway' && t !== 'parallelGateway' && t !== 'inclusiveGateway'
    })
    .map(n => {
      const nt = n.type || n.nodeType || 'custom'
      return {
        id: n.id || n.nodeId,
        name: n.name || NT_LABELS[nt] || nt,
        ntLabel: NT_LABELS[nt] || nt,
      }
    })
})

// ====== 数据加载 ======
async function loadAll() {
  if (!processKey.value) { message.error('缺少流程标识'); return }
  formLoading.value = true

  try {
    // 1. 获取流程定义
    const defRes = await getProcessDefinitionByKey(processKey.value)
    const def = defRes.data || defRes
    processInfo.value = {
      processName: def.processName || processKey.value,
      processKey: def.processKey || processKey.value,
      category: def.category || '',
      version: def.version || 1,
      description: def.description || '',
    }

    if (def?.processJson) {
      const pj = typeof def.processJson === 'string' ? JSON.parse(def.processJson) : def.processJson
      flowNodesRaw.value = pj.nodes || []
      flowEdgesRaw.value = pj.edges || []

      // 提取 formKey
      let formKey = null
      for (const n of (pj.nodes || [])) {
        if (n.type === 'userTask' && n.properties?.formKey) { formKey = n.properties.formKey; break }
      }

      // 加载表单
      if (formKey) {
        try {
          const formRes = await getForm(formKey)
          const formDef = formRes.data || formRes
          if (formDef.formJson) {
            const fj = typeof formDef.formJson === 'string' ? JSON.parse(formDef.formJson) : formDef.formJson
            formJson.value = fj

            // 初始化表单值
            const vals = {}
            if (fj.sections && Array.isArray(fj.sections)) {
              for (const section of fj.sections) {
                for (const row of (section.children || [])) {
                  for (const cell of (row.cells || [])) {
                    for (const f of (cell.fields || [])) {
                      const k = f.field || f.key || f.id
                      if (k) vals[k] = f.type === 'checkbox' ? [] : undefined
                    }
                  }
                }
              }
            } else if (fj.fields) {
              for (const f of fj.fields) {
                if (f.key) vals[f.key] = f.type === 'checkbox' ? [] : undefined
              }
            }
            formValues.value = vals
          }
        } catch { /* ignore */ }
      }
    }
  } catch (e) {
    message.error('加载流程定义失败: ' + (e.message || ''))
  }
  formLoading.value = false
}

// ====== 提交 ======
async function handleSubmit() {
  submitLoading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    await startProcessInstance({
      processKey: processInfo.value.processKey,
      startUser: userId,
      variables: Object.keys(formValues.value).length > 0 ? formValues.value : undefined
    })
    message.success('流程发起成功')
    router.push('/task/todo')
  } catch {
    // handled by interceptor
  }
  submitLoading.value = false
}

onMounted(loadAll)
</script>

<style scoped>
.start-page {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
  margin: -16px;
}

/* 顶部深蓝标题栏 */
.top-bar {
  height: 44px;
  background: #0052a5;
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 20px;
  justify-content: space-between;
}
.top-title {
  font-size: 16px;
  font-weight: normal;
}
.top-opt a {
  color: #fff;
  margin-left: 16px;
  opacity: 0.9;
  cursor: pointer;
}
.top-opt a:hover { opacity: 1; }

/* Tab 切换栏 */
.tab-wrap {
  background: #fff;
  border: 1px solid #e1e6ef;
  border-bottom: none;
  margin: 10px auto 0;
  max-width: 1400px;
  padding: 0 10px;
}
.tab-list {
  display: flex;
}
.tab-item {
  height: 38px;
  line-height: 38px;
  padding: 0 22px;
  border-right: 1px solid #e1e6ef;
  cursor: pointer;
  position: relative;
  font-size: 14px;
  color: #333;
}
.tab-item.active {
  color: #0052a5;
  font-weight: bold;
}
.tab-item.active::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 100%;
  height: 2px;
  background: #0052a5;
}

/* Tab 内容 */
.tab-content {
  max-width: 1400px;
  margin: 0 auto 10px;
  padding: 0 10px;
}
.tab-panel {
  background: #fff;
  border: 1px solid #e1e6ef;
  padding: 16px;
}

/* 表单Tab内左右分栏 */
.form-container {
  display: flex;
  gap: 16px;
}
.left-form {
  flex: 0 0 72%;
}
.right-history {
  flex: 0 0 26%;
  border: 1px solid #e1e6ef;
  max-height: 750px;
  overflow-y: auto;
}

/* 面板标题 */
.panel-title {
  height: 36px;
  line-height: 36px;
  background: #f0f4f9;
  padding: 0 12px;
  font-weight: bold;
  border: 1px solid #e1e6ef;
  margin-bottom: 12px;
  font-size: 14px;
}

/* 基本信息表格 */
.form-base { margin-bottom: 20px; }
.form-table { width: 100%; border-collapse: collapse; }
.form-table td {
  border: 1px solid #e8edf3;
  padding: 10px 12px;
  font-size: 14px;
}
.form-table td.label {
  width: 100px;
  background: #f7f9fc;
  text-align: right;
  color: #444;
  font-weight: 500;
}

/* 表单内容区 */
.form-body {
  margin: 0 0 20px;
  padding: 12px;
  background: #f7f9fc;
  min-height: 100px;
  border: 1px solid #e1e6ef;
}

/* 底部操作按钮栏 */
.opt-bottom {
  padding: 12px 0;
  border-top: 1px solid #e1e6ef;
  margin-top: 12px;
}
.btn-group { display: flex; gap: 10px; flex-wrap: wrap; }
.btn-submit { background: #0052a5 !important; border-color: #0052a5 !important; }
.btn-back { color: #666 !important; border-color: #c0c6cf !important; }

/* 右侧流程说明 */
.history-inner { padding: 12px; }
.info-item {
  padding: 10px 0;
  border-bottom: 1px dashed #eee;
}
.info-item:last-child { border-bottom: none; }
.info-label {
  font-weight: 500;
  font-size: 13px;
  color: #444;
  margin-bottom: 4px;
}
.info-value {
  font-size: 13px;
  color: #666;
}

/* 节点列表 */
.node-list { display: flex; flex-direction: column; gap: 6px; }
.node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f7f9fc;
  border-radius: 4px;
  font-size: 13px;
}
.node-idx {
  width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  background: #0052a5;
  color: #fff;
  border-radius: 50%;
  font-size: 11px;
  flex-shrink: 0;
}
.node-name { color: #333; }

/* 流程图 - 横向节点流 */
.flow-chart-box {
  padding: 30px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  overflow-x: auto;
}
.flow-node {
  width: 130px;
  min-height: 70px;
  border: 2px solid #d0d7df;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #fafbfd;
  flex-shrink: 0;
  padding: 8px;
}
.flow-node-name { font-weight: bold; margin-bottom: 4px; font-size: 13px; text-align: center; }
.flow-node-user { font-size: 12px; color: #666; text-align: center; }
.flow-line {
  width: 40px;
  height: 3px;
  background: #d0d7df;
  margin: 0 -2px;
  z-index: -1;
  flex-shrink: 0;
}
</style>
