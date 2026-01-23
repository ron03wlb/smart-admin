package net.lab1024.sa.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 请求url返回对象
 *
 * @author 1024创新实验室: 李善逸
 * @since 2021/9/1 20:15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class RequestUrlVO {

  @Schema(description = "注释说明")
  private String comment;

  @Schema(description = "controller.method")
  private String name;

  @Schema(description = "url")
  private String url;
}
