import request from './request'

// ===== 系统字典（/api/v1/system/dict/*）=====
// 按 dictCode 取字典项（用于来源、点位类型等下拉）
export const getDictItems = (dictCode, params) =>
  request.get(`/system/dict/items/code/${dictCode}`, { params })

// ===== 基础数据（/api/v1/ems/base/*）=====
export const getCustomers = (params) => request.get('/ems/base/customers', { params })
export const createCustomer = (data) => request.post('/ems/base/customers', data)
export const updateCustomer = (id, data) => request.put(`/ems/base/customers/${id}`, data)
export const disableCustomer = (id) => request.post(`/ems/base/customers/${id}/disable`)
export const batchDeleteCustomers = (ids) => request.post('/ems/base/customers/batch-delete', ids)
export const importCustomers = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/ems/base/customers/import', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const getCustomerTemplate = () =>
  request.get('/ems/base/customers/template', { responseType: 'blob' })
// 客户详情：基本信息 + 检测委托清单
export const getCustomerDetail = (id) => request.get(`/ems/base/customers/${id}/detail`)

export const getEntrusts = (params) => request.get('/ems/base/entrusts', { params })
export const getEntrust = (id) => request.get(`/ems/base/entrusts/${id}`)
// 创建/更新委托：data = { entrust, points }
export const saveEntrust = (data) => request.post('/ems/base/entrusts', data)
export const submitEntrust = (id, submitBy) =>
  request.post(`/ems/base/entrusts/${id}/submit`, null, { params: { submitBy } })
export const techConfirmEntrust = (id, reviewerId, opinion) =>
  request.post(`/ems/base/entrusts/${id}/tech-confirm`, null, { params: { reviewerId, opinion } })
export const rejectEntrust = (id, reviewerId, opinion) =>
  request.post(`/ems/base/entrusts/${id}/reject`, null, { params: { reviewerId, opinion } })
export const batchDeleteEntrusts = (ids) =>
  request.post('/ems/base/entrusts/batch-delete', ids)
// 委托操作历史（详情页「操作记录」）
export const getEntrustHistory = (id) => request.get(`/ems/base/entrusts/${id}/history`)

// 采样任务 / 调度派单（ISSUE-023）
export const genSamplingOrders = (entrustId) =>
  request.post(`/ems/base/sampling-order/gen`, null, { params: { entrustId } })
// 按采集频率再次派单：同一委托追加生成一张待派单订单（要求委托已确认，BR-023-09）
export const redispatchSamplingOrder = (entrustId) =>
  request.post(`/ems/base/sampling-order/redispatch`, null, { params: { entrustId } })

// ===== 采样参数配置（TRD 5.1）=====
export const getSampleParamConfigs = (params) =>
  request.get('/ems/base/sample-param-config', { params })
export const getSampleParamConfig = (id) =>
  request.get(`/ems/base/sample-param-config/${id}`)
export const saveSampleParamConfig = (data) =>
  request.post('/ems/base/sample-param-config', data)
export const deleteSampleParamConfig = (id) =>
  request.delete(`/ems/base/sample-param-config/${id}`)
export const batchDeleteSampleParamConfig = (ids) =>
  request.post('/ems/base/sample-param-config/batch-delete', ids)
export const getSamplingOrders = (params) => request.get('/ems/base/sampling-orders', { params })
// 采样调度看板聚合（点位名称/派单计划区间/负责人），支持按订单号/负责人/状态筛选
export const getDispatchBoardList = (params) => request.get('/ems/base/sampling-orders/dispatch-list', { params })
// 删除采样任务（级联删除关联派单）
export const deleteSamplingOrder = (id) => request.delete(`/ems/base/sampling-orders/${id}`)
// 批量派单：同一派单信息派发到多个订单（参数均为 query）
export const batchDispatch = (params) => request.post('/ems/base/sampling-orders/batch-dispatch', null, { params })
// 派单参数均为后端 @RequestParam（query），不含 body
export const createDispatch = (params) => request.post('/ems/base/dispatch', null, { params })
export const checkDispatchConflict = (params) => request.get('/ems/base/dispatch/check', { params })
export const getDispatchDetail = (orderId) => request.get('/ems/base/dispatch', { params: { orderId } })
// 车辆使用日历（ISSUE-035）
export const getVehicleUsage = (params) => request.get('/ems/base/dispatch/vehicle-usage', { params })
// 设备使用日历
export const getInstrumentUsage = (params) => request.get('/ems/base/dispatch/instrument-usage', { params })
export const getAvailableVehicles = (params) => request.get('/ems/base/dispatch/available-vehicles', { params })

