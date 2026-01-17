package net.lab1024.sa.admin.module.business.oa.enterprise.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 企业员工
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022/7/28 20:37:15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class EnterpriseEmployeeForm {

  @Schema(description = "企业id")
  @NotNull(message = "企业id不能为空")
  private Long enterpriseId;

  @Schema(description = "员工信息id")
  @NotEmpty(message = "员工信息id不能为空")
  private List<Long> employeeIdList;
}
