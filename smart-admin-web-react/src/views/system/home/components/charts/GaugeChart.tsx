/**
 * Gauge Chart - 儀表盤圖表
 * 業績完成度儀表盤
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-17
 */

import ReactECharts from 'echarts-for-react';
import DefaultHomeCard from '../DefaultHomeCard';
import type { EChartsOption } from 'echarts';
import './GaugeChart.css';

export interface GaugeChartProps {
  /** 完成度百分比 (0-100) */
  percent?: number;
}

const GaugeChart: React.FC<GaugeChartProps> = ({ percent = 78 }) => {
  /**
   * 儀表盤配置
   */
  const getOption = (): EChartsOption => {
    return {
      series: [
        {
          type: 'gauge',
          startAngle: 90,
          endAngle: -270,
          pointer: {
            show: false,
          },
          progress: {
            show: true,
            overlap: false,
            roundCap: true,
            clip: false,
            itemStyle: {
              borderWidth: 1,
              borderColor: '#464646',
            },
          },
          axisLine: {
            lineStyle: {
              width: 40,
            },
          },
          splitLine: {
            show: false,
          },
          axisTick: {
            show: false,
          },
          axisLabel: {
            show: false,
          },
          data: [
            {
              value: percent,
              name: '完成度',
              title: {
                offsetCenter: ['0%', '-30%'],
              },
              detail: {
                valueAnimation: true,
                offsetCenter: ['0%', '0%'],
              },
            },
          ],
          detail: {
            fontSize: 16,
            fontWeight: 'bold',
            formatter: '{value}%',
          },
        },
      ],
    };
  };

  return (
    <DefaultHomeCard icon="DashboardOutlined" title="業績完成度">
      <div className="gauge-chart-box">
        <ReactECharts option={getOption()} style={{ width: '260px', height: '260px' }} />
      </div>
    </DefaultHomeCard>
  );
};

export default GaugeChart;
