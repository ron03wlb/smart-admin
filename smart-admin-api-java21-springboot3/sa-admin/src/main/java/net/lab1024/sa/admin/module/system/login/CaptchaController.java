package net.lab1024.sa.admin.module.system.login;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import net.lab1024.sa.base.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.base.web.base.SupportBaseController;
import net.lab1024.sa.foundation.captcha.CaptchaService;
import net.lab1024.sa.foundation.captcha.CaptchaVO;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码业务
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-09-02 20:21:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CAPTCHA)
@RestController
public class CaptchaController extends SupportBaseController {

  @Resource private CaptchaService captchaService;

  @Operation(summary = "获取图形验证码 @author 胡克")
  @GetMapping("/captcha")
  public ResponseDTO<CaptchaVO> generateCaptcha() {
    return ResponseDTO.ok(captchaService.generateCaptcha());
  }
}
