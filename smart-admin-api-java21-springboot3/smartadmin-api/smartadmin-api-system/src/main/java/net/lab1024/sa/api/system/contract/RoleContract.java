package net.lab1024.sa.api.system.contract;

import io.vavr.control.Option;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import net.lab1024.sa.api.system.dto.RoleDTO;

/**
 * Role API Contract
 *
 * <p>API contract interface for role-related operations between modules.
 *
 * <p>Design principles: Same as EmployeeContract (Vavr Option, Feign-compatible, high reusability,
 * read-only)
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public interface RoleContract {

  /**
   * Get role by ID
   *
   * @param roleId role ID (must not be null)
   * @return Option containing role entity (Option.none() if not found)
   * @throws IllegalArgumentException if roleId is null
   */
  Option<RoleDTO> getById(@NotNull Long roleId);

  /**
   * Batch query roles by IDs
   *
   * @param roleIds role ID collection (must not be null)
   * @return role list (ordered by input IDs, non-null collection, may be empty)
   * @throws IllegalArgumentException if roleIds is null
   */
  List<RoleDTO> queryByIds(@NotNull Collection<Long> roleIds);

  /**
   * Query all roles
   *
   * @return role list (non-null collection, may be empty)
   */
  List<RoleDTO> listAll();
}
