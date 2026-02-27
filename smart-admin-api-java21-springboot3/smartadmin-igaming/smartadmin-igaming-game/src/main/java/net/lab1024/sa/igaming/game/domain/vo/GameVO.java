package net.lab1024.sa.igaming.game.domain.vo;

import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Game catalog VO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class GameVO {

  private Long gameId;
  private String gameCode;
  private String gameName;
  private Integer category;
  private String providerCode;
  private String thumbnailUrl;
  private Long playCount;
  private Boolean enabled;
  private OffsetDateTime createTime;
}
