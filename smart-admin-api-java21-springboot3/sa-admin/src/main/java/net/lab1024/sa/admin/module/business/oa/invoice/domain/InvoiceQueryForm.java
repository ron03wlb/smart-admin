package net.lab1024.sa.admin.module.business.oa.invoice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;
import org.hibernate.validator.constraints.Length;

/**
 * OA发票信息查询
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-06-23 19:32:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InvoiceQueryForm extends PageParam {

  @Schema(description = "企业ID")
  private Long enterpriseId;

  @Schema(description = "关键字")
  @Length(max = 200, message = "关键字最多200字符")
  private String keywords;

  @Schema(description = "开始时间")
  private LocalDate startTime;

  @Schema(description = "结束时间")
  private LocalDate endTime;

  @Schema(description = "禁用状态")
  private Boolean disabledFlag;

  @Schema(description = "删除状态", hidden = true)
  private Boolean deletedFlag;
}
