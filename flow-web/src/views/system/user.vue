<template>
  <div class="page-wrap">
    <a-row :gutter="16">
      <!-- 左侧部门树 -->
      <a-col :span="6">
        <div class="card-wrap dept-panel">
          <div class="page-header">
            <span class="page-title">部门</span>
          </div>
          <div class="tree-header">
            <a-input-search
              v-model:value="deptSearchText"
              placeholder="搜索部门名称"
              allow-clear
              size="small"
            />
            <div class="tree-actions">
              <span class="tree-action-btn" @click="expandAll" title="展开全部">
                <DownCircleOutlined /> 展开全部
              </span>
              <a-divider type="vertical" style="margin: 0 6px" />
              <span class="tree-action-btn" @click="collapseAll" title="折叠全部">
                <RightCircleOutlined /> 折叠全部
              </span>
            </div>
          </div>
          <div class="dept-tree-wrap">
            <a-tree
              v-if="filteredDeptTree.length"
              v-model:expanded-keys="expandedKeys"
              v-model:selected-keys="selectedKeys"
              :tree-data="filteredDeptTree"
              :field-names="{ title: 'deptName', key: 'id', children: 'children' }"
              @select="onDeptSelect"
            >
              <template #title="node">
                <span class="tree-node-title">
                  <span v-if="deptKeyword && (node.deptName || '').toLowerCase().includes(deptKeyword)">
                    {{ node.deptName.slice(0, node.deptName.toLowerCase().indexOf(deptKeyword)) }}<span class="dept-name-hit">{{ node.deptName.slice(node.deptName.toLowerCase().indexOf(deptKeyword), node.deptName.toLowerCase().indexOf(deptKeyword) + deptKeyword.length) }}</span>{{ node.deptName.slice(node.deptName.toLowerCase().indexOf(deptKeyword) + deptKeyword.length) }}
                  </span>
                  <span v-else>{{ node.deptName }}</span>
                  <span v-if="node.leaderName" class="tree-node-badge">{{ node.leaderName }}</span>
                </span>
              </template>
            </a-tree>
            <a-empty
              v-else
              :image="simpleEmptyImage"
              description="未找到相关部门"
              class="dept-empty"
            />
          </div>
        </div>
      </a-col>

      <!-- 右侧用户列表 -->
      <a-col :span="18">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">用户管理</span>
            <a-space>
              <a-input-search
                v-model:value="searchText"
                placeholder="搜索用户名/姓名"
                style="width: 200px"
                @search="loadData"
              />
              <a-button v-if="hasPerm('system:user:create')" type="primary" @click="showModal()">新建用户</a-button>
            </a-space>
          </div>

          <a-table
            :columns="columns"
            :data-source="dataList"
            :loading="loading"
            :pagination="pagination"
            :resize-column="true"
            @resizeColumn="handleResizeColumn"
            row-key="id"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'realName'">
                <span class="action-link" @click="router.push(`/system/user-detail?id=${record.id}`)">
                  {{ record.realName }}
                </span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <span class="action-link" @click="showModal(record)">编辑</span>
                <a-divider type="vertical" />
                <span class="action-link" @click="showRoleModal(record)">授权角色</span>
                <a-divider type="vertical" />
                <span class="action-link" @click="handleResetPwd(record)">重置密码</span>
                <a-divider type="vertical" />
                <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
                  <span class="action-link danger">删除</span>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
    </a-row>

    <a-modal
      v-model:open="modalVisible"
      :title="editingRecord ? '编辑用户' : '新建用户'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="用户名" required>
          <a-input v-model:value="formState.username" :disabled="!!editingRecord" />
        </a-form-item>
        <a-form-item label="姓名" required>
          <a-input v-model:value="formState.realName" />
        </a-form-item>
        <a-form-item label="密码" v-if="!editingRecord">
          <a-input-password v-model:value="formState.password" />
        </a-form-item>
        <a-form-item label="部门">
          <a-tree-select
            v-model:value="formState.deptId"
            :tree-data="deptTree"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formState.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 授权角色弹窗 -->
    <a-modal
      v-model:open="roleModalVisible"
      title="授权角色"
      @ok="handleRoleSubmit"
      :confirm-loading="roleSubmitLoading"
    >
      <div class="role-user-info">
        <span>用户：</span>
        <strong>{{ roleUserRecord?.realName }}</strong>
        <span style="margin-left: 8px; color: #888">({{ roleUserRecord?.username }})</span>
      </div>
      <a-divider style="margin: 12px 0" />
      <a-checkbox-group v-model:value="selectedRoleIds" style="width: 100%">
        <div v-for="role in allRoles" :key="role.id" class="role-option">
          <a-checkbox :value="role.id">
            {{ role.roleName }}
            <span class="role-key">({{ role.roleKey }})</span>
          </a-checkbox>
        </div>
        <a-empty v-if="allRoles.length === 0" description="暂无角色" :image="simpleEmptyImage" />
      </a-checkbox-group>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Empty } from 'ant-design-vue'
