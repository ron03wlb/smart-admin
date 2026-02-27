package net.lab1024.sa.igaming.player.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * KYC document entity — verification document records.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_kyc_document")
public class KycDocumentEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long kycDocumentId;

  private Long playerId;

  /** Document type. See {@link net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum}. */
  private Integer documentType;

  private String documentUrl;

  /**
   * Verification status. See {@link
   * net.lab1024.sa.igaming.common.constant.KycVerificationStatusEnum}.
   */
  private Integer verificationStatus;

  private String reviewerComment;
}
