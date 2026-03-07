/**
 * Pie Chart Component - Overtime Statistics
 *
 * Corresponds to Vue's pie.vue
 */
import React from 'react';
import ReactECharts from 'echarts-for-react';
import { Card, Space } from 'antd';
import { PieChartOutlined } from '@ant-design/icons';

const PieChart: React.FC = () => {
  const option = {
    tooltip: {
      trigger: 'item' as const,
    },
    legend: {
      top: '5%',
      left: 'center',
    },
    series: [
      {
        name: '加班統計',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold',
          },
        },
        labelLine: {
          show: false,
        },
        data: [
          { value: 10, name: '初曉' },
          { value: 8, name: '善逸' },
          { value: 3, name: '胡克' },
          { value: 1, name: '羅伊' },
        ],
      },
    ],
  };

  return (
    <Card
      title={
        <Space size={8}>
          <PieChartOutlined style={{ color: '#722ed1' }} />
          <span>加班統計</span>
        </Space>
      }
      size="small"
      style={{ height: '100%' }}
    >
      <ReactECharts option={option} style={{ height: 260 }} />
    </Card>
  );
};

export default PieChart;
