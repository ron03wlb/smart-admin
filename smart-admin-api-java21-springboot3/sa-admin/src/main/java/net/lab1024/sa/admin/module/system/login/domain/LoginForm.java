package net.lab1024.sa.admin.module.system.login.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.base.constant.LoginDeviceEnum;
import net.lab1024.sa.base.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.captcha.CaptchaForm;
import net.lab1024.sa.foundation.validation.annotation.CheckEnum;
import org.hibernate.validator.constraints.Length;

/**
 * 员工登录
 *
 * @author 1024创新实验室: 开云
 * @since 2021-12-19 11:49:45 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LoginForm extends CaptchaForm {

  @Schema(description = "登录账号")
  @NotBlank(message = "登录账号不能为空")
  @Length(max = 30, message = "登录账号最多30字符")
  private String loginName;

  @Schema(description = "密码")
  @NotBlank(message = "密码不能为空")
  private String password;

  @SchemaEnum(desc = "登录终端", value = LoginDeviceEnum.class)
  @CheckEnum(value = LoginDeviceEnum.class, required = true, message = "此终端不允许登录")
  private Integer loginDevice;

  @Schema(description = "邮箱验证码")
  private String emailCode;
}
