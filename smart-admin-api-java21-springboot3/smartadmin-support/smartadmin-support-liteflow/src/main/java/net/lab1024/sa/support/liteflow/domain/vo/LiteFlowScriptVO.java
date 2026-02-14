package net.lab1024.sa.support.liteflow.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * LiteFlow 腳本 VO
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Data
@Schema(description = "LiteFlow腳本VO")
public class LiteFlowScriptVO {

  @Schema(description = "腳本ID")
  private Long scriptId;

  @Schema(description = "腳本名稱")
  private String scriptName;

  @Schema(description = "腳本編碼")
  private String scriptCode;

  @Schema(description = "腳本類型")
  private String scriptType;

  @Schema(description = "腳本內容")
  private String scriptData;

  @Schema(description = "版本號")
  private Integer version;

  @Schema(description = "狀態 0-禁用 1-啟用")
  private Integer status;

  @Schema(description = "備註")
  private String remark;

  @Schema(description = "創建時間")
  private OffsetDateTime createTime;

  @Schema(description = "更新時間")
  private OffsetDateTime updateTime;
}