// 派单人员：从后台用户管理远程检索（支持分页+关键字）
export const searchUsers = (params) => request.get('/system/users/page', { params })

// 车辆台账（TRD 4.4）
export const listVehicles = (params) => request.get('/ems/base/vehicles', { params })
export const createVehicle = (data) => request.post('/ems/base/vehicles', data)
export const updateVehicle = (id, data) => request.put(`/ems/base/vehicles/${id}`, data)
export const deleteVehicle = (id) => request.delete(`/ems/base/vehicles/${id}`)
// 车辆维修保养（ISSUE-036）
export const createVehicleMaintenance = (vehicleId, data) => request.post(`/ems/base/vehicles/${vehicleId}/maintenances`, data)
export const listVehicleMaintenances = (vehicleId) => request.get(`/ems/base/vehicles/${vehicleId}/maintenances`)
export const deleteVehicleMaintenance = (mid) => request.delete(`/ems/base/vehicles/maintenances/${mid}`)
export const getVehicleDetail = (vehicleId) => request.get(`/ems/base/vehicles/${vehicleId}/detail`)

// 仪器设备全生命周期台账（TRD 5.5）
export const listInstruments = (params) => request.get('/ems/base/instruments', { params })
export const createInstrument = (data) => request.post('/ems/base/instruments', data)
export const updateInstrument = (id, data) => request.put(`/ems/base/instruments/${id}`, data)
export const deleteInstrument = (id) => request.delete(`/ems/base/instruments/${id}`)
export const calibrateInstrument = (id, params) =>
  request.post(`/ems/base/instruments/${id}/calibrate`, null, { params })
export const expiringInstruments = () => request.get('/ems/base/instruments/expiring')
export const getInstrumentDetail = (id) => request.get(`/ems/base/instruments/${id}/detail`)

export const getDepartments = () => request.get('/ems/base/departments')
export const createDepartment = (data) => request.post('/ems/base/departments', data)

export const upsertCfg = (data) => request.post('/ems/base/integration-cfg', data)
export const getCfgPlain = (key) => request.get(`/ems/base/integration-cfg/${key}/plain`)

// ===== 共享实体（/api/v1/ems/shared/*）=====
export const getFiles = (params) => request.get('/ems/shared/files', { params })
export const archiveFile = (data) => request.post('/ems/shared/files', data)

export const getAlerts = (params) => request.get('/ems/shared/alerts', { params })
export const pushAlert = (data) => request.post('/ems/shared/alerts', data)

export const getMessages = (params) => request.get('/ems/shared/messages', { params })
export const sendMessage = (data) => request.post('/ems/shared/messages', data)

// ===== 采样与样品管理（ISSUE-024，路径 /api/v1/ems/base/sampling/*）=====
// 采样记录
export const createSamplingRecord = (data) => request.post('/ems/base/sampling/records', data)
export const updateSamplingRecord = (id, data) => request.put(`/ems/base/sampling/records/${id}`, data)
export const completeSamplingRecord = (id) => request.post(`/ems/base/sampling/records/${id}/complete`)
export const getSamplingRecords = (params) => request.get('/ems/base/sampling/records', { params })
export const deleteSamplingRecord = (id) => request.delete(`/ems/base/sampling/records/${id}`)

// 样品
export const createSample = (data) => request.post('/ems/base/sampling/samples', data)
// 手动收集样品（收样工作台，无采样记录/小程序上报场景），创建即收样
export const manualCollectSample = (data) => request.post('/ems/base/sampling/samples/manual-collect', data)
// 收样工作台-样品采集：按状态（默认已派单）拉取采样派单列表
export const getCollectDispatchList = (params) =>
  request.get('/ems/base/sampling-orders/dispatch-list', { params })
// 收样工作台-样品采集：提交完整采集数据（关联派单/点位，含采样参数/固定剂/质控/留样/照片）
export const collectSample = (data) => request.post('/ems/base/sampling/samples/collect', data)
// 现场照片批量上传（返回相对路径数组）
export const uploadSamplePhoto = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/ems/base/sampling/samples/photo-upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const updateSample = (id, data) => request.put(`/ems/base/sampling/samples/${id}`, data)
export const receiveSample = (id, data) => request.post(`/ems/base/sampling/samples/${id}/receive`, data)
export const dispatchSample = (id, params) => request.post(`/ems/base/sampling/samples/${id}/dispatch`, null, { params })
export const getSamples = (params) => request.get('/ems/base/sampling/samples', { params })
export const getSampleDetail = (id) => request.get(`/ems/base/sampling/samples/${id}/detail`)
export const disposeSample = (id, data) => request.post(`/ems/base/sampling/samples/${id}/dispose`, data)
export const deleteSample = (id) => request.delete(`/ems/base/sampling/samples/${id}`)

