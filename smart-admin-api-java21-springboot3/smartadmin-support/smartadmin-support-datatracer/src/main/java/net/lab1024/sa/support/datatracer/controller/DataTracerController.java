package net.lab1024.sa.support.datatracer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.datatracer.domain.form.DataTracerQueryForm;
import net.lab1024.sa.support.datatracer.domain.vo.DataTracerVO;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据变动记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-07-23 19:38:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.DATA_TRACER)
@RestController
@RequiredArgsConstructor
public class DataTracerController extends SupportBaseController {

  private final DataTracerService dataTracerService;

  @Operation(summary = "分页查询业务操作日志 - @author 卓大")
  @PostMapping("/dataTracer/query")
  public ResponseDTO<PageResult<DataTracerVO>> query(
      @Valid @RequestBody DataTracerQueryForm queryForm) {
    return dataTracerService.query(queryForm);
  }
}
