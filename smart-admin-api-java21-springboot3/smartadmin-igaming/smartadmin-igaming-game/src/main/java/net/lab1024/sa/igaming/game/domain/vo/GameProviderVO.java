package net.lab1024.sa.igaming.game.domain.vo;

import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Game provider VO — excludes sensitive API key.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class GameProviderVO {

  private Long providerId;
  private String providerCode;
  private String providerName;
  private String apiUrl;
  private String callbackUrl;
  private Boolean enabled;
  private Integer healthStatus;
  private OffsetDateTime lastHealthCheck;
  private OffsetDateTime createTime;
  private OffsetDateTime updateTime;
}
