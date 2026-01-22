package net.lab1024.sa.admin.module.business.oa.notice.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;

/**
 * 通知公告 员工查询表单
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NoticeEmployeeQueryForm extends PageParam {

  @Schema(description = "标题、作者、来源、文号")
  private String keywords;

  @Schema(description = "分类")
  private Long noticeTypeId;

  @Schema(description = "发布-开始时间")
  private LocalDate publishTimeBegin;

  @Schema(description = "未读标识")
  private Boolean notViewFlag;

  @Schema(description = "发布-截止时间")
  private LocalDate publishTimeEnd;
}
