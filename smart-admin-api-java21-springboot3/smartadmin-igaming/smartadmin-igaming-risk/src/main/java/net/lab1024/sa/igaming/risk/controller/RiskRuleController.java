package net.lab1024.sa.igaming.risk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleAddForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleQueryForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleUpdateForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskRuleVO;
import net.lab1024.sa.igaming.risk.service.RiskRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk rule admin controller.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.RISK)
@RequiredArgsConstructor
public class RiskRuleController {

  private final RiskRuleService riskRuleService;

  @Operation(summary = "Query risk rules with pagination")
  @PostMapping("/igaming/risk/rule/query")
  @SaCheckPermission("risk:rule:query")
  public ResponseDTO<PageResult<RiskRuleVO>> queryPage(@RequestBody @Valid RiskRuleQueryForm form) {
    return riskRuleService.queryPage(form);
  }

  @Operation(summary = "Get risk rule by ID")
  @GetMapping("/igaming/risk/rule/get/{ruleParamId}")
  @SaCheckPermission("risk:rule:query")
  public ResponseDTO<RiskRuleVO> getById(@PathVariable Long ruleParamId) {
    return riskRuleService.getById(ruleParamId);
  }

  @Operation(summary = "Add a new risk rule")
  @PostMapping("/igaming/risk/rule/add")
  @SaCheckPermission("risk:rule:add")
  public ResponseDTO<String> add(@RequestBody @Valid RiskRuleAddForm form) {
    return riskRuleService.add(form);
  }

  @Operation(summary = "Update an existing risk rule")
  @PutMapping("/igaming/risk/rule/update")
  @SaCheckPermission("risk:rule:update")
  public ResponseDTO<String> update(@RequestBody @Valid RiskRuleUpdateForm form) {
    return riskRuleService.update(form);
  }

  @Operation(summary = "Delete a risk rule (soft delete)")
  @PostMapping("/igaming/risk/rule/delete/{ruleParamId}")
  @SaCheckPermission("risk:rule:delete")
  public ResponseDTO<String> delete(@PathVariable Long ruleParamId) {
    return riskRuleService.delete(ruleParamId);
  }
}
