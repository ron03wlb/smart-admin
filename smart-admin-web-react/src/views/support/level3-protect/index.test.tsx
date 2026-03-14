/**
 * Level 3 Protect Config Page Tests
 * 三級等保配置頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Level3ProtectPage from './index';
import { level3ProtectApi } from '@/api/support/level3ProtectApi';

// Mock level3ProtectApi
vi.mock('@/api/support/level3ProtectApi', () => ({
  level3ProtectApi: {
    getConfig: vi.fn(),
    updateConfig: vi.fn(),
  },
}));

describe('Level3ProtectPage', () => {
  const mockConfig = {
    twoFactorLoginEnabled: true,
    loginFailMaxTimes: 5,
    loginFailLockMinutes: 30,
    loginActiveTimeoutMinutes: 30,
    passwordComplexityEnabled: true,
    regularChangePasswordMonths: 3,
    regularChangePasswordNotAllowRepeatTimes: 3,
    fileDetectFlag: true,
    maxUploadFileSizeMb: 50,
  };

  beforeEach(() => {
    vi.clearAllMocks();

    // Mock default response
    vi.mocked(level3ProtectApi.getConfig).mockResolvedValue({
      code: 200,
      data: JSON.stringify(mockConfig),
      ok: true,
      msg: '',
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('應該正確渲染三級等保配置頁面', async () => {
    render(
      <BrowserRouter>
        <Level3ProtectPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('三級等保配置')).toBeInTheDocument();
    });

    expect(screen.getByText('保存配置')).toBeInTheDocument();
    expect(screen.getByText('恢復三級等保默認配置')).toBeInTheDocument();
    expect(screen.getByText('清除所有配置')).toBeInTheDocument();
  });

  it('應該在初始加載時調用 getConfig API', async () => {
    render(
      <BrowserRouter>
        <Level3ProtectPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(level3ProtectApi.getConfig).toHaveBeenCalledTimes(1);
    });
  });

  it('應該顯示配置說明 Alert', async () => {
    render(
      <BrowserRouter>
        <Level3ProtectPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/三級等保是中國國家等級保護認證/)).toBeInTheDocument();
    });
  });

  it('應該顯示所有表單項', async () => {
    render(
      <BrowserRouter>
        <Level3ProtectPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('配置雙因子登錄模式')).toBeInTheDocument();
      expect(screen.getByText('最大連續登錄失敗次數')).toBeInTheDocument();
      expect(screen.getByText('連續登錄失敗鎖定分鐘')).toBeInTheDocument();
      expect(screen.getByText('登錄後無操作自動退出的分鐘')).toBeInTheDocument();
      expect(screen.getByText('開啟密碼複雜度')).toBeInTheDocument();
      expect(screen.getByText('定期修改密碼時間間隔')).toBeInTheDocument();
      expect(screen.getByText('定期修改密碼不允許重複次數')).toBeInTheDocument();
      expect(screen.getByText('文件安全檢測')).toBeInTheDocument();
      expect(screen.getByText('上傳文件大小限制')).toBeInTheDocument();
    });
  });
});
