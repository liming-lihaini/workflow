<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">部门管理</span>
        <div style="display: flex; gap: 8px; align-items: center;">
          <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
            <a-radio-button value="table">表格视图</a-radio-button>
            <a-radio-button value="tree">树形视图</a-radio-button>
          </a-radio-group>
          <a-button v-if="hasPerm('system:dept:create')" type="primary" @click="showModal()">新建部门</a-button>
        </div>
      </div>

      <!-- ====== 表格视图 ====== -->
      <template v-if="viewMode === 'table'">
        <div class="search-bar">
          <a-input v-model:value="searchForm.deptName" placeholder="部门名称" style="width: 200px" allow-clear @press-enter="handleSearch" />
          <a-select v-model:value="searchForm.status" placeholder="状态" style="width: 120px" allow-clear>
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
        </div>
        <a-table :columns="columns" :data-source="dataList" :loading="loading" row-key="id" :pagination="pagination" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'leaderName'">
              <span v-if="record.leaderName">{{ record.leaderName }}</span>
              <span v-else style="color: #999">未设置</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <span class="action-link" @click="showLeaderModal(record)">设置领导</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showModal(record, 'add')">添加子部门</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showModal(record, 'edit')">编辑</span>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
                <span class="action-link danger">删除</span>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </template>

      <!-- ====== 树形视图 ====== -->
      <template v-if="viewMode === 'tree'">
        <div class="tree-layout">
          <!-- 左侧：部门树 -->
          <div class="tree-left">
            <div class="tree-header">
              <a-input v-model:value="treeSearch" placeholder="搜索部门" allow-clear size="small" />
            </div>
            <div class="tree-body">
              <a-tree
                v-if="treeData.length"
                :tree-data="treeData"
                :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                :selected-keys="treeSelectedKeys"
                :expanded-keys="expandedKeys"
                :auto-expand-parent="autoExpandParent"
                @select="handleTreeSelect"
                @expand="onExpand"
              >
                <template #title="{ deptName, leaderName }">
                  <span class="tree-node-title">
                    <span>{{ deptName }}</span>
                    <span v-if="leaderName" class="tree-node-badge">{{ leaderName }}</span>
                  </span>
                </template>
              </a-tree>
              <a-empty v-else description="暂无部门数据" :image-style="{ height: '40px' }" />
            </div>
          </div>

          <!-- 右侧：详情面板 -->
          <div class="tree-right">
            <template v-if="selectedDept">
              <a-tabs v-model:activeKey="activeTab">
                <a-tab-pane key="info" tab="部门信息">
                  <div class="detail-section">
                    <div class="detail-row">
                      <span class="detail-label">部门名称</span>
                      <span class="detail-value">{{ selectedDept.deptName }}</span>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">部门 ID</span>
                      <span class="detail-value">{{ selectedDept.id }}</span>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">排序号</span>
                      <span class="detail-value">{{ selectedDept.sortOrder ?? '-' }}</span>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">状态</span>
                      <a-tag :color="selectedDept.status === 1 ? 'green' : 'default'">{{ selectedDept.status === 1 ? '启用' : '禁用' }}</a-tag>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">部门领导</span>
                      <span class="detail-value">
                        <span v-if="selectedDept.leaderName">{{ selectedDept.leaderName }}</span>
                        <span v-else style="color: #999">未设置</span>
                      </span>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">创建时间</span>
                      <span class="detail-value">{{ formatDate(selectedDept.createTime) || '-' }}</span>
                    </div>
                    <div style="margin-top: 16px; display: flex; gap: 8px;">
                      <a-button type="primary" size="small" @click="showLeaderModal(selectedDept)">设置领导</a-button>
                      <a-button size="small" @click="showModal(selectedDept, 'add')">添加子部门</a-button>
                      <a-button size="small" @click="showModal(selectedDept, 'edit')">编辑</a-button>
                      <a-popconfirm title="确定删除？" @confirm="handleDelete(selectedDept)">
                        <a-button size="small" danger>删除</a-button>
                      </a-popconfirm>
                    </div>
                  </div>
                </a-tab-pane>

                <a-tab-pane key="members" tab="直属人员">
                  <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
                    <span style="font-size: 13px; color: #666;">部门 {{ selectedDept.deptName }} 的人员列表</span>
                    <a-button size="small" type="link" @click="loadDeptMembers">刷新</a-button>
                  </div>
                  <a-table
                    :columns="memberColumns"
                    :data-source="deptMembers"
                    :loading="membersLoading"
                    row-key="id"
                    :pagination="{ pageSize: 10 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'status'">
                        <a-tag :color="record.status === 1 ? 'green' : 'default'" size="small">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
                      </template>
                    </template>
                  </a-table>
                </a-tab-pane>
              </a-tabs>
            </template>
            <template v-else>
              <div class="empty-detail">
                <a-empty description="请在左侧选择部门查看详情" />
              </div>
            </template>
          </div>
        </div>
      </template>
    </div>

    <!-- 新建/编辑部门弹窗 -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleSubmit" :confirm-loading="submitLoading">
      <a-form :model="formState" layout="vertical">
        <a-form-item label="部门名称" required>
          <a-input v-model:value="formState.deptName" placeholder="请输入部门名称" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formState.sortOrder" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formState.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 设置领导弹窗 -->
    <a-modal v-model:open="leaderModalVisible" title="设置部门领导" @ok="handleSetLeader" :confirm-loading="leaderSubmitLoading" width="480px">
      <div style="margin-bottom: 12px">
        <span style="color: #666">部门：</span>
        <a-tag color="blue">{{ leaderDept?.deptName }}</a-tag>
        <span v-if="leaderDept?.leaderName" style="margin-left: 8px; color: #999; font-size: 12px">当前领导：{{ leaderDept.leaderName }}</span>
      </div>
      <a-form-item label="选择领导" style="margin-bottom: 0">
        <a-select
          v-model:value="selectedLeaderId"
          show-search
          :filter-option="false"
          placeholder="输入姓名搜索用户"
          :loading="userSearchLoading"
          @search="handleUserSearch"
          allow-clear
          style="width: 100%"
        >
          <a-select-option v-for="u in userOptions" :key="u.id" :value="u.id">
            {{ u.realName || u.username }} ({{ u.username }})
          </a-select-option>
        </a-select>
      </a-form-item>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { getDeptsPage, getDeptTree, getDept, createDept, updateDept, deleteDept, setDeptLeader, getUsersPage } from '../../api/system'
