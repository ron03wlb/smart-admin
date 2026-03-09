/**
 * App Root Component
 * 應用根組件（集成路由系統）
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-09
 */

import { ConfigProvider } from 'antd';
import { RouterProvider } from 'react-router-dom';
import zhCN from 'antd/locale/zh_CN';
import { router } from './router';

/**
 * 應用根組件
 * 提供全局配置（Ant Design 中文語言包 + React Router）
 */
function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <RouterProvider router={router} />
    </ConfigProvider>
  );
}

export default App;
