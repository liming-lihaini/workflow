<template>
  <div class="rule-admin">
    <a-page-header title="规则引擎配置" sub-title="业务规则表达式维护（ISSUE-029 底座）" />

    <a-row :gutter="16">
      <!-- 规则列表 -->
      <a-col :span="14">
        <a-card title="规则列表">
          <template #extra>
            <a-button type="primary" size="small" @click="openCreate">新增</a-button>
            <a-button size="small" class="ml" @click="load">刷新</a-button>
          </template>
          <a-table
            :columns="columns"
            :data-source="rules"
            :pagination="false"
            size="small"
            row-key="id"
            :loading="loading"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'enabled'">
                <a-tag :color="record.enabled === 1 ? 'green' : 'red'">
                  {{ record.enabled === 1 ? '启用' : '停用' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a @click="openEdit(record)">编辑</a>
                <a-divider type="vertical" />
                <a-popconfirm title="确认删除该规则?" @confirm="del(record)">
                  <a style="color: #cf1322">删除</a>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <!-- 规则编辑 / 调试 -->
      <a-col :span="10">
        <a-card :title="editing ? '编辑规则' : '新增规则'">
          <a-form :model="form" layout="vertical">
            <a-form-item label="规则标识">
              <a-input v-model:value="form.ruleKey" :disabled="!!form.id" placeholder="如 dispatch_gate" />
            </a-form-item>
            <a-form-item label="规则名称">
              <a-input v-model:value="form.ruleName" placeholder="如 派单资质闸门" />
            </a-form-item>
            <a-form-item label="表达式（SpEL）">
              <a-textarea v-model:value="form.expr" :rows="3" placeholder="#staffQualified == true" />
            </a-form-item>
            <a-form-item label="说明">
              <a-input v-model:value="form.remark" />
            </a-form-item>
            <a-form-item label="启用">
              <a-switch v-model:checked="form.enabledChecked" />
            </a-form-item>
            <a-space>
              <a-button type="primary" :loading="saving" @click="save">保存</a-button>
              <a-button @click="resetForm">取消</a-button>
            </a-space>
          </a-form>

          <a-divider />
          <a-typography-title :level="5">规则调试</a-typography-title>
          <a-space>
            <a-select v-model:value="debugKey" style="width: 200px" placeholder="选择规则">
              <a-select-option v-for="r in rules" :key="r.ruleKey" :value="r.ruleKey">
                {{ r.ruleKey }}
              </a-select-option>
            </a-select>
            <a-input v-model:value="debugCtx" style="width: 240px"
                     placeholder='{"staffQualified":true}' />
            <a-button @click="debugEval">求值</a-button>
          </a-space>
          <a-alert v-if="debugResult !== null" class="mt" :type="debugResult ? 'success' : 'error'"
                   :message="'求值结果：' + debugResult" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  listRules, saveRule, removeRule, evalRule
} from '../../../api/ems'

const loading = ref(false)
const saving = ref(false)
const rules = ref([])
const editing = ref(false)

const columns = [
  { title: '标识', dataIndex: 'ruleKey', key: 'ruleKey' },
  { title: '名称', dataIndex: 'ruleName', key: 'ruleName' },
  { title: '表达式', dataIndex: 'expr', key: 'expr', ellipsis: true },
  { title: '状态', dataIndex: 'enabled', key: 'enabled' },
  { title: '操作', key: 'action' }
]

const form = reactive({
  id: null, ruleKey: '', ruleName: '', expr: '', remark: '', enabledChecked: true
})

const debugKey = ref('')
const debugCtx = ref('')
const debugResult = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listRules()
    rules.value = res.data || []
  } catch (e) {
    message.error('加载规则失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editing.value = false
  form.id = null
  form.ruleKey = ''
  form.ruleName = ''
  form.expr = ''
  form.remark = ''
  form.enabledChecked = true
}

function openCreate() {
  resetForm()
}

function openEdit(r) {
  editing.value = true
  form.id = r.id
  form.ruleKey = r.ruleKey
  form.ruleName = r.ruleName
  form.expr = r.expr
  form.remark = r.remark
  form.enabledChecked = r.enabled === 1
}

async function save() {
  if (!form.ruleKey || !form.expr) {
    message.warning('标识与表达式必填')
    return
  }
  saving.value = true
  try {
    await saveRule({
      id: form.id,
      ruleKey: form.ruleKey,
      ruleName: form.ruleName,
      expr: form.expr,
      remark: form.remark,
      enabled: form.enabledChecked ? 1 : 0
    })
    message.success('保存成功')
    resetForm()
    await load()
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function del(r) {
  try {
    await removeRule(r.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error('删除失败')
  }
}

async function debugEval() {
  if (!debugKey.value) {
    message.warning('请选择规则')
    return
  }
  let ctx = {}
  try {
    if (debugCtx.value.trim()) ctx = JSON.parse(debugCtx.value)
  } catch (e) {
    message.error('上下文 JSON 格式错误')
    return
  }
  try {
    const res = await evalRule(debugKey.value, ctx)
    debugResult.value = res.data.result
  } catch (e) {
    message.error('求值失败')
  }
}

onMounted(load)
</script>

<style scoped>
.rule-admin {
  padding: 0 16px 24px;
}
.ml {
  margin-left: 8px;
}
.mt {
  margin-top: 12px;
}
</style>
