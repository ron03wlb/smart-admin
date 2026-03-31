/**
 * MFA API
 *
 * Corresponds to Vue's api/system/mfa-api.ts
 */
import { getRequest, postRequest } from '@/api/base/request';

export interface MfaStatus {
  mfaEnabled: boolean;
  mfaType: string;
  backupCodesGenerated: boolean;
  remainingBackupCodes: number;
  lastVerifiedAt: string | null;
  enforcedByRole: boolean;
  needRegenerateBackupCodes: boolean;
  qrCodeConfirmed: boolean;
}

export interface MfaSetupResult {
  secret: string;
  qrCodeUrl: string;
}

export interface MfaBackupCodesResult {
  backupCodes: string[];
}

export const mfaApi = {
  /** Initialize MFA setup (generate TOTP secret + QR code) */
  setupInit: () => postRequest<MfaSetupResult>('/mfa/setup/init'),

  /** Enable MFA (verify TOTP + generate backup codes) */
  setupEnable: (params: { totpToken: string; trustDevice?: boolean; deviceName?: string }) =>
    postRequest<MfaBackupCodesResult>('/mfa/setup/enable', params),

  /** Disable MFA (requires TOTP verification) */
  setupDisable: (totpToken: string) =>
    postRequest<void>(`/mfa/setup/disable?totpToken=${totpToken}`),

  /** Query MFA status */
  getStatus: () => getRequest<MfaStatus>('/mfa/status'),

  /** Verify MFA (TOTP/backup code) */
  verify: (params: { mfaToken: string; trustDevice?: boolean; deviceName?: string }) =>
    postRequest<void>('/mfa/verify', params),

  /** Regenerate backup codes (requires TOTP verification) */
  regenerateBackupCodes: (totpToken: string) =>
    postRequest<MfaBackupCodesResult>(`/mfa/backup-codes/regenerate?totpToken=${totpToken}`),

  /** Get remaining backup code count */
  getBackupCodeCount: () => getRequest<number>('/mfa/backup-codes/count'),
};
