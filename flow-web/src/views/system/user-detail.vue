<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">用户详情</span>
        <a-button @click="router.back()">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
      </div>

      <a-spin :spinning="loading">
        <template v-if="detail">
          <!-- 基本信息 -->
          <div class="section-title">基本信息</div>
          <a-descriptions :column="2" bordered size="small" class="info-desc">
            <a-descriptions-item label="用户ID">{{ detail.id }}</a-descriptions-item>
            <a-descriptions-item label="用户名">{{ detail.username }}</a-descriptions-item>
            <a-descriptions-item label="姓名">{{ detail.realName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="detail.status === 1 ? 'green' : detail.status === 0 ? 'default' : 'orange'">
                {{ statusMap[detail.status] || '未知' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="手机号">{{ detail.phone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ detail.email || '-' }}</a-descriptions-item>
            <a-descriptions-item label="所属部门">
              <span v-if="detail.deptId" class="dept-link" @click="goDeptDetail(detail.deptId)">
                {{ detail.deptName || `ID:${detail.deptId}` }}
              </span>
              <span v-else style="color: #999">未分配</span>
            </a-descriptions-item>
            <a-descriptions-item label="密级">
              <a-tag :color="securityColor(detail.securityLevel)">{{ securityMap[detail.securityLevel] || '未知' }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(detail.createTime) || '-' }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ formatDate(detail.updateTime) || '-' }}</a-descriptions-item>
          </a-descriptions>

          <!-- 角色信息 -->
          <div class="section-title">角色信息</div>
          <a-table
            :columns="roleColumns"
            :data-source="detail.roles || []"
            :pagination="false"
            :resize-column="true"
            @resizeColumn="handleRoleResize"
            row-key="id"
            size="small"
            class="section-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'roleName'">
                <span>{{ record.roleName }}</span>
                <span class="role-key-badge">{{ record.roleKey }}</span>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!detail.roles || detail.roles.length === 0" description="暂无角色" :image="simpleEmptyImage" />

          <!-- 兼职部门 -->
          <div class="section-title">部门兼职</div>
          <a-table
            :columns="postColumns"
            :data-source="detail.posts || []"
            :pagination="false"
            :resize-column="true"
            @resizeColumn="handlePostResize"
            row-key="id"
            size="small"
            class="section-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'deptName'">
                <span v-if="record.deptId" class="dept-link" @click="goDeptDetail(record.deptId)">
                  {{ record.deptName || `ID:${record.deptId}` }}
                </span>
                <span v-else style="color: #999">-</span>
              </template>
              <template v-if="column.key === 'isMain'">
                <a-tag :color="record.isMain === 1 ? 'blue' : 'default'">
                  {{ record.isMain === 1 ? '主部门' : '兼职' }}
                </a-tag>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!detail.posts || detail.posts.length === 0" description="暂无兼职记录" :image="simpleEmptyImage" />
        </template>
        <a-empty v-else-if="!loading" description="用户不存在" />
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { getUserDetail } from '../../api/system'
import { formatDate } from '../../utils/date'
import { useResizableColumns } from '../../composables/useResizableTable'

const route = useRoute()
const router = useRouter()
const simpleEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE

const loading = ref(false)
const detail = ref(null)

const statusMap = { 0: '禁用', 1: '正常', 2: '锁定' }
const securityMap = { 1: '公开', 2: '内部', 3: '秘密', 4: '机密' }

function securityColor(level) {
  return { 1: 'default', 2: 'blue', 3: 'orange', 4: 'red' }[level] || 'default'
}

const { columns: roleColumns, handleResizeColumn: handleRoleResize } = useResizableColumns([
  { title: '角色名称', key: 'roleName', dataIndex: 'roleName', sorter: true },
  { title: '角色描述', dataIndex: 'description', key: 'description' }
])

const { columns: postColumns, handleResizeColumn: handlePostResize } = useResizableColumns([
  { title: '部门名称', key: 'deptName', dataIndex: 'deptName', sorter: true },
  { title: '类型', key: 'isMain', dataIndex: 'isMain', width: 100 }
])

function goDeptDetail(deptId) {
  router.push(`/system/dept-detail?id=${deptId}`)
}

async function loadDetail() {
  const id = route.query.id
  if (!id) return
  loading.value = true
  try {
    const res = await getUserDetail(id)
    detail.value = res.data || res
  } catch {
    detail.value = null
  }
  loading.value = false
}

onMounted(loadDetail)
</script>

<style scoped>
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
  margin: 24px 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--color-primary);
}
.section-table {
  margin-bottom: 8px;
}
.dept-link {
  color: var(--color-primary);
  cursor: pointer;
}
.dept-link:hover {
  text-decoration: underline;
}
.role-key-badge {
  font-size: 11px;
  color: #999;
  margin-left: 6px;
}
</style>