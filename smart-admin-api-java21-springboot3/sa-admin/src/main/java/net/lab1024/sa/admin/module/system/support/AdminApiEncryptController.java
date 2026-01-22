package net.lab1024.sa.admin.module.system.support;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.base.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.base.web.base.SupportBaseController;
import net.lab1024.sa.common.apiencrypt.annotation.ApiDecrypt;
import net.lab1024.sa.common.apiencrypt.annotation.ApiEncrypt;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * api 加密
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/21 09:21:20 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@RestController
@Tag(name = SwaggerTagConst.Support.PROTECT)
public class AdminApiEncryptController extends SupportBaseController {

  @ApiDecrypt
  @PostMapping("/apiEncrypt/testRequestEncrypt")
  @Operation(summary = "测试 请求加密")
  public ResponseDTO<JweForm> testRequestEncrypt(@RequestBody @Valid JweForm form) {
    return ResponseDTO.ok(form);
  }

  @ApiEncrypt
  @PostMapping("/apiEncrypt/testResponseEncrypt")
  @Operation(summary = "测试 返回加密")
  public ResponseDTO<JweForm> testResponseEncrypt(@RequestBody @Valid JweForm form) {
    return ResponseDTO.ok(form);
  }

  @ApiDecrypt
  @ApiEncrypt
  @PostMapping("/apiEncrypt/testDecryptAndEncrypt")
  @Operation(summary = "测试 请求参数加密和解密、返回数据加密和解密")
  public ResponseDTO<JweForm> testDecryptAndEncrypt(@RequestBody @Valid JweForm form) {
    return ResponseDTO.ok(form);
  }

  @ApiDecrypt
  @ApiEncrypt
  @PostMapping("/apiEncrypt/testArray")
  @Operation(summary = "测试 数组加密和解密")
  public ResponseDTO<List<JweForm>> testArray(@RequestBody @Valid List<JweForm> list) {
    return ResponseDTO.ok(list);
  }

  @EqualsAndHashCode(callSuper = false)
  @Data
  public static class JweForm {

    @NotBlank(message = "姓名 不能为空")
    String name;

    @NotNull
    @Min(value = 1)
    Integer age;
  }
}