import { DownCircleOutlined, RightCircleOutlined } from '@ant-design/icons-vue'
import { getUsersPage, createUser, updateUser, deleteUser, resetPassword, getDeptTree, getRoles, getUserRoles, setUserRoles } from '../../api/system'
import { renderDate } from '../../utils/date'
import { usePermission } from '../../composables/usePermission'
import { useResizableColumns } from '../../composables/useResizableTable'

const simpleEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE
const { hasPerm } = usePermission()
const router = useRouter()

const loading = ref(false)
const dataList = ref([])
const deptTree = ref([])
const modalVisible = ref(false)
const submitLoading = ref(false)
const editingRecord = ref(null)
const searchText = ref('')
const selectedDeptId = ref(null)

// 部门树：搜索 / 展开折叠控制
const deptSearchText = ref('')
const deptKeyword = computed(() => deptSearchText.value.trim().toLowerCase())
const expandedKeys = ref([])
const selectedKeys = ref([])
const allDeptKeys = ref([])

function collectAllKeys(nodes) {
  const keys = []
  const walk = (list) => {
    for (const node of list || []) {
      keys.push(node.id)
      walk(node.children)
    }
  }
  walk(nodes)
  return keys
}

function collectMatchedPathKeys(nodes, kw) {
  const keys = []
  const walk = (list, parents) => {
    for (const node of list || []) {
      const path = [...parents, node.id]
      if ((node.deptName || '').toLowerCase().includes(kw)) {
        keys.push(...path)
      }
      walk(node.children, path)
    }
  }
  walk(nodes, [])
  return [...new Set(keys)]
}

function filterDeptNodes(nodes, kw) {
  const result = []
  for (const node of nodes || []) {
    const matched = (node.deptName || '').toLowerCase().includes(kw)
    const children = filterDeptNodes(node.children, kw)
    if (matched || children.length) {
      result.push({ ...node, children })
    }
  }
  return result
}

const filteredDeptTree = computed(() => {
  const kw = deptKeyword.value
  if (!kw) return deptTree.value
  return filterDeptNodes(deptTree.value, kw)
})

watch(deptSearchText, (val) => {
  const kw = (val || '').trim().toLowerCase()
  expandedKeys.value = kw ? collectMatchedPathKeys(deptTree.value, kw) : [...allDeptKeys.value]
})

function expandAll() {
  expandedKeys.value = [...allDeptKeys.value]
}

function collapseAll() {
  expandedKeys.value = []
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const { columns, handleResizeColumn } = useResizableColumns([
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60, sorter: true },
  { title: '用户名', dataIndex: 'username', key: 'username', sorter: true },
  { title: '姓名', dataIndex: 'realName', key: 'realName', sorter: true },
  { title: '部门', dataIndex: 'deptName', key: 'deptName' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate, sorter: true },
  { title: '操作', key: 'action', width: 300 }
])

