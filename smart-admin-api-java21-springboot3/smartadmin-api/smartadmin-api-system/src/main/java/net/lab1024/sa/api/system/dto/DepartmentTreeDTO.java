package net.lab1024.sa.api.system.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Department Tree Data Transfer Object
 *
 * <p>Extends DepartmentDTO with tree structure information for hierarchical department queries.
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Department Tree DTO")
public class DepartmentTreeDTO extends DepartmentDTO {

  private static final long serialVersionUID = 1L;

  @Schema(description = "Previous sibling department ID")
  private Long preId;

  @Schema(description = "Next sibling department ID")
  private Long nextId;

  @Schema(description = "Child departments")
  private List<DepartmentTreeDTO> children;

  @Schema(description = "Self and all recursive children ID list")
  private List<Long> selfAndAllChildrenIdList;
}
