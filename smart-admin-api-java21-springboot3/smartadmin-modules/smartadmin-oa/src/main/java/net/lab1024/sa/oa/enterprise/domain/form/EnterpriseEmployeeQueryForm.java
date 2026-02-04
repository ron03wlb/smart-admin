package net.lab1024.sa.oa.enterprise.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import org.hibernate.validator.constraints.Length;

/**
 * 查询企业员工
 *
 * @author 1024创新实验室: 开云
 * @since 2021-12-20 21:06:49 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EnterpriseEmployeeQueryForm extends PageParam {

  @Schema(description = "搜索词")
  @Length(max = 20, message = "搜索词最多20字符")
  private String keyword;

  @Schema(description = "公司Id")
  @NotNull(message = "公司id 不能为空")
  private Long enterpriseId;

  @Schema(description = "删除标识", hidden = true)
  private Boolean deletedFlag;
}
