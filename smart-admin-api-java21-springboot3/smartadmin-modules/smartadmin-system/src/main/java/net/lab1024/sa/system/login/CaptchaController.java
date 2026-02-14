package net.lab1024.sa.system.login;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.captcha.CaptchaService;
import net.lab1024.sa.common.captcha.CaptchaVO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码业务
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-09-02 20:21:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CAPTCHA)
@RequiredArgsConstructor
@RestController
public class CaptchaController extends SupportBaseController {

  private final CaptchaService captchaService;

  @Operation(summary = "获取图形验证码 @author 胡克")
  @GetMapping("/captcha")
  public ResponseDTO<CaptchaVO> generateCaptcha() {
    return ResponseDTO.ok(captchaService.generateCaptcha());
  }
}
