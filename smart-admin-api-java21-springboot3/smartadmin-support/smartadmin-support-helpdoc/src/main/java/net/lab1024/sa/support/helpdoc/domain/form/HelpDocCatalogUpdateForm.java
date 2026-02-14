package net.lab1024.sa.support.helpdoc.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帮助文档 目录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HelpDocCatalogUpdateForm extends HelpDocCatalogAddForm {

  @Schema(description = "id")
  @NotNull(message = "id")
  private Long helpDocCatalogId;
}
