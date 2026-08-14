import request from './request'

// ===== 高层领导数据驾驶舱（PRD-03，路径 /api/v1/ems/dashboard/executive/*）=====
// request 拦截器返回 { code, data, message }，此处统一解包为 data。

const unwrap = (p) => p.then((res) => res.data)

// C1 顶部 KPI 汇总
export const getExecutiveKpi = () => unwrap(request.get('/ems/dashboard/executive/kpi'))

// C2 合同金额月度趋势（签约额 + 收款额，万元）
export const getExecutiveMonthlyTrend = () => unwrap(request.get('/ems/dashboard/executive/contract/monthly-trend'))

// C3 合同状态分布
export const getExecutiveStatusDist = () => unwrap(request.get('/ems/dashboard/executive/contract/status-dist'))

// C4 客户合同金额 TOP（limit 默认 10）
export const getExecutiveTopCustomers = (limit = 10) =>
  unwrap(request.get('/ems/dashboard/executive/contract/top-customers', { params: { limit } }))

// C5 业务全链路流转漏斗
export const getExecutiveFunnel = () => unwrap(request.get('/ems/dashboard/executive/funnel'))

// C6 检测结果明细（分页）
export const getExecutiveDetectionResults = (page = 1, size = 8) =>
  unwrap(request.get('/ems/dashboard/executive/detection/results', { params: { page, size } }))

// C7 系统预警列表（status：未处理 / 全部）
export const getExecutiveAlerts = (status) =>
  unwrap(request.get('/ems/dashboard/executive/alerts', { params: status ? { status } : {} }))

// C8 质控合格率（仪表盘）
export const getExecutiveQcRate = () => unwrap(request.get('/ems/dashboard/executive/quality/qc-rate'))

// C9 仪器设备状态分布
export const getExecutiveInstrumentStatus = () => unwrap(request.get('/ems/dashboard/executive/instrument/status'))

// C10 合同到期预警（剩余 days 天内，按到期升序）
export const getExecutiveContractExpiring = (days = 180) =>
  unwrap(request.get('/ems/dashboard/executive/contract/expiring', { params: { days } }))

// C11 实时动态滚动条
export const getExecutiveTicker = () => unwrap(request.get('/ems/dashboard/executive/ticker'))
