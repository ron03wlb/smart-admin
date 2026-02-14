package net.lab1024.sa.api.system.contract;

import io.vavr.control.Option;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import net.lab1024.sa.api.system.dto.MenuDTO;

/**
 * Menu API Contract
 *
 * <p>API contract interface for menu-related operations between modules.
 *
 * <p>Design principles: Same as EmployeeContract (Vavr Option, Feign-compatible, high reusability,
 * read-only)
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public interface MenuContract {

  /**
   * Get menu by ID
   *
   * @param menuId menu ID (must not be null)
   * @return Option containing menu entity (Option.none() if not found)
   * @throws IllegalArgumentException if menuId is null
   */
  Option<MenuDTO> getById(@NotNull Long menuId);

  /**
   * Query all menus
   *
   * @return menu list (non-null collection, may be empty)
   */
  List<MenuDTO> listAll();

  /**
   * Query menus by role ID
   *
   * @param roleId role ID (must not be null)
   * @return menu list (non-null collection, may be empty)
   * @throws IllegalArgumentException if roleId is null
   */
  List<MenuDTO> queryByRoleId(@NotNull Long roleId);
}
