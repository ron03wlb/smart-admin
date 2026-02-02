package net.lab1024.sa.base.module.support.liteflow.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * LiteFlow 腳本節點定義實體
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@TableName("t_liteflow_script")
public class LiteFlowScriptEntity {

  /** 腳本ID（主鍵） */
  @TableId(type = IdType.AUTO)
  private Long scriptId;

  /** 腳本名稱 */
  private String scriptName;

  /** 腳本編碼（唯一） */
  private String scriptCode;

  /** 腳本類型：qlexpress/groovy/javascript */
  private String scriptType;

  /** 腳本內容（代碼） */
  private String scriptData;

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

  /** 創建時間 */
  private LocalDateTime createTime;

  /** 更新人ID */
  private Long updateUserId;

  /** 更新人姓名 */
  private String updateUserName;

  /** 更新時間 */
  private LocalDateTime updateTime;
}
