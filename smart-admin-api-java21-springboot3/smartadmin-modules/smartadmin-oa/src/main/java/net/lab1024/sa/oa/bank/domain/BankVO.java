package net.lab1024.sa.oa.bank.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * OA办公-OA银行信息
 *
 * @author 1024创新实验室:善逸
 * @since 2022/6/23 21:59:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class BankVO {

  @Schema(description = "银行信息ID")
  private Long bankId;

  @Schema(description = "开户银行")
  private String bankName;

  @Schema(description = "账户名称")
  private String accountName;

  @Schema(description = "账号")
  private String accountNumber;

  @Schema(description = "备注")
  private String remark;

  @Schema(description = "是否对公")
  private Boolean businessFlag;

  @Schema(description = "企业ID")
  private Long enterpriseId;

  @Schema(description = "企业名称")
  private String enterpriseName;

  @Schema(description = "禁用状态")
  private Boolean disabledFlag;

  @Schema(description = "创建人ID")
  private Long createUserId;

  @Schema(description = "创建人名称")
  private String createUserName;

  @Schema(description = "创建时间")
  private OffsetDateTime createTime;

  @Schema(description = "更新时间")
  private OffsetDateTime updateTime;
}
