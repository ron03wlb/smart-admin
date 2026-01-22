package net.lab1024.sa.base.module.support.message.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.base.module.support.message.constant.MessageTypeEnum;
import net.lab1024.sa.base.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.core.domain.PageParam;
import net.lab1024.sa.foundation.validation.annotation.CheckEnum;
import org.hibernate.validator.constraints.Length;

/**
 * 消息查询form
 *
 * @author luoyi
 * @since 2024/06/22 20:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageQueryForm extends PageParam {

  @Schema(description = "搜索词")
  @Length(max = 50, message = "搜索词最多50字符")
  private String searchWord;

  @SchemaEnum(MessageTypeEnum.class)
  @CheckEnum(value = MessageTypeEnum.class, message = "消息类型")
  private Integer messageType;

  @Schema(description = "是否已读")
  private Boolean readFlag;

  @Schema(description = "查询开始时间")
  private LocalDate startDate;

  @Schema(description = "查询结束时间")
  private LocalDate endDate;

  @Schema(description = "接收人")
  private Long receiverUserId;

  @Schema(description = "接收人类型")
  private Integer receiverUserType;
}
