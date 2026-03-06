/**
 * App 根組件
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import { Provider } from 'react-redux';
import { PersistGate } from 'redux-persist/integration/react';
import { RouterProvider } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { store, persistor } from '@/store';
import { router } from '@/router';
import { ErrorBoundary } from '@/components/framework/error';
import './App.css';

export default function App() {
  return (
    <Provider store={store}>
      <PersistGate loading={null} persistor={persistor}>
        <ConfigProvider locale={zhCN}>
          <ErrorBoundary>
            <RouterProvider router={router} />
          </ErrorBoundary>
        </ConfigProvider>
      </PersistGate>
    </Provider>
  );
}
