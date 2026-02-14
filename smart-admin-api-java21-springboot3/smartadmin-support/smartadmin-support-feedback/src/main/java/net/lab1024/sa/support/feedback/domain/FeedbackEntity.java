package net.lab1024.sa.support.feedback.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 意见反馈 表
 *
 * @author 1024创新实验室: 开云
 * @since 2022-08-11 20:48:09 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_feedback")
@SuppressWarnings("PMD.LongVariable")
public class FeedbackEntity extends SmartAdminBaseEntity {
  /** 主键 */
  @TableId(type = IdType.AUTO)
  private Long feedbackId;

  /** 反馈内容 */
  private String feedbackContent;

  /** 反馈附件 */
  private String feedbackAttachment;

  /** 创建人id */
  private Long userId;

  /** 用户类型 */
  private Integer userType;

  /** 创建人姓名 */
  private String userName;
}
