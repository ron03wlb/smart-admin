/**
 * JobFormModal Tests
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import JobFormModal from '../JobFormModal';

vi.mock('@/api/support/job-api', () => ({
  jobApi: {
    add: vi.fn().mockResolvedValue({ code: 1 }),
    update: vi.fn().mockResolvedValue({ code: 1 }),
  },
}));

import { jobApi } from '@/api/support/job-api';

describe('JobFormModal', () => {
  const onCancel = vi.fn();
  const onSuccess = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  it('should render add mode', () => {
    render(<JobFormModal open={true} onCancel={onCancel} onSuccess={onSuccess} />);
    expect(screen.getByText(/添加任务/)).toBeDefined();
  });

  it('should render edit mode with job data', () => {
    const job = {
      jobId: 1, jobName: 'TestJob', jobClass: 'com.TestJob', triggerType: 'CRON',
      triggerValue: '0 0 * * *', sort: 1, enabledFlag: true, remark: 'test',
      param: '', updateTime: '', createTime: '', deletedFlag: false,
    };
    render(<JobFormModal open={true} job={job} onCancel={onCancel} onSuccess={onSuccess} />);
    expect(screen.getByDisplayValue('TestJob')).toBeDefined();
    expect(screen.getByDisplayValue('com.TestJob')).toBeDefined();
  });

  it('should call add API for new job', async () => {
    render(<JobFormModal open={true} onCancel={onCancel} onSuccess={onSuccess} />);

    fireEvent.change(screen.getByLabelText('任务名称'), { target: { value: 'NewJob' } });
    fireEvent.change(screen.getByLabelText('任务类'), { target: { value: 'com.NewJob' } });
    fireEvent.change(screen.getByLabelText('触发配置'), { target: { value: '0 0 * * *' } });

    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(jobApi.add).toHaveBeenCalledWith(
        expect.objectContaining({ jobName: 'NewJob', jobClass: 'com.NewJob' }),
      );
    }, { timeout: 5000 });
  });

  it('should call update API for existing job', async () => {
    const job = {
      jobId: 1, jobName: 'OldJob', jobClass: 'com.OldJob', triggerType: 'CRON',
      triggerValue: '0 0 * * *', sort: 1, enabledFlag: true, remark: '',
      param: '', updateTime: '', createTime: '', deletedFlag: false,
    };
    render(<JobFormModal open={true} job={job} onCancel={onCancel} onSuccess={onSuccess} />);

    fireEvent.change(screen.getByDisplayValue('OldJob'), { target: { value: 'UpdatedJob' } });
    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(jobApi.update).toHaveBeenCalledWith(
        expect.objectContaining({ jobId: 1, jobName: 'UpdatedJob' }),
      );
    }, { timeout: 5000 });
  });
});
