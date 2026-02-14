package net.lab1024.sa.support.codegenerator.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 表信息
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2022/9/21 18:07:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class TableVO {

  @Schema(description = "表名")
  private String tableName;

  @Schema(description = "表备注")
  private String tableComment;

  @Schema(description = "配置时间")
  private OffsetDateTime configTime;
}