const formState = reactive({
  username: '',
  realName: '',
  password: '',
  deptId: null,
  status: 1
})

function showModal(record) {
  if (record) {
    editingRecord.value = record
    formState.username = record.username
    formState.realName = record.realName
    formState.deptId = record.deptId
    formState.status = record.status
  } else {
    editingRecord.value = null
    formState.username = ''
    formState.realName = ''
    formState.password = '123456'
    formState.deptId = selectedDeptId.value
    formState.status = 1
  }
  modalVisible.value = true
}

function onDeptSelect(keys) {
  selectedDeptId.value = keys[0] || null
  pagination.current = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUsersPage({
      page: pagination.current,
      size: pagination.pageSize,
      deptId: selectedDeptId.value,
      keyword: searchText.value || undefined
    })
    const data = res.data || res
    dataList.value = data.list || []
    pagination.total = data.total || 0
  } catch {
    // ignore
  }
  loading.value = false
}

async function loadDepts() {
  try {
    const res = await getDeptTree()
    const data = res.data || res
    deptTree.value = Array.isArray(data) ? data : []
    allDeptKeys.value = collectAllKeys(deptTree.value)
    expandedKeys.value = [...allDeptKeys.value]
  } catch {
    // ignore
  }
}

async function handleSubmit() {
  if (!formState.username || !formState.realName) {
    message.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (editingRecord.value) {
      await updateUser(editingRecord.value.id, {
        realName: formState.realName,
        deptId: formState.deptId,
        status: formState.status
      })
      message.success('更新成功')
    } else {
      await createUser(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // ignore
  }
  submitLoading.value = false
}

async function handleResetPwd(record) {
  try {
    await resetPassword(record.id, { password: '123456' })
    message.success('密码已重置为 123456')
  } catch {
    // ignore
  }
}

async function handleDelete(record) {
  try {
    await deleteUser(record.id)
    message.success('删除成功')
    loadData()
  } catch {
    // ignore
  }
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// ========== 授权角色 ==========
const roleModalVisible = ref(false)
const roleSubmitLoading = ref(false)
const roleUserRecord = ref(null)
const allRoles = ref([])
const selectedRoleIds = ref([])

async function showRoleModal(record) {
  roleUserRecord.value = record
  selectedRoleIds.value = []
  roleModalVisible.value = true
  // 加载全部角色
  try {
    const res = await getRoles()
    allRoles.value = res.data || res || []
  } catch { allRoles.value = [] }
  // 加载用户当前角色
  try {
    const res = await getUserRoles(record.id)
    const roles = res.data || res || []
    selectedRoleIds.value = roles.map(r => r.id)
  } catch { selectedRoleIds.value = [] }
}

async function handleRoleSubmit() {
  roleSubmitLoading.value = true
  try {
    await setUserRoles(roleUserRecord.value.id, selectedRoleIds.value)
    message.success('角色授权成功')
    roleModalVisible.value = false
  } catch {
    // ignore
  }
  roleSubmitLoading.value = false
}

onMounted(() => {
  loadDepts()
  loadData()
})
</script>

<style scoped>
/* 左侧部门树面板 */
.dept-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}
.tree-header {
  padding: 0 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.tree-actions {
  display: flex;
  align-items: center;
}
.tree-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  font-size: 12px;
  color: #595959;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s, color 0.2s;
}
.tree-action-btn:hover {
  background: #e6f4ff;
  color: #1677ff;
}
.tree-action-btn:active {
  background: #bae0ff;
  color: #0958d9;
}
.dept-tree-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 8px 6px;
}
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
.dept-name-hit {
  color: #f5222d;
  font-weight: 600;
}
.dept-empty {
  padding: 32px 0;
}

/* 授权角色弹窗 */
.role-option {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.role-option:last-child {
  border-bottom: none;
}
.role-key {
  color: #999;
  font-size: 12px;
  margin-left: 4px;
}
.role-user-info {
  margin-bottom: 4px;
}
</style>
