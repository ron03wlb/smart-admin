/**
 * Category Bar Chart Component - Sales Statistics
 *
 * Corresponds to Vue's category.vue
 */
import React from 'react';
import ReactECharts from 'echarts-for-react';
import { Card, Space } from 'antd';
import { BarChartOutlined } from '@ant-design/icons';

const CategoryChart: React.FC = () => {
  const option = {
    tooltip: {
      trigger: 'axis' as const,
      axisPointer: {
        type: 'shadow' as const,
      },
    },
    legend: {
      data: ['善逸', '胡克', '開雲', '初曉'],
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: [
      {
        type: 'category' as const,
        data: ['週一', '週二', '週三', '週四', '週五'],
      },
    ],
    yAxis: [
      {
        type: 'value' as const,
      },
    ],
    series: [
      {
        name: '善逸',
        type: 'bar',
        emphasis: { focus: 'series' as const },
        data: [320, 332, 301, 334, 390],
      },
      {
        name: '胡克',
        type: 'bar',
        emphasis: { focus: 'series' as const },
        data: [120, 132, 101, 134, 90],
      },
      {
        name: '開雲',
        type: 'bar',
        emphasis: { focus: 'series' as const },
        data: [220, 182, 191, 234, 290],
      },
      {
        name: '初曉',
        type: 'bar',
        emphasis: { focus: 'series' as const },
        data: [150, 232, 201, 154, 190],
      },
    ],
  };

  return (
    <Card
      title={
        <Space size={8}>
          <BarChartOutlined style={{ color: '#1890ff' }} />
          <span>銷量統計</span>
        </Space>
      }
      size="small"
      style={{ height: '100%' }}
    >
      <ReactECharts option={option} style={{ height: 260 }} />
    </Card>
  );
};

export default CategoryChart;
