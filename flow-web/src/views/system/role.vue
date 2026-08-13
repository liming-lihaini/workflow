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

    <!-- 权限分配弹窗（扁平化：分组 -> 权限项平铺） -->
    <a-modal v-model:open="permModalVisible" :title="`权限分配 - ${currentRoleForPerm?.roleName || ''}`" @ok="handleAssignPerm" width="760px">
      <div class="perm-tree-wrap">
        <a-tree
          v-model:checkedKeys="checkedPermKeys"
          :tree-data="permTree"
          checkable
          check-strictly
          default-expand-all
          block-node
          :field-names="{ title: 'title', key: 'key', children: 'children' }"
        >
          <template #title="node">
            <span v-if="String(node.key).startsWith('group_')" class="perm-group-title">{{ node.title }}（{{ (node.children || []).length }} 项）</span>
            <span v-else class="perm-item-title">
              {{ node.title }}
              <a-tag :color="permTypeColor(node.permType)" class="perm-type-tag">{{ node.permTypeLabel }}</a-tag>
              <span class="perm-key">{{ node.permKey }}</span>
            </span>
          </template>
        </a-tree>
      </div>
    </a-modal>
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
const permTree = ref([])
const checkedPermKeys = ref({ checked: [], halfChecked: [] })
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
  // 加载分组权限清单（扁平化两层结构）
  try {
    const res = await getPermissionsGrouped()
    const data = res.data || res
    permTree.value = Array.isArray(data) ? data : []
  } catch {
    permTree.value = []
  }
  // 加载已分配权限
  try {
    const res = await getRolePermissions(record.id)
    const data = res.data || res
    const ids = Array.isArray(data) ? data.map(p => p.id || p) : []
    checkedPermKeys.value = { checked: ids, halfChecked: [] }
  } catch {
    checkedPermKeys.value = { checked: [], halfChecked: [] }
  }
  permModalVisible.value = true
}

function permTypeColor(permType) {
  if (permType === 1) return 'blue'
  if (permType === 2) return 'green'
  if (permType === 3) return 'orange'
  return 'default'
}

async function handleAssignPerm() {
  try {
    // check-strictly 模式下 checkedKeys 为对象结构，过滤掉分组节点 key（字符串），只保留权限 ID（数字）
    const keys = checkedPermKeys.value.checked || []
    const permIds = keys.filter(k => typeof k === 'number')
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
.perm-tree-wrap {
  max-height: 480px;
  overflow-y: auto;
  padding: 4px 0;
}
.perm-group-title {
  font-weight: 600;
  color: #262626;
}
.perm-item-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}
.perm-type-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
  flex-shrink: 0;
}
.perm-key {
  color: #999;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
