/**
 * Message Component Unit Tests
 * 我的消息組件單元測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Message from './Message';

describe('Message', () => {
  it('should render title', () => {
    render(
      <MemoryRouter>
        <Message />
      </MemoryRouter>
    );

    expect(screen.getByText('我的消息')).toBeInTheDocument();
  });

  it('should render navigation button', () => {
    render(
      <MemoryRouter>
        <Message />
      </MemoryRouter>
    );

    expect(screen.getByRole('button', { name: /前往消息管理/i })).toBeInTheDocument();
  });
});
