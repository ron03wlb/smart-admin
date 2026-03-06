/**
 * 測試 Fixtures 統一導出
 *
 * 方便統一導入所有測試數據生成器
 *
 * @example
 * ```typescript
 * // ✅ 統一從 fixtures 導入
 * import {
 *   createMockPermissions,
 *   createMockMenuTree,
 *   createMockLoginResult,
 * } from '@/test/fixtures';
 *
 * // ❌ 不需要單獨導入
 * // import { createMockPermissions } from '@/test/fixtures/user.fixtures';
 * ```
 *
 * @author Claude AI Assistant
 * @since 2026-03-06
 */

export {
  createMockPermissions,
  createMockMenuTree,
  createMockLoginResult,
} from './user.fixtures';

// 未來可擴展：
// export * from './menu.fixtures';
// export * from './dashboard.fixtures';
// export * from './player.fixtures';
// export * from './wallet.fixtures';
