package net.lab1024.sa.base.module.support.file.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import net.lab1024.sa.base.core.validator.enumeration.CheckEnum;
import net.lab1024.sa.base.module.support.file.constant.FileFolderTypeEnum;
import net.lab1024.sa.base.swagger.annotation.SchemaEnum;

/**
 * url上传文件
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class FileUrlUploadForm {

  @SchemaEnum(value = FileFolderTypeEnum.class, desc = "业务类型")
  @CheckEnum(value = FileFolderTypeEnum.class, required = true, message = "业务类型错误")
  private Integer folder;

  @Schema(description = "文件url")
  @NotBlank(message = "文件url不能为空")
  private String fileUrl;
}
