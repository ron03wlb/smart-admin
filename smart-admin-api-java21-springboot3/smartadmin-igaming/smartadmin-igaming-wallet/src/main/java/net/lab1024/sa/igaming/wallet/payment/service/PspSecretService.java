package net.lab1024.sa.igaming.wallet.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.wallet.payment.dao.PspDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PspEntity;
import org.springframework.stereotype.Service;

/**
 * PSP secret lookup service.
 *
 * <p>Retrieves PSP API keys and webhook secrets. Decryption is handled transparently by {@link
 * net.lab1024.sa.common.security.encrypt.EncryptedFieldTypeHandler} on PspEntity fields.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PspSecretService {

  private final PspDao pspDao;

  /**
   * Get decrypted API key for a PSP.
   *
   * @param pspCode unique PSP identifier
   * @return decrypted API key, or null if PSP not found
   */
  public String getDecryptedApiKey(String pspCode) {
    PspEntity psp = findByCode(pspCode);
    if (psp == null) {
      log.warn("[PspSecret] PSP not found: pspCode={}", pspCode);
      return null;
    }
    return psp.getApiKeyEncrypted();
  }

  /**
   * Get decrypted webhook secret for a PSP.
   *
   * @param pspCode unique PSP identifier
   * @return decrypted webhook secret, or null if PSP not found
   */
  public String getDecryptedWebhookSecret(String pspCode) {
    PspEntity psp = findByCode(pspCode);
    if (psp == null) {
      log.warn("[PspSecret] PSP not found: pspCode={}", pspCode);
      return null;
    }
    return psp.getWebhookSecretEncrypted();
  }

  /**
   * Save PSP with plaintext secrets (TypeHandler encrypts on DB write).
   *
   * @param psp PSP entity to save
   * @param rawApiKey plaintext API key
   * @param rawWebhookSecret plaintext webhook secret
   */
  public void savePspWithEncryptedSecrets(
      PspEntity psp, String rawApiKey, String rawWebhookSecret) {
    psp.setApiKeyEncrypted(rawApiKey);
    psp.setWebhookSecretEncrypted(rawWebhookSecret);
    pspDao.insert(psp);
    log.info("[PspSecret] PSP saved with encrypted secrets: pspCode={}", psp.getPspCode());
  }

  private PspEntity findByCode(String pspCode) {
    return pspDao.selectOne(Wrappers.<PspEntity>lambdaQuery().eq(PspEntity::getPspCode, pspCode));
  }
}
