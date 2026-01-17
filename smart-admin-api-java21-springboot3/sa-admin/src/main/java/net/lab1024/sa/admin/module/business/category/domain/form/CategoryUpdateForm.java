package net.lab1024.sa.admin.module.business.category.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 类目 更新
 *
 * @author 1024创新实验室: 胡克
 * @since 2021/08/05 21:26:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CategoryUpdateForm extends CategoryAddForm {

  @Schema(description = "类目id")
  @NotNull(message = "类目id不能为空")
  private Long categoryId;
}
