/**
 * Bank Module Types
 * 銀行信息模塊類型定義
 *
 * 參考：後端 BankVO.java, BankQueryForm.java, BankCreateForm.java, BankUpdateForm.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

/**
 * Bank VO
 * 銀行信息視圖對象
 */
export interface BankVO {
  bankId: number; // 銀行信息ID（主鍵）
  bankName: string; // 開戶銀行
  accountName: string; // 賬戶名稱
  accountNumber: string; // 賬號
  remark?: string; // 備註
  businessFlag: boolean; // 是否對公（true=對公, false=對私）
  enterpriseId: number; // 企業ID
  enterpriseName: string; // 企業名稱
  disabledFlag: boolean; // 禁用狀態（true=禁用, false=啟用）
  createUserId: number; // 創建人ID
  createUserName: string; // 創建人名稱
  createTime: string; // 創建時間
  updateTime: string; // 更新時間
}

/**
 * Bank Query Form
 * 銀行信息查詢表單
 */
export interface BankQueryForm {
  pageNum: number; // 頁碼
  pageSize: number; // 每頁條數
  enterpriseId?: number; // 企業ID過濾
  keywords?: string; // 關鍵字（搜索銀行名、賬戶名、賬號）
  startTime?: string; // 開始時間（創建時間範圍）
  endTime?: string; // 結束時間（創建時間範圍）
  disabledFlag?: boolean; // 禁用狀態過濾
  deletedFlag?: boolean; // 刪除標記過濾（true=已刪除, false=未刪除, null=全部）
}

/**
 * Bank Create Form
 * 銀行信息新建表單
 */
export interface BankCreateForm {
  bankName: string; // 開戶銀行（必填，最大200字符）
  accountName: string; // 賬戶名稱（必填，最大200字符）
  accountNumber: string; // 賬號（必填，最大200字符）
  remark?: string; // 備註（可選，最大500字符）
  businessFlag: boolean; // 是否對公（必填）
  enterpriseId: number; // 企業ID（必填）
  disabledFlag: boolean; // 禁用狀態（必填）
}

/**
 * Bank Update Form
 * 銀行信息更新表單
 */
export interface BankUpdateForm extends BankCreateForm {
  bankId: number; // 銀行信息ID
}

/**
 * Bank Form Data (for Modal)
 * 銀行信息表單數據（用於 Modal）
 */
export interface BankFormData {
  bankId?: number; // 銀行信息ID（編輯時）
  bankName: string; // 開戶銀行
  accountName: string; // 賬戶名稱
  accountNumber: string; // 賬號
  remark?: string; // 備註
  businessFlag: boolean; // 是否對公
  enterpriseId: number; // 企業ID
  disabledFlag: boolean; // 禁用狀態
}
