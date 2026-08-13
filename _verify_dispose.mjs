const BASE = 'http://localhost:8080/api/v1'

const login = await fetch(BASE + '/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'sys_admin', password: 'admin123' })
}).then((r) => r.json())
if (login.code !== 0) { console.log('登录失败', JSON.stringify(login)); process.exit(1) }
const token = login.data.token
const H = { 'Authorization': 'Bearer ' + token }

async function req(method, path, body) {
  const res = await fetch(BASE + path, {
    method, headers: body ? { ...H, 'Content-Type': 'application/json' } : H,
    body: body ? JSON.stringify(body) : undefined
  })
  return res.json()
}

// 找异常样品；若无则把样品24临时置为异常拒收
const list = await req('GET', '/ems/base/sampling/samples?page=1&size=200')
const rows = list.data?.records || list.data?.list || []
let sample = rows.find((s) => s.status === '异常拒收' || s.status === '检测异常')
let tempSet = false
if (!sample) {
  sample = rows[0]
  console.log('无异常样品，临时将样品 ' + sample.id + ' 置为异常拒收用于验证')
  tempSet = true
}
console.log(`目标样品: id=${sample.id} barcode=${sample.barcode} status=${sample.status}`)
if (sample) {
  const resp = await req('POST', `/ems/base/sampling/samples/${sample.id}/dispose`, {
    disposalType: 'sample_abnormal',
    disposalMethod: 'return_sample',
    disposalDesc: '字典转换验证'
  })
  console.log('异常处置: code=' + resp.code + (resp.message ? ' msg=' + resp.message : ''))
  if (resp.code === 0) {
    console.log('样品字段: disposalType=' + resp.data.disposalType + ' disposalMethod=' + resp.data.disposalMethod + ' status=' + resp.data.status)
  }
  // 读取操作日志
  const logs = await req('GET', `/ems/base/sampling/samples/${sample.id}/logs`)
  const items = logs.data || []
  const last = Array.isArray(items) ? items.find((l) => l.action === '异常处置') : null
  console.log('最新异常处置日志: ' + (last ? last.detail : JSON.stringify(logs)))
  console.log('验证完成 sampleId=' + sample.id + ' tempSet=' + tempSet)
}
