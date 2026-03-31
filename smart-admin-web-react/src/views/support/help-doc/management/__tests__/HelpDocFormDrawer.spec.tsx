/**
 * HelpDocFormDrawer Tests
 */
import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import HelpDocFormDrawer from '../HelpDocFormDrawer';

// Mock RichTextEditor — must match actual import path in HelpDocFormDrawer
vi.mock('@/components/RichTextEditor/RichTextEditor', () => ({
  default: ({ value, onChange, placeholder }: { value?: string; onChange?: (v: string) => void; placeholder?: string }) => (
    <textarea data-testid="rich-editor" value={value || ''} onChange={(e) => onChange?.(e.target.value)} placeholder={placeholder} />
  ),
}));

vi.mock('@/api/support/help-doc-api', () => ({
  helpDocApi: {
    add: vi.fn().mockResolvedValue({ code: 1 }),
    update: vi.fn().mockResolvedValue({ code: 1 }),
    getDetail: vi.fn().mockResolvedValue({ code: 1, data: {} }),
  },
  helpDocCatalogApi: {
    getAll: vi.fn().mockResolvedValue({ code: 1, data: [] }),
  },
}));

describe('HelpDocFormDrawer', () => {
  const onClose = vi.fn();
  const onSuccess = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  it('should render add mode', () => {
    render(<HelpDocFormDrawer open={true} onClose={onClose} onSuccess={onSuccess} />);
    expect(screen.getByText(/新建文档/)).toBeDefined();
  });

  it('should render form fields', () => {
    render(<HelpDocFormDrawer open={true} onClose={onClose} onSuccess={onSuccess} />);
    expect(screen.getByLabelText('标题')).toBeDefined();
    expect(screen.getByLabelText('作者')).toBeDefined();
  });

  it('should render rich text editor', () => {
    render(<HelpDocFormDrawer open={true} onClose={onClose} onSuccess={onSuccess} />);
    expect(screen.getByTestId('rich-editor')).toBeDefined();
  });

  it('should render save and cancel buttons', () => {
    render(<HelpDocFormDrawer open={true} onClose={onClose} onSuccess={onSuccess} />);
    expect(screen.getByRole('button', { name: /保\s*存/ })).toBeDefined();
    expect(screen.getByRole('button', { name: /取\s*消/ })).toBeDefined();
  });
});
