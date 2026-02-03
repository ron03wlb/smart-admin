package net.lab1024.sa.system.datascope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.datascope.domain.DataScopeAndViewTypeVO;
import net.lab1024.sa.system.datascope.service.DataScopeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查询支持的数据范围类型
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-03-18 20:59:17 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_DATA_SCOPE)
public class DataScopeController {

  private final DataScopeService dataScopeService;

  @Operation(summary = "获取当前系统所配置的所有数据范围 @author 罗伊")
  @GetMapping("/dataScope/list")
  public ResponseDTO<List<DataScopeAndViewTypeVO>> dataScopeList() {
    return dataScopeService.dataScopeList();
  }
}