// 质控样 / 照片 / 日志
export const bindSampleQc = (id, data) => request.post(`/ems/base/sampling/samples/${id}/qc`, data)
export const unbindSampleQc = (id) => request.delete(`/ems/base/sampling/qc/${id}`)
export const addSamplePhoto = (data) => request.post('/ems/base/sampling/photos', data)
export const getPhotos = (params) => request.get('/ems/base/sampling/photos', { params })
export const getSampleLogs = (id) => request.get(`/ems/base/sampling/samples/${id}/logs`)

// 留样库
export const retainSample = (id, params) => request.post(`/ems/base/sampling/samples/${id}/retain`, null, { params })
export const disposeRetain = (id, params) => request.post(`/ems/base/sampling/retains/${id}/dispose`, null, { params })
export const getRetains = (params) => request.get('/ems/base/sampling/retains', { params })
export const getExpiringRetains = (params) => request.get('/ems/base/sampling/retains/expiring', { params })
export const getRetainStats = () => request.get('/ems/base/sampling/retains/stats')
export const applyDispose = (id, startUser, formData) => request.post(`/ems/base/sampling/retains/${id}/dispose`, formData, { params: { startUser } })
export const deleteRetain = (id) => request.delete(`/ems/base/sampling/retains/${id}`)

// ===== 检测数据录入与复核（ISSUE-025，路径 /api/v1/ems/base/detection/*）=====
// 录入工作台
export const getPendingSamples = (params) => request.get('/ems/base/detection/pending-samples', { params })
export const createDetectionTask = (data) => request.post('/ems/base/detection/task', data)
export const getDetectionTasks = (params) => request.get('/ems/base/detection/tasks', { params })
export const getDetectionTaskStat = () => request.get('/ems/base/detection/task-stat')
export const getDetectionTaskDetail = (id) => request.get(`/ems/base/detection/task/${id}`)
export const saveDetectionResults = (id, data) => request.post(`/ems/base/detection/task/${id}/results`, data)
export const submitDetection = (id) => request.post(`/ems/base/detection/task/${id}/submit`)

// 复核工作台
export const getPendingReviews = (params) => request.get('/ems/base/detection/pending-review', { params })
export const approveDetection = (id, data) => request.post(`/ems/base/detection/task/${id}/approve`, data)
export const rejectDetection = (id, data) => request.post(`/ems/base/detection/task/${id}/reject`, data)

// ===== 质量控制（ISSUE-026，路径 /api/v1/ems/quality/*）=====
// 标准物质
export const saveMaterial = (data) => request.post('/ems/quality/materials', data)
export const getMaterials = (params) => request.get('/ems/quality/materials', { params })
export const getMaterialDetail = (id) => request.get(`/ems/quality/materials/${id}/detail`)
// 耗材
export const saveConsumable = (data) => request.post('/ems/quality/consumables', data)
export const getConsumables = (params) => request.get('/ems/quality/consumables', { params })
export const getConsumableDetail = (id) => request.get(`/ems/quality/consumables/${id}/detail`)
// 危化品台账
export const saveHazardous = (data) => request.post('/ems/quality/hazardous', data)
export const getHazardous = (params) => request.get('/ems/quality/hazardous', { params })
export const getHazardousDetail = (id) => request.get(`/ems/quality/hazardous/${id}/detail`)
export const applyHazardous = (id, data) => request.post(`/ems/quality/hazardous/${id}/apply`, data)
export const approveHazardous = (id, data) => request.post(`/ems/quality/hazardous/${id}/approve`, data)
// 质控计划
export const saveQcPlan = (data, params) => request.post('/ems/quality/plans', data, { params })
export const getQcPlans = (params) => request.get('/ems/quality/plans', { params })
export const getQcPlanDetail = (id) => request.get(`/ems/quality/plans/${id}`)
export const deleteQcPlan = (id, params) => request.delete(`/ems/quality/plans/${id}`, { params })
export const submitQcPlan = (id, data, params) => request.post(`/ems/quality/plans/${id}/submit`, data, { params })
export const approveQcPlan = (id, data, params) => request.post(`/ems/quality/plans/${id}/approve`, data, { params })
export const completeQcPlan = (id, params) => request.post(`/ems/quality/plans/${id}/complete`, null, { params })
// 监控活动
export const saveQcActivity = (data, params) => request.post('/ems/quality/activities', data, { params })
export const getQcActivities = (params) => request.get('/ems/quality/activities', { params })
export const getQcActivityTodos = (params) => request.get('/ems/quality/activities/todos', { params })
export const getQcActivityDetail = (id) => request.get(`/ems/quality/activities/${id}`)
export const deleteQcActivity = (id, params) => request.delete(`/ems/quality/activities/${id}`, { params })
// 质控处置历史（计划/监控活动）
export const getQcHistory = (params) => request.get('/ems/quality/history', { params })
// 闸门校验
export const checkMaterialGate = () => request.get('/ems/quality/gate/material')
export const checkInstrumentGate = () => request.get('/ems/quality/gate/instrument')

