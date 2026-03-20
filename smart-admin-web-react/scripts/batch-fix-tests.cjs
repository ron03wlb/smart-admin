#!/usr/bin/env node
/**
 * 批量修復測試文件腳本
 * Batch Fix Test Files Script
 *
 * @description 自動修復常見的測試問題:
 *   1. Mock Store 配置不完整
 *   2. Ant Design 棄用 API
 *   3. 統一使用 test-utils
 *
 * @usage node scripts/batch-fix-tests.js [options]
 * @author SmartAdmin React Team
 * @date 2026-03-20
 */

const fs = require('fs');
const path = require('path');

// ANSI 顏色碼
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
};

const log = {
  info: (msg) => console.log(`${colors.blue}[INFO]${colors.reset} ${msg}`),
  success: (msg) => console.log(`${colors.green}[SUCCESS]${colors.reset} ${msg}`),
  warning: (msg) => console.log(`${colors.yellow}[WARNING]${colors.reset} ${msg}`),
  error: (msg) => console.log(`${colors.red}[ERROR]${colors.reset} ${msg}`),
};

/**
 * 遞歸查找測試文件
 */
function findTestFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);

  files.forEach((file) => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      if (!file.startsWith('.') && file !== 'node_modules') {
        findTestFiles(filePath, fileList);
      }
    } else if (file.endsWith('.test.ts') || file.endsWith('.test.tsx')) {
      fileList.push(filePath);
    }
  });

  return fileList;
}

// 統計計數器
const stats = {
  totalFiles: 0,
  modifiedFiles: 0,
  skippedFiles: 0,
  errors: 0,
  fixes: {
    mockStore: 0,
    antdApi: 0,
    testUtils: 0,
  },
};

/**
 * Fix 1: 更新 Mock Store 配置
 */
