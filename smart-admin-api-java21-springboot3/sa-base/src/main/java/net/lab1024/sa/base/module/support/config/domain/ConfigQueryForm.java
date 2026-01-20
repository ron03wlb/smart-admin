package net.lab1024.sa.base.module.support.config.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.PageParam;
import org.hibernate.validator.constraints.Length;

/**
 * 分页查询 系统配置
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-14 20:46:27 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConfigQueryForm extends PageParam {

  @Schema(description = "参数KEY")
  @Length(max = 50, message = "参数Key最多50字符")
  private String configKey;
}
