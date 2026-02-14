package net.lab1024.sa.oa.notice.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.support.file.json.serializer.FileKeyVoSerializer;

/**
 * 用于更新 【通知、公告】 的 VO 对象
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class NoticeUpdateFormVO extends NoticeVO {

  @Schema(description = "纯文本内容")
  private String contentText;

  @Schema(description = "html内容")
  private String contentHtml;

  @Schema(description = "附件")
  @JsonSerialize(using = FileKeyVoSerializer.class)
  private String attachment;

  @Schema(description = "可见范围")
  private List<NoticeVisibleRangeVO> visibleRangeList;
}
