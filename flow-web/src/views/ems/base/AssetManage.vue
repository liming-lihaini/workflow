<template>
  <div class="page-wrap">
    <a-tabs v-model:activeKey="tab">
      <!-- ===== 设备台账 ===== -->
      <a-tab-pane key="instrument" tab="设备台账">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">仪器设备（全生命周期）</span>
            <a-space wrap>
              <a-radio-group v-model:value="instViewMode" button-style="solid">
                <a-radio-button value="list">列表</a-radio-button>
                <a-radio-button value="calendar">使用日历</a-radio-button>
              </a-radio-group>
              <a-input-search v-if="instViewMode === 'list'" v-model:value="kw" placeholder="搜索编号/名称/型号" style="width: 220px" allow-clear @search="loadInstruments" />
              <a-select v-if="instViewMode === 'list'" v-model:value="statusFilter" placeholder="状态" allow-clear style="width: 120px" :options="statusOptions" @change="loadInstruments" />
              <template v-if="instViewMode === 'list'">
                <a-button type="primary" @click="startInboundProcess('SBTKRKSQ')">单品入库申请</a-button>
                <a-button type="primary" @click="startInboundProcess('SBTKRKSQ_PL')">批量入库申请</a-button>
                <a-button type="primary" @click="showInstDrawer()">新增设备</a-button>
              </template>
            </a-space>
          </div>

          <!-- 列表模式 -->
          <template v-if="instViewMode === 'list'">
            <a-alert
              v-if="expiring.length"
              type="warning"
              show-icon
              style="margin-bottom: 12px"
              :message="`校准预警：${expiring.length} 台设备临近到期或已停用`"
            />
            <div class="tbl-box">
            <a-table
              :columns="instColumns"
              :data-source="instList"
              :loading="instLoading"
              :pagination="instPagination"
              row-key="id"
              :scroll="{ y: scrollY }"
              @change="handleInstTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'code'">
                  <span class="code-link" @click="openInstDetail(record)">{{ record.code }}</span>
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="instStatusColor(record.status)">{{ record.status }}</a-tag>
                </template>
                <template v-if="column.key === 'calibDue'">
                  <span :style="{ color: isExpiring(record) ? '#fa8c16' : 'inherit' }">{{ record.calibDue || '-' }}</span>
                </template>
                <template v-if="column.key === 'action'">
                  <span class="action-link" @click="showInstDrawer(record)">编辑</span>
                  <a-divider type="vertical" />
                  <span class="action-link" @click="showCalibrate(record)">校准登记</span>
                  <a-divider type="vertical" />
                  <span v-if="record.status !== '报废'" class="action-link danger" @click="startScrapProcess('设备', record, 'code')">报废</span>
                  <template v-if="record.status !== '报废'"><a-divider type="vertical" /></template>
                  <a-popconfirm title="删除该设备？" @confirm="handleInstDelete(record)">
                    <span class="action-link danger">删除</span>
                  </a-popconfirm>
                </template>
              </template>
            </a-table>
            </div>
          </template>

          <!-- 使用日历模式：参考车辆使用日历 -->
          <div v-if="instViewMode === 'calendar'" class="calendar-host">
            <a-space wrap class="cal-toolbar">
              <a-radio-group v-model:value="instCalUnit" button-style="solid" size="small" @change="instOnCalUnitChange">
                <a-radio-button value="week">周</a-radio-button>
                <a-radio-button value="month">月</a-radio-button>
              </a-radio-group>
              <a-range-picker
                v-model:value="instCalRange"
                :allow-clear="false"
                size="small"
                value-format="YYYY-MM-DD"
                @change="instApplyUnit(false)"
              />
              <a-button size="small" @click="instShiftRange(-1)">{{ instCalUnit === 'month' ? '‹ 上一月' : '‹ 上一周' }}</a-button>
              <a-button size="small" @click="instShiftRange(1)">{{ instCalUnit === 'month' ? '下一月 ›' : '下一周 ›' }}</a-button>
              <a-button size="small" @click="instGoToday">今天</a-button>
              <a-button size="small" type="primary" :loading="instCalLoading" @click="loadInstrumentUsage">刷新</a-button>
              <span class="cal-tip">
                <i class="cal-dot cal-dot-dispatch" />派单占用
                <i class="cal-dot cal-dot-maint" />校准占用
              </span>
            </a-space>

            <div v-if="instCalLoading" class="cal-loading"><a-spin /></div>
            <div v-else class="cal-grid">
              <div class="cal-row cal-head">
                <div class="cal-label">设备</div>
                <div class="cal-track">
                  <div v-for="d in instDays" :key="d.key" class="cal-cell cal-cell-head">
                    <div class="cal-dow">{{ d.dow }}</div>
                    <div v-if="instCalUnit === 'week'" class="cal-date">{{ d.date }}</div>
                  </div>
                </div>
              </div>
              <div v-for="v in instUsageList" :key="v.instrumentId" class="cal-row">
                <div class="cal-label" :title="v.code + ' ' + v.name">
                  <div class="cal-plate"><b>{{ v.code }}</b></div>
                  <div class="cal-model">{{ v.name }}{{ v.model ? ' / ' + v.model : '' }}</div>
                </div>
                <div class="cal-track">
                  <div v-for="d in instDays" :key="d.key" class="cal-cell">
                    <template v-for="blk in instBlocksOf(v, d)" :key="blk.key">
                      <div
                        class="cal-block"
                        :class="[blk.kind === 'maint' ? 'cal-block-maint' : 'cal-block-dispatch', { 'cal-block-partial': !blk.fullDay }]"
                        :title="blk.tip"
                      >{{ blk.fullDay ? '' : (blk.kind === 'maint' ? '校' : '派') }}</div>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <a-empty v-if="!instCalLoading && !instUsageList.length" description="所选区间暂无设备占用记录" style="margin-top: 40px" />
          </div>
        </div>
      </a-tab-pane>

      <!-- ===== 标准物质 ===== -->
      <a-tab-pane key="material" tab="标准物质">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openMaterial()">+ 新增标物</a-button>
            <a-button type="primary" @click="startMaterialProcess('WZRKSQ', '标准物质')">入库申请</a-button>
            <a-button type="primary" @click="startMaterialProcess('WZSYSQ', '标准物质')">使用申请</a-button>
            <a-input-search v-model:value="mk" placeholder="名称搜索" style="width:200px" @search="loadMaterials" allow-clear />
            <a-select v-model:value="mStatus" style="width:140px" @change="loadMaterials">
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="在库">在库</a-select-option>
              <a-select-option value="临期">临期</a-select-option>
              <a-select-option value="过期">过期</a-select-option>
            </a-select>
            <a-button @click="checkGate">效期闸门校验</a-button>
          </a-space>
          <div class="tbl-box">
          <a-table :columns="materialCols" :data-source="materials" row-key="id" :pagination="mp" :loading="ml" :scroll="{ y: scrollY }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <span class="cell-link" @click="openMaterialDetail(record)">{{ record.lotNo }}</span>
              </template>
              <template v-else-if="column.key === 'name'">
                <span class="cell-link" @click="openMaterialDetail(record)">{{ record.name }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status==='已报废'||record.status==='过期'?'red':(record.status==='临期'?'orange':'green')">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="startMaterialProcess('WZRKSQ', '标准物质', record)">入库</a>
                  <a @click="startMaterialProcess('WZSYSQ', '标准物质', record)">使用</a>
                  <a v-if="record.status !== '已报废'" class="danger-link" @click="startScrapProcess('标准物质', record, 'spec')">报废</a>
                  <a @click="openMaterial(record)">编辑</a>
                </a-space>
              </template>
            </template>
          </a-table>
          </div>
        </a-card>
      </a-tab-pane>

      <!-- ===== 耗材 ===== -->
      <a-tab-pane key="consumable" tab="耗材">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openConsumable()">+ 新增耗材</a-button>
            <a-button type="primary" @click="startMaterialProcess('WZRKSQ', '耗材')">入库申请</a-button>
            <a-button type="primary" @click="startMaterialProcess('WZSYSQ', '耗材')">使用申请</a-button>
            <a-input-search v-model:value="ck" placeholder="名称搜索" style="width:200px" @search="loadConsumables" allow-clear />
            <a-select v-model:value="cStatus" style="width:140px" @change="loadConsumables">
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="在库">在库</a-select-option>
              <a-select-option value="临期">临期</a-select-option>
              <a-select-option value="过期">过期</a-select-option>
            </a-select>
          </a-space>
          <div class="tbl-box">
          <a-table :columns="consumableCols" :data-source="consumables" row-key="id" :pagination="cp" :loading="cl" :scroll="{ y: scrollY }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <span class="cell-link" @click="openConsumableDetail(record)">{{ record.id ? 'HC' + String(record.id).padStart(4, '0') : '' }}</span>
              </template>
              <template v-else-if="column.key === 'name'">
                <span class="cell-link" @click="openConsumableDetail(record)">{{ record.name }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status==='已报废'||record.status==='过期'?'red':(record.status==='临期'?'orange':'green')">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="startMaterialProcess('WZRKSQ', '耗材', record)">入库</a>
                  <a @click="startMaterialProcess('WZSYSQ', '耗材', record)">使用</a>
                  <a v-if="record.status !== '已报废'" class="danger-link" @click="startScrapProcess('耗材', record, 'spec')">报废</a>
                  <a @click="openConsumable(record)">编辑</a>
                </a-space>
              </template>
            </template>
          </a-table>
          </div>
        </a-card>
      </a-tab-pane>

      <!-- ===== 危化品（由危化品管理并入物质管理） ===== -->
      <a-tab-pane key="hazardous" tab="危化品">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openHaz()">+ 新增危化品</a-button>
            <a-input-search v-model:value="hzk" placeholder="名称/CAS搜索" style="width:220px" @search="loadHaz" allow-clear />
            <a-select v-model:value="hzStatus" style="width:140px" @change="loadHaz">
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="在库">在库</a-select-option>
              <a-select-option value="待审批">待审批</a-select-option>
              <a-select-option value="已领用">已领用</a-select-option>
              <a-select-option value="已报废">已报废</a-select-option>
            </a-select>
          </a-space>
          <a-table :columns="hazCols" :data-source="hazRows" row-key="id" :pagination="hzpg" :loading="hzLoading">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status==='已报废'?'red':(record.status==='待审批'?'orange':(record.status==='已领用'?'blue':'green'))">
                  {{ record.status }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="openHazDetail(record)">详情</a>
                  <a v-if="record.status==='在库'" @click="applyHaz(record)">申请</a>
                  <a v-if="record.status==='待审批'" @click="approveHaz(record)">审批</a>
                  <a v-if="record.status==='在库'" class="danger-link" @click="startScrapProcess('危化品', record, 'casNo')">报废</a>
                  <a @click="openHaz(record)">编辑</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- 危化品表单 -->
    <a-modal v-model:open="hzVisible" :title="hzForm.id?'编辑危化品':'新增危化品'" @ok="saveHaz" @cancel="hzVisible=false">
      <a-form :model="hzForm" layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="hzForm.name" /></a-form-item>
        <a-form-item label="CAS号"><a-input v-model:value="hzForm.casNo" /></a-form-item>
        <a-form-item label="类别">
          <a-select v-model:value="hzForm.category" placeholder="请选择类别" :options="hazCatOptions" show-search allow-clear />
        </a-form-item>
        <a-form-item label="数量"><a-input v-model:value="hzForm.qty" /></a-form-item>
        <a-form-item label="单位"><a-input v-model:value="hzForm.unit" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="hzApplyVisible" title="申请领用/报废" @ok="doApplyHaz" @cancel="hzApplyVisible=false">
      <a-form layout="vertical">
        <a-form-item label="申请人"><a-input v-model:value="hzApplyBy" /></a-form-item>
        <a-form-item label="用途/原因"><a-textarea v-model:value="hzApplyReason" :rows="3" /></a-form-item>
        <a-form-item label="目标状态">
          <a-radio-group v-model:value="hzTargetStatus">
            <a-radio value="已领用">领用</a-radio>
            <a-radio value="已报废">报废</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="hzApproveVisible" title="审批" @ok="doApproveHaz" @cancel="hzApproveVisible=false">
      <a-form layout="vertical">
        <a-form-item label="审批人"><a-input v-model:value="hzApproveBy" /></a-form-item>
        <a-form-item label="意见"><a-textarea v-model:value="hzApproveOpinion" :rows="3" /></a-form-item>
        <a-form-item label="结果">
          <a-radio-group v-model:value="hzApproveOk">
            <a-radio :value="true">通过</a-radio>
            <a-radio :value="false">退回</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 设备新增/编辑 -->
    <a-drawer v-model:open="instVisible" :title="instEditing ? '编辑设备' : '新增设备'" width="560" @close="instVisible = false">
      <a-form :model="instForm" layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="仪器编号"><a-input v-model:value="instForm.code" placeholder="YQ001" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="仪器名称" required><a-input v-model:value="instForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="型号"><a-input v-model:value="instForm.model" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="生产厂商"><a-input v-model:value="instForm.manufacturer" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="购置日期"><a-date-picker v-model:value="instForm.purchaseDate" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="校准到期日"><a-date-picker v-model:value="instForm.calibDue" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="instForm.status" :options="statusOptions" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="校准证书号"><a-input v-model:value="instForm.certNo" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="备注"><a-textarea v-model:value="instForm.remark" :rows="2" /></a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="instVisible = false">取消</a-button>
          <a-button type="primary" :loading="instSubmitting" @click="handleInstSave">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 设备校准登记 -->
    <a-modal v-model:open="calibVisible" title="校准登记" @ok="submitCalibrate" ok-text="登记">
      <a-form layout="vertical">
        <a-form-item label="校准日期"><a-date-picker v-model:value="calibForm.calibDate" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item>
        <a-form-item label="下次校准到期日" required><a-date-picker v-model:value="calibForm.calibDue" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item>
        <a-form-item label="校准证书编号"><a-input v-model:value="calibForm.certNo" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 设备详情 -->
    <InstrumentDetail :open="instDetailVisible" :instrument-id="instDetailId" @close="instDetailVisible = false" />

    <!-- 标物新增/编辑抽屉 -->
    <a-drawer v-model:open="mVisible" :title="mForm.id ? '编辑标准物质' : '新增标准物质'" width="1000" @close="mVisible = false">
      <a-form :model="mForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="名称"><a-input v-model:value="mForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="批号"><a-input v-model:value="mForm.lotNo" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="规格"><a-input v-model:value="mForm.spec" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="效期"><a-date-picker v-model:value="mExpire" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="库存"><a-input-number v-model:value="mForm.stock" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="证书编号"><a-input v-model:value="mForm.certNo" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="mVisible = false">取消</a-button>
          <a-button type="primary" :loading="mSubmitting" @click="submitMaterial">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 标物详情抽屉 -->
    <a-drawer v-model:open="mDetailVisible" title="标准物质详情" width="1000" @close="mDetailVisible = false">
      <a-descriptions :column="2" bordered size="middle" v-if="mDetail">
        <a-descriptions-item label="编号">{{ mDetail.lotNo }}</a-descriptions-item>
        <a-descriptions-item label="名称">{{ mDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="规格">{{ mDetail.spec }}</a-descriptions-item>
        <a-descriptions-item label="效期">{{ mDetail.expireDate }}</a-descriptions-item>
        <a-descriptions-item label="库存">{{ mDetail.stock }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="mDetail.status==='已报废'||mDetail.status==='过期'?'red':(mDetail.status==='临期'?'orange':'green')">{{ mDetail.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="证书编号">{{ mDetail.certNo }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ mDetail.createTime }}</a-descriptions-item>
      </a-descriptions>
      <a-divider orientation="left">关联流程</a-divider>
      <a-table
        v-if="mDetail && mDetail.relatedProcesses && mDetail.relatedProcesses.length"
        :dataSource="mDetail.relatedProcesses"
        :columns="materialProcessCols"
        rowKey="processInstanceId"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusText'">
            <a-tag :color="materialProcessStatusColor(record.statusText)">{{ record.statusText }}</a-tag>
          </template>
          <template v-else-if="column.key === 'bizType'">
            <a-tag v-if="record.bizType" :color="record.bizType === '入库' ? 'green' : (record.bizType === '报废' ? 'red' : 'orange')">{{ record.bizType }}</a-tag>
            <span v-else style="color:#999">审批中</span>
          </template>
          <template v-else-if="column.key === 'startTime'">{{ formatFlowTime(record.startTime) }}</template>
        </template>
      </a-table>
      <a-empty v-else description="暂无关联流程" />
    </a-drawer>

    <!-- 耗材新增/编辑抽屉 -->
    <a-drawer v-model:open="cVisible" :title="cForm.id ? '编辑耗材' : '新增耗材'" width="1000" @close="cVisible = false">
      <a-form :model="cForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="名称"><a-input v-model:value="cForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="规格"><a-input v-model:value="cForm.spec" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="数量"><a-input-number v-model:value="cForm.qty" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="效期"><a-date-picker v-model:value="cExpire" style="width:100%" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="cVisible = false">取消</a-button>
          <a-button type="primary" :loading="cSubmitting" @click="submitConsumable">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 耗材详情抽屉 -->
    <a-drawer v-model:open="cDetailVisible" title="耗材详情" width="1000" @close="cDetailVisible = false">
      <a-descriptions :column="2" bordered size="middle" v-if="cDetail">
        <a-descriptions-item label="编号">{{ cDetail.id ? 'HC' + String(cDetail.id).padStart(4, '0') : '' }}</a-descriptions-item>
        <a-descriptions-item label="名称">{{ cDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="规格">{{ cDetail.spec }}</a-descriptions-item>
        <a-descriptions-item label="数量">{{ cDetail.stock }}</a-descriptions-item>
        <a-descriptions-item label="效期">{{ cDetail.expireDate }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="cDetail.status==='已报废'||cDetail.status==='过期'?'red':(cDetail.status==='临期'?'orange':'green')">{{ cDetail.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ cDetail.createTime }}</a-descriptions-item>
      </a-descriptions>
      <a-divider orientation="left">关联流程</a-divider>
      <a-table
        v-if="cDetail && cDetail.relatedProcesses && cDetail.relatedProcesses.length"
        :dataSource="cDetail.relatedProcesses"
        :columns="materialProcessCols"
        rowKey="processInstanceId"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusText'">
            <a-tag :color="materialProcessStatusColor(record.statusText)">{{ record.statusText }}</a-tag>
          </template>
          <template v-else-if="column.key === 'bizType'">
            <a-tag v-if="record.bizType" :color="record.bizType === '入库' ? 'green' : (record.bizType === '报废' ? 'red' : 'orange')">{{ record.bizType }}</a-tag>
            <span v-else style="color:#999">审批中</span>
          </template>
          <template v-else-if="column.key === 'startTime'">{{ formatFlowTime(record.startTime) }}</template>
        </template>
      </a-table>
      <a-empty v-else description="暂无关联流程" />
    </a-drawer>

    <!-- 危化品详情抽屉 -->
    <a-drawer v-model:open="hzDetailVisible" title="危化品详情" width="1000" @close="hzDetailVisible = false">
      <a-descriptions :column="2" bordered size="middle" v-if="hzDetail">
        <a-descriptions-item label="名称">{{ hzDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="CAS号">{{ hzDetail.casNo }}</a-descriptions-item>
        <a-descriptions-item label="类别">{{ hzDetail.category }}</a-descriptions-item>
        <a-descriptions-item label="数量">{{ hzDetail.qty }} {{ hzDetail.unit }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="hzDetail.status==='已报废'?'red':(hzDetail.status==='待审批'?'orange':(hzDetail.status==='已领用'?'blue':'green'))">{{ hzDetail.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="申请人">{{ hzDetail.applyBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请原因" :span="2">{{ hzDetail.applyReason || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ hzDetail.createTime }}</a-descriptions-item>
      </a-descriptions>
      <a-divider orientation="left">关联流程</a-divider>
      <a-table
        v-if="hzDetail && hzDetail.relatedProcesses && hzDetail.relatedProcesses.length"
        :dataSource="hzDetail.relatedProcesses"
        :columns="materialProcessCols"
        rowKey="processInstanceId"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusText'">
            <a-tag :color="materialProcessStatusColor(record.statusText)">{{ record.statusText }}</a-tag>
          </template>
          <template v-else-if="column.key === 'bizType'">
            <a-tag v-if="record.bizType" color="red">{{ record.bizType }}</a-tag>
            <span v-else style="color:#999">审批中</span>
          </template>
          <template v-else-if="column.key === 'startTime'">{{ formatFlowTime(record.startTime) }}</template>
        </template>
      </a-table>
      <a-empty v-else description="暂无关联流程" />
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, nextTick, h } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import minMax from 'dayjs/plugin/minMax'
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore'
import isSameOrAfter from 'dayjs/plugin/isSameOrAfter'
dayjs.extend(minMax)
dayjs.extend(isSameOrBefore)
dayjs.extend(isSameOrAfter)
import {
  listInstruments, createInstrument, updateInstrument, deleteInstrument,
  calibrateInstrument, expiringInstruments, getDictItems, getInstrumentUsage,
  saveMaterial, getMaterials, getMaterialDetail, saveConsumable, getConsumables, getConsumableDetail, checkMaterialGate,
  saveHazardous, getHazardous, getHazardousDetail, applyHazardous, approveHazardous
} from '../../../api/ems'
import InstrumentDetail from './InstrumentDetail.vue'

const router = useRouter()
const tab = ref('instrument')

/* ============ 表格铺满页面高度（精确测量 tbl-box，避免撑出主页滚动条） ============ */
const scrollY = ref(420)
function syncTableHeight() {
  // 测量当前激活标签页内 .tbl-box 的可用高度
  // scroll.y = tbl-box 高度 - 表头 - 分页(含margin) - 安全余量
  const pane = document.querySelector('.ant-tabs-tabpane-active')
  const box = pane && pane.querySelector('.tbl-box')
  if (!box) return
  const boxRect = box.getBoundingClientRect()
  const headerEl = box.querySelector('.ant-table-thead')
  const headerH = headerEl ? headerEl.getBoundingClientRect().height : 40
  const pagEl = box.querySelector('.ant-table-pagination')
  let reservedBottom = 0
  if (pagEl) {
    // 分页底部相对 box 底部的距离（含 margin）
    const pagRect = pagEl.getBoundingClientRect()
    reservedBottom = boxRect.bottom - pagRect.top
  }
  // 若无分页或未渲染，兜底预留 80px
  if (!reservedBottom) reservedBottom = 80
  const h = boxRect.height - headerH - reservedBottom - 4
  scrollY.value = h > 320 ? Math.floor(h) : 320
}

/* ============ 设备台账 ============ */
const instLoading = ref(false)
const instSubmitting = ref(false)
const instVisible = ref(false)
const calibVisible = ref(false)
const instEditing = ref(null)
const instDetailVisible = ref(false)
const instDetailId = ref(null)
const kw = ref('')
const statusFilter = ref(undefined)
const instList = ref([])
const expiring = ref([])
const statusOptions = ref([])
const instStatusFallback = [
  { label: '在用', value: '在用' }, { label: '临期', value: '临期' },
  { label: '停用', value: '停用' }, { label: '维修', value: '维修' }, { label: '报废', value: '报废' }
]
const instPagination = reactive({ current: 1, pageSize: 10, total: 0 })

const instColumns = [
  { title: '编号', dataIndex: 'code', key: 'code', width: 160 },
  { title: '名称', dataIndex: 'name', key: 'name', sorter: true },
  { title: '型号', dataIndex: 'model', key: 'model', width: 120 },
  { title: '厂商', dataIndex: 'manufacturer', key: 'manufacturer', width: 200 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90 },
  { title: '校准到期', key: 'calibDue', dataIndex: 'calibDue', width: 120 },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo', width: 160 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' }
]

const instForm = reactive({ id: undefined, code: '', name: '', model: '', manufacturer: '', purchaseDate: null, calibDue: null, status: '在用', certNo: '', remark: '' })
const calibForm = reactive({ id: undefined, calibDate: null, calibDue: null, certNo: '' })

function instStatusColor(s) {
  return { '在用': 'green', '临期': 'orange', '停用': 'red', '维修': 'blue', '报废': 'default' }[s] || 'default'
}
function isExpiring(r) {
  if (r.status === '停用' || r.status === '临期') return true
  if (r.calibDue) {
    const diff = dayjs(r.calibDue).diff(dayjs(), 'day')
    return diff <= 30 && diff >= 0
  }
  return false
}

function loadInstDict() {
  getDictItems('moni_instrument_status').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    statusOptions.value = list.length
      ? list.map((i) => ({ label: i.itemText, value: i.itemText }))
      : instStatusFallback
  }).catch(() => { statusOptions.value = instStatusFallback })
}

function loadInstruments() {
  instLoading.value = true
  listInstruments({ keyword: kw.value || undefined, status: statusFilter.value, page: instPagination.current, size: instPagination.pageSize })
    .then((res) => {
      const data = res.data || res
      const list = Array.isArray(data) ? data : (data.records || data.list || [])
      instList.value = list
      instPagination.total = Array.isArray(data) ? list.length : (data.total || list.length)
    })
    .catch(() => {})
    .finally(() => { instLoading.value = false })
  loadExpiring()
}

function loadExpiring() {
  expiringInstruments().then((res) => {
    const data = res.data || res
    expiring.value = Array.isArray(data) ? data : (data.list || [])
  }).catch(() => {})
}

function openInstDetail(record) {
  instDetailId.value = record.id
  instDetailVisible.value = true
}

function showInstDrawer(record) {
  if (record) {
    instEditing.value = record
    Object.assign(instForm, {
      id: record.id, code: record.code, name: record.name, model: record.model,
      manufacturer: record.manufacturer, purchaseDate: record.purchaseDate,
      calibDue: record.calibDue, status: record.status, certNo: record.certNo, remark: record.remark
    })
  } else {
    instEditing.value = null
    Object.assign(instForm, { id: undefined, code: '', name: '', model: '', manufacturer: '', purchaseDate: null, calibDue: null, status: '在用', certNo: '', remark: '' })
  }
  instVisible.value = true
}

function handleInstSave() {
  if (!instForm.name) { message.warning('请填写仪器名称'); return }
  instSubmitting.value = true
  const api = instEditing.value ? updateInstrument(instForm.id, instForm) : createInstrument(instForm)
  api.then(() => { message.success('保存成功'); instVisible.value = false; loadInstruments() })
    .catch(() => {}).finally(() => { instSubmitting.value = false })
}

function showCalibrate(record) {
  Object.assign(calibForm, { id: record.id, calibDate: dayjs().format('YYYY-MM-DD'), calibDue: null, certNo: record.certNo })
  calibVisible.value = true
}

function submitCalibrate() {
  if (!calibForm.calibDue) { message.warning('请填写下次校准到期日'); return }
  calibrateInstrument(calibForm.id, {
    calibDate: calibForm.calibDate, calibDue: calibForm.calibDue, certNo: calibForm.certNo
  }).then(() => { message.success('校准登记成功，状态已重算'); calibVisible.value = false; loadInstruments() })
    .catch(() => {})
}

function handleInstDelete(record) {
  deleteInstrument(record.id).then(() => { message.success('已删除'); loadInstruments() }).catch(() => {})
}

function handleInstTableChange(pag) {
  instPagination.current = pag.current
  instPagination.pageSize = pag.pageSize
  loadInstruments()
}

// 发起入库流程：跳转到流程发起详情页（单品 SBTKRKSQ / 批量 SBTKRKSQ_PL）
function startInboundProcess(processKey) {
  router.push({ path: '/task/start-detail', query: { processKey } })
}

// 发起物资入库(WZRKSQ)/使用(WZSYSQ)申请：跳转流程发起详情页，预填物资类型；
// 行内发起时额外预填名称/规格/批号，审批通过后由 Webhook 更新库存或新建物资
function startMaterialProcess(processKey, materialType, record) {
  const query = { processKey, materialType }
  if (record) {
    if (record.name) query.name = record.name
    if (record.spec) query.spec = record.spec
    if (record.lotNo) query.lotNo = record.lotNo
  }
  router.push({ path: '/task/start-detail', query })
}

// 发起资产报废申请(ZCBFSQ)：跳转流程发起详情页，预填资产类型/资产ID/名称/规格(编号)；
// 申请人需在表单中说明报废原因与处置方式，审批通过后由 Webhook 更新台账状态为报废
function startScrapProcess(assetType, record, specKey) {
  const query = { processKey: 'ZCBFSQ', assetType, assetId: record.id }
  if (record.name) query.name = record.name
  const spec = specKey ? record[specKey] : record.spec
  if (spec) query.spec = spec
  router.push({ path: '/task/start-detail', query })
}

/* ============ 物资关联流程（详情抽屉内展示） ============ */
const materialProcessCols = [
  { title: '实例编号', dataIndex: 'instanceNo', key: 'instanceNo', width: 170 },
  { title: '流程名称', dataIndex: 'processName', key: 'processName' },
  { title: '类型', key: 'bizType', width: 80 },
  { title: '数量', dataIndex: 'qty', key: 'qty', width: 70 },
  { title: '状态', key: 'statusText', width: 90 },
  { title: '发起人', dataIndex: 'startUser', key: 'startUser', width: 100 },
  { title: '发起时间', key: 'startTime', width: 160 }
]
function materialProcessStatusColor(s) {
  return { '运行中': 'blue', '已完成': 'green', '已暂停': 'orange', '已终止': 'red' }[s] || 'default'
}
function formatFlowTime(t) {
  if (!t) return '-'
  return dayjs(String(t).replace(' ', 'T')).format('YYYY-MM-DD HH:mm')
}

/* ============ 设备使用日历（参考车辆使用日历：行×日网格，占用区间取采样任务派单 planStart~planEnd） ============ */
const instViewMode = ref('list')
const instCalLoading = ref(false)
const instCalUnit = ref('month')
const instCalRange = ref([dayjs().startOf('month'), dayjs().endOf('month')])
const instDays = ref([])
const instUsageList = ref([])

function instBuildDays(start, end) {
  const days = []
  let d = start
  while (d.isBefore(end) || d.isSame(end, 'day')) {
    days.push({
      date: d.format('YYYY-MM-DD'),
      dow: instCalUnit.value === 'week' ? d.format('ddd') : d.format('MM/DD'),
      key: d.format('YYYY-MM-DD')
    })
    d = d.add(1, 'day')
  }
  return days
}

function instApplyUnit() {
  let s
  const r = instCalRange.value || []
  if (r && r.length === 2 && r[0]) {
    s = r[0]
  } else {
    s = dayjs()
  }
  if (instCalUnit.value === 'week') {
    s = dayjs().startOf('week')
    const e = s.add(6, 'day').endOf('day')
    instCalRange.value = [s, e]
    instDays.value = instBuildDays(s, e)
  } else {
    s = s.startOf('month')
    const e = s.endOf('month')
    instCalRange.value = [s, e]
    instDays.value = instBuildDays(s, e)
  }
  loadInstrumentUsage()
}

function instOnCalUnitChange() { instApplyUnit() }
function instShiftRange(delta) {
  const [s] = instCalRange.value
  if (instCalUnit.value === 'week') {
    instCalRange.value = [s.add(delta * 7, 'day'), s.add(delta * 7, 'day')]
  } else {
    instCalRange.value = [s.add(delta, 'month'), s.add(delta, 'month')]
  }
  instApplyUnit()
}
function instGoToday() {
  instCalRange.value = [dayjs(), dayjs()]
  instApplyUnit()
}

function loadInstrumentUsage() {
  instCalLoading.value = true
  const [s, e] = instCalRange.value
  const ps = s.startOf('day').format('YYYY-MM-DDTHH:mm:ss')
  const pe = e.endOf('day').format('YYYY-MM-DDTHH:mm:ss')
  getInstrumentUsage({ start: ps, end: pe }).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || [])
    // 计算每台设备的占用区间标签（最早开始~最晚结束）
    for (const v of list) {
      const all = [...(v.ranges || []), ...(v.maintenances || [])]
        .map((x) => ({ s: x.start ? dayjs(String(x.start).replace(' ', 'T')) : null, e: x.end ? dayjs(String(x.end).replace(' ', 'T')) : null }))
        .filter((x) => x.s || x.e)
      if (all.length) {
        const minS = dayjs.min(all.map((x) => x.s).filter(Boolean))
        const maxE = dayjs.max(all.map((x) => x.e).filter(Boolean))
        v.periodLabel = '占用：' + instFmtPeriod(minS, maxE)
      } else {
        v.periodLabel = '空闲'
      }
    }
    instUsageList.value = list
  }).catch(() => {}).finally(() => { instCalLoading.value = false })
}

function instFmtPeriod(s, e) {
  if (!s && !e) return ''
  const a = s ? s.format('MM-DD') : '?'
  const b = e ? e.format('MM-DD') : '?'
  return a + ' ~ ' + b
}
function instMaintTypeText(m) {
  return m.type || '校准'
}

// 返回某台设备在某天的占用块（与车辆使用日历一致：dispatch 派单占用 / maint 校准占用）
function instBlocksOf(v, day) {
  const out = []
  const ranges = v.ranges || []
  for (const r of ranges) {
    if (!r.start || !r.end) continue
    const s = dayjs(String(r.start).replace(' ', 'T'))
    const e = dayjs(String(r.end).replace(' ', 'T'))
    if (day.date >= s.format('YYYY-MM-DD') && day.date <= e.format('YYYY-MM-DD')) {
      const ds = dayjs(day.date)
      const fullDay = s.isSameOrBefore(ds.startOf('day')) && e.isSameOrAfter(ds.endOf('day'))
      out.push({
        kind: 'dispatch',
        fullDay,
        key: 'd' + r.dispatchId,
        tip: `派单#${r.dispatchId} ${r.status}\n${instFmtPeriod(s, e)}`
      })
    }
  }
  const ms = v.maintenances || []
  for (const m of ms) {
    if (!m.start || !m.end) continue
    const s = dayjs(String(m.start).replace(' ', 'T'))
    const e = dayjs(String(m.end).replace(' ', 'T'))
    if (day.date >= s.format('YYYY-MM-DD') && day.date <= e.format('YYYY-MM-DD')) {
      const ds = dayjs(day.date)
      const fullDay = s.isSameOrBefore(ds.startOf('day')) && e.isSameOrAfter(ds.endOf('day'))
      out.push({
        kind: 'maint',
        fullDay,
        key: 'm' + m.id,
        tip: `${instMaintTypeText(m)} ${m.certNo ? '(' + m.certNo + ')' : ''}\n${instFmtPeriod(s, e)}`
      })
    }
  }
  return out
}

/* ============ 标准物质 ============ */
const ml = ref(false)
const mk = ref(''), mStatus = ref('')
const materials = ref([])
const mp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { mp.current = p; loadMaterials() } })
const materialCols = [
  { title: '编号', dataIndex: 'lotNo', key: 'code', width: 140 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '规格', dataIndex: 'spec', key: 'spec' },
  { title: '效期', dataIndex: 'expireDate', key: 'expireDate' },
  { title: '库存', dataIndex: 'stock', key: 'stock' },
  { title: '状态', key: 'status' },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo' },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]
const mVisible = ref(false)
const mSubmitting = ref(false)
const mDetailVisible = ref(false)
const mDetail = ref(null)
const mForm = reactive({ id: null, name: '', lotNo: '', spec: '', expireDate: '', stock: 0, certNo: '' })
const mExpire = ref(null)

function toStr(d) { return d ? (d.$d ? d.format('YYYY-MM-DD') : d) : null }
function openMaterial(r) {
  if (r) { Object.assign(mForm, r); mExpire.value = r.expireDate ? dayjs(r.expireDate) : null }
  else { Object.keys(mForm).forEach(k => mForm[k] = k === 'stock' ? 0 : null); mExpire.value = null }
  mVisible.value = true
}
function openMaterialDetail(r) {
  mDetail.value = r
  mDetailVisible.value = true
  // 加载详情（含关联流程：入库申请/使用申请）
  getMaterialDetail(r.id).then((res) => {
    const vo = res.data || res
    if (vo && vo.id) mDetail.value = vo
  }).catch(() => {})
}
async function submitMaterial() {
  if (!mForm.name) { message.warning('请填写名称'); return }
  mSubmitting.value = true
  mForm.expireDate = toStr(mExpire.value)
  await saveMaterial({ ...mForm }); message.success('已保存'); mVisible.value = false; loadMaterials()
  mSubmitting.value = false
}
async function loadMaterials() {
  ml.value = true
  try {
    const res = await getMaterials({ keyword: mk.value, status: mStatus.value, page: mp.current, size: mp.pageSize })
    const p = res.data || res; materials.value = p.records || p.list || []; mp.total = p.total || materials.value.length
  } finally { ml.value = false }
}

/* ============ 耗材 ============ */
const cl = ref(false)
const ck = ref(''), cStatus = ref('')
const consumables = ref([])
const cp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { cp.current = p; loadConsumables() } })
const consumableCols = [
  { title: '编号', key: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '规格', dataIndex: 'spec', key: 'spec' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '效期', dataIndex: 'expireDate', key: 'expireDate' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]
const cVisible = ref(false)
const cSubmitting = ref(false)
const cDetailVisible = ref(false)
const cDetail = ref(null)
const cForm = reactive({ id: null, name: '', spec: '', qty: 0, expireDate: '' })
const cExpire = ref(null)

function openConsumable(r) {
  if (r) { Object.assign(cForm, r); cExpire.value = r.expireDate ? dayjs(r.expireDate) : null }
  else { Object.keys(cForm).forEach(k => cForm[k] = k === 'qty' ? 0 : null); cExpire.value = null }
  cVisible.value = true
}
function openConsumableDetail(r) {
  cDetail.value = { ...r, stock: r.qty }
  cDetailVisible.value = true
  // 加载详情（含关联流程：入库申请/使用申请）
  getConsumableDetail(r.id).then((res) => {
    const vo = res.data || res
    if (vo && vo.id) cDetail.value = vo
  }).catch(() => {})
}
async function submitConsumable() {
  if (!cForm.name) { message.warning('请填写名称'); return }
  cSubmitting.value = true
  cForm.expireDate = toStr(cExpire.value)
  await saveConsumable({ ...cForm }); message.success('已保存'); cVisible.value = false; loadConsumables()
  cSubmitting.value = false
}
async function loadConsumables() {
  cl.value = true
  try {
    const res = await getConsumables({ keyword: ck.value, status: cStatus.value, page: cp.current, size: cp.pageSize })
    const p = res.data || res; consumables.value = p.records || p.list || []; cp.total = p.total || consumables.value.length
  } finally { cl.value = false }
}
async function checkGate() {
  const res = await checkMaterialGate()
  const g = res.data || res
  if (g.pass) message.success('物资效期闸门通过')
  else message.warning(`有 ${g.blocked.length} 项标物临近/已过期`)
}

let pageObserver = null
onMounted(() => {
  loadInstDict()
  loadInstruments()
  loadMaterials()
  loadConsumables()
  loadHaz()
  loadHazCat()
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

// 切换标签页后重新测量（不同标签页工具栏/预警条高度不同）
watch(tab, () => { nextTick(syncTableHeight) })
// 设备台账切换至使用日历时加载占用数据
watch(instViewMode, (v) => { if (v === 'calendar') instApplyUnit(); else nextTick(syncTableHeight) })

onUnmounted(() => {
  window.removeEventListener('resize', syncTableHeight)
  if (pageObserver) pageObserver.disconnect()
})

// ===== 危化品（并入物资管理）=====
const hzLoading = ref(false)
const hzk = ref(''), hzStatus = ref('')
const hazRows = ref([])
const hzpg = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { hzpg.current = p; loadHaz() } })
const hazCols = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: 'CAS号', dataIndex: 'casNo', key: 'casNo' },
  { title: '类别', dataIndex: 'category', key: 'category' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '申请人', dataIndex: 'applyBy', key: 'applyBy' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

const hzVisible = ref(false)
const hzForm = reactive({ id: null, name: '', casNo: '', category: '', qty: '', unit: '' })
const hazCatOptions = ref([])
async function loadHazCat() {
  try {
    const res = await getDictItems('moni_hazardous_category')
    const list = res.data || res || []
    hazCatOptions.value = list.map(i => ({ label: i.itemText, value: i.itemValue }))
  } catch (e) { hazCatOptions.value = [] }
}
function openHaz(r) {
  if (r) Object.assign(hzForm, r)
  else Object.keys(hzForm).forEach(k => hzForm[k] = null)
  hzVisible.value = true
}

// 危化品详情（含关联流程：报废申请 ZCBFSQ）
const hzDetailVisible = ref(false)
const hzDetail = ref(null)
function openHazDetail(r) {
  hzDetail.value = r
  hzDetailVisible.value = true
  getHazardousDetail(r.id).then((res) => {
    const vo = res.data || res
    if (vo && vo.id) hzDetail.value = vo
  }).catch(() => {})
}
async function saveHaz() {
  await saveHazardous({ ...hzForm }); message.success('已保存'); hzVisible.value = false; loadHaz()
}

const hzApplyVisible = ref(false), hzApproveVisible = ref(false)
const hzCur = ref(null)
const hzApplyBy = ref(''), hzApplyReason = ref(''), hzTargetStatus = ref('已领用')
const hzApproveBy = ref(''), hzApproveOpinion = ref(''), hzApproveOk = ref(true)
function applyHaz(r) { hzCur.value = r; hzApplyVisible.value = true }
function approveHaz(r) { hzCur.value = r; hzApproveVisible.value = true }
async function doApplyHaz() {
  await applyHazardous(hzCur.value.id, { applyBy: hzApplyBy.value, applyReason: hzApplyReason.value, targetStatus: hzTargetStatus.value })
  message.success('已提交审批'); hzApplyVisible.value = false; loadHaz()
}
async function doApproveHaz() {
  await approveHazardous(hzCur.value.id, { approveBy: hzApproveBy.value, approveOpinion: hzApproveOpinion.value, approve: hzApproveOk.value })
  message.success('审批完成'); hzApproveVisible.value = false; loadHaz()
}

async function loadHaz() {
  hzLoading.value = true
  try {
    const res = await getHazardous({ keyword: hzk.value, status: hzStatus.value, page: hzpg.current, size: hzpg.pageSize })
    const p = res.data || res; hazRows.value = p.records || p.list || []; hzpg.total = p.total || hazRows.value.length
  } finally { hzLoading.value = false }
}
</script>

<style scoped>
.action-link { color: #1677ff; cursor: pointer; }
.action-link.danger { color: #ff4d4f; }
.danger-link { color: #ff4d4f; }
.code-link { color: #2563EB; cursor: pointer; font-weight: 600; }
.code-link:hover { text-decoration: underline; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.page-title { font-size: 16px; font-weight: 600; }
.page-wrap { height: 100%; min-height: calc(100vh - 84px); display: flex; flex-direction: column; overflow: hidden; padding: 16px; box-sizing: border-box; }
.page-wrap :deep(.ant-tabs) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.page-wrap :deep(.ant-tabs-nav) { background: #fff; margin-bottom: 0; padding: 0 8px; }
.page-wrap :deep(.ant-tabs-content-holder) { flex: 1; min-height: 0; }
.page-wrap :deep(.ant-tabs-content) { height: 100%; }
.page-wrap :deep(.ant-tabs-tabpane) { height: 100%; }
.page-wrap :deep(.ant-tabs-tabpane-active) { display: flex !important; flex-direction: column; }
.card-wrap { flex: 1; min-height: 0; display: flex; flex-direction: column; background: #fff; padding: 16px; border-radius: 8px; box-sizing: border-box; overflow: hidden; }
.page-wrap :deep(.ant-card) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.page-wrap :deep(.ant-card-body) { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 8px 0; }
/* tbl-box 撑满卡片剩余空间，由 JS 测量其高度设置 scroll.y */
.tbl-box { flex: 1; min-height: 320px; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-table-wrapper) { flex: 1; min-height: 0; height: 100%; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-spin-nested-loading) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-spin-container) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-table-pagination) { margin: 8px 0 16px !important; flex: 0 0 auto; }
.cell-link { color: #1677ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 设备使用日历（与车辆使用日历样式保持一致） */
.calendar-host { padding-top: 4px; flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.cal-toolbar { margin-bottom: 16px !important; flex: 0 0 auto; }
.cal-tip { color: #888; font-size: 12px; display: inline-flex; align-items: center; gap: 4px; }
.cal-dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin: 0 4px 0 8px; vertical-align: middle; }
.cal-dot-dispatch { background: #409eff; }
.cal-dot-maint { background: #eb2f96; }
.cal-loading { padding: 40px 0; text-align: center; }
.cal-grid { border: 1px solid #f0f0f0; border-radius: 6px; overflow: auto; flex: 1; min-height: 0; }
.cal-row { display: flex; border-bottom: 1px solid #f5f5f5; }
.cal-row:last-child { border-bottom: none; }
.cal-head { background: #fafafa; font-weight: 600; position: sticky; top: 0; z-index: 3; }
.cal-label {
  width: 140px; flex: 0 0 140px; padding: 6px 10px; border-right: 1px solid #f0f0f0;
  display: flex; flex-direction: column; justify-content: center;
  position: sticky; left: 0; z-index: 2; background: #fff;
}
.cal-head .cal-label { background: #fafafa; }
.cal-plate { font-weight: 600; }
.cal-model { font-size: 12px; color: #999; }
.cal-track { flex: 1 1 auto; display: flex; }
.cal-cell {
  flex: 1 1 0; min-width: 40px; border-right: 1px solid #f5f5f5; min-height: 40px;
  display: flex; align-items: center; justify-content: center; padding: 2px;
}
.cal-cell:last-child { border-right: none; }
.cal-cell-head { flex-direction: column; min-height: 44px; }
.cal-dow { font-size: 12px; color: #666; }
.cal-date { font-size: 13px; }
.cal-block {
  width: 100%; height: 28px; border-radius: 4px; color: #fff;
  font-size: 12px; display: flex; align-items: center; justify-content: center;
}
.cal-block-dispatch { background: #409eff; }
.cal-block-maint { background: #eb2f96; }
.cal-block-partial { opacity: 0.85; }
</style>
