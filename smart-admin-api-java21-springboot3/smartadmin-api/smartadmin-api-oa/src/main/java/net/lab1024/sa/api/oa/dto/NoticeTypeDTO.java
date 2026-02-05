package net.lab1024.sa.api.oa.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 通知類型 DTO
 *
 * <p>用於跨模塊 API 調用的數據傳輸對象。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>實現 Serializable 支持 RPC 序列化
 *   <li>簡化設計，僅包含必要字段
 *   <li>使用 Lombok 簡化代碼
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
public class NoticeTypeDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 通知類型 ID */
  private Long noticeTypeId;

  /** 通知類型名稱 */
  private String noticeTypeName;
}
