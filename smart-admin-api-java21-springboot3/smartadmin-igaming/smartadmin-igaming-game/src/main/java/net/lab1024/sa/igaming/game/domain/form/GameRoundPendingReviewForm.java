package net.lab1024.sa.igaming.game.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for marking a game round as pending review.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Data
public class GameRoundPendingReviewForm {

  @Schema(description = "Game round ID")
  @NotNull
  private Long roundId;

  @Schema(description = "Reason for marking as pending review")
  @NotBlank
  @Size(max = 256)
  private String reason;
}
