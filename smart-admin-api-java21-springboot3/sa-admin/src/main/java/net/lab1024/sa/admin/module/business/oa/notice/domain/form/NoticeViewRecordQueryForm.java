package net.lab1024.sa.admin.module.business.oa.notice.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.foundation.domain.request.PageParam;

/**
 * 通知公告 阅读记录查询
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NoticeViewRecordQueryForm extends PageParam {

  @Schema(description = "通知公告id")
  @NotNull(message = "通知公告id不能为空")
  private Long noticeId;

  @Schema(description = "部门id")
  private Long departmentId;

  @Schema(description = "关键字")
  private String keywords;
}
