package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.lab1024.sa.igaming.wallet.payment.dao.PspDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PspEntity;
import net.lab1024.sa.igaming.wallet.payment.service.PspSecretService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PspSecretService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PspSecretService 單元測試")
class PspSecretServiceTest {

  @Mock private PspDao pspDao;
  @InjectMocks private PspSecretService pspSecretService;

  @Nested
  @DisplayName("getDecryptedApiKey")
  class GetDecryptedApiKeyTest {

    @Test
    @DisplayName("成功 — 返回解密後 API key")
    @SuppressWarnings("unchecked")
    void getDecryptedApiKey_success() {
      PspEntity psp = buildPsp();
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(psp);

      String result = pspSecretService.getDecryptedApiKey("test-psp");

      assertThat(result).isEqualTo("decrypted-api-key");
    }

    @Test
    @DisplayName("PSP 不存在 — 返回 null")
    @SuppressWarnings("unchecked")
    void getDecryptedApiKey_pspNotFound() {
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      String result = pspSecretService.getDecryptedApiKey("nonexistent");

      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("getDecryptedWebhookSecret")
  class GetDecryptedWebhookSecretTest {

    @Test
    @DisplayName("成功 — 返回解密後 webhook secret")
    @SuppressWarnings("unchecked")
    void getDecryptedWebhookSecret_success() {
      PspEntity psp = buildPsp();
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(psp);

      String result = pspSecretService.getDecryptedWebhookSecret("test-psp");

      assertThat(result).isEqualTo("decrypted-webhook-secret");
    }

    @Test
    @DisplayName("PSP 不存在 — 返回 null")
    @SuppressWarnings("unchecked")
    void getDecryptedWebhookSecret_pspNotFound() {
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      String result = pspSecretService.getDecryptedWebhookSecret("nonexistent");

      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("savePspWithEncryptedSecrets")
  class SavePspTest {

    @Test
    @DisplayName("成功 — 設定密鑰 + insert")
    void savePsp_success() {
      PspEntity psp = new PspEntity();
      psp.setPspCode("new-psp");

      pspSecretService.savePspWithEncryptedSecrets(psp, "raw-key", "raw-secret");

      assertThat(psp.getApiKeyEncrypted()).isEqualTo("raw-key");
      assertThat(psp.getWebhookSecretEncrypted()).isEqualTo("raw-secret");
      verify(pspDao).insert(psp);
    }
  }

  private PspEntity buildPsp() {
    PspEntity psp = new PspEntity();
    psp.setPspId(1L);
    psp.setPspCode("test-psp");
    psp.setPspName("Test PSP");
    // TypeHandler decrypts automatically, so these are already plaintext in the entity
    psp.setApiKeyEncrypted("decrypted-api-key");
    psp.setWebhookSecretEncrypted("decrypted-webhook-secret");
    psp.setEnabled(true);
    return psp;
  }
}
