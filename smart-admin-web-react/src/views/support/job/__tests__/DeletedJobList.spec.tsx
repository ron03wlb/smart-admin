/**
 * DeletedJobList Tests
 */
import { render, screen, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import DeletedJobList from '../DeletedJobList';

// Mock JobLogListModal to avoid nested component issues
vi.mock('../JobLogListModal', () => ({
  default: () => null,
}));

vi.mock('@/api/support/job-api', () => ({
  jobApi: {
    query: vi.fn().mockResolvedValue({
      code: 1,
      data: {
        list: [
          {
            jobId: 1, jobName: '定时清理', jobClass: 'com.CleanJob',
            triggerType: 'CRON', triggerValue: '0 0 * * *', sort: 1,
            enabledFlag: false, deletedFlag: true, remark: '',
            param: '', updateTime: '', createTime: '',
          },
        ],
        total: 1,
      },
    }),
  },
}));

import { jobApi } from '@/api/support/job-api';

describe('DeletedJobList', () => {
  beforeEach(() => vi.clearAllMocks());

  it('should render list with data', async () => {
    render(<DeletedJobList />);

    await waitFor(() => {
      expect(screen.getByText('定时清理')).toBeDefined();
      expect(screen.getByText('com.CleanJob')).toBeDefined();
    });
  });

  it('should call query API on mount', async () => {
    render(<DeletedJobList />);

    await waitFor(() => {
      expect(jobApi.query).toHaveBeenCalledWith(
        expect.objectContaining({ deletedFlag: true }),
      );
    });
  });

  it('should render search input', async () => {
    render(<DeletedJobList />);
    expect(screen.getByPlaceholderText(/任务名/)).toBeDefined();
  });

  it('should display total count', async () => {
    render(<DeletedJobList />);

    await waitFor(() => {
      expect(screen.getByText(/共1条/)).toBeDefined();
    });
  });
});
