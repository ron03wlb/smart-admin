/**
 * Mfa Component Unit Tests
 * 多因素認證組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Mfa from './Mfa';

describe('Mfa', () => {
  it('should render placeholder title', () => {
    render(<Mfa />);

    expect(screen.getByText('多因素認證')).toBeInTheDocument();
  });

  it('should render placeholder message', () => {
    render(<Mfa />);

    expect(screen.getByText(/多因素認證功能開發中/i)).toBeInTheDocument();
  });
});
