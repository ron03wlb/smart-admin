package net.lab1024.sa.api.oa.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 發票信息 DTO
 *
 * <p>用於跨模塊 API 調用的數據傳輸對象。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>實現 Serializable 支持 RPC 序列化
 *   <li>包含冗餘字段（enterpriseName）以避免 N+1 查詢
 *   <li>使用 Lombok 簡化代碼
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
public class InvoiceDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 發票 ID */
  private Long invoiceId;

  /** 企業 ID */
  private Long enterpriseId;

  /** 企業名稱（冗餘字段，避免 N+1 查詢） */
  private String enterpriseName;

  /** 發票抬頭 */
  private String invoiceHeads;

  /** 納稅人識別號 */
  private String taxpayerIdentificationNumber;

  /** 銀行名稱 */
  private String bankName;

  /** 賬號 */
  private String accountNumber;

  /** 備註 */
  private String remark;

  /** 禁用狀態 */
  private Boolean disabledFlag;

  /** 更新時間 */
  private OffsetDateTime updateTime;

  /** 創建時間 */
  private OffsetDateTime createTime;
}
