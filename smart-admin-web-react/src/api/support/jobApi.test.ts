/**
 * Job API Tests
 * 定時任務 API 測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import { describe, it, expect, vi } from 'vitest';
import request from '@/utils/request';
import { jobApi } from './jobApi';
import type {
  JobQueryForm,
  JobAddForm,
  JobUpdateForm,
  JobEnabledUpdateForm,
  JobExecuteForm,
} from '@/views/support/job/types';

// Mock request module
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

describe('Job API', () => {
  it('should call queryJob with correct parameters', async () => {
    const params: JobQueryForm = {
      searchWord: 'test',
      triggerType: 'CRON',
      enabledFlag: true,
      pageNum: 1,
      pageSize: 10,
    };

    await jobApi.queryJob(params);

    expect(request.post).toHaveBeenCalledWith('/support/job/query', params);
  });

  it('should call queryJobInfo with correct job ID', async () => {
    const jobId = 123;

    await jobApi.queryJobInfo(jobId);

    expect(request.get).toHaveBeenCalledWith('/support/job/123');
  });

  it('should call addJob with correct parameters', async () => {
    const params: JobAddForm = {
      jobName: 'Test Job',
      jobClass: 'com.example.TestJob',
      triggerType: 'CRON',
      triggerValue: '0 0 * * * ?',
      param: '{}',
      enabledFlag: true,
      remark: 'Test job description',
      sort: 1,
    };

    await jobApi.addJob(params);

    expect(request.post).toHaveBeenCalledWith('/support/job/add', params);
  });

  it('should call updateJob with correct parameters', async () => {
    const params: JobUpdateForm = {
      jobId: 123,
      jobName: 'Updated Job',
      jobClass: 'com.example.UpdatedJob',
      triggerType: 'FIXED_DELAY',
      triggerValue: '60',
      param: '{}',
      enabledFlag: false,
      remark: 'Updated description',
      sort: 2,
    };

    await jobApi.updateJob(params);

    expect(request.post).toHaveBeenCalledWith('/support/job/update', params);
  });

  it('should call updateJobEnabled with correct parameters', async () => {
    const params: JobEnabledUpdateForm = {
      jobId: 123,
      enabledFlag: true,
    };

    await jobApi.updateJobEnabled(params);

    expect(request.post).toHaveBeenCalledWith('/support/job/update/enabled', params);
  });

  it('should call executeJob with correct parameters', async () => {
    const params: JobExecuteForm = {
      jobId: 123,
    };

    await jobApi.executeJob(params);

    expect(request.post).toHaveBeenCalledWith('/support/job/execute', params);
  });

  it('should call deleteJob with correct job ID', async () => {
    const jobId = 123;

    await jobApi.deleteJob(jobId);

    expect(request.get).toHaveBeenCalledWith('/support/job/delete?jobId=123');
  });
});
