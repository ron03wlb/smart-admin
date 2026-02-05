package net.lab1024.sa.api.oa.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 企業完整信息 DTO
 *
 * <p>用於跨模塊 API 調用的數據傳輸對象。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>實現 Serializable 支持 RPC 序列化
 *   <li>包含完整的企業信息字段
 *   <li>使用 Lombok 簡化代碼
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
public class EnterpriseDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 企業 ID */
  private Long enterpriseId;

  /** 企業名稱 */
  private String enterpriseName;

  /** 企業類型 */
  private Integer type;

  /** 統一社會信用代碼 */
  private String unifiedSocialCreditCode;

  /** 聯繫人 */
  private String contact;

  /** 聯繫電話 */
  private String contactPhone;

  /** Logo URL */
  private String logo;

  /** 營業執照 URL */
  private String businessLicense;

  /** 禁用狀態 */
  private Boolean disabledFlag;

  /** 刪除狀態 */
  private Boolean deletedFlag;

  /** 創建時間 */
  private LocalDateTime createTime;

  /** 更新時間 */
  private LocalDateTime updateTime;
}
