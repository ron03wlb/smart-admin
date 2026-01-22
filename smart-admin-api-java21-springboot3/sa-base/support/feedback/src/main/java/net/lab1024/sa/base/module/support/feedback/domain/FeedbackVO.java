package net.lab1024.sa.base.module.support.feedback.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import net.lab1024.sa.base.module.support.file.json.deserializer.FileKeyVoDeserializer;
import net.lab1024.sa.base.module.support.file.json.serializer.FileKeyVoSerializer;
import net.lab1024.sa.base.swagger.annotation.SchemaEnum;
import net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum;

/**
 * 意见反馈 返回对象
 *
 * @author 1024创新实验室: 开云
 * @since 2022-08-11 20:48:09 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressWarnings("PMD.LongVariable")
public class FeedbackVO {

  @Schema(description = "主键")
  private Long feedbackId;

  @Schema(description = "反馈内容")
  private String feedbackContent;

  @Schema(description = "反馈图片")
  @JsonSerialize(using = FileKeyVoSerializer.class)
  @JsonDeserialize(using = FileKeyVoDeserializer.class)
  private String feedbackAttachment;

  @Schema(description = "创建人id")
  private Long userId;

  @Schema(description = "创建人姓名")
  private String userName;

  @SchemaEnum(value = UserTypeEnum.class, desc = "创建人类型")
  private Integer userType;

  @Schema(description = "更新时间")
  private LocalDateTime updateTime;

  @Schema(description = "创建时间")
  private LocalDateTime createTime;
}
