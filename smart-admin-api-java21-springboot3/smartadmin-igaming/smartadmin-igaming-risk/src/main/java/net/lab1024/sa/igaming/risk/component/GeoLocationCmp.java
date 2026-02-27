package net.lab1024.sa.igaming.risk.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.risk.dao.GeoRestrictionDao;
import net.lab1024.sa.igaming.risk.domain.RiskContext;

/**
 * Geo-location component — checks IP/location against restricted jurisdictions.
 *
 * <p>Flags requests from restricted countries. Score range: 0-80.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@LiteflowComponent("geoLocation")
@RequiredArgsConstructor
public class GeoLocationCmp extends NodeComponent {

  private static final String COMPONENT_ID = "geoLocation";

  private final GeoRestrictionDao geoRestrictionDao;

  @Override
  public void process() throws Exception {
    RiskContext ctx = this.getContextBean(RiskContext.class);
    String countryCode = ctx.getCountryCode();
    Long tenantId = ctx.getTenantId();

    int score = 0;

    if (countryCode == null || countryCode.isBlank()) {
      // Unable to determine location is suspicious
      score = 40;
    } else if (geoRestrictionDao.isRestrictedCountry(countryCode, tenantId)) {
      score = 80;
    }

    log.debug(
        "GeoLocation: playerId={}, countryCode={}, score={}",
        ctx.getPlayerId(),
        countryCode,
        score);
    ctx.addRuleScore(COMPONENT_ID, score);
  }
}
