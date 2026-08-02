<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">客户管理</span>
        <a-space wrap>
          <a-input-search
            v-model:value="searchText"
            placeholder="搜索客户名称/信用代码"
            style="width: 220px"
            allow-clear
            @search="loadData"
          />
          <a-button v-if="hasPerm('ems:customer:create')" type="primary" @click="showDrawer()">新建客户</a-button>
          <a-button v-if="hasPerm('ems:customer:import')" @click="templateVisible = true">导入模板</a-button>
          <a-upload
            v-if="hasPerm('ems:customer:import')"
            :show-upload-list="false"
            :before-upload="handleImport"
            accept=".xlsx,.xls"
          >
            <a-button :loading="importLoading">批量导入</a-button>
          </a-upload>
          <a-popconfirm
            v-if="hasPerm('ems:customer:delete')"
            :disabled="!hasSelected"
            title="确定批量删除选中的客户？"
            @confirm="handleBatchDelete"
          >
            <a-button danger :disabled="!hasSelected">批量删除</a-button>
          </a-popconfirm>
        </a-space>
      </div>

      <div class="tbl-box">
        <a-table
          :columns="columns"
          :data-source="dataList"
          :loading="loading"
          :pagination="pagination"
          :scroll="{ y: scrollY }"
          :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
          :resize-column="true"
          @resizeColumn="handleResizeColumn"
          row-key="id"
          @change="handleTableChange"
        >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="showDrawer(record)">编辑</span>
            <a-divider type="vertical" />
            <a-popconfirm
              :title="record.status === 1 ? '确定停用该客户？' : '确定启用该客户？'"
              @confirm="handleDisable(record)"
            >
              <span class="action-link">{{ record.status === 1 ? '停用' : '启用' }}</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      </div>
      </div>

    <!-- 新建/编辑：右侧抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="editingRecord ? '编辑客户' : '新建客户'"
      width="1000"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @close="drawerVisible = false"
    >
      <a-form :model="formState" layout="vertical">
        <a-form-item label="客户名称" required>
          <a-input v-model:value="formState.custName" />
        </a-form-item>
        <a-form-item label="统一信用代码">
          <a-input v-model:value="formState.creditCode" />
        </a-form-item>
        <a-form-item label="联系人">
          <a-input v-model:value="formState.contact" />
        </a-form-item>
        <a-form-item label="联系电话">
          <a-input v-model:value="formState.tel" />
        </a-form-item>
        <a-form-item label="所在城市">
          <a-input v-model:value="formState.city" placeholder="如：深圳市" />
        </a-form-item>
        <a-form-item label="办公地址">
          <a-input v-model:value="formState.address" placeholder="如：南山区科技园路 1 号" />
        </a-form-item>
        <a-form-item label="开票抬头">
          <a-input v-model:value="formState.invoiceTitle" />
        </a-form-item>
        <a-form-item label="税号">
          <a-input v-model:value="formState.taxNo" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formState.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 导入模板说明弹窗 -->
    <a-modal
      v-model:open="templateVisible"
      title="下载导入模板"
      @ok="downloadTemplate"
      ok-text="下载"
    >
      <p>请下载 Excel 模板，按模板填写客户数据后使用「批量导入」上传。</p>
      <p>模板包含列：客户名称、统一信用代码、联系人、联系电话、开票抬头、税号、所在城市、办公地址。</p>
      <p class="tip">注：客户名称+统一信用代码唯一的记录才会被导入，重复或必填缺失的行将被跳过。</p>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { createCustomer, getCustomers, disableCustomer, updateCustomer, batchDeleteCustomers, importCustomers, getCustomerTemplate } from '../../../api/ems'
import { downloadBlob } from '../../../api/request'
import { renderDate } from '../../../utils/date'
import { usePermission } from '../../../composables/usePermission'
import { useResizableColumns } from '../../../composables/useResizableTable'

const { hasPerm } = usePermission()
const loading = ref(false)
const dataList = ref([])
const drawerVisible = ref(false)
const submitLoading = ref(false)
const editingRecord = ref(null)
const searchText = ref('')
const templateVisible = ref(false)
const importLoading = ref(false)

const scrollY = ref(420)
function syncTableHeight() {
  const box = document.querySelector('.page-wrap .tbl-box')
  if (!box) return
  const boxRect = box.getBoundingClientRect()
  const headerEl = box.querySelector('.ant-table-thead')
  const headerH = headerEl ? headerEl.getBoundingClientRect().height : 40
  const pagEl = box.querySelector('.ant-table-pagination')
  let reservedBottom = 0
  if (pagEl) {
    const pagRect = pagEl.getBoundingClientRect()
    reservedBottom = boxRect.bottom - pagRect.top
  }
  if (!reservedBottom) reservedBottom = 80
  const h = boxRect.height - headerH - reservedBottom - 4
  scrollY.value = h > 200 ? Math.floor(h) : 200
}

