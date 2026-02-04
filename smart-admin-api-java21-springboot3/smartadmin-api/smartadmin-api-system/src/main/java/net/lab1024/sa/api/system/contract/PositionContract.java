package net.lab1024.sa.api.system.contract;

import io.vavr.control.Option;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import net.lab1024.sa.api.system.dto.PositionDTO;

/**
 * Position API Contract
 *
 * <p>API contract interface for position-related operations between modules.
 *
 * <p>Design principles: Same as EmployeeContract (Vavr Option, Feign-compatible, high reusability,
 * read-only)
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public interface PositionContract {

  /**
   * Get position by ID
   *
   * @param positionId position ID (must not be null)
   * @return Option containing position entity (Option.none() if not found)
   * @throws IllegalArgumentException if positionId is null
   */
  Option<PositionDTO> getById(@NotNull Long positionId);

  /**
   * Query all positions
   *
   * @return position list (non-null collection, may be empty)
   */
  List<PositionDTO> listAll();
}
