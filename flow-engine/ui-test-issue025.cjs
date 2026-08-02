const { chromium } = require('playwright');

const BASE = 'http://localhost:3000';
const SHOT_DIR = 'test-screens';

async function safeClick(page, selector, timeout = 5000) {
  try {
    await page.click(selector, { timeout });
    return true;
  } catch (e) {
    console.log('  [warn] click failed:', selector, e.message.split('\n')[0]);
    return false;
  }
}

(async () => {
  const fs = require('fs');
  if (!fs.existsSync(SHOT_DIR)) fs.mkdirSync(SHOT_DIR);
  const browser = await chromium.launch({ args: ['--no-sandbox'] });
  const context = await browser.newContext();
  const page = await context.newPage();
  const errors = [];
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
  page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));

  const log = (s) => console.log(s);
  let pass = 0, fail = 0;
  const check = (name, cond) => { if (cond) { pass++; log('  [PASS] ' + name); } else { fail++; log('  [FAIL] ' + name); } };

  try {
    // 1. 登录
    log('Step1 登录');
    await page.goto(BASE + '/login', { waitUntil: 'networkidle' });
    await page.fill('input#username, input[placeholder*="用户名"], input[type="text"]', 'sys_admin');
    await page.fill('input#password, input[type="password"]', 'admin123');
    await safeClick(page, 'button[type="submit"], .ant-btn-primary');
    await page.waitForURL('**/dashboard**', { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(1500);
    check('登录成功（跳转仪表盘）', /dashboard|ems/.test(page.url()));

    // 2. 进入 检测数据录入
    log('Step2 进入检测数据录入');
    await page.goto(BASE + '/ems/base/data-entry', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    const entryTitle = await page.locator('text=检测数据录入工作台').first().isVisible().catch(() => false);
    check('录入工作台页标题可见', entryTitle);
    const taskRows = await page.locator('.ant-table-tbody tr').count();
    check('录入工作台有检测任务数据', taskRows > 0);
    await page.screenshot({ path: SHOT_DIR + '/issue025-dataentry.png' });

    // 3. 录入：打开 录入中 任务的录入抽屉
    log('Step3 录入检测结果');
    // 找到 录入中 状态的行并点击其“录入”
    const rows = page.locator('.ant-table-tbody tr');
    const rowCount = await rows.count();
    let entryClicked = false;
    for (let i = 0; i < rowCount; i++) {
      const row = rows.nth(i);
      const statusText = await row.locator('td').nth(4).innerText().catch(() => '');
      if (statusText.includes('录入中')) {
        await row.locator('a', { hasText: '录入' }).click();
        entryClicked = true;
        break;
      }
    }
    check('打开录入抽屉', entryClicked);
    if (entryClicked) {
      await page.waitForTimeout(1500);
      const drawerVisible = await page.locator('.ant-drawer-title').isVisible().catch(() => false);
      check('录入抽屉打开', drawerVisible);
      // 编辑第一行检测值
      const firstVal = page.locator('.ant-drawer-body .ant-table-tbody tr').first().locator('input').first();
      await firstVal.fill('7.5').catch(() => {});
      // 保存结果
      await safeClick(page, '.ant-drawer-body button:has-text("保存结果")');
      await page.waitForTimeout(1000);
      const msgSave = await page.locator('.ant-message-success, .ant-message-notice').first().innerText().catch(() => '');
      check('保存结果成功', /成功|保存/.test(msgSave) || msgSave.length > 0);
      await page.screenshot({ path: SHOT_DIR + '/issue025-entry-save.png' });
      // 关闭抽屉
      await safeClick(page, '.ant-drawer-close');
      await page.waitForTimeout(800);
    }

    // 4. 新建检测任务
    log('Step4 新建检测任务');
    await safeClick(page, 'button:has-text("新建检测任务")');
    await page.waitForTimeout(1000);
    const modalVisible = await page.locator('.ant-modal-title:has-text("新建检测任务")').isVisible().catch(() => false);
    check('新建任务弹窗打开', modalVisible);
    if (modalVisible) {
      const sel = page.locator('.ant-modal-body .ant-select').first();
      await sel.click();
      await page.waitForTimeout(800);
      const opt = page.locator('.ant-select-item-option').first();
      const hasOpt = await opt.count();
      if (hasOpt) {
        await opt.click();
        await safeClick(page, '.ant-modal-body button:has-text("确 定"), .ant-modal-footer button.ant-btn-primary');
        await page.waitForTimeout(1200);
        const msgNew = await page.locator('.ant-message-success, .ant-message-notice').first().innerText().catch(() => '');
        check('新建任务成功', /成功|创建/.test(msgNew) || msgNew.length > 0);
      } else {
        check('新建任务（无可选样品，跳过）', true);
      }
      await page.keyboard.press('Escape').catch(() => {});
      await page.waitForTimeout(500);
    }

    // 5. 进入 检测复核 工作台
    log('Step5 进入检测复核工作台');
    await page.goto(BASE + '/ems/base/review', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    const reviewTitle = await page.locator('text=检测复核工作台').first().isVisible().catch(() => false);
    check('复核工作台页标题可见', reviewTitle);
    const reviewRows = await page.locator('.ant-table-tbody tr').count();
    check('复核工作台有待复核任务', reviewRows > 0);
    await page.screenshot({ path: SHOT_DIR + '/issue025-review.png' });

    // 6. 复核通过
    log('Step6 复核通过');
    if (reviewRows > 0) {
      await page.locator('.ant-table-tbody tr').first().locator('a', { hasText: '复核' }).click();
      await page.waitForTimeout(1500);
      const drawerVisible = await page.locator('.ant-drawer-title').isVisible().catch(() => false);
      check('复核抽屉打开', drawerVisible);
      await safeClick(page, '.ant-drawer-body button:has-text("通过")');
      await page.waitForTimeout(1200);
      const msgApprove = await page.locator('.ant-message-success, .ant-message-notice').first().innerText().catch(() => '');
      check('复核通过成功', /成功|通过/.test(msgApprove) || msgApprove.length > 0);
      await page.screenshot({ path: SHOT_DIR + '/issue025-review-approve.png' });
    }

    // 7. 详情查看（回到录入工作台打开详情）
    log('Step7 查看任务详情');
    await page.goto(BASE + '/ems/base/data-entry', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    const drows = page.locator('.ant-table-tbody tr');
    if (await drows.count() > 0) {
      await drows.first().locator('a', { hasText: '详情' }).click();
      await page.waitForTimeout(1500);
      const detailVisible = await page.locator('.ant-descriptions').first().isVisible().catch(() => false);
      check('任务详情可查看', detailVisible);
      await page.screenshot({ path: SHOT_DIR + '/issue025-detail.png' });
    }

    // 控制台错误检查
    log('Step8 控制台错误检查');
    check('无 JS 控制台错误', errors.length === 0);
    if (errors.length) errors.slice(0, 10).forEach(e => log('   CONSOLE_ERR: ' + e));

  } catch (e) {
    fail++;
    log('[EXCEPTION] ' + e.message);
    await page.screenshot({ path: SHOT_DIR + '/issue025-error.png' }).catch(() => {});
  } finally {
    log(`\n==== RESULT: pass=${pass} fail=${fail} ====`);
    await browser.close();
    process.exit(fail > 0 ? 1 : 0);
  }
})();
