package net.lab1024.sa.support.liteflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowMonitorOverviewVO;
import net.lab1024.sa.support.liteflow.service.LiteFlowMonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LiteFlow 監控管理 Controller
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/liteflow/monitor")
@RequiredArgsConstructor
@Tag(name = "LiteFlow監控管理")
public class LiteFlowMonitorController {

  private final LiteFlowMonitorService monitorService;

  @Operation(summary = "獲取監控概覽")
  @GetMapping("/overview")
  @SaCheckPermission("liteflow:monitor:query")
  public ResponseDTO<LiteFlowMonitorOverviewVO> getOverview() {
    return monitorService.getOverview();
  }
}
