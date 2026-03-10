/**
 * Tenant Types
 * 租戶類型定義
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

/**
 * 租戶信息
 */
export interface TenantInfo {
  /** 租戶 ID */
  tenantId: number | null;
  /** 時區 */
  timezone: string;
  /** 租戶代碼 */
  tenantCode: string;
}

/**
 * 租戶狀態
 */
export interface TenantState extends TenantInfo {
  // 可擴展其他租戶相關狀態
}