// ===== 报告生成与审核（ISSUE-027，路径 /api/v1/ems/report/*）=====
// 报告模板
export const getReportTemplates = () => request.get('/ems/report/templates')
export const createReportTemplate = (data) => request.post('/ems/report/template', data)
// 待生成报告的可选检测任务（已复核）
export const getReportPendingTasks = () => request.get('/ems/report/pending-tasks')
// 生成报告
export const generateReport = (data) => request.post('/ems/report/generate', data)
// 报告列表
export const getReports = (status) =>
  request.get('/ems/report/list', { params: status ? { status } : {} })
// 报告详情
export const getReportDetail = (id) => request.get(`/ems/report/${id}`)
// 审核通过（发布）
export const approveReport = (id, data) =>
  request.post(`/ems/report/${id}/approve`, data)
// 审核退回
export const rejectReport = (id, data) =>
  request.post(`/ems/report/${id}/reject`, data)

// ===== 监测数据驾驶舱与统计（ISSUE-028，路径 /api/v1/ems/dashboard/*）=====
export const getDashboardOverview = () =>
  request.get('/ems/dashboard/overview')
export const getEntrustCards = (params) =>
  request.get('/ems/dashboard/entrust-cards', { params })

// ===== 合同管理台账（PRD-02，路径 /api/v1/ems/contract/*）=====
// 台账列表（编号/名称/类型/状态/相对方/负责人/签订日期区间筛选）
export const getContracts = (params) => request.get('/ems/contract', { params })
// 台账统计卡片（应收应付/已收已付/逾期节点数）
export const getContractStatistics = () => request.get('/ems/contract/statistics')
// 新建/编辑合同：data = { contract, nodes, entrustIds }
export const saveContract = (data) => request.post('/ems/contract', data)
// 合同详情（节点核销进度 + 流水 + 关联委托 + 操作历史）
export const getContract = (id) => request.get(`/ems/contract/${id}`)
// 删除草稿合同
export const deleteContract = (id) => request.delete(`/ems/contract/${id}`)
// 状态流转
export const submitContract = (id) => request.post(`/ems/contract/${id}/submit`)
export const suspendContract = (id, reason) =>
  request.post(`/ems/contract/${id}/suspend`, null, { params: { reason } })
export const resumeContract = (id) => request.post(`/ems/contract/${id}/resume`)
export const cancelContract = (id) => request.post(`/ems/contract/${id}/cancel`)
// 收付款节点整体替换
export const saveContractNodes = (id, nodes) => request.post(`/ems/contract/${id}/nodes`, nodes)
// 收款/支付登记：data = { txnDate, amount, payMethod, txnNo, remark, allocations: [{nodeId, amount}] }
export const addContractTxn = (id, data) => request.post(`/ems/contract/${id}/txn`, data)
// 撤销收付款登记
export const deleteContractTxn = (txnId) => request.delete(`/ems/contract/txn/${txnId}`)
// 合同操作历史
export const getContractHistory = (id) => request.get(`/ems/contract/${id}/history`)

// ===== 基础设施底座（ISSUE-029，路径 /api/v1/*）=====
// 状态机：驱动迁移
export const fireState = (data) => request.post('/statemachine/fire', data)
// 状态机：查询可用迁移
export const getTransitions = (bizType, fromState) =>
  request.get('/statemachine/transitions', { params: { bizType, fromState } })
// 编号：生成唯一单号
export const nextSeq = (biz) => request.get('/seq/next', { params: { biz } })
// 公式：计算
export const calcFormula = (formula, params) =>
  request.post('/calc', { formula, params })
