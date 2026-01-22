package net.lab1024.sa.admin.module.system.employee.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;
import org.hibernate.validator.constraints.Length;

/**
 * 员工列表
 *
 * @author 1024创新实验室: 开云
 * @since 2021-12-20 21:06:49 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeQueryForm extends PageParam {

  @Schema(description = "搜索词")
  @Length(max = 20, message = "搜索词最多20字符")
  private String keyword;

  @Schema(description = "部门id")
  private Long departmentId;

  @Schema(description = "是否禁用")
  private Boolean disabledFlag;

  @Schema(description = "员工id集合")
  @Size(max = 99, message = "最多查询99个员工")
  private List<Long> employeeIdList;

  @Schema(description = "删除标识", hidden = true)
  private Boolean deletedFlag;
}
