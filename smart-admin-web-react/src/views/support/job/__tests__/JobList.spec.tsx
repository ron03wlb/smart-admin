import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { renderWithProviders } from '@/test/utils/test-utils';
import JobList from '../JobList';
import { jobApi } from '@/api/support/job-api';

vi.mock('@/api/support/job-api');

const mockJobs = [
  { jobId: 1, jobName: '清理日志', jobClass: 'net.lab1024.CleanLogJob', triggerType: 'CRON', triggerValue: '0 0 2 * * ?', enabledFlag: true, sort: 0, lastJobLog: { successFlag: true } },
  { jobId: 2, jobName: '同步数据', jobClass: 'net.lab1024.SyncDataJob', triggerType: 'FIXED_DELAY', triggerValue: '60', enabledFlag: false, sort: 1, lastJobLog: null },
];

describe('JobList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(jobApi.query).mockResolvedValue({
      code: 1,
      data: { list: mockJobs, total: 2 },
      success: true,
    });
    vi.mocked(jobApi.queryLog).mockResolvedValue({
      code: 1,
      data: { list: [], total: 0 },
      success: true,
    });
  });

  it('should display job list on mount', async () => {
    renderWithProviders(<JobList />);

    await waitFor(() => {
      expect(screen.getByText('清理日志')).toBeInTheDocument();
      expect(screen.getByText('同步数据')).toBeInTheDocument();
    });

    expect(jobApi.query).toHaveBeenCalled();
  });

  it('should display trigger type tags', async () => {
    renderWithProviders(<JobList />);

    await waitFor(() => {
      expect(screen.getByText('CRON')).toBeInTheDocument();
      expect(screen.getByText('固定延迟')).toBeInTheDocument();
    });
  });

  it('should open add modal when add button clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<JobList />);

    await waitFor(() => {
      expect(screen.getByText('清理日志')).toBeInTheDocument();
    });

    const addButton = screen.getByRole('button', { name: /新\s*建/ });
    await user.click(addButton);

    await waitFor(() => {
      expect(screen.getByText('添加任务')).toBeInTheDocument();
    });
  });
});
