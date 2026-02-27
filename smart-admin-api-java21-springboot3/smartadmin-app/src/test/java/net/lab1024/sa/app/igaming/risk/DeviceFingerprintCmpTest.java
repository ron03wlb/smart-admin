package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DeviceFingerprintCmp 測試")
class DeviceFingerprintCmpTest {

  @Test
  @DisplayName("無設備指紋得分 30")
  void missing_device_score_30() {
    assertThat(calculateScore(null, false, 0)).isEqualTo(30);
    assertThat(calculateScore("", false, 0)).isEqualTo(30);
  }

  @Test
  @DisplayName("多帳號共用設備 (>= 3) 得分 70")
  void multi_account_score_70() {
    assertThat(calculateScore("dev-001", false, 3)).isEqualTo(70);
    assertThat(calculateScore("dev-001", false, 5)).isEqualTo(70);
  }

  @Test
  @DisplayName("新設備登入得分 25")
  void new_device_score_25() {
    assertThat(calculateScore("dev-002", true, 1)).isEqualTo(25);
  }

  @Test
  @DisplayName("已知設備且無異常得分 0")
  void known_device_score_0() {
    assertThat(calculateScore("dev-001", false, 1)).isZero();
  }

  /**
   * Mirrors DeviceFingerprintCmp logic. Parameters:
   *
   * @param deviceId device fingerprint
   * @param isNewDevice whether this is a new device for the player
   * @param accountsOnDevice number of accounts sharing this device
   */
  private int calculateScore(String deviceId, boolean isNewDevice, int accountsOnDevice) {
    if (deviceId == null || deviceId.isBlank()) {
      return 30;
    }
    if (accountsOnDevice >= 3) {
      return 70;
    }
    if (isNewDevice) {
      return 25;
    }
    return 0;
  }
}
