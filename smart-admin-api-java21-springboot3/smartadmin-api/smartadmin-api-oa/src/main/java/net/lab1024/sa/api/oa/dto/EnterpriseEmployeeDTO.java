package net.lab1024.sa.api.oa.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * 企業員工關聯 DTO
 *
 * <p>用於跨模塊 API 調用的數據傳輸對象。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>實現 Serializable 支持 RPC 序列化
 *   <li>包含冗餘字段（employeeName, departmentName）以避免 N+1 查詢
 *   <li>使用 Lombok 簡化代碼
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Data
public class EnterpriseEmployeeDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 企業 ID */
  private Long enterpriseId;

  /** 員工 ID */
  private Long employeeId;

  /** 員工姓名（冗餘字段，避免 N+1 查詢） */
  private String employeeName;

  /** 部門名稱（冗餘字段，避免 N+1 查詢） */
  private String departmentName;
}
