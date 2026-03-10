/**
 * SmartLoading Component Tests
 * SmartLoading 組件測試
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { store } from '@/store';
import SmartLoadingComponent from './index';
import { SmartLoading } from '@/utils/SmartLoading';

// Wrapper 組件（提供 Redux Provider）
const Wrapper = ({ children }: { children: React.ReactNode }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('SmartLoadingComponent', () => {
  beforeEach(() => {
    // 確保每次測試前 loading 狀態為 false
    SmartLoading.hide();
  });

  describe('基本渲染', () => {
    it('當 loading 為 false 時不應該渲染', () => {
      const { container } = render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      // 容器應該是空的
      expect(container.firstChild).toBeNull();
    });

    it('當 loading 為 true 時應該渲染 Spin 組件', async () => {
      render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      // 顯示 loading
      SmartLoading.show();

      // 應該顯示 Spin 組件
      await waitFor(() => {
        const loadingContainer = document.querySelector('.smart-loading-container');
        expect(loadingContainer).toBeInTheDocument();
      });

      // 應該顯示 Spin（大尺寸）
      await waitFor(() => {
        const spin = document.querySelector('.ant-spin-lg');
        expect(spin).toBeInTheDocument();
      });
    });
  });

  describe('狀態響應', () => {
    it('應該響應 loading 狀態變化（false -> true）', async () => {
      const { container } = render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      // 初始狀態：不顯示
      expect(container.firstChild).toBeNull();

      // 顯示 loading
      SmartLoading.show();

      // 應該顯示 loading
      await waitFor(() => {
        const loadingContainer = document.querySelector('.smart-loading-container');
        expect(loadingContainer).toBeInTheDocument();
      });
    });

    it('應該響應 loading 狀態變化（true -> false）', async () => {
      render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      // 先顯示 loading
      SmartLoading.show();

      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).toBeInTheDocument();
      });

      // 隱藏 loading
      SmartLoading.hide();

      // 應該隱藏 loading
      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).not.toBeInTheDocument();
      });
    });

    it('應該響應多次狀態切換', async () => {
      const { container } = render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      // 第一次顯示
      SmartLoading.show();
      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).toBeInTheDocument();
      });

      // 第一次隱藏
      SmartLoading.hide();
      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).not.toBeInTheDocument();
      });

      // 第二次顯示
      SmartLoading.show();
      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).toBeInTheDocument();
      });

      // 第二次隱藏
      SmartLoading.hide();
      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).not.toBeInTheDocument();
      });
    });
  });

  describe('Spin 配置', () => {
    it('應該使用 large 尺寸', async () => {
      render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      SmartLoading.show();

      await waitFor(() => {
        const spin = document.querySelector('.ant-spin-lg');
        expect(spin).toBeInTheDocument();
      });
    });

  });

  describe('樣式類名', () => {
    it('應該應用 smart-loading-container 類名', async () => {
      render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      SmartLoading.show();

      await waitFor(() => {
        const container = document.querySelector('.smart-loading-container');
        expect(container).toBeInTheDocument();
        expect(container).toHaveClass('smart-loading-container');
      });
    });
  });

  describe('與業務代碼集成', () => {
    it('應該支持典型的異步操作使用場景', async () => {
      render(
        <Wrapper>
          <SmartLoadingComponent />
        </Wrapper>
      );

      // 模擬異步操作
      const mockAsyncOperation = async () => {
        SmartLoading.show();
        try {
          await new Promise(resolve => setTimeout(resolve, 100));
        } finally {
          SmartLoading.hide();
        }
      };

      // 執行前：不顯示
      expect(document.querySelector('.smart-loading-container')).not.toBeInTheDocument();

      // 執行中
      const promise = mockAsyncOperation();

      // 應該顯示 loading
      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).toBeInTheDocument();
      });

      // 等待完成
      await promise;

      // 應該隱藏 loading
      await waitFor(() => {
        expect(document.querySelector('.smart-loading-container')).not.toBeInTheDocument();
      });
    });
  });
});
