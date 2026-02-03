package net.lab1024.sa.support.config.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配置更新表单
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-14 20:46:27 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConfigUpdateForm extends ConfigAddForm {

  @Schema(description = "configId")
  @NotNull(message = "configId不能为空")
  private Long configId;
}
