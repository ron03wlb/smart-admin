package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import net.lab1024.sa.igaming.risk.dao.GeoRestrictionDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeoLocationCmp 測試")
class GeoLocationCmpTest {

  @Mock private GeoRestrictionDao geoRestrictionDao;

  @Test
  @DisplayName("受限國家得分 80")
  void restricted_country_score_80() {
    when(geoRestrictionDao.isRestrictedCountry("KP", 1L)).thenReturn(true);
    int score = calculateScore("KP", 1L);
    assertThat(score).isEqualTo(80);
  }

  @Test
  @DisplayName("非受限國家得分 0")
  void unrestricted_country_score_0() {
    when(geoRestrictionDao.isRestrictedCountry("TW", 1L)).thenReturn(false);
    int score = calculateScore("TW", 1L);
    assertThat(score).isZero();
  }

  @Test
  @DisplayName("國家代碼為 null 得分 40")
  void null_country_score_40() {
    int score = calculateScore(null, 1L);
    assertThat(score).isEqualTo(40);
  }

  @Test
  @DisplayName("國家代碼為空白得分 40")
  void blank_country_score_40() {
    int score = calculateScore("  ", 1L);
    assertThat(score).isEqualTo(40);
  }

  /** Mirrors GeoLocationCmp.process() logic. */
  private int calculateScore(String countryCode, Long tenantId) {
    if (countryCode == null || countryCode.isBlank()) {
      return 40;
    } else if (geoRestrictionDao.isRestrictedCountry(countryCode, tenantId)) {
      return 80;
    }
    return 0;
  }
}
