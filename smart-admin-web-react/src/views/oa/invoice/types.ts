/**
 * Invoice Module Types
 * 發票信息模塊類型定義
 *
 * 參考：後端 InvoiceVO.java, InvoiceQueryForm.java, InvoiceAddForm.java, InvoiceUpdateForm.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

/**
 * Invoice VO
 * 發票信息視圖對象
 */
export interface InvoiceVO {
  invoiceId: number; // 發票信息ID（主鍵）
  invoiceHeads: string; // 開票抬頭
  taxpayerIdentificationNumber: string; // 納稅人識別號
  accountNumber: string; // 銀行賬戶
  bankName: string; // 開戶行
  remark?: string; // 備註
  enterpriseId: number; // 企業ID
  enterpriseName: string; // 企業名稱
  disabledFlag: boolean; // 禁用狀態（true=禁用, false=啟用）
  createUserId: number; // 創建人ID
  createUserName: string; // 創建人名稱
  createTime: string; // 創建時間
  updateTime: string; // 更新時間
}

/**
 * Invoice Query Form
 * 發票信息查詢表單
 */
export interface InvoiceQueryForm {
  pageNum: number; // 頁碼
  pageSize: number; // 每頁條數
  enterpriseId?: number; // 企業ID過濾
  keywords?: string; // 關鍵字（搜索開票抬頭、納稅人識別號、銀行賬戶）
  startTime?: string; // 開始時間（創建時間範圍）
  endTime?: string; // 結束時間（創建時間範圍）
  disabledFlag?: boolean; // 禁用狀態過濾
  deletedFlag?: boolean; // 刪除標記過濾（true=已刪除, false=未刪除, null=全部）
}

/**
 * Invoice Add Form
 * 發票信息新建表單
 */
export interface InvoiceAddForm {
  invoiceHeads: string; // 開票抬頭（必填，最大200字符）
  taxpayerIdentificationNumber: string; // 納稅人識別號（必填，最大200字符）
  accountNumber: string; // 銀行賬戶（必填，最大200字符）
  bankName: string; // 開戶行（必填，最大200字符）
  disabledFlag: boolean; // 禁用狀態（必填）
  remark?: string; // 備註（可選，最大500字符）
  enterpriseId: number; // 企業ID（必填）
}

/**
 * Invoice Update Form
 * 發票信息更新表單
 */
export interface InvoiceUpdateForm extends InvoiceAddForm {
  invoiceId: number; // 發票信息ID
}

/**
 * Invoice Form Data (for Modal)
 * 發票信息表單數據（用於 Modal）
 */
export interface InvoiceFormData {
  invoiceId?: number; // 發票信息ID（編輯時）
  invoiceHeads: string; // 開票抬頭
  taxpayerIdentificationNumber: string; // 納稅人識別號
  accountNumber: string; // 銀行賬戶
  bankName: string; // 開戶行
  disabledFlag: boolean; // 禁用狀態
  remark?: string; // 備註
  enterpriseId: number; // 企業ID
}
