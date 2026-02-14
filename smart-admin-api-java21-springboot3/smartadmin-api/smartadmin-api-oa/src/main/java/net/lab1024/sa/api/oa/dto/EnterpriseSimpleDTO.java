package net.lab1024.sa.api.oa.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 企業簡化信息 DTO（列表專用）
 *
 * <p>用於跨模塊 API 調用的數據傳輸對象。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>實現 Serializable 支持 RPC 序列化
 *   <li>僅包含列表展示必要的字段
 *   <li>減少數據傳輸量
 *   <li>使用 Lombok 簡化代碼
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
public class EnterpriseSimpleDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 企業 ID */
  private Long enterpriseId;

  /** 企業名稱 */
  private String enterpriseName;

  /** 企業類型 */
  private Integer type;
}
