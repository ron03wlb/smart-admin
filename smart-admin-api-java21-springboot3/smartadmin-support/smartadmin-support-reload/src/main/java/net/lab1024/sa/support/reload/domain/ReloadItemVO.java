package net.lab1024.sa.support.reload.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * reload (内存热加载、钩子等)
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class ReloadItemVO {

  @Schema(description = "加载项标签")
  private String tag;

  @Schema(description = "参数")
  private String args;

  @Schema(description = "运行标识")
  private String identification;

  @Schema(description = "更新时间")
  private OffsetDateTime updateTime;

  @Schema(description = "创建时间")
  private OffsetDateTime createTime;
}
