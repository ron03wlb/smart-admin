package net.lab1024.sa.igaming.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalQueryForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalReviewForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskProposalVO;
import org.springframework.stereotype.Service;

/**
 * Risk proposal query and review service.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class RiskProposalService {

  private final RiskProposalDao riskProposalDao;

  @SuppressWarnings("unchecked")
  public ResponseDTO<PageResult<RiskProposalVO>> queryPage(RiskProposalQueryForm form) {
    Page<RiskProposalEntity> page =
        (Page<RiskProposalEntity>) SmartPageUtil.convert2PageQuery(form);

    LambdaQueryWrapper<RiskProposalEntity> wrapper =
        Wrappers.<RiskProposalEntity>lambdaQuery()
            .eq(form.getStatus() != null, RiskProposalEntity::getStatus, form.getStatus())
            .eq(form.getPriority() != null, RiskProposalEntity::getPriority, form.getPriority())
            .eq(form.getPlayerId() != null, RiskProposalEntity::getPlayerId, form.getPlayerId())
            .orderByDesc(RiskProposalEntity::getCreateTime);

    Page<RiskProposalEntity> result = riskProposalDao.selectPage(page, wrapper);
    PageResult<RiskProposalVO> pageResult =
        SmartPageUtil.convert2PageResult(page, result.getRecords(), RiskProposalVO.class);
    return ResponseDTO.ok(pageResult);
  }

  public ResponseDTO<String> approve(RiskProposalReviewForm form) {
    return Option.of(riskProposalDao.selectById(form.getProposalId()))
        .filter(
            e ->
                RiskProposalStatusEnum.PENDING.getValue().equals(e.getStatus())
                    || RiskProposalStatusEnum.ASSIGNED.getValue().equals(e.getStatus()))
        .map(
            entity -> {
              entity.setStatus(RiskProposalStatusEnum.APPROVED.getValue());
              entity.setReviewComment(form.getReviewComment());
              entity.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
              riskProposalDao.updateById(entity);
              return ResponseDTO.<String>ok();
            })
        .getOrElse(() -> ResponseDTO.userErrorParam("Proposal not found or already reviewed"));
  }

  public ResponseDTO<String> reject(RiskProposalReviewForm form) {
    return Option.of(riskProposalDao.selectById(form.getProposalId()))
        .filter(
            e ->
                RiskProposalStatusEnum.PENDING.getValue().equals(e.getStatus())
                    || RiskProposalStatusEnum.ASSIGNED.getValue().equals(e.getStatus()))
        .map(
            entity -> {
              entity.setStatus(RiskProposalStatusEnum.REJECTED.getValue());
              entity.setReviewComment(form.getReviewComment());
              entity.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
              riskProposalDao.updateById(entity);
              return ResponseDTO.<String>ok();
            })
        .getOrElse(() -> ResponseDTO.userErrorParam("Proposal not found or already reviewed"));
  }
}
