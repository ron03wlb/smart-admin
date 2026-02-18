package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.lab1024.sa.igaming.wallet.payment.security.CallbackSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CallbackSignatureVerifier unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@DisplayName("CallbackSignatureVerifier 單元測試")
class CallbackSignatureVerifierTest {

  private CallbackSignatureVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new CallbackSignatureVerifier();
  }

  @Test
  @DisplayName("正確簽名通過驗證")
  void verify_validSignature() throws Exception {
    String secret = "test-webhook-secret";
    String payload = "{\"order_id\":\"PAY_001\",\"status\":\"SUCCESS\"}";
    String validSignature = computeHmac(secret, payload);
    long now = System.currentTimeMillis();

    assertThat(verifier.verify(secret, payload, validSignature, now)).isTrue();
  }

  @Test
  @DisplayName("錯誤簽名不通過")
  void verify_invalidSignature() {
    String secret = "test-webhook-secret";
    String payload = "{\"order_id\":\"PAY_001\",\"status\":\"SUCCESS\"}";
    long now = System.currentTimeMillis();

    assertThat(verifier.verify(secret, payload, "invalid_signature_hex", now)).isFalse();
  }

  @Test
  @DisplayName("過期時間戳不通過 (重放攻擊防護)")
  void verify_expiredTimestamp() throws Exception {
    String secret = "test-webhook-secret";
    String payload = "{\"order_id\":\"PAY_001\"}";
    String validSignature = computeHmac(secret, payload);
    long expiredTimestamp = System.currentTimeMillis() - (10 * 60 * 1000); // 10 min ago

    assertThat(verifier.verify(secret, payload, validSignature, expiredTimestamp)).isFalse();
  }

  @Test
  @DisplayName("篡改 payload 不通過")
  void verify_tamperedPayload() throws Exception {
    String secret = "test-webhook-secret";
    String originalPayload = "{\"order_id\":\"PAY_001\",\"amount\":100}";
    String tamperedPayload = "{\"order_id\":\"PAY_001\",\"amount\":999}";
    String signature = computeHmac(secret, originalPayload);
    long now = System.currentTimeMillis();

    assertThat(verifier.verify(secret, tamperedPayload, signature, now)).isFalse();
  }

  private String computeHmac(String secret, String payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec =
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(keySpec);
    byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
    for (byte b : hmacBytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
