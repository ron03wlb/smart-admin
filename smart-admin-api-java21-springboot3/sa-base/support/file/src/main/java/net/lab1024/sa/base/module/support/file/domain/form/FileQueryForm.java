package net.lab1024.sa.base.module.support.file.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.base.module.support.file.constant.FileFolderTypeEnum;
import net.lab1024.sa.base.swagger.annotation.SchemaEnum;
import net.lab1024.sa.foundation.domain.request.PageParam;
import net.lab1024.sa.foundation.validation.annotation.CheckEnum;

/**
 * 文件信息查询
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileQueryForm extends PageParam {

  @SchemaEnum(value = FileFolderTypeEnum.class, desc = "文件夹类型")
  @CheckEnum(value = FileFolderTypeEnum.class, message = "文件夹类型 错误")
  private Integer folderType;

  @Schema(description = "文件名词")
  private String fileName;

  @Schema(description = "文件Key")
  private String fileKey;

  @Schema(description = "文件类型")
  private String fileType;

  @Schema(description = "创建人")
  private String creatorName;

  @Schema(description = "创建时间")
  private LocalDate createTimeBegin;

  @Schema(description = "创建时间")
  private LocalDate createTimeEnd;
}
