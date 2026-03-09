import React from 'react';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <div style={{ padding: '50px', textAlign: 'center' }}>
        <h1>SmartAdmin React - Phase 1 POC</h1>
        <p>專案初始化成功！</p>
        <p>React {React.version} + TypeScript + Vite + Ant Design 5</p>
      </div>
    </ConfigProvider>
  );
}

export default App;