import { formatDate, renderDate } from '../../utils/date'
import { usePermission } from '../../composables/usePermission'

// ====== 通用 ======
const { hasPerm } = usePermission()
const viewMode = ref('table')
const loading = ref(false)
const dataList = ref([])
const modalVisible = ref(false)
const submitLoading = ref(false)
const modalTitle = ref('')
const editMode = ref('add')
const parentRecord = ref(null)
const editingRecord = ref(null)

// 设置领导相关
const leaderModalVisible = ref(false)
const leaderSubmitLoading = ref(false)
const leaderDept = ref(null)
const selectedLeaderId = ref(null)
const userOptions = ref([])
const userSearchLoading = ref(false)
let userSearchTimer = null

// 表格视图
const searchForm = reactive({ deptName: '', status: undefined })
const pagination = reactive({
  current: 1, pageSize: 10, total: 0, showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})
const columns = [
  { title: '部门名称', dataIndex: 'deptName', key: 'deptName' },
  { title: '部门领导', key: 'leaderName', width: 120 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate },
  { title: '操作', key: 'action', width: 300 }
]
const formState = reactive({ deptName: '', sortOrder: 0, status: 1 })

// ====== 树形视图 ======
const treeData = ref([])
const treeSelectedKeys = ref([])
const expandedKeys = ref([])
const autoExpandParent = ref(true)
const treeSearch = ref('')
const selectedDept = ref(null)
const activeTab = ref('info')
const deptMembers = ref([])
const membersLoading = ref(false)

const memberColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate }
]

// ====== 数据加载 ======
async function loadData() {
  loading.value = true
  try {
    const res = await getDeptsPage({
      deptName: searchForm.deptName || undefined,
      status: searchForm.status,
      page: pagination.current,
      size: pagination.pageSize
    })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
  } catch { /* ignore */ }
  loading.value = false
}

async function loadTreeData() {
  try {
    const res = await getDeptTree()
    const data = res.data || res
    treeData.value = Array.isArray(data) ? data : []
    // 默认展开所有
    expandedKeys.value = flattenKeys(treeData.value)
  } catch { treeData.value = [] }
}

function flattenKeys(tree) {
  const keys = []
  function walk(nodes) {
    nodes.forEach(n => {
      keys.push(n.id)
      if (n.children?.length) walk(n.children)
    })
  }
  walk(tree)
  return keys
}

async function loadDeptMembers() {
  if (!selectedDept.value) return
  membersLoading.value = true
  try {
    const res = await getUsersPage({ deptId: selectedDept.value.id, page: 1, size: 50 })
    const d = res.data || res
    deptMembers.value = Array.isArray(d) ? d : (d.records || d.list || [])
  } catch { deptMembers.value = [] }
  membersLoading.value = false
}

// ====== 树形交互 ======
function handleTreeSelect(keys, { node }) {
  if (keys.length === 0) return
  treeSelectedKeys.value = keys
  selectedDept.value = node
  activeTab.value = 'info'
  deptMembers.value = []
  loadDeptMembers()
}

function onExpand(keys) {
  expandedKeys.value = keys
  autoExpandParent.value = false
}

