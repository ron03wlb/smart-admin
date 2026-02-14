package net.lab1024.sa.oa.bank.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OA办公-银行信息更新
 *
 * @author 1024创新实验室:善逸
 * @since 2022/6/23 21:59:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BankUpdateForm extends BankCreateForm {

  @Schema(description = "银行信息ID")
  @NotNull(message = "银行信息ID不能为空")
  private Long bankId;
}