function fixMockStore(content, filePath) {
  let modified = false;
  let newContent = content;

  // 檢查是否已經使用 test-utils
  if (content.includes('createTestStore') || content.includes('test-utils')) {
    log.warning(`${path.basename(filePath)}: Already using test-utils, skipping Mock Store fix`);
    return { content, modified: false };
  }

  // 模式 1: 查找 preloadedState 中缺少 pointsList 的 user state
  const userStateRegex = /preloadedState:\s*{[\s\S]*?user:\s*{[\s\S]*?privilegeList:\s*\[([\s\S]*?)\]/g;

  if (userStateRegex.test(content)) {
    // 檢查是否缺少 pointsList
    if (!content.includes('pointsList:')) {
      newContent = newContent.replace(
        /(privilegeList:\s*\[([\s\S]*?)\],)/,
        `menuTreeList: [],
        pointsList: $2.map((perm, index) => ({ webPerms: perm, menuId: index + 1 })),
        $1`
      );
      modified = true;
      stats.fixes.mockStore++;
    }

    // 檢查是否缺少 administratorFlag
    if (!content.includes('administratorFlag:')) {
      newContent = newContent.replace(
        /(roleList:\s*\[[\s\S]*?\],)/,
        `$1
        administratorFlag: false,`
      );
      modified = true;
      stats.fixes.mockStore++;
    }
  }

  // 模式 2: 修復 dict state
  if (content.includes('dict:') && !content.includes('dictMap:')) {
    newContent = newContent.replace(
      /(dict:\s*{\s*dictData:\s*{[\s\S]*?},)/,
      `$1
        dictMap: {},`
    );

    if (!content.includes('lastFetched:')) {
      newContent = newContent.replace(/(dict:\s*{[\s\S]*?error:\s*null,)/, `$1
        lastFetched: null,`);
    }

    modified = true;
    stats.fixes.mockStore++;
  }

  return { content: newContent, modified };
}

/**
 * Fix 2: 更新 Ant Design 棄用 API
 */
function fixAntdApis(content) {
  let modified = false;
  let newContent = content;

  // Fix 2.1: Modal destroyOnClose → destroyOnHidden
  if (content.includes('destroyOnClose')) {
    log.info('Fixing Modal.destroyOnClose → destroyOnHidden');
    newContent = newContent.replace(/destroyOnClose/g, 'destroyOnHidden');
    modified = true;
    stats.fixes.antdApi++;
  }

  // Fix 2.2: Card bordered → variant
  const cardBorderedRegex = /(<Card[^>]*)\sbordered={false}([^>]*>)/g;
  if (cardBorderedRegex.test(content)) {
    log.info('Fixing Card bordered={false} → variant="borderless"');
    newContent = newContent.replace(cardBorderedRegex, '$1 variant="borderless"$2');
    modified = true;
    stats.fixes.antdApi++;
  }

  // Fix 2.3: Drawer bodyStyle → styles.body
  const drawerBodyStyleRegex = /bodyStyle={{([^}]+)}}/g;
  if (drawerBodyStyleRegex.test(content)) {
    log.info('Fixing Drawer bodyStyle → styles.body');
    newContent = newContent.replace(drawerBodyStyleRegex, 'styles={{ body: {$1} }}');
    modified = true;
    stats.fixes.antdApi++;
  }

  // Fix 2.4: Tabs.TabPane → items (僅在測試文件中)
  if (content.includes('Tabs.TabPane')) {
    log.warning('Found Tabs.TabPane - manual fix required for items API');
    // 這個需要手動修復,因為轉換邏輯複雜
  }

  return { content: newContent, modified };
}

/**
 * Fix 3: 添加 test-utils 導入 (如果需要)
 */
function addTestUtilsImport(content) {
  if (content.includes('createTestStore') && !content.includes('test-utils')) {
    // 在 vitest 導入後添加 test-utils 導入
    const newContent = content.replace(
      /(import.*from ['"]vitest['"];)/,
      `$1
import { createTestStore, renderWithProviders, PERMISSIONS } from '@/test/test-utils';`
    );
    return { content: newContent, modified: true };
  }
  return { content, modified: false };
}

/**
 * 處理單個測試文件
 */
function processTestFile(filePath) {
  try {
    log.info(`Processing: ${path.relative(process.cwd(), filePath)}`);

    let content = fs.readFileSync(filePath, 'utf8');
    let fileModified = false;

    // Apply fixes in sequence
    const fix1 = fixMockStore(content, filePath);
    if (fix1.modified) {
      content = fix1.content;
      fileModified = true;
    }

    const fix2 = fixAntdApis(content);
    if (fix2.modified) {
      content = fix2.content;
      fileModified = true;
    }

    const fix3 = addTestUtilsImport(content);
    if (fix3.modified) {
      content = fix3.content;
      fileModified = true;
    }

    // Write back if modified
    if (fileModified) {
      fs.writeFileSync(filePath, content, 'utf8');
      stats.modifiedFiles++;
      log.success(`Modified: ${path.basename(filePath)}`);
    } else {
      stats.skippedFiles++;
      log.info(`Skipped (no changes): ${path.basename(filePath)}`);
    }
  } catch (error) {
    stats.errors++;
    log.error(`Error processing ${filePath}: ${error.message}`);
  }
}

/**
 * 主函數
 */
function main() {
  log.info('=== Batch Test Fixer ===');
  log.info('Starting batch fix for test files...\n');

  // Find all test files
  const srcDir = path.join(__dirname, '..', 'src');
  const testFiles = findTestFiles(srcDir);

  stats.totalFiles = testFiles.length;
  log.info(`Found ${stats.totalFiles} test files\n`);

  // Process each file
  testFiles.forEach(processTestFile);

  // Print summary
  console.log('\n' + '='.repeat(50));
  log.success('Batch fix completed!');
  console.log('='.repeat(50));
  console.log(`Total files:     ${stats.totalFiles}`);
  console.log(`Modified files:  ${colors.green}${stats.modifiedFiles}${colors.reset}`);
  console.log(`Skipped files:   ${stats.skippedFiles}`);
  console.log(`Errors:          ${stats.errors > 0 ? colors.red : colors.green}${stats.errors}${colors.reset}`);
  console.log('\nFixes applied:');
  console.log(`  - Mock Store:  ${stats.fixes.mockStore}`);
  console.log(`  - Ant Design:  ${stats.fixes.antdApi}`);
  console.log(`  - Test Utils:  ${stats.fixes.testUtils}`);
  console.log('='.repeat(50));

  if (stats.errors > 0) {
    log.warning('\nSome files had errors. Please review the logs above.');
    process.exit(1);
  }
}

// Run
main();
