<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">部门详情</span>
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
            <a-descriptions-item label="部门ID">{{ detail.id }}</a-descriptions-item>
            <a-descriptions-item label="部门名称">{{ detail.deptName }}</a-descriptions-item>
            <a-descriptions-item label="部门编码">{{ detail.deptCode || '-' }}</a-descriptions-item>
            <a-descriptions-item label="部门类型">{{ detail.deptType || '-' }}</a-descriptions-item>
            <a-descriptions-item label="排序号">{{ detail.sortOrder ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="detail.status === 1 ? 'green' : 'default'">
                {{ detail.status === 1 ? '启用' : '禁用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="上级部门">
              <span v-if="detail.parentId && detail.parentId > 0" class="dept-link" @click="goDeptDetail(detail.parentId)">
                {{ detail.parentDeptName || `ID:${detail.parentId}` }}
              </span>
              <span v-else style="color: #999">顶级部门</span>
            </a-descriptions-item>
            <a-descriptions-item label="部门领导">
              <span v-if="detail.leaderId" class="user-link" @click="goUserDetail(detail.leaderId)">
                {{ detail.leaderName }}
              </span>
              <span v-else style="color: #999">未设置</span>
            </a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ detail.phone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="成员人数">
              <a-badge :count="detail.memberCount || 0" :number-style="{ backgroundColor: detail.memberCount > 0 ? '#1677ff' : '#ccc' }" />
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(detail.createTime) || '-' }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ formatDate(detail.updateTime) || '-' }}</a-descriptions-item>
          </a-descriptions>

          <!-- 子部门 -->
          <div class="section-title">
            子部门
            <a-badge :count="detail.childDepts?.length || 0" :number-style="{ backgroundColor: '#87d068' }" style="margin-left: 8px;" />
          </div>
          <a-table
            v-if="detail.childDepts && detail.childDepts.length > 0"
            :columns="childDeptColumns"
            :data-source="detail.childDepts"
            :pagination="false"
            :resize-column="true"
            @resizeColumn="handleChildDeptResize"
            row-key="id"
            size="small"
            class="section-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'deptName'">
                <span class="dept-link" @click="goDeptDetail(record.id)">{{ record.deptName }}</span>
              </template>
              <template v-if="column.key === 'leaderName'">
                <span v-if="record.leaderName">{{ record.leaderName }}</span>
                <span v-else style="color: #999">未设置</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'default'" size="small">
                  {{ record.status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
            </template>
          </a-table>
          <a-empty v-else description="暂无子部门" :image="simpleEmptyImage" />

          <!-- 成员列表 -->
          <div class="section-title">
            成员列表
            <a-badge :count="detail.memberCount || 0" :number-style="{ backgroundColor: '#1677ff' }" style="margin-left: 8px;" />
          </div>
          <a-table
            v-if="detail.members && detail.members.length > 0"
            :columns="memberColumns"
            :data-source="detail.members"
            :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 人` }"
            :resize-column="true"
            @resizeColumn="handleMemberResize"
            row-key="id"
            size="small"
            class="section-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'realName'">
                <span class="user-link" @click="goUserDetail(record.id)">{{ record.realName || record.username }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'default'" size="small">
                  {{ record.status === 1 ? '正常' : '禁用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'createTime'">
                {{ formatDate(record.createTime) || '-' }}
              </template>
            </template>
          </a-table>
          <a-empty v-else description="暂无成员" :image="simpleEmptyImage" />
        </template>
        <a-empty v-else-if="!loading" description="部门不存在" />
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { getDeptDetail } from '../../api/system'
import { formatDate } from '../../utils/date'
import { useResizableColumns } from '../../composables/useResizableTable'

const route = useRoute()
const router = useRouter()
const simpleEmptyImage = Empty.PRESENTED_IMAGE_SIMPLE

const loading = ref(false)
const detail = ref(null)

const { columns: childDeptColumns, handleResizeColumn: handleChildDeptResize } = useResizableColumns([
  { title: '部门名称', key: 'deptName', dataIndex: 'deptName', sorter: true },
  { title: '部门领导', key: 'leaderName', dataIndex: 'leaderName', width: 120 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 80 }
])

const { columns: memberColumns, handleResizeColumn: handleMemberResize } = useResizableColumns([
  { title: '姓名', key: 'realName', dataIndex: 'realName', sorter: true },
  { title: '用户名', dataIndex: 'username', key: 'username', sorter: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 80 },
  { title: '加入时间', key: 'createTime', dataIndex: 'createTime', width: 120, sorter: true }
])

function goDeptDetail(deptId) {
  router.push(`/system/dept-detail?id=${deptId}`)
}

function goUserDetail(userId) {
  router.push(`/system/user-detail?id=${userId}`)
}

async function loadDetail() {
  const id = route.query.id
  if (!id) return
  loading.value = true
  try {
    const res = await getDeptDetail(id)
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
  display: flex;
  align-items: center;
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
.user-link {
  color: var(--color-primary);
  cursor: pointer;
}
.user-link:hover {
  text-decoration: underline;
}
</style>
