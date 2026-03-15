package net.lab1024.sa.oa.notice.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler;

/**
 * 通知公告
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notice")
public class NoticeEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long noticeId;

  /** 类型 */
  private Long noticeTypeId;

  /** 标题 */
  private String title;

  /** 是否全部可见 */
  @TableField(value = "all_visible", typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean allVisibleFlag;

  /** 是否定时发布 */
  @TableField(value = "scheduled_publish", typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean scheduledPublishFlag;

  /** 发布时间 */
  private OffsetDateTime publishTime;

  /** 内容 纯文本 */
  private String contentText;

  /** 内容 html */
  private String contentHtml;

  /** 附件 多个英文逗号分隔 */
  private String attachment;

  /** 页面浏览量 */
  private Integer pageViewCount;

  /** 用户浏览量 */
  private Integer userViewCount;

  /** 来源 */
  private String source;

  /** 作者 */
  private String author;

  /** 文号 */
  private String documentNumber;

  @TableField(value = "deleted", typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean deletedFlag;

  private Long createUserId;
}
