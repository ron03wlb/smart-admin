package net.lab1024.sa.support.message.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.support.message.constant.MessageTypeEnum;

/**
 * 消息实体
 *
 * @author luoyi
 * @since 2024/06/22 20:20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_message")
public class MessageEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long messageId;

  /**
   * 消息类型
   *
   * @see MessageTypeEnum
   */
  private Integer messageType;

  /**
   * 接收者类型
   *
   * @see net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum
   */
  private Integer receiverUserType;

  /** 接收者id */
  private Long receiverUserId;

  /** 相关业务id */
  private String dataId;

  /** 消息标题 */
  private String title;

  /** 消息内容 */
  private String content;

  /** 是否已读 */
  private Boolean readFlag;

  /** 已读时间 */
  private OffsetDateTime readTime;
}
