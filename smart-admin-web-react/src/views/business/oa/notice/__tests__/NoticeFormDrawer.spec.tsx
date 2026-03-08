/**
 * NoticeFormDrawer Tests
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import NoticeFormDrawer from '../NoticeFormDrawer';

// Mock RichTextEditor — must match actual import path in NoticeFormDrawer
vi.mock('@/components/RichTextEditor/RichTextEditor', () => ({
  default: ({ value, onChange, placeholder }: { value?: string; onChange?: (v: string) => void; placeholder?: string }) => (
    <textarea data-testid="rich-editor" value={value || ''} onChange={(e) => onChange?.(e.target.value)} placeholder={placeholder} />
  ),
}));

vi.mock('@/api/business/oa/notice-api', () => ({
  noticeApi: {
    add: vi.fn().mockResolvedValue({ code: 1 }),
    update: vi.fn().mockResolvedValue({ code: 1 }),
    getUpdateVO: vi.fn().mockResolvedValue({ code: 1, data: {} }),
  },
}));

import { noticeApi } from '@/api/business/oa/notice-api';

const mockNoticeTypes = [
  { noticeTypeId: 1, noticeTypeName: '通知' },
  { noticeTypeId: 2, noticeTypeName: '公告' },
];

describe('NoticeFormDrawer', () => {
  const onClose = vi.fn();
  const onSuccess = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  it('should render add mode', () => {
    render(
      <NoticeFormDrawer open={true} noticeTypes={mockNoticeTypes} onClose={onClose} onSuccess={onSuccess} />,
    );
    expect(screen.getByText(/新建通知公告/)).toBeDefined();
  });

  it('should render form fields', () => {
    render(
      <NoticeFormDrawer open={true} noticeTypes={mockNoticeTypes} onClose={onClose} onSuccess={onSuccess} />,
    );
    expect(screen.getByLabelText('标题')).toBeDefined();
    expect(screen.getByLabelText('作者')).toBeDefined();
  });

  it('should render rich text editor', () => {
    render(
      <NoticeFormDrawer open={true} noticeTypes={mockNoticeTypes} onClose={onClose} onSuccess={onSuccess} />,
    );
    expect(screen.getByTestId('rich-editor')).toBeDefined();
  });

  it('should render save and cancel buttons', () => {
    render(
      <NoticeFormDrawer open={true} noticeTypes={mockNoticeTypes} onClose={onClose} onSuccess={onSuccess} />,
    );
    expect(screen.getByRole('button', { name: /保\s*存/ })).toBeDefined();
    expect(screen.getByRole('button', { name: /取\s*消/ })).toBeDefined();
  });
});
