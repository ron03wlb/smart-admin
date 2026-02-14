package net.lab1024.sa.support.message.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.support.message.constant.MessageTypeEnum;

/**
 * 消息
 *
 * @author luoyi
 * @since 2024/06/22 20:20
 */
@Data
public class MessageVO {

  private Long messageId;

  @SchemaEnum(MessageTypeEnum.class)
  private Integer messageType;

  @SchemaEnum(UserTypeEnum.class)
  private Integer receiverUserType;

  @Schema(description = "接收者id")
  private Long receiverUserId;

  @Schema(description = "相关业务id")
  private String dataId;

  @Schema(description = "消息标题")
  private String title;

  @Schema(description = "消息内容")
  private String content;

  @Schema(description = "是否已读")
  private Boolean readFlag;

  @Schema(description = "已读时间")
  private OffsetDateTime readTime;

  @Schema(description = "创建时间")
  private OffsetDateTime createTime;
}