// ====== 表格视图操作 ======
function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.deptName = ''; searchForm.status = undefined; pagination.current = 1; loadData() }
function handleTableChange(pag) { pagination.current = pag.current; pagination.pageSize = pag.pageSize; loadData() }

// ====== 视图切换时刷新数据 ======
watch(viewMode, (mode) => {
  if (mode === 'table') loadData()
  else loadTreeData()
})

// ====== 弹窗操作 ======
function showModal(record, mode) {
  formState.deptName = ''; formState.sortOrder = 0; formState.status = 1
  if (mode === 'edit' && record) {
    editMode.value = 'edit'; editingRecord.value = record; parentRecord.value = null
    formState.deptName = record.deptName; formState.sortOrder = record.sortOrder; formState.status = record.status
    modalTitle.value = '编辑部门'
  } else if (mode === 'add' && record) {
    editMode.value = 'addChild'; parentRecord.value = record; editingRecord.value = null
    modalTitle.value = `添加子部门（${record.deptName}）`
  } else {
    editMode.value = 'add'; parentRecord.value = null; editingRecord.value = null
    modalTitle.value = '新建部门'
  }
  modalVisible.value = true
}

function showLeaderModal(record) {
  leaderDept.value = record
  selectedLeaderId.value = record.leaderId || null
  userOptions.value = []
  if (record.leaderId && record.leaderName) {
    userOptions.value = [{ id: record.leaderId, realName: record.leaderName, username: record.leaderName }]
  }
  leaderModalVisible.value = true
}

async function handleUserSearch(keyword) {
  clearTimeout(userSearchTimer)
  if (!keyword) { userOptions.value = []; return }
  userSearchTimer = setTimeout(async () => {
    userSearchLoading.value = true
    try {
      const res = await getUsersPage({ keyword, page: 1, size: 10 })
      const d = res.data || res
      const records = Array.isArray(d) ? d : (d.records || d.list || [])
      userOptions.value = records
    } catch { userOptions.value = [] }
    finally { userSearchLoading.value = false }
  }, 300)
}

async function handleSetLeader() {
  if (!selectedLeaderId.value) { message.warning('请选择领导'); return }
  const selectedUser = userOptions.value.find(u => u.id === selectedLeaderId.value)
  if (!selectedUser) { message.warning('未找到选中用户'); return }
  leaderSubmitLoading.value = true
  try {
    const leaderName = selectedUser.realName || selectedUser.username
    await setDeptLeader(leaderDept.value.id, selectedLeaderId.value, leaderName)
    message.success('设置领导成功')
    leaderModalVisible.value = false
    refreshAfterChange()
  } catch (e) {
    message.error('设置失败: ' + (e.message || ''))
  }
  leaderSubmitLoading.value = false
}

async function handleSubmit() {
  if (!formState.deptName) { message.warning('请输入部门名称'); return }
  submitLoading.value = true
  try {
    const payload = { ...formState }
    if (editMode.value === 'addChild' && parentRecord.value) payload.parentId = parentRecord.value.id
    if (editMode.value === 'edit' && editingRecord.value) {
      await updateDept(editingRecord.value.id, payload)
      message.success('更新成功')
    } else {
      await createDept(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    refreshAfterChange()
  } catch { /* ignore */ }
  submitLoading.value = false
}

async function handleDelete(record) {
  try { await deleteDept(record.id); message.success('删除成功'); refreshAfterChange() } catch { /* ignore */ }
}

function refreshAfterChange() {
  if (viewMode.value === 'table') loadData()
  else {
    loadTreeData()
    // 如果当前有选中节点，重新加载其信息
    if (selectedDept.value) {
      setTimeout(async () => {
        try {
          const res = await getDept(selectedDept.value.id)
          const updated = res.data || res
          selectedDept.value = { ...selectedDept.value, ...updated }
          if (activeTab.value === 'members') loadDeptMembers()
        } catch { /* ignore */ }
      }, 300)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

/* 树形视图布局 */
.tree-layout {
  display: flex;
  gap: 0;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  min-height: 500px;
  overflow: hidden;
}
.tree-left {
  width: 300px;
  min-width: 260px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.tree-header {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
}
.tree-right {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #fff;
}

/* 树节点标题 */
.tree-node-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 4px;
}
.tree-node-badge {
  font-size: 10px;
  color: #1677ff;
  background: #e6f4ff;
  padding: 0 4px;
  border-radius: 2px;
  line-height: 16px;
  flex-shrink: 0;
  margin-left: auto;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 详情面板 */
.detail-section {
  padding: 8px 0;
}
.detail-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.detail-label {
  width: 90px;
  font-size: 13px;
  color: #888;
  flex-shrink: 0;
}
.detail-value {
  font-size: 13px;
  color: #333;
}
.empty-detail {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
}
</style>
