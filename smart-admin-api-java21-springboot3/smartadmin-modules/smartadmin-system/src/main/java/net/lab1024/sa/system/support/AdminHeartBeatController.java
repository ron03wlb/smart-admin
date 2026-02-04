package net.lab1024.sa.system.support;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.support.heartbeat.domain.HeartBeatRecordQueryForm;
import net.lab1024.sa.support.heartbeat.domain.HeartBeatRecordVO;
import net.lab1024.sa.support.heartbeat.service.HeartBeatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 心跳记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.HEART_BEAT)
@RequiredArgsConstructor
@RestController
public class AdminHeartBeatController extends SupportBaseController {

  private final HeartBeatService heartBeatService;

  @PostMapping("/heartBeat/query")
  @Operation(summary = "查询心跳记录 @author 卓大")
  public ResponseDTO<PageResult<HeartBeatRecordVO>> query(
      @RequestBody @Valid HeartBeatRecordQueryForm pageParam) {
    return heartBeatService.pageQuery(pageParam);
  }
}
