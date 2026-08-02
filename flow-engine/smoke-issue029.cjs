// issue-029 后端接口冒烟测试（复用现有 ISSUE-014 操作日志）
const BASE = 'http://localhost:8080/api/v1'

async function login() {
  const r = await fetch(BASE + '/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'sys_admin', password: 'admin123' })
  })
  const j = await r.json()
  if (!j.data || !j.data.token) throw new Error('login failed: ' + JSON.stringify(j))
  return j.data.token
}

async function call(token, method, path, body) {
  const opt = { method, headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' } }
  if (body) opt.body = JSON.stringify(body)
  const r = await fetch(BASE + path, opt)
  const j = await r.json().catch(() => ({}))
  return { status: r.status, code: j.code, data: j.data, msg: j.message }
}

;(async () => {
  const token = await login()
  console.log('LOGIN OK')

  // 1. 状态机 fire（合法迁移）
  let r = await call(token, 'POST', '/statemachine/fire',
    { bizType: 'entrust', bizId: '1001', event: 'submit', fromState: '草稿', operator: 'sys_admin' })
  console.log('FIRE submit:', r.status, r.code, JSON.stringify(r.data))
  if (!r.data || !r.data.allowed) throw new Error('fire submit 应成功')

  // 2. 非法迁移（应 code=409）
  r = await call(token, 'POST', '/statemachine/fire',
    { bizType: 'entrust', bizId: '1001', event: 'hack', fromState: '草稿' })
  console.log('FIRE illegal:', r.status, r.code)
  if (r.code !== 409) throw new Error('非法迁移应 code=409')

  // 3. 规则 eval 通过/拒绝
  r = await call(token, 'POST', '/rule/eval',
    { ruleKey: 'dispatch_gate', context: { staffQualified: true, instAvailable: true } })
  if (r.data.result !== true) throw new Error('dispatch_gate 应 true')
  console.log('RULE ok:', JSON.stringify(r.data))
  r = await call(token, 'POST', '/rule/eval',
    { ruleKey: 'dispatch_gate', context: { staffQualified: false, instAvailable: true } })
  if (r.data.result !== false) throw new Error('dispatch_gate 应 false')
  console.log('RULE fail:', JSON.stringify(r.data))

  // 4. 编号 next
  r = await call(token, 'GET', '/seq/next?biz=report')
  console.log('SEQ:', JSON.stringify(r.data))
  if (!r.data.no) throw new Error('编号应返回 no')

  // 5. 公式计算
  r = await call(token, 'POST', '/calc', { formula: '#a + #b * 2', params: { a: 10, b: 5 } })
  console.log('CALC:', JSON.stringify(r.data))
  if (Number(r.data.result) !== 20) throw new Error('calc 应为 20')

  // 6. 规则列表
  r = await call(token, 'GET', '/rule')
  console.log('RULE LIST count:', r.data ? r.data.length : 0)
  if (!r.data || r.data.length < 4) throw new Error('规则数应 >=4')

  // 7. 验证操作审计已写入现有 sys_operation_log（复用 ISSUE-014，不重复建表）
  r = await call(token, 'GET', '/system/logs/operation?module=' + encodeURIComponent('基础设施底座-状态机'))
  const logs = (r.data && r.data.list) || []
  console.log('OPLOG count for 状态机 module:', logs.length)
  if (logs.length < 1) throw new Error('底座状态机操作未记录到 sys_operation_log')
  console.log('SAMPLE OPLOG:', JSON.stringify(logs[0]))

  console.log('ISSUE-029 BACKEND SMOKE PASS (复用现有日志)')
})().catch((e) => { console.error('SMOKE FAIL:', e.message); process.exit(1) })
