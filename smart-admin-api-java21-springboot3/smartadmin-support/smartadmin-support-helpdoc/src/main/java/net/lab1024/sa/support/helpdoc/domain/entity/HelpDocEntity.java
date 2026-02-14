package net.lab1024.sa.support.helpdoc.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 帮助文档
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_help_doc")
public class HelpDocEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long helpDocId;

  /** 类型 */
  private Long helpDocCatalogId;

  /** 标题 */
  private String title;

  /** 内容 纯文本 */
  private String contentText;

  /** 内容 html */
  private String contentHtml;

  /** 附件 多个英文逗号分隔 */
  private String attachment;

  /** 排序 */
  private Integer sort;

  /** 页面浏览量 */
  private Integer pageViewCount;

  /** 用户浏览量 */
  private Integer userViewCount;

  /** 作者 */
  private String author;
}
