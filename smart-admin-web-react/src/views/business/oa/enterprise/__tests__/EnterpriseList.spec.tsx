import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import EnterpriseList from '../EnterpriseList';
import { enterpriseApi } from '@/api/business/oa/enterprise-api';

vi.mock('@/api/business/oa/enterprise-api');

const mockEnterprises = [
  { enterpriseId: 1, enterpriseName: '测试企业A', unifiedSocialCreditCode: '91440300MA5FAKE01', contactName: '张三', contactPhone: '13800138001', disabledFlag: false, createTime: '2025-01-01' },
  { enterpriseId: 2, enterpriseName: '测试企业B', unifiedSocialCreditCode: '91440300MA5FAKE02', contactName: '李四', contactPhone: '13800138002', disabledFlag: true, createTime: '2025-01-02' },
];

describe('EnterpriseList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(enterpriseApi.pageQuery).mockResolvedValue({
      code: 1,
      data: { list: mockEnterprises, total: 2 },
      success: true,
    });
  });

  it('should display enterprise list on mount', async () => {
    renderWithProviders(<EnterpriseList />);

    await waitFor(() => {
      expect(screen.getByText('测试企业A')).toBeInTheDocument();
      expect(screen.getByText('测试企业B')).toBeInTheDocument();
    });

    expect(enterpriseApi.pageQuery).toHaveBeenCalled();
  });

  it('should display status tags', async () => {
    renderWithProviders(<EnterpriseList />);

    await waitFor(() => {
      expect(screen.getByText('正常')).toBeInTheDocument();
      expect(screen.getByText('禁用')).toBeInTheDocument();
    });
  });

  it('should open add modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<EnterpriseList />);

    await waitFor(() => {
      expect(screen.getByText('测试企业A')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /新\s*建/ });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('新建企业')).toBeInTheDocument();
    });
  });
});
