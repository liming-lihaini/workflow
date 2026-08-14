<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">角色管理</span>
        <a-button v-if="hasPerm('system:role:create')" type="primary" @click="showModal()">新建角色</a-button>
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
          <template v-if="column.key === 'action'">
            <template v-if="hasPerm('system:role:update')">
              <span class="action-link" @click="showModal(record)">编辑</span>
              <a-divider type="vertical" />
            </template>
            <template v-if="hasPerm('system:role:assign-perm')">
              <span class="action-link" @click="showPermModal(record)">权限分配</span>
              <a-divider type="vertical" />
            </template>
            <a-popconfirm v-if="hasPerm('system:role:delete')" title="确定删除？" @confirm="handleDelete(record)">
              <span class="action-link danger">删除</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新建/编辑角色 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingRecord ? '编辑角色' : '新建角色'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="角色名称" required>
          <a-input v-model:value="formState.roleName" />
        </a-form-item>
        <a-form-item label="角色标识" required>
          <a-input v-model:value="formState.roleKey" :disabled="!!editingRecord" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 权限分配抽屉（二级分组：分组 -> 菜单子分组 -> 权限项，每行 3 项） -->
    <a-drawer
      v-model:open="permModalVisible"
      :title="`权限分配 - ${currentRoleForPerm?.roleName || ''}`"
      width="1000px"
      placement="right"
      :destroy-on-close="true"
    >
      <div class="perm-panel-wrap">
        <div v-for="group in permGroups" :key="group.key" class="perm-group">
          <div class="perm-group-header">
            <a-checkbox
              :checked="isGroupChecked(group)"
              :indeterminate="isGroupIndeterminate(group)"
              @change="(e) => toggleGroup(group, e.target.checked)"
            >
              <span class="perm-group-title">{{ group.title }}</span>
            </a-checkbox>
            <span class="perm-count">{{ countGroupPerms(group) }} 项</span>
          </div>
          <div v-for="sub in group.subGroups" :key="sub.key" class="perm-sub">
            <div class="perm-sub-header">
              <a-checkbox
                :checked="isSubChecked(sub)"
                :indeterminate="isSubIndeterminate(sub)"
                @change="(e) => toggleSub(sub, e.target.checked)"
              >
                <span class="perm-sub-title">{{ sub.title }}</span>
              </a-checkbox>
            </div>
            <div class="perm-grid">
              <a-checkbox
                v-for="p in sub.perms"
                :key="p.id"
                :checked="checkedIds.includes(p.id)"
                :title="p.permKey"
                @change="() => togglePerm(p.id)"
              >
                <span class="perm-item-title">
                  {{ p.title }}
                  <a-tag :color="permTypeColor(p.permType)" class="perm-type-tag">{{ p.permTypeLabel }}</a-tag>
                </span>
              </a-checkbox>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="perm-drawer-footer">
          <a-button @click="permModalVisible = false">取消</a-button>
          <a-button type="primary" @click="handleAssignPerm">确定</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getRoles, createRole, updateRole, deleteRole, getRolePermissions, assignRolePermissions } from '../../api/system'
import { getPermissionsGrouped } from '../../api/system'
import { renderDate } from '../../utils/date'
import { usePermission } from '../../composables/usePermission'
import { useResizableColumns } from '../../composables/useResizableTable'

const { hasPerm } = usePermission()
const loading = ref(false)
const dataList = ref([])
const modalVisible = ref(false)
const submitLoading = ref(false)
const editingRecord = ref(null)
const permModalVisible = ref(false)
const permGroups = ref([])
const checkedIds = ref([])
const currentRoleForPerm = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const { columns, handleResizeColumn } = useResizableColumns([
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60, sorter: true },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', sorter: true },
  { title: '角色标识', dataIndex: 'roleKey', key: 'roleKey', sorter: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate, sorter: true },
  { title: '操作', key: 'action', width: 220 }
])

const formState = reactive({
  roleName: '',
  roleKey: ''
})

