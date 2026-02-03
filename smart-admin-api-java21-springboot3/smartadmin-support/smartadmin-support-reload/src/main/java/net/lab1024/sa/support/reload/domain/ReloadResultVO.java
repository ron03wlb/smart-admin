package net.lab1024.sa.support.reload.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * reload结果
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class ReloadResultVO {

  @Schema(description = "加载项标签")
  private String tag;

  @Schema(description = "参数")
  private String args;

  @Schema(description = "运行结果")
  private Boolean result;

  @Schema(description = "异常")
  private String exception;

  @Schema(description = "创建时间")
  private LocalDateTime createTime;
}
