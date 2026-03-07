/**
 * Gradient Area Chart Component - Code Commit Volume
 *
 * Corresponds to Vue's gradient.vue
 */
import React from 'react';
import ReactECharts from 'echarts-for-react';
import * as echarts from 'echarts/core';
import { Card, Space } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';

const GradientChart: React.FC = () => {
  const option = {
    color: ['#80FFA5', '#00DDFF', '#37A2FF', '#FF0087', '#FFBF00'],
    tooltip: {
      trigger: 'axis' as const,
      axisPointer: {
        type: 'cross' as const,
        label: {
          backgroundColor: '#6a7985',
        },
      },
    },
    legend: {
      data: ['羅伊', '佩弦', '開雲', '清野', '飛葉'],
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
        boundaryGap: false,
        data: ['週一', '週二', '週三', '週四', '週五', '週六', '週日'],
      },
    ],
    yAxis: [
      {
        type: 'value' as const,
      },
    ],
    series: [
      {
        name: '羅伊',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: { width: 0 },
        showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgb(128, 255, 165)' },
            { offset: 1, color: 'rgb(1, 191, 236)' },
          ]),
        },
        emphasis: { focus: 'series' as const },
        data: [140, 232, 101, 264, 90, 340, 250],
      },
      {
        name: '佩弦',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: { width: 0 },
        showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgb(0, 221, 255)' },
            { offset: 1, color: 'rgb(77, 119, 255)' },
          ]),
        },
        emphasis: { focus: 'series' as const },
        data: [120, 282, 111, 234, 220, 340, 310],
      },
      {
        name: '開雲',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: { width: 0 },
        showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgb(55, 162, 255)' },
            { offset: 1, color: 'rgb(116, 21, 219)' },
          ]),
        },
        emphasis: { focus: 'series' as const },
        data: [320, 132, 201, 334, 190, 130, 220],
      },
      {
        name: '清野',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: { width: 0 },
        showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgb(255, 0, 135)' },
            { offset: 1, color: 'rgb(135, 0, 157)' },
          ]),
        },
        emphasis: { focus: 'series' as const },
        data: [220, 402, 231, 134, 190, 230, 120],
      },
      {
        name: '飛葉',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: { width: 0 },
        showSymbol: false,
        areaStyle: {
          opacity: 0.8,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgb(255, 191, 0)' },
            { offset: 1, color: 'rgb(224, 62, 76)' },
          ]),
        },
        emphasis: { focus: 'series' as const },
        data: [220, 302, 181, 234, 210, 290, 150],
      },
    ],
  };

  return (
    <Card
      title={
        <Space size={8}>
          <LineChartOutlined style={{ color: '#52c41a' }} />
          <span>代碼提交量</span>
        </Space>
      }
      size="small"
    >
      <ReactECharts option={option} style={{ height: 300 }} />
    </Card>
  );
};

export default GradientChart;
