/**
 * Category Chart - 柱狀圖組件
 * 銷量統計柱狀圖
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/home/components/echarts/category.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import ReactECharts from 'echarts-for-react';
import DefaultHomeCard from '../DefaultHomeCard';
import type { EChartsOption } from 'echarts';
import './CategoryChart.css';

const CategoryChart: React.FC = () => {
  /**
   * 柱狀圖配置
   */
  const getOption = (): EChartsOption => {
    return {
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五'],
      },
      yAxis: {
        type: 'value',
      },
      legend: {},
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
        },
      },
      series: [
        {
          name: '善逸',
          data: [120, 200, 150, 80, 70, 110, 130],
          type: 'bar',
          backgroundStyle: {
            color: 'rgba(180, 180, 180, 0.2)',
          },
        },
        {
          name: '胡克',
          data: [100, 80, 120, 77, 52, 22, 190],
          type: 'bar',
          backgroundStyle: {
            color: 'rgba(180, 180, 180, 0.2)',
          },
        },
        {
          name: '開雲',
          data: [200, 110, 85, 99, 120, 145, 180],
          type: 'bar',
          backgroundStyle: {
            color: 'rgba(180, 180, 180, 0.2)',
          },
        },
        {
          name: '初曉',
          data: [80, 70, 90, 110, 200, 44, 80],
          type: 'bar',
          backgroundStyle: {
            color: 'rgba(180, 180, 180, 0.2)',
          },
        },
      ],
    };
  };

  return (
    <DefaultHomeCard icon="ProfileOutlined" title="銷量統計">
      <div className="category-chart-box">
        <ReactECharts option={getOption()} style={{ width: '100%', height: '280px' }} />
      </div>
    </DefaultHomeCard>
  );
};

export default CategoryChart;
