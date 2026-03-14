/**
 * 404 Not Found Page Tests
 * 404 頁面測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import NotFoundPage from './404';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('NotFoundPage', () => {
  it('應該正確渲染 404 頁面', () => {
    render(
      <BrowserRouter>
        <NotFoundPage />
      </BrowserRouter>
    );

    expect(screen.getByText('對不起，您訪問的內容不存在！')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '返回首頁' })).toBeInTheDocument();
  });

  it('點擊返回首頁按鈕應該導航到首頁', () => {
    render(
      <BrowserRouter>
        <NotFoundPage />
      </BrowserRouter>
    );

    const homeButton = screen.getByRole('button', { name: '返回首頁' });
    fireEvent.click(homeButton);

    expect(mockNavigate).toHaveBeenCalledWith('/home');
  });
});
