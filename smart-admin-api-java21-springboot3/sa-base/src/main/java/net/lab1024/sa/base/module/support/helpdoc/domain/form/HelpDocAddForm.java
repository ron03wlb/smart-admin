package net.lab1024.sa.base.module.support.helpdoc.domain.form;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import net.lab1024.sa.base.common.json.deserializer.FileKeyVoDeserializer;
import org.hibernate.validator.constraints.Length;

/**
 * 帮助文档
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class HelpDocAddForm {

  @Schema(description = "标题")
  @NotBlank(message = "标题不能为空")
  @Length(max = 200, message = "标题最多200字符")
  private String title;

  @Schema(description = "分类")
  @NotNull(message = "分类不能为空")
  private Long helpDocCatalogId;

  @Schema(description = "纯文本内容")
  @NotNull(message = "文本内容不能为空")
  private String contentText;

  @Schema(description = "html内容")
  @NotNull(message = "html内容不能为空")
  private String contentHtml;

  @Schema(description = "附件,多个英文逗号分隔|可选")
  @Length(max = 1000, message = "最多1000字符")
  @JsonDeserialize(using = FileKeyVoDeserializer.class)
  private String attachment;

  @Schema(description = "排序")
  @NotNull(message = "排序不能为空")
  private Integer sort;

  @Schema(description = "关联的集合")
  private List<HelpDocRelationForm> relationList;

  @Schema(description = "作者")
  @NotBlank(message = "作者不能为空")
  private String author;
}
