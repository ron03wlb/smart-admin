/**
 * LoginLog Component Unit Tests
 * 登錄日誌組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LoginLog from './LoginLog';

describe('LoginLog', () => {
  it('should render title', () => {
    render(
      <MemoryRouter>
        <LoginLog />
      </MemoryRouter>
    );

    expect(screen.getByText('登錄日誌')).toBeInTheDocument();
  });

  it('should render navigation button', () => {
    render(
      <MemoryRouter>
        <LoginLog />
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: /前往登錄日誌/i })).toBeInTheDocument();
  });
});
