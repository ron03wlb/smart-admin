package net.lab1024.sa.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.base.web.base.SupportBaseController;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.reload.ReloadService;
import net.lab1024.sa.support.reload.domain.ReloadForm;
import net.lab1024.sa.support.reload.domain.ReloadItemVO;
import net.lab1024.sa.support.reload.domain.ReloadResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * reload (内存热加载、钩子等)
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = SwaggerTagConst.Support.RELOAD)
public class AdminReloadController extends SupportBaseController {

  private final ReloadService reloadService;

  @Operation(summary = "查询reload列表 @author 开云")
  @GetMapping("/reload/query")
  public ResponseDTO<List<ReloadItemVO>> query() {
    return reloadService.query();
  }

  @Operation(summary = "获取reload result @author 开云")
  @GetMapping("/reload/result/{tag}")
  @SaCheckPermission("support:reload:result")
  public ResponseDTO<List<ReloadResultVO>> queryReloadResult(@PathVariable("tag") String tag) {
    return reloadService.queryReloadItemResult(tag);
  }

  @Operation(summary = "通过tag更新标识 @author 开云")
  @PostMapping("/reload/update")
  @SaCheckPermission("support:reload:update")
  public ResponseDTO<String> updateByTag(@RequestBody @Valid ReloadForm reloadForm) {
    return reloadService.updateByTag(reloadForm);
  }
}
