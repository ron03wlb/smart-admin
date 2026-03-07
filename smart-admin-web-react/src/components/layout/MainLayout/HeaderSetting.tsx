/**
 * Header Setting Drawer
 *
 * Corresponds to Vue's header-setting.vue (375 lines)
 * Provides UI configuration: layout, theme, dark mode, language, etc.
 */
import React from 'react';
import { Drawer, Switch, Select, Slider, Divider, Space, Tag } from 'antd';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import {
  updateAppConfig,
  toggleDarkMode,
  resetAppConfig,
  selectLayout,
  selectDarkMode,
  selectLanguage,
  selectPageTagFlag,
  selectBreadCrumbFlag,
  selectFooterFlag,
  selectWatermarkFlag,
  selectCompactFlag,
} from '@/store/slices/appConfigSlice';
import { THEME_COLORS } from '@/theme/colors';

interface HeaderSettingProps {
  visible: boolean;
  onClose: () => void;
}

const HeaderSetting: React.FC<HeaderSettingProps> = ({ visible, onClose }) => {
  const dispatch = useAppDispatch();
  const layout = useAppSelector(selectLayout);
  const darkMode = useAppSelector(selectDarkMode);
  const language = useAppSelector(selectLanguage);
  const pageTagFlag = useAppSelector(selectPageTagFlag);
  const breadCrumbFlag = useAppSelector(selectBreadCrumbFlag);
  const footerFlag = useAppSelector(selectFooterFlag);
  const watermarkFlag = useAppSelector(selectWatermarkFlag);
  const compactFlag = useAppSelector(selectCompactFlag);
  const colorIndex = useAppSelector((s) => s.appConfig.colorIndex);
  const borderRadius = useAppSelector((s) => s.appConfig.borderRadius);

  return (
    <Drawer title="系统设置" open={visible} onClose={onClose} width={340}>
      {/* Theme Color */}
      <Divider orientation="left">主题颜色</Divider>
      <Space wrap>
        {THEME_COLORS.map((color, idx) => (
          <Tag
            key={color}
            color={color}
            style={{
              cursor: 'pointer',
              border: idx === colorIndex ? '2px solid #000' : '2px solid transparent',
              width: 28,
              height: 28,
            }}
            onClick={() => dispatch(updateAppConfig({ colorIndex: idx }))}
          />
        ))}
      </Space>

      {/* Border Radius */}
      <Divider orientation="left">圆角</Divider>
      <Slider
        min={0}
        max={16}
        value={borderRadius}
        onChange={(v) => dispatch(updateAppConfig({ borderRadius: v }))}
      />

      {/* Layout */}
      <Divider orientation="left">布局</Divider>
      <Select
        value={layout}
        onChange={(v) => dispatch(updateAppConfig({ layout: v }))}
        style={{ width: '100%' }}
        options={[
          { label: '传统', value: 'side' },
          { label: '展开', value: 'side-expand' },
          { label: '顶部', value: 'top' },
          { label: '分组', value: 'top-expand' },
        ]}
      />

      {/* Language */}
      <Divider orientation="left">语言</Divider>
      <Select
        value={language}
        onChange={(v) => dispatch(updateAppConfig({ language: v }))}
        style={{ width: '100%' }}
        options={[
          { label: '中文', value: 'zh_CN' },
          { label: 'English', value: 'en_US' },
        ]}
      />

      {/* Switches */}
      <Divider orientation="left">功能开关</Divider>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>暗黑模式</span>
          <Switch checked={darkMode} onChange={() => dispatch(toggleDarkMode())} />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>紧凑模式</span>
          <Switch
            checked={compactFlag}
            onChange={(v) => dispatch(updateAppConfig({ compactFlag: v }))}
          />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>标签页</span>
          <Switch
            checked={pageTagFlag}
            onChange={(v) => dispatch(updateAppConfig({ pageTagFlag: v }))}
          />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>面包屑</span>
          <Switch
            checked={breadCrumbFlag}
            onChange={(v) => dispatch(updateAppConfig({ breadCrumbFlag: v }))}
          />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>页脚</span>
          <Switch
            checked={footerFlag}
            onChange={(v) => dispatch(updateAppConfig({ footerFlag: v }))}
          />
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>水印</span>
          <Switch
            checked={watermarkFlag}
            onChange={(v) => dispatch(updateAppConfig({ watermarkFlag: v }))}
          />
        </div>
      </div>

      {/* Reset */}
      <Divider />
      <a onClick={() => dispatch(resetAppConfig())} style={{ color: '#ff4d4f' }}>
        重置所有配置
      </a>
    </Drawer>
  );
};

export default HeaderSetting;
