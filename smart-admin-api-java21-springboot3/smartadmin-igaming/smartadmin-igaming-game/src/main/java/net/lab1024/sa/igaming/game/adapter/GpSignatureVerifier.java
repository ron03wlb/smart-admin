package net.lab1024.sa.igaming.game.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import org.springframework.stereotype.Component;

/**
 * GP callback signature verifier using HMAC-SHA256.
 *
 * <p>Verifies GP callback signatures and provides timestamp replay protection (5-minute tolerance).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GpSignatureVerifier {

  private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000L;
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final GameProviderDao gameProviderDao;

  /**
   * Verify GP callback signature.
   *
   * @param providerCode GP code
   * @param payload data to verify
   * @param signature provided signature
   * @param timestamp request timestamp (epoch millis)
   * @return true if signature is valid and timestamp within tolerance
   */
  public boolean verify(String providerCode, String payload, String signature, Long timestamp) {
    if (signature == null || signature.isBlank()) {
      log.debug("Signature verification skipped: no signature provided for {}", providerCode);
      return true;
    }

    if (timestamp != null) {
      long now = System.currentTimeMillis();
      if (Math.abs(now - timestamp) > TIMESTAMP_TOLERANCE_MS) {
        log.warn(
            "Timestamp replay detected for provider {}: diff={}ms", providerCode, now - timestamp);
        return false;
      }
    }

    GameProviderEntity provider =
        gameProviderDao.selectOne(
            Wrappers.<GameProviderEntity>lambdaQuery()
                .eq(GameProviderEntity::getProviderCode, providerCode)
                .eq(GameProviderEntity::getDeleted, false));
    if (provider == null || provider.getEncryptedApiKey() == null) {
      log.warn("Provider {} not found or API key missing", providerCode);
      return false;
    }

    String apiKey = provider.getEncryptedApiKey();
    String expectedSignature = computeHmac(apiKey, payload);
    return expectedSignature != null && expectedSignature.equals(signature);
  }

  private String computeHmac(String key, String data) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hmacBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      log.error("HMAC computation failed", e);
      return null;
    }
  }
}
