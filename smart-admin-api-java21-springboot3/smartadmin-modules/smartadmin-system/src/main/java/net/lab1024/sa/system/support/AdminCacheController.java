package net.lab1024.sa.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.cache.CacheService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021/10/11 20:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = SwaggerTagConst.Support.CACHE)
public class AdminCacheController extends SupportBaseController {

  private final CacheService cacheService;

  @Operation(summary = "获取所有缓存 @author 罗伊")
  @GetMapping("/cache/names")
  @SaCheckPermission("support:cache:keys")
  public ResponseDTO<List<String>> cacheNames() {
    return ResponseDTO.ok(cacheService.cacheNames());
  }

  @Operation(summary = "移除某个缓存 @author 罗伊")
  @GetMapping("/cache/remove/{cacheName}")
  @SaCheckPermission("support:cache:delete")
  public ResponseDTO<String> removeCache(@PathVariable String cacheName) {
    cacheService.removeCache(cacheName);
    return ResponseDTO.ok();
  }

  @Operation(summary = "获取某个缓存的所有key @author 罗伊")
  @GetMapping("/cache/keys/{cacheName}")
  @SaCheckPermission("support:cache:keys")
  public ResponseDTO<List<String>> cacheKeys(@PathVariable String cacheName) {
    return ResponseDTO.ok(cacheService.cacheKey(cacheName));
  }
}