let pageObserver = null

const selectedRowKeys = ref([])
const hasSelected = computed(() => selectedRowKeys.value.length > 0)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const { columns, handleResizeColumn } = useResizableColumns([
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60, sorter: true },
  { title: '客户名称', dataIndex: 'custName', key: 'custName', sorter: true },
  { title: '信用代码', dataIndex: 'creditCode', key: 'creditCode' },
  { title: '联系人', dataIndex: 'contact', key: 'contact' },
  { title: '电话', dataIndex: 'tel', key: 'tel' },
  { title: '所在城市', dataIndex: 'city', key: 'city' },
  { title: '办公地址', dataIndex: 'address', key: 'address' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 140, customRender: renderDate, sorter: true },
  { title: '操作', key: 'action', width: 140 }
])

const formState = reactive({
  custName: '',
  creditCode: '',
  contact: '',
  tel: '',
  city: '',
  address: '',
  invoiceTitle: '',
  taxNo: '',
  status: 1
})

function resetForm() {
  Object.assign(formState, {
    custName: '', creditCode: '', contact: '', tel: '',
    city: '', address: '', invoiceTitle: '', taxNo: '', status: 1
  })
}

function showDrawer(record) {
  if (record) {
    editingRecord.value = record
    Object.assign(formState, {
      custName: record.custName,
      creditCode: record.creditCode,
      contact: record.contact,
      tel: record.tel,
      city: record.city,
      address: record.address,
      invoiceTitle: record.invoiceTitle,
      taxNo: record.taxNo,
      status: record.status
    })
  } else {
    editingRecord.value = null
    resetForm()
  }
  drawerVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await getCustomers({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchText.value || undefined
    })
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || [])
    dataList.value = list
    pagination.total = Array.isArray(data) ? list.length : (data.total || list.length)
  } catch {
    // ignore
  }
  loading.value = false
  nextTick(syncTableHeight)
}

async function handleSubmit() {
  if (!formState.custName) {
    message.warning('请填写客户名称')
    return
  }
  submitLoading.value = true
  try {
    if (editingRecord.value) {
      await updateCustomer(editingRecord.value.id, { ...formState })
    } else {
      await createCustomer({ ...formState })
    }
    message.success('保存成功')
    drawerVisible.value = false
    loadData()
  } catch {
    // ignore
  }
  submitLoading.value = false
}

async function handleDisable(record) {
  try {
    await disableCustomer(record.id)
    message.success(record.status === 1 ? '已停用' : '已启用')
    loadData()
  } catch {
    // ignore
  }
}

function onSelectChange(keys) {
  selectedRowKeys.value = keys
}

async function handleBatchDelete() {
  try {
    await batchDeleteCustomers(selectedRowKeys.value)
    message.success('已删除选中客户')
    selectedRowKeys.value = []
    loadData()
  } catch {
    // ignore
  }
}

async function handleImport(file) {
  importLoading.value = true
  try {
    const res = await importCustomers(file)
    const cnt = (res.data && typeof res.data === 'number') ? res.data : res.data?.data
    message.success(`导入成功，新增 ${cnt ?? '?'} 条`)
    loadData()
  } catch {
    // ignore
  }
  importLoading.value = false
  return false // 阻止自动上传
}

function downloadTemplate() {
  downloadBlob(getCustomerTemplate(), '客户导入模板.xlsx')
    .then(() => {
      message.success('模板已开始下载')
      templateVisible.value = false
    })
    .catch(() => message.error('模板下载失败'))
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(() => {
  loadData()
  nextTick(() => {
    syncTableHeight()
    const wrap = document.querySelector('.page-wrap')
    if (wrap && 'ResizeObserver' in window) {
      pageObserver = new ResizeObserver(() => syncTableHeight())
      pageObserver.observe(wrap)
    }
    window.addEventListener('resize', syncTableHeight)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', syncTableHeight)
  if (pageObserver) pageObserver.disconnect()
})
</script>

<style scoped>
.page-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
  box-sizing: border-box;
}
.card-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-sizing: border-box;
  overflow: hidden;
}
.page-wrap :deep(.ant-card) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.page-wrap :deep(.ant-card-body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 12px 8px 0;
}
.tbl-box {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-table-wrapper) {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-spin-nested-loading) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-table) {
  flex: 1;
  min-height: 0;
}
.tbl-box :deep(.ant-table-pagination) {
  margin: 8px 0 16px !important;
  flex: 0 0 auto;
}
.tip {
  color: #999;
  font-size: 12px;
}
</style>
