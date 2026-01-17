package net.lab1024.sa.admin.module.system.department.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门 更新表单
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-12 20:37:48 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DepartmentUpdateForm extends DepartmentAddForm {

  @Schema(description = "部门id")
  @NotNull(message = "部门id不能为空")
  private Long departmentId;
}
