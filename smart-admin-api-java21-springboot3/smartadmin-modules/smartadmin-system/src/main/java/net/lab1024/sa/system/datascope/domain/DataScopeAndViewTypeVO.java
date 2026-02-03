package net.lab1024.sa.system.datascope.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/**
 * 数据范围
 *
 * @author 1024创新实验室: 罗伊
 * @since 2020/11/28 20:59:17 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class DataScopeAndViewTypeVO {

  @Schema(description = "数据范围类型")
  private Integer dataScopeType;

  @Schema(description = "数据范围名称")
  private String dataScopeTypeName;

  @Schema(description = "描述")
  private String dataScopeTypeDesc;

  @Schema(description = "顺序")
  private Integer dataScopeTypeSort;

  @Schema(description = "可见范围列表")
  private List<DataScopeViewTypeVO> viewTypeList;
}
