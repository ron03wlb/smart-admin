package net.lab1024.sa.system.menu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.common.web.util.SmartRequestUtil;
import net.lab1024.sa.common.core.domain.RequestUrlVO;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.menu.domain.form.MenuAddForm;
import net.lab1024.sa.system.menu.domain.form.MenuUpdateForm;
import net.lab1024.sa.system.menu.domain.vo.MenuTreeVO;
import net.lab1024.sa.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.system.menu.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜单
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-03-06 22:04:37 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_MENU)
public class MenuController {

  private final MenuService menuService;

  @Operation(summary = "添加菜单 @author 卓大")
  @PostMapping("/menu/add")
  @SaCheckPermission("system:menu:add")
  public ResponseDTO<String> addMenu(@RequestBody @Valid MenuAddForm menuAddForm) {
    menuAddForm.setCreateUserId(SmartRequestUtil.getRequestUserId());
    return menuService.addMenu(menuAddForm);
  }

  @Operation(summary = "更新菜单 @author 卓大")
  @PostMapping("/menu/update")
  @SaCheckPermission("system:menu:update")
  public ResponseDTO<String> updateMenu(@RequestBody @Valid MenuUpdateForm menuUpdateForm) {
    menuUpdateForm.setUpdateUserId(SmartRequestUtil.getRequestUserId());
    return menuService.updateMenu(menuUpdateForm);
  }

  @Operation(summary = "批量删除菜单 @author 卓大")
  @GetMapping("/menu/batchDelete")
  @SaCheckPermission("system:menu:batchDelete")
  public ResponseDTO<String> batchDeleteMenu(@RequestParam("menuIdList") List<Long> menuIdList) {
    return menuService.batchDeleteMenu(menuIdList, SmartRequestUtil.getRequestUserId());
  }

  @Operation(summary = "查询菜单列表 @author 卓大")
  @GetMapping("/menu/query")
  public ResponseDTO<List<MenuVO>> queryMenuList() {
    return ResponseDTO.ok(menuService.queryMenuList(null));
  }

  @Operation(summary = "查询菜单详情 @author 卓大")
  @GetMapping("/menu/detail/{menuId}")
  public ResponseDTO<MenuVO> getMenuDetail(@PathVariable Long menuId) {
    return menuService.getMenuDetail(menuId);
  }

  @Operation(summary = "查询菜单树 @author 卓大")
  @GetMapping("/menu/tree")
  public ResponseDTO<List<MenuTreeVO>> queryMenuTree(@RequestParam("onlyMenu") Boolean onlyMenu) {
    return menuService.queryMenuTree(onlyMenu);
  }

  @Operation(summary = "获取所有请求路径 @author 卓大")
  @GetMapping("/menu/auth/url")
  public ResponseDTO<List<RequestUrlVO>> getAuthUrl() {
    return menuService.getAuthUrl();
  }
}
