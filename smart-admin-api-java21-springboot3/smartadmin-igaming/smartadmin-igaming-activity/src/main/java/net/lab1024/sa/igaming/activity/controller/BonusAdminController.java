package net.lab1024.sa.igaming.activity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.domain.form.BonusForfeitForm;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import net.lab1024.sa.igaming.activity.service.BonusAdminService;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bonus admin controller — admin operations for bonus lifecycle management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class BonusAdminController {

  private final BonusAdminService bonusAdminService;

  @Operation(summary = "Query bonus records with pagination (admin)")
  @PostMapping("/igaming/admin/activity/bonus/query")
  @SaCheckPermission("activity:bonus:admin")
  public ResponseDTO<PageResult<PlayerBonusRecordVO>> query(
      @RequestBody @Valid WageringProgressQueryForm form) {
    return bonusAdminService.queryBonusRecords(form);
  }

  @Operation(summary = "Get bonus record detail by ID")
  @GetMapping("/igaming/admin/activity/bonus/{recordId}")
  @SaCheckPermission("activity:bonus:admin")
  public ResponseDTO<PlayerBonusRecordVO> getById(@PathVariable Long recordId) {
    return bonusAdminService.getBonusDetail(recordId);
  }

  @Operation(summary = "Forfeit an active bonus")
  @PostMapping("/igaming/admin/activity/bonus/forfeit")
  @SaCheckPermission("activity:bonus:admin")
  public ResponseDTO<String> forfeit(@RequestBody @Valid BonusForfeitForm form) {
    return bonusAdminService.forfeitBonus(form);
  }

  @Operation(summary = "Trigger batch expiration of overdue bonuses")
  @PostMapping("/igaming/admin/activity/bonus/expire/trigger")
  @SaCheckPermission("activity:bonus:admin")
  public ResponseDTO<Integer> triggerExpire(@RequestParam(defaultValue = "100") int batchSize) {
    return bonusAdminService.triggerExpireBatch(batchSize);
  }
}
