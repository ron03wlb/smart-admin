package net.lab1024.sa.support.job.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.support.job.constant.SmartJobTriggerTypeEnum;
import org.hibernate.validator.constraints.Length;

/**
 * 定时任务 分页查询
 *
 * @author huke
 * @since 2024/6/17 20:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SmartJobQueryForm extends PageParam {

  @Schema(description = "搜索词|可选")
  @Length(max = 50, message = "搜索词最多50字符")
  private String searchWord;

  @SchemaEnum(desc = "触发类型", value = SmartJobTriggerTypeEnum.class)
  @CheckEnum(value = SmartJobTriggerTypeEnum.class, message = "触发类型错误")
  private String triggerType;

  @Schema(description = "是否启用|可选")
  private Boolean enabledFlag;

  @Schema(description = "是否删除|可选")
  private Boolean deletedFlag;
}
