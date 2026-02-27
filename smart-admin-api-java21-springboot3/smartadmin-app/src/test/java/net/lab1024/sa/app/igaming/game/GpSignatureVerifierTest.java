package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.lab1024.sa.igaming.game.adapter.GpSignatureVerifier;
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GpSignatureVerifier unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GpSignatureVerifier 單元測試")
class GpSignatureVerifierTest {

  @Mock private GameProviderDao gameProviderDao;
  @InjectMocks private GpSignatureVerifier verifier;

  @Test
  @DisplayName("無簽名 — 跳過驗證返回 true")
  void verify_noSignature_skipVerification() {
    boolean result = verifier.verify("mock", "data", null, null);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("有效簽名 — 驗證通過")
  @SuppressWarnings("unchecked")
  void verify_validSignature() throws Exception {
    String apiKey = "test-secret-key";
    String payload = "test-payload";

    GameProviderEntity provider = new GameProviderEntity();
    provider.setEncryptedApiKey(apiKey);
    when(gameProviderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(provider);

    String signature = computeHmac(apiKey, payload);
    boolean result = verifier.verify("mock", payload, signature, System.currentTimeMillis());
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("無效簽名 — 驗證失敗")
  @SuppressWarnings("unchecked")
  void verify_invalidSignature() {
    GameProviderEntity provider = new GameProviderEntity();
    provider.setEncryptedApiKey("real-key");
    when(gameProviderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(provider);

    boolean result =
        verifier.verify("mock", "payload", "wrong-signature", System.currentTimeMillis());
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("過期時間戳 — 重放保護拒絕")
  void verify_expiredTimestamp() {
    long expiredTimestamp = System.currentTimeMillis() - 10 * 60 * 1000L;
    boolean result = verifier.verify("mock", "payload", "some-sig", expiredTimestamp);
    assertThat(result).isFalse();
  }

  private String computeHmac(String key, String data) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
