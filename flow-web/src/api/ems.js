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

// 采样订单 / 调度派单（ISSUE-023）
export const genSamplingOrders = (entrustId) =>
  request.post(`/ems/base/sampling-order/gen`, null, { params: { entrustId } })
export const getSamplingOrders = (params) => request.get('/ems/base/sampling-orders', { params })
// 派单参数均为后端 @RequestParam（query），不含 body
export const createDispatch = (params) => request.post('/ems/base/dispatch', null, { params })
export const checkDispatchConflict = (params) => request.get('/ems/base/dispatch/check', { params })
export const getDispatchDetail = (orderId) => request.get('/ems/base/dispatch', { params: { orderId } })

// 派单人员：从后台用户管理远程检索（支持分页+关键字）
export const searchUsers = (params) => request.get('/system/users/page', { params })

// 车辆台账（TRD 4.4）
export const listVehicles = (params) => request.get('/ems/base/vehicles', { params })
export const createVehicle = (data) => request.post('/ems/base/vehicles', data)
export const updateVehicle = (id, data) => request.put(`/ems/base/vehicles/${id}`, data)
export const deleteVehicle = (id) => request.delete(`/ems/base/vehicles/${id}`)

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
export const updateSample = (id, data) => request.put(`/ems/base/sampling/samples/${id}`, data)
export const receiveSample = (id, params) => request.post(`/ems/base/sampling/samples/${id}/receive`, null, { params })
export const dispatchSample = (id, params) => request.post(`/ems/base/sampling/samples/${id}/dispatch`, null, { params })
export const getSamples = (params) => request.get('/ems/base/sampling/samples', { params })
export const getSampleDetail = (id) => request.get(`/ems/base/sampling/samples/${id}/detail`)
export const deleteSample = (id) => request.delete(`/ems/base/sampling/samples/${id}`)

// 质控样 / 照片 / 日志
export const bindSampleQc = (id, data) => request.post(`/ems/base/sampling/samples/${id}/qc`, data)
export const unbindSampleQc = (id) => request.delete(`/ems/base/sampling/qc/${id}`)
export const addSamplePhoto = (data) => request.post('/ems/base/sampling/photos', data)
export const getPhotos = (params) => request.get('/ems/base/sampling/photos', { params })
export const getSampleLogs = (id) => request.get(`/ems/base/sampling/samples/${id}/logs`)

// 收样工作台
export const getReceiveWorkbench = (params) => request.get('/ems/base/sampling/workbench', { params })

// 留样库
export const retainSample = (id, params) => request.post(`/ems/base/sampling/samples/${id}/retain`, null, { params })
export const disposeRetain = (id, params) => request.post(`/ems/base/sampling/retains/${id}/dispose`, null, { params })
export const getRetains = (params) => request.get('/ems/base/sampling/retains', { params })
export const getExpiringRetains = (params) => request.get('/ems/base/sampling/retains/expiring', { params })

// ===== 检测数据录入与复核（ISSUE-025，路径 /api/v1/ems/base/detection/*）=====
// 录入工作台
export const getPendingSamples = (params) => request.get('/ems/base/detection/pending-samples', { params })
export const createDetectionTask = (data) => request.post('/ems/base/detection/task', data)
export const getDetectionTasks = (params) => request.get('/ems/base/detection/tasks', { params })
export const getDetectionTaskDetail = (id) => request.get(`/ems/base/detection/task/${id}`)
export const saveDetectionResults = (id, data) => request.post(`/ems/base/detection/task/${id}/results`, data)
export const submitDetection = (id) => request.post(`/ems/base/detection/task/${id}/submit`)

// 复核工作台
export const getPendingReviews = (params) => request.get('/ems/base/detection/pending-review', { params })
export const approveDetection = (id, data) => request.post(`/ems/base/detection/task/${id}/approve`, data)
export const rejectDetection = (id, data) => request.post(`/ems/base/detection/task/${id}/reject`, data)
