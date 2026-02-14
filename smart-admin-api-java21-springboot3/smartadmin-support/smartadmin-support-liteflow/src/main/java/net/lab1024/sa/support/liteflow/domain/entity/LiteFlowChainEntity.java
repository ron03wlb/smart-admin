package net.lab1024.sa.support.liteflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * LiteFlow 流程定義實體
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_liteflow_chain")
public class LiteFlowChainEntity extends SmartAdminBaseEntity {

  /** 流程ID（主鍵） */
  @TableId(type = IdType.AUTO)
  private Long chainId;

  /** 流程名稱 */
  private String chainName;

  /** 流程編碼（唯一） */
  private String chainCode;

  /** 流程類型：1-普通 2-條件 3-循環 */
  private Integer chainType;

  /** EL 表達式定義，如: THEN(a, b, c) */
  private String chainData;

  /** 版本號（更新時自動遞增） */
  private Integer version;

  /** 狀態：0-禁用 1-啟用 */
  private Integer status;

  /** 刪除標記：0-未刪除 1-已刪除 */
  private Integer deletedFlag;

  /** 備註 */
  private String remark;

  /** 創建人ID */
  private Long createUserId;

  /** 創建人姓名 */
  private String createUserName;

  /** 更新人ID */
  private Long updateUserId;

  /** 更新人姓名 */
  private String updateUserName;
}
