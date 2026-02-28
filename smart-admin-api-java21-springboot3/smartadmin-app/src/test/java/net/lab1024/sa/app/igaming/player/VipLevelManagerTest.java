package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.dao.VipChangeLogDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.entity.VipChangeLogEntity;
import net.lab1024.sa.igaming.player.manager.VipLevelManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * VipLevelManager unit tests — VIP level transitions with change logging.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VipLevelManager 單元測試")
class VipLevelManagerTest {

  @Mock private PlayerDao playerDao;
  @Mock private VipChangeLogDao vipChangeLogDao;

  @InjectMocks private VipLevelManager vipLevelManager;

  @Test
  @DisplayName("更新 VIP 等級 → 驗證 playerDao.updateById + vipChangeLogDao.insert")
  void updateVipLevel_success() {
    PlayerEntity player = new PlayerEntity();
    player.setPlayerId(100L);
    player.setVipLevel(VipLevelEnum.BRONZE.getValue());

    vipLevelManager.updateVipLevel(player, VipLevelEnum.GOLD, "Monthly promotion");

    assertThat(player.getVipLevel()).isEqualTo(VipLevelEnum.GOLD.getValue());
    verify(playerDao).updateById(player);
    verify(vipChangeLogDao).insert(org.mockito.ArgumentMatchers.any(VipChangeLogEntity.class));
  }

  @Test
  @DisplayName("驗證日誌內容 → ArgumentCaptor 驗證 VipChangeLogEntity 欄位")
  void updateVipLevel_verifyLogContent() {
    PlayerEntity player = new PlayerEntity();
    player.setPlayerId(200L);
    player.setVipLevel(VipLevelEnum.SILVER.getValue());

    vipLevelManager.updateVipLevel(player, VipLevelEnum.DIAMOND, "VIP upgrade event");

    ArgumentCaptor<VipChangeLogEntity> captor = ArgumentCaptor.forClass(VipChangeLogEntity.class);
    verify(vipChangeLogDao).insert(captor.capture());

    VipChangeLogEntity logEntity = captor.getValue();
    assertThat(logEntity.getPlayerId()).isEqualTo(200L);
    assertThat(logEntity.getOldLevel()).isEqualTo(VipLevelEnum.SILVER.getValue());
    assertThat(logEntity.getNewLevel()).isEqualTo(VipLevelEnum.DIAMOND.getValue());
    assertThat(logEntity.getReason()).isEqualTo("VIP upgrade event");
  }
}
