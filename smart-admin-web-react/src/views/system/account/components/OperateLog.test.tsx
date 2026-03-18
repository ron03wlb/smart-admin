/**
 * OperateLog Component Unit Tests
 * 操作日誌組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OperateLog from './OperateLog';

describe('OperateLog', () => {
  it('should render title', () => {
    render(
      <MemoryRouter>
        <OperateLog />
      </MemoryRouter>
    );

    expect(screen.getByText('操作日誌')).toBeInTheDocument();
  });

  it('should render navigation button', () => {
    render(
      <MemoryRouter>
        <OperateLog />
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: /前往操作日誌/i })).toBeInTheDocument();
  });
});
