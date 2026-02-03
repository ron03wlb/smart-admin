package net.lab1024.sa.support.table.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 自定义表格列
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 22:52:21 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class TableColumnUpdateForm {

  @NotNull(message = "表id不能为空")
  private Integer tableId;

  @NotEmpty(message = "请上传列")
  private List<TableColumnItemForm> columnList;
}
