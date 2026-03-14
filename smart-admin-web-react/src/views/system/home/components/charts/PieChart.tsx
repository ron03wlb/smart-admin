/**
 * Pie Chart - 餅圖組件
 * 加班統計餅圖
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/components/echarts/pie.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import ReactECharts from 'echarts-for-react';
import DefaultHomeCard from '../DefaultHomeCard';
import type { EChartsOption } from 'echarts';
import './PieChart.css';

const PieChart: React.FC = () => {
  /**
   * 餅圖配置
   */
  const getOption = (): EChartsOption => {
    return {
      tooltip: {
        trigger: 'item',
      },
      legend: {
        top: '5%',
        left: 'center',
      },
      series: [
        {
          name: '加班次數',
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
              fontSize: 40,
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
  };

  return (
    <DefaultHomeCard icon="PieChartOutlined" title="加班統計">
      <div className="pie-chart-box">
        <ReactECharts option={getOption()} style={{ width: '260px', height: '260px' }} />
      </div>
    </DefaultHomeCard>
  );
};

export default PieChart;
