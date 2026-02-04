package net.lab1024.sa.business.brand.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.brand.domain.form.BrandAddForm;
import net.lab1024.sa.business.brand.domain.form.BrandQueryForm;
import net.lab1024.sa.business.brand.domain.form.BrandUpdateForm;
import net.lab1024.sa.business.brand.domain.vo.BrandVO;
import net.lab1024.sa.business.brand.service.BrandService;
import net.lab1024.sa.business.constant.SwaggerTagConst;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Brand Controller
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@RestController
@Tag(name = SwaggerTagConst.Business.MANAGER_GOODS)
@RequiredArgsConstructor
public class BrandController {

  private final BrandService brandService;

  @Operation(summary = "Query brands with pagination")
  @PostMapping("/brand/query")
  public ResponseDTO<PageResult<BrandVO>> queryBrand(@RequestBody @Valid BrandQueryForm queryForm) {
    return brandService.queryBrand(queryForm);
  }

  @Operation(summary = "Add brand")
  @PostMapping("/brand/add")
  public ResponseDTO<String> addBrand(@RequestBody @Valid BrandAddForm addForm) {
    return brandService.addBrand(addForm);
  }

  @Operation(summary = "Update brand")
  @PostMapping("/brand/update")
  public ResponseDTO<String> updateBrand(@RequestBody @Valid BrandUpdateForm updateForm) {
    return brandService.updateBrand(updateForm);
  }

  @Operation(summary = "Batch delete brands")
  @PostMapping("/brand/batchDelete")
  public ResponseDTO<String> batchDelete(@RequestBody List<Long> brandIdList) {
    return brandService.batchDelete(brandIdList);
  }

  @Operation(summary = "Get brand by ID")
  @GetMapping("/brand/get/{brandId}")
  public ResponseDTO<BrandVO> getById(@PathVariable Long brandId) {
    return brandService
        .getById(brandId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("Brand does not exist"));
  }
}
