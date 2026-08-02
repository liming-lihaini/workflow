// Issue-029 基础设施底座 UI 自动化测试（规则引擎配置后台）
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
  await page.fill('input[placeholder="请输入用户名"]', 'sys_admin')
  await page.fill('input[type="password"]', 'admin123')
  await page.locator('input[type="password"]').press('Enter')
  await page.waitForURL('**/dashboard', { timeout: 12000 })
  console.log('LOGIN OK')

  // 2. 直接访问规则配置后台
  await page.goto(BASE + '/ems/base/rule-admin', { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)
  await page.waitForSelector('.ant-page-header:has-text("规则引擎配置")', { timeout: 10000 })
  console.log('PAGE OK')

  // 3. 验证规则列表渲染（应有内置规则：派单资质闸门等）
  await page.waitForSelector('text=派单资质闸门', { timeout: 10000 })
  await page.waitForSelector('text=超标判定', { timeout: 10000 })
  console.log('RULE LIST OK')

  // 4. 规则调试：选 dispatch_gate + 上下文，点击求值
  await page.locator('.rule-admin .ant-select').first().click()
  await page.locator('.ant-select-item-option:has-text("dispatch_gate")').click()
  await page.fill('input[placeholder=\'{"staffQualified":true}\']', '{"staffQualified":true,"instAvailable":true}')
  await page.screenshot({ path: SCREEN_DIR + '/issue029-debug-before.png' })
  // 滚动到规则调试区（求值按钮可能在视口外）
  await page.locator('text=规则调试').scrollIntoViewIfNeeded()
  await page.waitForTimeout(500)
  // 调试区内点击"求值"按钮
  await page.locator('button', { hasText: '求' }).last().click({ force: true })
  await page.waitForTimeout(1500)
  await page.screenshot({ path: SCREEN_DIR + '/issue029-debug-after.png' })
  const hasResult = await page.locator('.rule-admin:has-text("求值结果：true")').count()
  if (hasResult < 1) {
    const alertTxt = await page.locator('.rule-admin .ant-alert').first().innerText().catch(() => 'NO ALERT')
    throw new Error('求值结果未出现 true，alert=' + alertTxt)
  }
  console.log('DEBUG OK (true)')

  await page.screenshot({ path: SCREEN_DIR + '/issue029-rule-admin.png', fullPage: true })

  if (errors.length) {
    console.log('CONSOLE ERRORS:', errors.slice(0, 10))
    throw new Error('存在前端控制台错误')
  }

  await browser.close()
  console.log('ISSUE-029 UI TEST PASS')
}

main().catch((e) => {
  console.error('TEST FAILED:', e.message)
  process.exit(1)
})
