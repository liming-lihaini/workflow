// Issue-028 监测数据驾驶舱 UI 自动化测试
const { chromium } = require('playwright')
const fs = require('fs')

const BASE = 'http://localhost:3000'
const SCREEN_DIR = 'test-screens'
if (!fs.existsSync(SCREEN_DIR)) fs.mkdirSync(SCREEN_DIR, { recursive: true })

async function main() {
  const browser = await chromium.launch()
  const page = await browser.newPage()
  const errors = []
  page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', (e) => errors.push('PAGEERROR: ' + e.message))

  // 1. 登录
  await page.goto(BASE + '/login', { waitUntil: 'networkidle' })
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 10000 })
  await page.fill('input[placeholder="请输入用户名"]', 'sys_admin')
  await page.fill('input[type="password"]', 'admin123')
  await page.screenshot({ path: SCREEN_DIR + '/issue028-login.png' })
  await page.locator('input[type="password"]').press('Enter')
  try {
    await page.waitForURL('**/dashboard', { timeout: 12000 })
  } catch (e) {
    await page.screenshot({ path: SCREEN_DIR + '/issue028-login-fail.png' })
    throw new Error('登录后未跳转到 /dashboard: ' + e.message)
  }
  console.log('LOGIN OK')

  // 2. 导航到监测驾驶舱（直接访问路由，避免菜单折叠遮挡）
  await page.goto(BASE + '/ems/dashboard', { waitUntil: 'networkidle' })
  await page.waitForTimeout(2500)
  console.log('URL after nav:', page.url())
  const bodyText = await page.locator('body').innerText()
  console.log('HAS_TITLE:', bodyText.includes('监测数据驾驶舱'))
  await page.screenshot({ path: SCREEN_DIR + '/issue028-nav.png', fullPage: true })
  if (!bodyText.includes('监测数据驾驶舱')) {
    await page.screenshot({ path: SCREEN_DIR + '/issue028-nav-fail.png', fullPage: true })
    throw new Error('驾驶舱页面未渲染标题')
  }
  console.log('NAV OK')

  // 3. 验证核心 KPI 卡片（委托总数 / 检测任务 / 报告总数 等）
  await page.waitForSelector('text=委托总数', { timeout: 10000 })
  await page.waitForSelector('text=检测任务', { timeout: 10000 })
  await page.waitForSelector('text=报告总数', { timeout: 10000 })
  await page.waitForSelector('text=累计超标项', { timeout: 10000 })
  console.log('KPI OK')

  // 4. 验证分布与趋势区块
  await page.waitForSelector('text=委托状态分布', { timeout: 10000 })
  await page.waitForSelector('text=检测结论分布', { timeout: 10000 })
  await page.waitForSelector('text=报告状态分布', { timeout: 10000 })
  await page.waitForSelector('text=近 6 个月报告 / 超标趋势', { timeout: 10000 })
  console.log('BLOCKS OK')

  // 5. 验证趋势表有 6 行（近 6 个月）
  const rows = await page.locator('.ant-card:has-text("近 6 个月") table tbody tr').count()
  if (rows < 6) throw new Error('趋势表行数不足: ' + rows)
  console.log('TREND ROWS =', rows)

  await page.screenshot({ path: SCREEN_DIR + '/issue028-dashboard.png', fullPage: true })

  if (errors.length) {
    console.log('CONSOLE ERRORS:', errors.slice(0, 10))
    throw new Error('存在前端控制台错误')
  }

  await browser.close()
  console.log('ISSUE-028 UI TEST PASS')
}

main().catch((e) => {
  console.error('TEST FAILED:', e.message)
  process.exit(1)
})
