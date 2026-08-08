<template>
  <div class="page">
    <a-card :bordered="false" class="sample-page-card">
      <template #title>样品管理</template>
      <template #extra>
        <a-button type="primary" @click="openCollect">样品登记</a-button>
      </template>

      <a-form layout="inline" class="filter" @finish="loadData">
        <a-form-item label="状态">
          <a-select v-model:value="filters.status" style="width:140px" allow-clear @change="loadData">
            <a-select-option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关键字">
          <a-input v-model:value="filters.keyword" placeholder="样品名称/条码" @press-enter="loadData" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button style="margin-left:8px;" @click="resetFilter">重置</a-button>
        </a-form-item>
      </a-form>

      <div class="sample-list-wrap">
        <a-spin :spinning="loading">
        <div class="sample-grid" v-if="list.length">
          <div class="sample-card" v-for="(record, idx) in list" :key="record.id">
            <div class="sample-card-header">
              <span class="sample-barcode" @click="openDetail(record)">{{ record.barcode }}</span>
              <a-tag :color="statusColor(record.status)" class="sample-status-tag">{{ record.status }}</a-tag>
            </div>
            <div class="sample-card-barcode">
              <svg :ref="el => setCardBarcodeRef(el, idx)" class="card-barcode-svg"></svg>
            </div>
            <div class="sample-card-body">
              <div class="sample-name" :title="record.name">{{ record.name }}</div>
              <div class="sample-meta">
                <div class="meta-row"><span class="meta-label">类型</span><span class="meta-value">{{ record.type || '-' }}</span></div>
                <div class="meta-row"><span class="meta-label">来源</span><span class="meta-value">{{ record.source || '-' }}</span></div>
                <div class="meta-row"><span class="meta-label">保存条件</span><span class="meta-value">{{ record.preserve || '-' }}</span></div>
                <div class="meta-row"><span class="meta-label">收样人</span><span class="meta-value">{{ record.receiveBy || '-' }}</span></div>
                <div class="meta-row"><span class="meta-label">留样到期</span><span class="meta-value">{{ record.retainUntil || '-' }}</span></div>
              </div>
            </div>
            <div class="sample-card-actions">
              <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
              <a-divider type="vertical" />
              <a-button type="link" size="small" danger @click="openDispose(record)" v-if="record.status === '异常拒收' || record.status === '检测异常'">异常处置</a-button>
              <a-divider type="vertical" v-if="record.status === '异常拒收' || record.status === '检测异常'" />
              <a-button type="link" size="small" @click="openReceive(record)" :disabled="record.status !== '待收样'">收样</a-button>
              <a-divider type="vertical" />
              <a-button type="link" size="small" @click="openRetain(record)" :disabled="record.status !== '已收样'">留样</a-button>
              <a-divider type="vertical" />
              <a-button type="link" size="small" @click="openQc(record)">质控</a-button>
              <a-divider type="vertical" />
              <a-popconfirm title="确认删除该样品？" @confirm="remove(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无样品数据" style="padding:60px 0;" />
      </a-spin>
      </div>

      <a-pagination
        v-model:current="pagination.current"
        v-model:pageSize="pagination.pageSize"
        :total="pagination.total"
        :show-total="(t) => `共 ${t} 条`"
        show-size-changer
        show-quick-jumper
        @change="onPageChange"
        style="margin-top:16px;text-align:right;"
      />
    </a-card>

    <!-- 收样 -->
    <a-modal
      v-model:open="receiveOpen"
      title="登记收样"
      :footer="null"
      @cancel="receiveOpen = false"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="样品条码">
          <span>{{ current?.barcode }}</span>
        </a-form-item>
        <a-form-item label="样品名称">
          <span>{{ current?.name }}</span>
        </a-form-item>
        <a-form-item label="收样人" required>
          <a-select
            v-model:value="receiveForm.receiveBy"
            show-search
            placeholder="选择收样人"
            :options="userOptions"
            :filter-option="filterUser"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="收样时间">
          <a-date-picker v-model:value="receiveDate" value-format="YYYY-MM-DD" style="width:100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="receiveForm.remark" rows="2" />
        </a-form-item>
        <a-form-item label="核验项">
          <a-checkbox-group v-model:value="receiveForm.checkItems" :options="receiveCheckOptions" class="sample-check-group" />
        </a-form-item>
        <a-form-item label="是否留样" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-switch v-model:checked="receiveRetainChecked" checked-children="留样" un-checked-children="不留样" />
        </a-form-item>
        <template v-if="receiveRetainChecked">
          <a-form-item label="留样保存天数" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-input-number v-model:value="receiveForm.retainDays" :min="1" :precision="0" style="width:100%" />
          </a-form-item>
          <a-form-item label="留样人" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-select
              v-model:value="receiveForm.retainBy"
              show-search
              placeholder="选择留样人"
              :options="userOptions"
              :filter-option="filterUser"
              style="width:100%"
            />
          </a-form-item>
          <a-form-item label="留样日期" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-date-picker v-model:value="receiveRetainDate" value-format="YYYY-MM-DD" style="width:100%" />
          </a-form-item>
          <a-form-item label="存放位置" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-input v-model:value="receiveForm.retainLocation" placeholder="如 留样室A区3层货架2号" style="width:100%" />
          </a-form-item>
        </template>
      </a-form>
      <div class="receive-footer">
        <a-button type="primary" @click="submitReceive('receive')" :loading="submitting">正常收样</a-button>
        <a-button danger @click="submitReceive('reject')" :loading="submitting">异常拒收</a-button>
        <a-button @click="receiveOpen = false">取消</a-button>
      </div>
    </a-modal>

    <!-- 异常处置 -->
    <a-modal
      v-model:open="disposeOpen"
      title="异常处置"
      :width="500"
      @ok="submitDispose"
      :confirm-loading="submitting"
      ok-text="确定"
      cancel-text="取消"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="样品编号">
          <span>{{ disposeForm.sampleBarcode }}</span>
        </a-form-item>
        <a-form-item label="处置类型" required>
          <a-select
            v-model:value="disposeForm.disposalType"
            placeholder="请选择处置类型"
            :options="dictOptions('disposalType')"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="处置方式" required>
          <a-select
            v-model:value="disposeForm.disposalMethod"
            placeholder="请选择处置方式"
            :options="dictOptions('disposalMethod')"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="处置说明" required>
          <RichTextEditor v-model:value="disposeForm.disposalDesc" placeholder="请输入异常处置说明" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 收集样品 -->
    <a-drawer
      v-model:open="collectOpen"
      title="收集样品"
      width="1000"
      @close="resetCollect"
    >
      <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 14 }">
        <a-divider class="title-divider" orientation="left">基本信息</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="派单号" required>
              <a-select
                v-model:value="collectForm.dispatchId"
                show-search
                placeholder="选择派单"
                :options="dispatchOptions"
                :filter-option="filterDispatch"
                style="width:100%"
                @change="onCollectDispatchChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="委托单位">
              <a-input :value="collectForm.entrustName" readonly />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户名称">
              <a-input :value="collectForm.custName" readonly />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采样点位" required>
              <a-select
                v-model:value="collectForm.pointId"
                show-search
                placeholder="选择点位"
                :options="pointOptions"
                :filter-option="filterPoint"
                style="width:100%"
                @change="onCollectPointChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="样品类别">
              <a-input :value="collectForm.category" readonly />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检测项目" required>
              <a-select
                v-model:value="collectForm.item"
                mode="multiple"
                placeholder="选择检测项目"
                :options="itemOptions"
                style="width:100%"
                @change="onCollectItemChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="样品类型" required>
              <a-select
                v-model:value="collectForm.sampleType"
                placeholder="选择样品类型"
                :options="sampleTypeOptions"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采样天气状况">
              <a-input v-model:value="collectForm.weather" placeholder="如 晴/多云/小雨/风力3级" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采样人" required>
              <a-select
                v-model:value="collectForm.sampler"
                show-search
                placeholder="选择采样人"
                :options="userOptions"
                :filter-option="filterUser"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采样时间" required>
              <a-date-picker
                v-model:value="collectForm.sampleTime"
                show-time
                value-format="YYYY-MM-DD HH:mm"
                placeholder="选择采样时间"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数量规格">
              <a-input v-model:value="collectForm.amount" placeholder="如 500mL / 1kg" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="保存容器">
              <a-input v-model:value="collectForm.container" placeholder="如 聚乙烯瓶 / 玻璃瓶" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="保存条件">
              <a-input v-model:value="collectForm.preserve" placeholder="如 冷藏/避光/4℃" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider class="title-divider" orientation="left">采样信息（监测参数）</a-divider>
        <template v-if="selectedParams.length">
          <div v-for="grp in selectedParams" :key="grp.item" class="param-group">
            <div class="param-group-title">{{ grp.itemLabel || grp.item }}</div>
            <a-table
              :columns="paramColumns"
              :data-source="grp.params"
              :pagination="false"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'value'">
                  <a-input v-model:value="record.value" placeholder="实测值" style="width:100%" />
                </template>
              </template>
            </a-table>
          </div>
        </template>
        <a-empty v-else description="请先选择检测项目" />

        <!-- 固定剂 / 现场质控 -->
        <a-divider class="title-divider" orientation="left">固定剂</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="固定剂">
              <a-select
                v-model:value="collectForm.preservatives"
                mode="multiple"
                placeholder="多选，数据来源数据字典"
                :options="preservativeOptions"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="质控方式">
              <a-select
                v-model:value="collectForm.qcTypes"
                mode="multiple"
                placeholder="多选，数据来源数据字典"
                :options="qcTypeOptions"
                style="width:100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 现场照片 -->
        <a-divider class="title-divider" orientation="left">现场采样照片</a-divider>
        <div class="photo-block">
          <a-upload
            list-type="picture-card"
            v-model:file-list="fileList"
            :before-upload="beforeCollectUpload"
            :custom-request="customCollectUpload"
            @preview="handleCollectPreview"
            @remove="handleCollectRemove"
            multiple
            accept="image/*"
          >
            <div v-if="fileList.length < 20">
              <plus-outlined />
              <div style="margin-top:8px;">上传</div>
            </div>
          </a-upload>
          <a-modal :open="previewOpen" :footer="null" @cancel="previewOpen = false">
            <img alt="preview" style="width:100%" :src="previewImage" />
          </a-modal>
          <div class="hint">支持批量选择图片，上传后显示缩略图，点击可放大预览。</div>
        </div>
      </a-form>

      <template #footer>
        <a-button @click="collectOpen = false">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="submitCollect">保存</a-button>
      </template>
    </a-drawer>

    <!-- 留样 -->
    <a-modal
      v-model:open="retainOpen"
      title="登记留样"
      @ok="submitRetain"
      @cancel="retainOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="留样天数" required>
          <a-input-number v-model:value="retainForm.retainDays" :min="1" style="width:100%" />
        </a-form-item>
        <a-form-item label="留样人" required>
          <a-input v-model:value="retainForm.retainBy" />
        </a-form-item>
        <a-form-item label="留样日期">
          <a-date-picker v-model:value="retainDate" value-format="YYYY-MM-DD" style="width:100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="retainForm.remark" rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 质控样 -->
    <a-modal
      v-model:open="qcOpen"
      title="绑定质控样"
      @ok="submitQc"
      @cancel="qcOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="样品编号">
          <a-input v-model:value="qcForm.sampleNo" />
        </a-form-item>
        <a-form-item label="质控类型" required>
          <a-select v-model:value="qcForm.qcType" placeholder="请选择">
            <a-select-option v-for="t in qcTypes" :key="t" :value="t">{{ t }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="qcForm.remark" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <a-drawer v-model:open="detailOpen" title="样品详情" width="1000" @close="detailOpen = false">
      <template v-if="detail">
        <!-- 条形码图 -->
        <a-card size="small" class="barcode-card" :bordered="true">
          <div class="barcode-wrap">
            <svg ref="barcodeRef" class="barcode-svg"></svg>
          </div>
          <div class="barcode-meta">
            <span class="barcode-text">{{ detail.sample?.barcode }}</span>
            <span class="barcode-name">{{ detail.sample?.name }}</span>
          </div>
        </a-card>

        <!-- 采样信息 -->
        <a-divider class="title-divider" orientation="left">采样信息</a-divider>
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="样品条码">{{ detail.sample?.barcode }}</a-descriptions-item>
          <a-descriptions-item label="样品名称">{{ detail.sample?.name }}</a-descriptions-item>
          <a-descriptions-item label="样品类型">{{ dictText('sampleType', detail.sample?.type) }}</a-descriptions-item>
          <a-descriptions-item label="来源">{{ detail.sample?.source }}</a-descriptions-item>
          <a-descriptions-item label="容器">{{ detail.sample?.container }}</a-descriptions-item>
          <a-descriptions-item label="数量/规格">{{ detail.sample?.amount }}</a-descriptions-item>
          <a-descriptions-item label="检测类别">{{ detail.sample?.category }}</a-descriptions-item>
          <a-descriptions-item label="检测项目">{{ detail.sample?.item }}</a-descriptions-item>
          <a-descriptions-item label="保存条件">{{ detail.sample?.preserve }}</a-descriptions-item>
          <a-descriptions-item label="固定剂">{{ dictListText('preservative', detail.sample?.preservatives) }}</a-descriptions-item>
          <a-descriptions-item label="现场质控方式">{{ dictListText('qcType', detail.sample?.qcTypes) }}</a-descriptions-item>
          <a-descriptions-item label="采样天气">{{ detail.record?.weather || detail.sample?.weather || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采样人">{{ detail.sample?.sampler || detail.record?.sampler || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采样时间">{{ detail.sample?.sampleTime || detail.record?.sampleTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采样点位">{{ detail.pointName || detail.record?.pointName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(detail.sample?.status)">{{ detail.sample?.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="收样人">{{ detail.sample?.receiveBy }}</a-descriptions-item>
          <a-descriptions-item label="收样时间">{{ detail.sample?.receiveTime }}</a-descriptions-item>
        </a-descriptions>

        <!-- 核验项：单独整行展示在表格最下方 -->
        <a-descriptions :column="1" bordered size="small" class="check-items-row">
          <a-descriptions-item label="核验项">{{ dictListText('receiveCheck', detail.sample?.checkItems) || '未核验' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 采样参数 -->
        <a-divider class="title-divider" orientation="left">采样参数（监测指标）</a-divider>
        <template v-if="sampleParams.length">
          <div v-for="grp in sampleParams" :key="grp.item" class="param-group">
            <div class="param-group-title">{{ grp.item }}</div>
            <a-table
              :columns="paramColumns"
              :data-source="grp.params"
              size="small"
              row-key="code"
              :pagination="false"
              bordered
            />
          </div>
        </template>
        <a-empty v-else description="无采样参数" />

        <!-- 留样信息 -->
        <a-divider class="title-divider" orientation="left">留样信息</a-divider>
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="是否留样">
            <a-tag :color="(detail.sample?.retainFlag === 1 || detail.sample?.retainFlag === '1') ? 'green' : 'default'">
              {{ (detail.sample?.retainFlag === 1 || detail.sample?.retainFlag === '1') ? '留样' : '不留样' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="留样到期">{{ detail.sample?.retainUntil || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留样天数">{{ detail.sample?.retainDays || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留样人">{{ detail.sample?.retainBy || '-' }}</a-descriptions-item>
          <a-descriptions-item label="留样日期">{{ detail.sample?.retainDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="存放位置">{{ detail.sample?.retainLocation || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-divider class="title-divider" orientation="left">质控样</a-divider>
      <a-table
        v-if="detail.qcList && detail.qcList.length"
        :columns="qcColumns"
        :data-source="detail.qcList"
        size="small"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" danger @click="unbindQc(record)">解绑</a-button>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="无质控样" />

      <!-- 现场图片 -->
      <a-divider class="title-divider" orientation="left">现场图片</a-divider>
      <div class="photo-grid" v-if="photoList.length">
        <a-image
          v-for="(p, i) in photoList"
          :key="i"
          :src="p"
          :width="120"
          :height="120"
          :preview="{ mask: '查看' }"
          style="border-radius:6px;overflow:hidden;"
        />
      </div>
      <a-empty v-else description="无现场图片" />

      <!-- 异常处置信息（仅异常拒收/检测异常样品且有处置记录时展示） -->
      <template v-if="detail.sample && detail.sample.disposalTime">
        <a-divider class="title-divider" orientation="left">异常处置信息</a-divider>
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="处置类型">{{ dictText('disposalType', detail.sample.disposalType) }}</a-descriptions-item>
          <a-descriptions-item label="处置方式">{{ dictText('disposalMethod', detail.sample.disposalMethod) }}</a-descriptions-item>
          <a-descriptions-item label="处置人">{{ detail.sample.disposalBy }}</a-descriptions-item>
          <a-descriptions-item label="处置时间">{{ detail.sample.disposalTime }}</a-descriptions-item>
          <a-descriptions-item label="处置说明" :span="2">
            <div class="rich-content" v-html="detail.sample.disposalDesc"></div>
          </a-descriptions-item>
        </a-descriptions>
      </template>

      <a-divider class="title-divider" orientation="left">操作日志</a-divider>
      <a-timeline v-if="detail.logs && detail.logs.length">
        <a-timeline-item v-for="log in detail.logs" :key="log.id">
          <b>{{ log.action }}</b> · {{ log.operator }} · {{ log.detail }}
          <span style="color:#999;">（{{ log.createTime }}）</span>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无日志" />
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { Upload } from 'ant-design-vue'
import JsBarcode from 'jsbarcode'
import { useUserStore } from '../../../stores/user'
import RichTextEditor from '../../../components/RichTextEditor.vue'
import {
  getSamples,
  receiveSample,
  retainSample,
  bindSampleQc,
  unbindSampleQc,
  getSampleDetail,
  deleteSample,
  disposeSample,
  collectSample,
  getCollectDispatchList,
  getEntrust,
  getSampleParamConfigs,
  getDictItems,
  uploadSamplePhoto
} from '../../../api/ems'
import { getUsers } from '../../../api/system'

const loading = ref(false)
const list = ref([])

// 卡片条形码 DOM 收集（函数式 ref）
const cardBarcodeRefs = ref([])
function setCardBarcodeRef(el, idx) {
  if (el) cardBarcodeRefs.value[idx] = el
}

// 渲染所有卡片条形码
function renderCardBarcodes() {
  nextTick(() => {
    list.value.forEach((rec, idx) => {
      const el = cardBarcodeRefs.value[idx]
      if (el && rec.barcode) {
        try {
          JsBarcode(el, String(rec.barcode), {
            format: 'CODE128',
            width: 1.5,
            height: 36,
            margin: 4,
            displayValue: true,
            fontSize: 12,
            textAlign: 'center'
          })
        } catch (e) {
          el.innerHTML = ''
        }
      }
    })
  })
}
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: undefined, keyword: '' })
const statusOptions = ['待收样', '已收样', '异常拒收', '留样中', '实验室监测中', '检测数据复核中', '已完成', '检测异常', '已处置']
const qcTypes = ['全程序空白', '现场空白', '平行样', '加标回收', '密码样']

const qcColumns = [
  { title: '样品编号', dataIndex: 'sampleNo', key: 'sampleNo' },
  { title: '质控类型', dataIndex: 'qcType', key: 'qcType' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 80 }
]

function statusColor(status) {
  return {
    '待收样': 'orange',
    '已收样': 'blue',
    '异常拒收': 'orange',
    '留样中': 'purple',
    '实验室监测中': 'cyan',
    '检测数据复核中': 'gold',
    '已完成': 'green',
    '检测异常': 'red',
    '已处置': 'default'
  }[status] || 'default'
}

// 兼容 字符串 / 数组 / 空 三种来源的展示
function formatList(val) {
  if (!val) return '-'
  if (Array.isArray(val)) return val.join('、') || '-'
  if (typeof val === 'string') return val.split(',').map(s => s.trim()).filter(Boolean).join('、') || '-'
  return '-'
}

// ===== 详情：数据字典 value → itemText 转换 =====
// 字典编码 → { value: text } 映射，openDetail 时加载
const DETAIL_DICT_CODES = {
  sampleType: 'moni_sample_type',
  preservative: 'sample_preservative',
  qcType: 'moni_qc_type',
  receiveCheck: 'sample_receive_check',
  disposalType: 'moni_disposal_type',
  disposalMethod: 'moni_disposal_method'
}
const dictMaps = reactive({
  sampleType: {},
  preservative: {},
  qcType: {},
  receiveCheck: {},
  disposalType: {},
  disposalMethod: {}
})

async function loadDetailDicts() {
  const entries = Object.entries(DETAIL_DICT_CODES)
  const results = await Promise.all(
    entries.map(([, code]) => getDictItems(code).catch(() => null))
  )
  entries.forEach(([key], idx) => {
    const list = results[idx]?.data || []
    const m = {}
    list.forEach(it => { m[it.itemValue] = it.itemText })
    dictMaps[key] = m
  })
}

// 单值字典字段转展示文本
function dictText(key, val) {
  if (val === null || val === undefined || val === '') return '-'
  return dictMaps[key]?.[val] ?? val
}

// 逗号分隔的字典字段转展示文本
function dictListText(key, val) {
  if (!val) return '-'
  const arr = Array.isArray(val) ? val : String(val).split(',').map(s => s.trim()).filter(Boolean)
  if (!arr.length) return '-'
  return arr.map(v => dictMaps[key]?.[v] ?? v).join('、')
}

// 字典映射转 a-select 的 options 数组
function dictOptions(key) {
  const m = dictMaps[key] || {}
  return Object.keys(m).map(value => ({ label: m[value], value }))
}

const receiveOpen = ref(false)
const receiveForm = reactive({ receiveBy: '', remark: '', checkItems: [], retainDays: 30, retainBy: undefined, retainLocation: '' })
const receiveDate = ref(null)
const receiveRetainChecked = ref(false)
const receiveRetainDate = ref(null)
const retainOpen = ref(false)
const retainForm = reactive({ retainDays: 30, retainBy: '', remark: '' })
const retainDate = ref(null)
const qcOpen = ref(false)
const qcForm = reactive({ sampleNo: '', qcType: '', remark: '' })
const detailOpen = ref(false)
const detail = ref(null)
const current = ref(null)

// 异常处置
const disposeOpen = ref(false)
const disposeTarget = ref(null)
const disposeForm = reactive({ sampleBarcode: '', disposalType: undefined, disposalMethod: undefined, disposalDesc: '' })

function openDispose(record) {
  disposeTarget.value = record
  disposeForm.sampleBarcode = record.barcode || record.sampleNo || ''
  disposeForm.disposalType = undefined
  disposeForm.disposalMethod = undefined
  disposeForm.disposalDesc = ''
  loadDetailDicts()
  disposeOpen.value = true
}

async function submitDispose() {
  if (!disposeForm.disposalType) { message.warning('请选择处置类型'); return }
  if (!disposeForm.disposalMethod) { message.warning('请选择处置方式'); return }
  if (!disposeForm.disposalDesc || !disposeForm.disposalDesc.trim()) { message.warning('请输入处置说明'); return }
  submitting.value = true
  try {
    await disposeSample(disposeTarget.value.id, {
      disposalType: disposeForm.disposalType,
      disposalMethod: disposeForm.disposalMethod,
      disposalDesc: disposeForm.disposalDesc
    })
    message.success('异常处置已提交')
    disposeOpen.value = false
    await loadData()
    if (detailOpen.value) await openDetail(disposeTarget.value)
  } catch (e) {
    message.error(e?.response?.data?.message || e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ===== 详情：条形码 / 图片 / 参数 =====
const barcodeRef = ref(null)
const PHOTO_BASE = '/api/v1/ems/base/sampling/samples/photo/'

const photoList = computed(() => {
  const raw = detail.value?.sample?.samplePhoto
  if (!raw || typeof raw !== 'string') return []
  return raw.split(',').map(s => s.trim()).filter(Boolean).map(p => PHOTO_BASE + p)
})

const sampleParams = computed(() => {
  const raw = detail.value?.sample?.sampleParams
  if (!raw) return []
  let parsed
  try {
    parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) {
    return []
  }
  const arr = Array.isArray(parsed) ? parsed : []
  // 新数据：参数项自带 item 字段，直接信任使用
  // 旧数据（无 item）：通过样品检测项目列表 + 参数配置反查所属项目
  //   同一 code 可能出现在多个项目中（如 sample_time），按样品 item 字段的声明顺序
  //   首次匹配优先，保证与收集表单展示顺序一致
  const hasItem = arr.some(it => it.item)
  if (hasItem) {
    // 新数据模式：直接按 it.item 分组
    const map = new Map()
    arr.forEach(it => {
      const key = it.item || '其他'
      if (!map.has(key)) map.set(key, { item: key, params: [] })
      map.get(key).params.push(it)
    })
    return Array.from(map.values())
  }
  // 旧数据兼容模式：构建 [项目名 → Set<code>] 映射，按样品 item 顺序匹配
  const sampleItems = (detail.value?.sample?.item || '').split(',').map(s => s.trim()).filter(Boolean)
  const itemCodes = new Map() // item → Set<code>
  ;(sampleParamConfigs.value || []).forEach(c => {
    if (!sampleItems.includes(c.item)) return
    const codes = new Set()
    ;(c.sampleParams || []).forEach(p => { if (p.code) codes.add(p.code) })
    if (codes.size) itemCodes.set(c.item, codes)
  })
  const map = new Map()
  arr.forEach(it => {
    let key = '其他'
    for (const itemName of sampleItems) {
      const codes = itemCodes.get(itemName)
      if (codes && codes.has(it.code)) { key = itemName; break }
    }
    if (!map.has(key)) map.set(key, { item: key, params: [] })
    map.get(key).params.push(it)
  })
  return Array.from(map.values())
})

// 条形码渲染：detail.sample.barcode 变化时重绘
// immediate + 双层 nextTick：detail 赋值后 <template v-if="detail"> 才渲染 svg，
// 需要 nextTick 等 DOM 挂载完成，barcodeRef 才有值
watch(
  () => detail.value?.sample?.barcode,
  (code) => {
    if (!code) {
      if (barcodeRef.value) barcodeRef.value.innerHTML = ''
      return
    }
    nextTick(() => {
      nextTick(() => {
        if (!barcodeRef.value) return
        try {
          JsBarcode(barcodeRef.value, String(code), {
            format: 'CODE128',
            width: 2,
            height: 56,
            margin: 8,
            displayValue: true,
            fontSize: 14,
            textAlign: 'center'
          })
        } catch (e) {
          if (barcodeRef.value) barcodeRef.value.innerHTML = ''
        }
      })
    })
  },
  { immediate: true }
)
const submitting = ref(false)

// ===== 收集样品相关 =====
const userStore = useUserStore()
const userOptions = ref([])
const sampleTypeOptions = ref([])
const preservativeOptions = ref([])
const qcTypeOptions = ref([])
const receiveCheckOptions = ref([])
const itemOptions = ref([])
const dispatchOptions = ref([])
const pointOptions = ref([])
const sampleParamConfigs = ref([])
const selectedParams = ref([])
const paramColumns = [
  { title: '参数编码', dataIndex: 'code', key: 'code' },
  { title: '参数名称', dataIndex: 'name', key: 'name' },
  { title: '实测值', dataIndex: 'value', key: 'value' },
  { title: '单位', dataIndex: 'unit', key: 'unit' }
]

const collectOpen = ref(false)
const fileList = ref([])
const previewOpen = ref(false)
const previewImage = ref('')

const collectForm = reactive({
  dispatchId: undefined,
  dispatchNo: '',
  entrustId: undefined,
  entrustName: '',
  custName: undefined,
  pointId: undefined,
  category: '',
  item: [],
  sampleType: undefined,
  weather: '',
  sampler: undefined,
  sampleTime: '',
  amount: '',
  container: '',
  preserve: '',
  preservatives: [],
  qcTypes: []
})

function todayStr() {
  const d = new Date()
  const p = n => (n < 10 ? '0' + n : '' + n)
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
}

// 当前时刻字符串，格式与采样时间选择器 value-format 一致：YYYY-MM-DD HH:mm
function nowDateTimeStr() {
  const d = new Date()
  const p = n => (n < 10 ? '0' + n : '' + n)
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
    + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
}

function filterUser(input, option) {
  const label = (option.label || '').toLowerCase()
  return label.includes(input.toLowerCase())
}
function filterDispatch(input, option) {
  return (option.label || '').toLowerCase().includes(input.toLowerCase())
}
function filterPoint(input, option) {
  return (option.label || '').toLowerCase().includes(input.toLowerCase())
}

async function loadUsers() {
  try {
    const res = await getUsers()
    const arr = res.data || res || []
    userOptions.value = arr.map(u => ({
      label: u.realName ? `${u.realName}（${u.username}）` : u.username,
      value: u.realName || u.username
    }))
  } catch (e) { /* ignore */ }
}

async function loadCollectDicts() {
  try {
    const [pres, qc, st, chk] = await Promise.all([
      getDictItems('sample_preservative'),
      getDictItems('moni_qc_type'),
      getDictItems('moni_sample_type'),
      getDictItems('sample_receive_check')
    ])
    // DictItem 字段为 itemText / itemValue（非 label/value）
    const toOpts = (arr) => (arr || []).map(d => ({ label: d.itemText, value: d.itemValue }))
    preservativeOptions.value = toOpts(pres.data)
    qcTypeOptions.value = toOpts(qc.data)
    sampleTypeOptions.value = toOpts(st.data)
    receiveCheckOptions.value = toOpts(chk.data)
  } catch (e) { /* ignore */ }
}

async function loadCollectDispatchOptions() {
  try {
    // 仅拉取「已派单」状态的采样订单，待派单/已完成的不在收样工作台可选
    const res = await getCollectDispatchList({ status: '已派单' })
    const arr = res.data || []
    dispatchOptions.value = arr.map(d => {
      // 后端派单看板接口返回的派单号字段是 orderNo（非 dispatchNo），同时无 points/items
      const orderNo = d.orderNo || `派单#${d.id}`
      const tag = d.entrustName || d.custName || ''
      return {
        label: `${orderNo}${tag ? '（' + tag + '）' : ''}`,
        value: d.id,
        raw: d
      }
    })
  } catch (e) { /* ignore */ }
}

async function onCollectDispatchChange(id) {
  const opt = dispatchOptions.value.find(o => o.value === id)
  if (!opt) return
  const d = opt.raw
  collectForm.dispatchNo = d.orderNo || ''
  collectForm.entrustId = d.entrustId
  collectForm.custName = d.custName
  collectForm.entrustName = d.entrustName || d.custName
  collectForm.category = d.category || ''
  // 派单看板无 points/items，通过委托详情回填点位（含 factors）与检测项目
  let points = []
  if (d.entrustId) {
    try {
      const er = await getEntrust(d.entrustId)
      const e = er.data || er
      if (e && Array.isArray(e.points)) points = e.points
    } catch (e2) { /* ignore */ }
  }
  pointOptions.value = points.map(p => ({ label: p.pointName || p.name, value: p.id, raw: p }))
  // 检测项目候选：汇总所有点位的 factors（逗号分隔）去重
  const factorSet = new Set()
  points.forEach(p => {
    if (p && typeof p.factors === 'string') {
      p.factors.split(',').map(s => s.trim()).filter(Boolean).forEach(f => factorSet.add(f))
    }
  })
  itemOptions.value = Array.from(factorSet).map(f => ({ label: f, value: f }))
}

function onCollectPointChange(id) {
  const p = pointOptions.value.find(o => o.value === id)
  if (!p || !p.raw) return
  // 类别取点位类型 pointType（EmsMonitorPoint 无 category 字段）
  collectForm.category = p.raw.pointType || collectForm.category
  // 选点位后，检测项目候选过滤为该点位的 factors
  const factors = (p.raw.factors || '').split(',').map(s => s.trim()).filter(Boolean)
  itemOptions.value = factors.map(f => ({ label: f, value: f }))
  // 清空已选但不再属于该点位的项目
  if (Array.isArray(collectForm.item)) {
    collectForm.item = collectForm.item.filter(i => factors.includes(i))
    onCollectItemChange(collectForm.item)
  }
}

function onCollectItemChange(vals) {
  // 选中检测项目后，按监测项目分组，每组对应一组参数输入
  // 保留参数项与监测项目的对应关系（item 字段），用于分组渲染和提交
  const groups = vals
    .map(v => sampleParamConfigs.value.find(c => c.item === v))
    .filter(Boolean)
    .map(c => ({
      item: c.item,
      itemLabel: c.item,
      params: (c.sampleParams || []).map(it => ({
        code: it.code, name: it.name, value: '', unit: it.unit, item: c.item
      }))
    }))
    .filter(g => g.params.length)
  selectedParams.value = groups
}

async function loadCollectParams() {
  try {
    const res = await getSampleParamConfigs()
    sampleParamConfigs.value = res.data || []
  } catch (e) { /* ignore */ }
}

function beforeCollectUpload(file) {
  const isImg = file.type && file.type.startsWith('image/')
  if (!isImg) {
    message.error('只能上传图片文件')
    return Upload.LIST_IGNORE
  }
  if (file.size / 1024 / 1024 > 10) {
    message.error('图片不能超过 10MB')
    return Upload.LIST_IGNORE
  }
  return true // 允许加入列表，由 custom-request 处理实际上传
}

async function customCollectUpload({ file, onSuccess, onError }) {
  try {
    const res = await uploadSamplePhoto(file)
    const data = res.data || {}
    const photoUrl = data.url
    const photoPath = data.path
    // 找到 a-upload 在 fileList 中自动生成的对应项（非原始 File 对象）
    const idx = fileList.value.findIndex(f => f.uid === file.uid)
    if (idx > -1) {
      const target = fileList.value[idx]
      target.url = photoUrl
      target.thumbUrl = photoUrl
      target.status = 'done'
      target.photoPath = photoPath
      target.response = res
      // 强制替换引用触发渲染
      fileList.value = [...fileList.value]
      onSuccess(res, target)
    } else {
      // 兜底：直接回填原始对象
      file.url = photoUrl
      file.thumbUrl = photoUrl
      file.status = 'done'
      file.photoPath = photoPath
      file.response = res
      onSuccess(res, file)
    }
    message.success(`${file.name} 上传成功`)
  } catch (e) {
    const idx = fileList.value.findIndex(f => f.uid === file.uid)
    if (idx > -1) {
      fileList.value[idx].status = 'error'
      fileList.value = [...fileList.value]
    } else {
      file.status = 'error'
    }
    onError(e)
    message.error(`${file.name} 上传失败`)
  }
}

function handleCollectPreview(file) {
  previewImage.value = file.url || file.thumbUrl ||
    (file.response && file.response.data && file.response.data.url)
  previewOpen.value = true
}

function handleCollectRemove(file) {
  const idx = fileList.value.findIndex(f => f.uid === file.uid)
  if (idx > -1) fileList.value.splice(idx, 1)
}

function openCollect() {
  collectOpen.value = true
  resetCollect()
  // 采样人默认当前登录用户，采样时间默认当前时刻
  collectForm.sampler = userStore.realName || userStore.username || undefined
  collectForm.sampleTime = nowDateTimeStr()
  loadCollectDispatchOptions()
  loadCollectDicts()
  loadCollectParams()
  loadUsers()
}

function resetCollect() {
  Object.assign(collectForm, {
    dispatchId: undefined,
    dispatchNo: '',
    entrustId: undefined,
    entrustName: '',
    custName: undefined,
    pointId: undefined,
    category: '',
    item: [],
    sampleType: undefined,
    weather: '',
    sampler: undefined,
    sampleTime: '',
    amount: '',
    container: '',
    preserve: '',
    preservatives: [],
    qcTypes: []
  })
  fileList.value = []
  selectedParams.value = []
  pointOptions.value = []
  itemOptions.value = []
}

async function submitCollect() {
  if (!collectForm.dispatchId) return message.warning('请选择派单')
  if (!collectForm.pointId) return message.warning('请选择采样点位')
  if (!collectForm.item.length) return message.warning('请选择检测项目')
  if (!collectForm.sampleType) return message.warning('请选择样品类型')
  if (!collectForm.sampler) return message.warning('请选择采样人')
  if (!collectForm.sampleTime) return message.warning('请选择采样时间')
  submitting.value = true
  try {
    await collectSample({
      dispatchId: collectForm.dispatchId,
      dispatchNo: collectForm.dispatchNo,
      entrustId: collectForm.entrustId,
      custName: collectForm.custName,
      pointId: collectForm.pointId,
      category: collectForm.category,
      item: collectForm.item.join(','),
      type: collectForm.sampleType,
      weather: collectForm.weather,
      sampler: collectForm.sampler,
      sampleTime: collectForm.sampleTime,
      amount: collectForm.amount,
      container: collectForm.container,
      preserve: collectForm.preserve,
      name: `${collectForm.custName}-${collectForm.item.join('/')}`,
      source: pointOptions.value.find(o => o.value === collectForm.pointId)?.raw?.name || '',
      sampleParams: selectedParams.value.flatMap(g => (g.params || []).map(p => ({
        item: g.item, code: p.code, name: p.name, value: p.value, unit: p.unit
      }))),
      preservatives: collectForm.preservatives,
      qcTypes: collectForm.qcTypes,
      photos: fileList.value.map(f => f.photoPath || (f.response && f.response.data && f.response.data.path)).filter(Boolean)
    })
    message.success('收集成功')
    collectOpen.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function onPageChange(page, pageSize) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadData()
}

function resetFilter() {
  filters.status = undefined
  filters.keyword = ''
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSamples({
      page: pagination.current,
      size: pagination.pageSize,
      status: filters.status,
      keyword: filters.keyword || undefined
    })
    const page = res.data || {}
    list.value = page.records || []
    pagination.total = page.total || 0
    renderCardBarcodes()
  } finally {
    loading.value = false
  }
}

function openReceive(record) {
  current.value = record
  // 回填样品已有收样信息
  receiveForm.receiveBy = userStore.realName || userStore.username || ''
  receiveForm.remark = record?.remark || ''
  receiveDate.value = record?.receiveTime || todayStr()
  // 同步继承留样信息：样品已留样或存在任一留样字段时自动勾选并回填
  const hasRetain = record?.retainFlag === 1 || record?.retainFlag === '1' ||
    record?.retainBy || record?.retainDays || record?.retainLocation || record?.retainDate
  receiveRetainChecked.value = !!hasRetain
  receiveForm.retainDays = record?.retainDays || 30
  receiveForm.retainBy = record?.retainBy || (userStore.realName || userStore.username || undefined)
  receiveForm.retainLocation = record?.retainLocation || ''
  receiveRetainDate.value = record?.retainDate || todayStr()
  receiveOpen.value = true
  loadUsers()
  loadCollectDicts()
}

async function submitReceive(action) {
  if (!receiveForm.receiveBy) return message.warning('请选择收样人')
  const isReject = action === 'reject'
  if (!isReject) {
    if (receiveRetainChecked.value && !receiveForm.retainDays) return message.warning('请填写留样保存天数')
    if (receiveRetainChecked.value && !receiveForm.retainBy) return message.warning('请选择留样人')
  }
  submitting.value = true
  try {
    await receiveSample(current.value.id, {
      action: action,
      receiveBy: receiveForm.receiveBy,
      receiveTime: receiveDate.value,
      remark: receiveForm.remark,
      checkItems: (receiveForm.checkItems || []).join(','),
      retainFlag: receiveRetainChecked.value ? 1 : 0,
      retainDays: receiveRetainChecked.value ? receiveForm.retainDays : null,
      retainBy: receiveRetainChecked.value ? receiveForm.retainBy : null,
      retainDate: receiveRetainChecked.value ? receiveRetainDate.value : null,
      retainLocation: receiveRetainChecked.value ? receiveForm.retainLocation : null
    })
    message.success(isReject ? '已记录异常拒收' : '收样登记成功')
    receiveOpen.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openRetain(record) {
  current.value = record
  retainForm.retainDays = 30
  retainForm.retainBy = ''
  retainForm.remark = ''
  retainDate.value = null
  retainOpen.value = true
}

async function submitRetain() {
  if (!retainForm.retainDays || retainForm.retainDays <= 0) return message.warning('请填写有效的留样天数')
  if (!retainForm.retainBy) return message.warning('请填写留样人')
  submitting.value = true
  try {
    await retainSample(current.value.id, {
      retainDays: retainForm.retainDays,
      retainBy: retainForm.retainBy,
      retainTime: retainDate.value,
      remark: retainForm.remark
    })
    message.success('留样登记成功')
    retainOpen.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openQc(record) {
  current.value = record
  qcForm.sampleNo = record.barcode
  qcForm.qcType = ''
  qcForm.remark = ''
  qcOpen.value = true
}

async function submitQc() {
  if (!qcForm.qcType) return message.warning('请选择质控类型')
  submitting.value = true
  try {
    await bindSampleQc(current.value.id, qcForm)
    message.success('质控样绑定成功')
    qcOpen.value = false
    if (detailOpen.value) openDetail(current.value)
  } finally {
    submitting.value = false
  }
}

async function unbindQc(record) {
  await unbindSampleQc(record.id)
  message.success('已解绑')
  if (current.value) openDetail(current.value)
}

async function openDetail(record) {
  // 先加载采样参数配置，确保后续 sampleParams 计算属性反查分组时有数据（避免异步时序导致旧数据无法分组）
  await loadCollectParams()
  const res = await getSampleDetail(record.id)
  detail.value = res.data
  current.value = record
  detailOpen.value = true
  // 加载详情所需字典（用于 value→text 转换）
  loadDetailDicts()
}

async function remove(record) {
  await deleteSample(record.id)
  message.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 4px;
  box-sizing: border-box;
  overflow: hidden;
}
/* 卡片占满整页，内部采用纵向 flex：查询栏 + 滚动区 + 分页栏 */
.sample-page-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.sample-page-card :deep(.ant-card-body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 16px;
}
/* 查询工具栏：固定在顶部，不随列表滚动 */
.filter {
  margin-bottom: 12px;
  flex: 0 0 auto;
}
/* 中间列表区：可滚动 */
.sample-list-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  margin-bottom: 12px;
}
/* 分页工具栏：固定在底部 */
.sample-page-card :deep(.ant-pagination) {
  flex: 0 0 auto;
  margin-top: 0;
  text-align: right;
}
.code-link { color: #1677ff; cursor: pointer; }
.code-link:hover { text-decoration: underline; }
.title-divider { margin: 16px 0 !important; font-weight: 600; }
.hint { color: #999; font-size: 12px; margin-top: 6px; }
.photo-block { text-align: left; }
.photo-block :deep(.ant-upload-list-picture-card) { text-align: left; }
.barcode-card { margin-bottom: 8px; background: #fafafa; }
.barcode-wrap { display: flex; justify-content: center; padding: 8px 0; }
.barcode-svg { max-width: 100%; height: auto; }
.barcode-meta { display: flex; justify-content: center; align-items: baseline; gap: 12px; margin-top: 4px; }
.barcode-text { font-family: 'Courier New', monospace; font-size: 15px; font-weight: 600; color: #333; letter-spacing: 1px; }
.barcode-name { font-size: 13px; color: #666; }
.photo-grid { display: flex; flex-wrap: wrap; gap: 12px; }
/* 样品卡片视图 */
.sample-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; margin-top: 12px; }
.sample-card { background: #fff; border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; transition: all .25s; display: flex; flex-direction: column; }
.sample-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.08); border-color: #d6e4ff; transform: translateY(-2px); }
.sample-card-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: linear-gradient(135deg, #fafcff 0%, #f0f5ff 100%); border-bottom: 1px solid #f0f0f0; }
.sample-barcode { font-family: 'Courier New', monospace; font-size: 15px; font-weight: 600; color: #1677ff; cursor: pointer; letter-spacing: .5px; }
.sample-barcode:hover { text-decoration: underline; }
.sample-status-tag { margin: 0; }
.sample-card-body { padding: 14px 16px; flex: 1; }
.sample-card-barcode { display: flex; justify-content: center; padding: 6px 0 0; background: #fff; }
.card-barcode-svg { max-width: 90%; height: auto; }
.sample-name { font-size: 15px; font-weight: 500; color: #333; margin-bottom: 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sample-meta { display: flex; flex-direction: column; gap: 6px; }
.meta-row { display: flex; font-size: 13px; line-height: 1.5; }
.meta-label { width: 70px; color: #999; flex-shrink: 0; }
.meta-value { color: #555; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sample-card-actions { display: flex; align-items: center; flex-wrap: wrap; padding: 8px 12px; border-top: 1px solid #f5f5f5; background: #fafafa; }
.sample-card-actions :deep(.ant-btn-link) { padding: 0 6px; height: 28px; font-size: 13px; }
.sample-card-actions :deep(.ant-divider-vertical) { height: 14px; margin: 0 2px; }

/* 按监测项目分组的采样参数区 */
.param-group { margin-bottom: 16px; }
.param-group-title { font-weight: 600; color: #1677ff; margin-bottom: 8px; padding-left: 8px; border-left: 3px solid #1677ff; }

/* 核验项复选项组：多列排布，避免选项过多时竖排过长 */
.sample-check-group :deep(.ant-checkbox-group) { display: flex; flex-wrap: wrap; gap: 4px 24px; }
.sample-check-group :deep(.ant-checkbox-wrapper) { margin-left: 0; }

/* 收样弹窗底部操作按钮区：右侧排列 */
.receive-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
</style>
