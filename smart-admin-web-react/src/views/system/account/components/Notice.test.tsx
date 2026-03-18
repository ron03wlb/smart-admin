/**
 * Notice Component Unit Tests
 * 通知公告組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Notice from './Notice';

describe('Notice', () => {
  it('should render title', () => {
    render(
      <MemoryRouter>
        <Notice />
      </MemoryRouter>
    );

    expect(screen.getByText('通知公告')).toBeInTheDocument();
  });

  it('should render navigation button', () => {
    render(
      <MemoryRouter>
        <Notice />
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: /前往公告管理/i })).toBeInTheDocument();
  });
});
