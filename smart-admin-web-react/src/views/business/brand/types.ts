/**
 * Brand Module Types
 * 品牌模塊類型定義
 *
 * 參考：後端 BrandVO.java, BrandQueryForm.java, BrandAddForm.java, BrandUpdateForm.java
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-26
 */

/**
 * Brand Status Enum
 * 品牌狀態枚舉
 */
export enum BrandStatusEnum {
  DISABLED = 0, // 禁用
  ENABLED = 1, // 啟用
}

/**
 * Brand VO
 * 品牌視圖對象
 */
export interface BrandVO {
  brandId: number; // 品牌ID（主鍵）
  brandName: string; // 品牌名稱（唯一）
  brandLogo?: string; // 品牌Logo URL
  description?: string; // 品牌描述
  sort: number; // 排序（升序）
  status: BrandStatusEnum; // 狀態（1=啟用, 0=禁用）
  updateTime: string; // 更新時間
  createTime: string; // 創建時間
}

/**
 * Brand Query Form
 * 品牌查詢表單
 */
export interface BrandQueryForm {
  pageNum: number; // 頁碼
  pageSize: number; // 每頁條數
  keyword?: string; // 搜索關鍵詞（品牌名稱）
  status?: BrandStatusEnum; // 狀態過濾
  deletedFlag?: boolean; // 刪除標記過濾（true=已刪除, false=未刪除, null=全部）
}

/**
 * Brand Add Form
 * 品牌新增表單
 */
export interface BrandAddForm {
  brandName: string; // 品牌名稱（最大50字符，唯一）
  brandLogo?: string; // 品牌Logo URL（最大200字符）
  description?: string; // 品牌描述（最大500字符）
  sort: number; // 排序
  status: BrandStatusEnum; // 狀態
}

/**
 * Brand Update Form
 * 品牌更新表單
 */
export interface BrandUpdateForm extends BrandAddForm {
  brandId: number; // 品牌ID
}

/**
 * Brand Form Data (for Modal)
 * 品牌表單數據（用於 Modal）
 */
export interface BrandFormData {
  brandId?: number; // 品牌ID（編輯時）
  brandName: string; // 品牌名稱
  brandLogo?: string; // 品牌Logo URL
  description?: string; // 品牌描述
  sort: number; // 排序
  status: BrandStatusEnum; // 狀態
}