function showModal(record) {
  if (record) {
    editingRecord.value = record
    formState.roleName = record.roleName
    formState.roleKey = record.roleKey
  } else {
    editingRecord.value = null
    formState.roleName = ''
    formState.roleKey = ''
  }
  modalVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await getRoles({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
  } catch {
    // ignore
  }
  loading.value = false
}

async function handleSubmit() {
  if (!formState.roleName || !formState.roleKey) {
    message.warning('请填写必填项')
    return
  }
  submitLoading.value = true
  try {
    if (editingRecord.value) {
      await updateRole(editingRecord.value.id, {
        roleName: formState.roleName
      })
      message.success('更新成功')
    } else {
      await createRole(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // ignore
  }
  submitLoading.value = false
}

async function handleDelete(record) {
  try {
    await deleteRole(record.id)
    message.success('删除成功')
    loadData()
  } catch {
    // ignore
  }
}

async function showPermModal(record) {
  currentRoleForPerm.value = record
  // 加载分组权限清单（三层结构：分组 -> 菜单子分组 -> 权限项）
  try {
    const res = await getPermissionsGrouped()
    const data = res.data || res
    permGroups.value = Array.isArray(data) ? data : []
  } catch {
    permGroups.value = []
  }
  // 加载已分配权限
  try {
    const res = await getRolePermissions(record.id)
    const data = res.data || res
    checkedIds.value = Array.isArray(data) ? data.map(p => p.id || p) : []
  } catch {
    checkedIds.value = []
  }
  permModalVisible.value = true
}

// ========== 二级分组勾选逻辑 ==========
function subPermIds(sub) {
  return (sub.perms || []).map(p => p.id)
}
function groupPermIds(group) {
  return (group.subGroups || []).flatMap(subPermIds)
}
function countGroupPerms(group) {
  return groupPermIds(group).length
}
function isSubChecked(sub) {
  const ids = subPermIds(sub)
  return ids.length > 0 && ids.every(id => checkedIds.value.includes(id))
}
function isSubIndeterminate(sub) {
  const ids = subPermIds(sub)
  const hit = ids.filter(id => checkedIds.value.includes(id)).length
  return hit > 0 && hit < ids.length
}
function toggleSub(sub, on) {
  const ids = subPermIds(sub)
  checkedIds.value = on
    ? [...new Set([...checkedIds.value, ...ids])]
    : checkedIds.value.filter(id => !ids.includes(id))
}
function isGroupChecked(group) {
  const ids = groupPermIds(group)
  return ids.length > 0 && ids.every(id => checkedIds.value.includes(id))
}
function isGroupIndeterminate(group) {
  const ids = groupPermIds(group)
  const hit = ids.filter(id => checkedIds.value.includes(id)).length
  return hit > 0 && hit < ids.length
}
function toggleGroup(group, on) {
  const ids = groupPermIds(group)
  checkedIds.value = on
    ? [...new Set([...checkedIds.value, ...ids])]
    : checkedIds.value.filter(id => !ids.includes(id))
}
function togglePerm(id) {
  checkedIds.value = checkedIds.value.includes(id)
    ? checkedIds.value.filter(x => x !== id)
    : [...checkedIds.value, id]
}

function permTypeColor(permType) {
  if (permType === 1) return 'blue'
  if (permType === 2) return 'green'
  if (permType === 3) return 'orange'
  return 'default'
}

async function handleAssignPerm() {
  try {
    // checkedIds 均为权限 ID（数字），分组/子分组头仅为展示与批量勾选
    const permIds = checkedIds.value.filter(k => typeof k === 'number')
    await assignRolePermissions(currentRoleForPerm.value.id, permIds)
    message.success('权限分配成功')
    permModalVisible.value = false
  } catch {
    // ignore
  }
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.perm-panel-wrap {
  padding: 4px 0;
}
.perm-group {
  margin-bottom: 12px;
}
.perm-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}
.perm-group-title {
  font-weight: 600;
  color: #262626;
}
.perm-count {
  color: #999;
  font-size: 12px;
}
.perm-sub {
  margin: 8px 0 0 8px;
  padding: 6px 10px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}
.perm-sub-header {
  margin-bottom: 4px;
}
.perm-sub-title {
  font-weight: 600;
  color: #434343;
}
.perm-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px 8px;
  padding-left: 24px;
}
.perm-item-title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  overflow: hidden;
}
.perm-type-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
  flex-shrink: 0;
}
.perm-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
