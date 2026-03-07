import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import FileUpload from '../FileUpload';

// Mock local-storage
vi.mock('@/utils/local-storage', () => ({
  LocalStorageKey: { USER_TOKEN: 'USER_TOKEN' },
  localRead: vi.fn().mockReturnValue('test-token'),
}));

describe('FileUpload', () => {
  it('should render upload component', () => {
    const { container } = render(<FileUpload />);
    expect(container.querySelector('.ant-upload')).toBeTruthy();
  });

  it('should show upload button when below maxCount', () => {
    const { container } = render(<FileUpload maxCount={3} value={[]} />);
    expect(container.querySelector('.ant-upload-select')).toBeTruthy();
  });

  it('should hide upload button when at maxCount', () => {
    const files = [
      { uid: '1', name: 'a.png', status: 'done' as const, url: '/a.png' },
      { uid: '2', name: 'b.png', status: 'done' as const, url: '/b.png' },
    ];
    const { container } = render(<FileUpload maxCount={2} value={files} />);
    // When maxCount is reached, the upload select should not be rendered
    const uploadSelect = container.querySelector('.ant-upload-select');
    // Ant Design hides the select via display:none or removes it
    expect(uploadSelect === null || (uploadSelect as HTMLElement).style.display === 'none').toBeTruthy();
  });
});
