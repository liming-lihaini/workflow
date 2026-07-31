<template>
  <a-modal
    :open="open"
    title="个人 Token 管理"
    :width="720"
    :footer="null"
    @cancel="$emit('update:open', false)"
  >
    <!-- 生成Token表单 -->
    <div class="create-row">
      <a-input v-model:value="createForm.tokenName" placeholder="Token名称（用途备注）" style="width: 220px" />
      <a-select v-model:value="createForm.expireDays" style="width: 140px">
        <a-select-option :value="30">30天有效</a-select-option>
        <a-select-option :value="90">90天有效</a-select-option>
        <a-select-option :value="365">365天有效</a-select-option>
        <a-select-option :value="0">永久有效</a-select-option>
      </a-select>
      <a-button type="primary" :loading="creating" @click="handleCreate">生成Token</a-button>
    </div>

    <!-- 新Token仅展示一次 -->
    <a-alert v-if="newToken" type="success" class="new-token-alert" show-icon>
      <template #message>Token已生成，请立即复制保存（仅本次展示）</template>
      <template #description>
        <div class="token-value-row">
          <code class="token-value">{{ newToken }}</code>
          <a-button size="small" @click="copyToken(newToken)">复制</a-button>
        </div>
      </template>
    </a-alert>

    <!-- Token列表 -->
    <a-table
      :data-source="tokens"
      :columns="columns"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
      style="margin-top: 12px"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'tokenMasked'">
          <code>{{ record.tokenMasked }}</code>
        </template>
        <template v-else-if="column.key === 'expireTime'">
          <a-tag v-if="record.expired" color="red">已过期</a-tag>
          <span v-else>{{ record.expireTime || '永久有效' }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
        </template>
      </template>
    </a-table>

    <div class="tip">
      使用方式：请求头携带 <code>Authorization: Bearer {Token值}</code> 即可调用系统 API。
    </div>
  </a-modal>
</template>

<script setup>
import { reactive, ref, watch, createVNode } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { listApiTokens, createApiToken, deleteApiToken } from '../api/profile'

const props = defineProps({
  open: { type: Boolean, default: false }
})
defineEmits(['update:open'])

const loading = ref(false)
const creating = ref(false)
const tokens = ref([])
const newToken = ref('')
const createForm = reactive({ tokenName: '', expireDays: 90 })

const columns = [
  { title: '名称', dataIndex: 'tokenName', key: 'tokenName', width: 140, ellipsis: true },
  { title: 'Token', dataIndex: 'tokenMasked', key: 'tokenMasked', width: 160 },
  { title: '过期时间', dataIndex: 'expireTime', key: 'expireTime', width: 150 },
  { title: '最近使用', dataIndex: 'lastUsedTime', key: 'lastUsedTime', width: 150 },
  { title: '操作', key: 'action', width: 70 }
]

watch(() => props.open, (val) => {
  if (val) {
    newToken.value = ''
    createForm.tokenName = ''
    createForm.expireDays = 90
    loadTokens()
  }
})

async function loadTokens() {
  loading.value = true
  try {
    const res = await listApiTokens()
    tokens.value = res.data || []
  } catch {
    tokens.value = []
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  creating.value = true
  try {
    const res = await createApiToken({
      tokenName: createForm.tokenName,
      expireDays: createForm.expireDays
    })
    newToken.value = res.data?.tokenValue || ''
    message.success('Token生成成功')
    createForm.tokenName = ''
    loadTokens()
  } catch {
    // 拦截器已提示
  } finally {
    creating.value = false
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '删除Token',
    icon: createVNode(ExclamationCircleOutlined),
    content: `确定删除Token「${record.tokenName}」吗？删除后使用该Token的调用将立即失效。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteApiToken(record.id)
      message.success('删除成功')
      loadTokens()
    }
  })
}

async function copyToken(value) {
  try {
    await navigator.clipboard.writeText(value)
    message.success('已复制到剪贴板')
  } catch {
    message.warning('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.create-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.new-token-alert {
  margin-top: 12px;
}

.token-value-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.token-value {
  word-break: break-all;
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
}

.tip {
  margin-top: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
