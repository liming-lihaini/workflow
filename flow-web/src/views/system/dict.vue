<template>
  <div class="page-wrap">
    <a-row :gutter="16">
      <!-- 左侧字典类型 -->
      <a-col :span="8">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">字典类型</span>
            <a-button type="primary" size="small" @click="showTypeModal()">新建</a-button>
          </div>
          <a-list
            :data-source="typeList"
            :loading="typeLoading"
            size="small"
          >
            <template #renderItem="{ item }">
              <a-list-item
                :class="{ 'active-item': selectedType?.id === item.id }"
                @click="selectType(item)"
              >
                <a-list-item-meta :title="item.dictName" :description="item.dictCode" />
                <template #actions>
                  <span class="action-link" @click.stop="showTypeModal(item, 'edit')">编辑</span>
                  <a-popconfirm title="确定删除？" @confirm="handleDeleteType(item)">
                    <span class="action-link danger" @click.stop>删除</span>
                  </a-popconfirm>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </a-col>

      <!-- 右侧字典项 -->
      <a-col :span="16">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">
              字典项 {{ selectedType ? `（${selectedType.dictName}）` : '' }}
            </span>
            <a-button type="primary" size="small" @click="showItemModal()" :disabled="!selectedType">
              新建
            </a-button>
          </div>
          <a-table
            :columns="itemColumns"
            :data-source="itemList"
            :loading="itemLoading"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <span class="action-link" @click="showItemModal(record)">编辑</span>
                <a-divider type="vertical" />
                <a-popconfirm title="确定删除？" @confirm="handleDeleteItem(record)">
                  <span class="action-link danger">删除</span>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
    </a-row>

    <!-- 字典类型弹窗 -->
    <a-modal
      v-model:open="typeModalVisible"
      :title="editingType ? '编辑字典类型' : '新建字典类型'"
      @ok="handleSubmitType"
    >
      <a-form :model="typeForm" layout="vertical">
        <a-form-item label="字典名称" required>
          <a-input v-model:value="typeForm.dictName" />
        </a-form-item>
        <a-form-item label="字典编码" required>
          <a-input v-model:value="typeForm.dictCode" :disabled="!!editingType" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="typeForm.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 字典项弹窗 -->
    <a-modal
      v-model:open="itemModalVisible"
      :title="editingItem ? '编辑字典项' : '新建字典项'"
      @ok="handleSubmitItem"
    >
      <a-form :model="itemForm" layout="vertical">
        <a-form-item label="显示文本" required>
          <a-input v-model:value="itemForm.itemText" />
        </a-form-item>
        <a-form-item label="字典值" required>
          <a-input v-model:value="itemForm.itemValue" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="itemForm.sortOrder" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getDictTypes, createDictType, updateDictType, deleteDictType,
  getDictItemsByTypeId, createDictItem, updateDictItem, deleteDictItem
} from '../../api/dict'

const typeLoading = ref(false)
const typeList = ref([])
const selectedType = ref(null)
const itemLoading = ref(false)
const itemList = ref([])

// 类型弹窗
const typeModalVisible = ref(false)
const editingType = ref(null)
const typeForm = reactive({ dictName: '', dictCode: '', description: '' })

// 项弹窗
const itemModalVisible = ref(false)
const editingItem = ref(null)
const itemForm = reactive({ itemText: '', itemValue: '', sortOrder: 0 })

const itemColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '显示文本', dataIndex: 'itemText' },
  { title: '字典值', dataIndex: 'itemValue' },
  { title: '排序', dataIndex: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140 }
]

async function loadTypes() {
  typeLoading.value = true
  try {
    const res = await getDictTypes()
    const data = res.data || res
    typeList.value = data.list || data.records || (Array.isArray(data) ? data : [])
  } catch {
    // ignore
  }
  typeLoading.value = false
}

function selectType(type) {
  selectedType.value = type
  loadItems()
}

async function loadItems() {
  if (!selectedType.value) return
  itemLoading.value = true
  try {
    const res = await getDictItemsByTypeId(selectedType.value.id)
    const data = res.data || res
    itemList.value = data.list || data.records || (Array.isArray(data) ? data : [])
  } catch {
    // ignore
  }
  itemLoading.value = false
}

function showTypeModal(record, mode) {
  if (mode === 'edit' && record) {
    editingType.value = record
    typeForm.dictName = record.dictName
    typeForm.dictCode = record.dictCode
    typeForm.description = record.description
  } else {
    editingType.value = null
    typeForm.dictName = ''
    typeForm.dictCode = ''
    typeForm.description = ''
  }
  typeModalVisible.value = true
}

async function handleSubmitType() {
  if (!typeForm.dictName || !typeForm.dictCode) {
    message.warning('请填写必填项')
    return
  }
  try {
    if (editingType.value) {
      await updateDictType(editingType.value.id, typeForm)
      message.success('更新成功')
    } else {
      await createDictType(typeForm)
      message.success('创建成功')
    }
    typeModalVisible.value = false
    loadTypes()
  } catch {
    // ignore
  }
}

async function handleDeleteType(record) {
  try {
    await deleteDictType(record.id)
    message.success('删除成功')
    if (selectedType.value?.id === record.id) {
      selectedType.value = null
      itemList.value = []
    }
    loadTypes()
  } catch {
    // ignore
  }
}

function showItemModal(record) {
  if (record) {
    editingItem.value = record
    itemForm.itemText = record.itemText
    itemForm.itemValue = record.itemValue
    itemForm.sortOrder = record.sortOrder
  } else {
    editingItem.value = null
    itemForm.itemText = ''
    itemForm.itemValue = ''
    itemForm.sortOrder = 0
  }
  itemModalVisible.value = true
}

async function handleSubmitItem() {
  if (!itemForm.itemText || !itemForm.itemValue) {
    message.warning('请填写必填项')
    return
  }
  try {
    const payload = { ...itemForm, dictTypeId: selectedType.value.id }
    if (editingItem.value) {
      await updateDictItem(editingItem.value.id, payload)
      message.success('更新成功')
    } else {
      await createDictItem(payload)
      message.success('创建成功')
    }
    itemModalVisible.value = false
    loadItems()
  } catch {
    // ignore
  }
}

async function handleDeleteItem(record) {
  try {
    await deleteDictItem(record.id)
    message.success('删除成功')
    loadItems()
  } catch {
    // ignore
  }
}

onMounted(loadTypes)
</script>

<style scoped>
.active-item {
  background: var(--color-primary-light);
}
</style>
