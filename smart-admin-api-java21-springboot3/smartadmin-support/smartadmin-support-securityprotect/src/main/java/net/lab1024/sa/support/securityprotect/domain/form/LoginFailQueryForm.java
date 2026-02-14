package net.lab1024.sa.support.securityprotect.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * 登录失败 分页查询表单
 *
 * @author 1024创新实验室-主任-卓大
 * @since 2023-10-17 18:02:37 Copyright 1024创新实验室
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LoginFailQueryForm extends PageParam {

  @Schema(description = "登录名")
  private String loginName;

  @Schema(description = "锁定状态")
  private Boolean lockFlag;

  @Schema(description = "登录失败锁定时间-开始")
  private LocalDate loginLockBeginTimeBegin;

  @Schema(description = "登录失败锁定时间-结束")
  private LocalDate loginLockBeginTimeEnd;
}
