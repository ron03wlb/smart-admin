#!/usr/bin/env node

/**
 * SmartAdmin Skills Monitoring - Metrics Aggregator
 *
 * Aggregates raw skill usage logs into daily summaries.
 *
 * Usage:
 *   node aggregate-metrics.js [--date=YYYY-MM-DD]
 */

const fs = require('fs');
const path = require('path');

// Parse command line arguments
const args = process.argv.slice(2).reduce((acc, arg) => {
  const [key, value] = arg.split('=');
  acc[key.replace('--', '')] = value;
  return acc;
}, {});

// Configuration
const RAW_DIR = path.join(__dirname, '../../metrics/raw');
const AGGREGATED_DIR = path.join(__dirname, '../../metrics/aggregated');

// Ensure aggregated directory exists
if (!fs.existsSync(AGGREGATED_DIR)) {
  fs.mkdirSync(AGGREGATED_DIR, { recursive: true });
}

// Main aggregation function
function aggregateMetrics() {
  const targetDate = args.date || new Date().toISOString().split('T')[0];
  const rawFile = path.join(RAW_DIR, `${targetDate}.json`);

  if (!fs.existsSync(rawFile)) {
    console.log(`ℹ️  No raw data found for ${targetDate}`);
    return;
  }

  // Read raw logs
  const rawLogs = JSON.parse(fs.readFileSync(rawFile, 'utf8'));

  // Group by skill
  const skillMetrics = {};

  rawLogs.forEach(log => {
    const { skill, event, success, duration } = log;

    if (!skillMetrics[skill]) {
      skillMetrics[skill] = {
        totalInvocations: 0,
        successCount: 0,
        failureCount: 0,
        totalDuration: 0,
        completedInvocations: 0,
        avgDuration: 0
      };
    }

    if (event === 'start') {
      skillMetrics[skill].totalInvocations++;
    }

    if (event === 'end') {
      skillMetrics[skill].completedInvocations++;

      if (success === true) {
        skillMetrics[skill].successCount++;
      } else if (success === false) {
        skillMetrics[skill].failureCount++;
      }

      if (duration) {
        skillMetrics[skill].totalDuration += duration;
      }
    }
  });

  // Calculate averages and success rates
  Object.keys(skillMetrics).forEach(skill => {
    const metrics = skillMetrics[skill];

    if (metrics.completedInvocations > 0) {
      metrics.avgDuration = Math.round(metrics.totalDuration / metrics.completedInvocations);
      metrics.successRate = (metrics.successCount / metrics.completedInvocations * 100).toFixed(1);
    } else {
      metrics.successRate = 0;
    }
  });

  // Create aggregated summary
  const summary = {
    date: targetDate,
    generatedAt: new Date().toISOString(),
    totalInvocations: Object.values(skillMetrics).reduce((sum, m) => sum + m.totalInvocations, 0),
    totalCompleted: Object.values(skillMetrics).reduce((sum, m) => sum + m.completedInvocations, 0),
    overallSuccessRate: calculateOverallSuccessRate(skillMetrics),
    skills: skillMetrics
  };

  // Write aggregated summary
  const summaryFile = path.join(AGGREGATED_DIR, `${targetDate}-summary.json`);
  fs.writeFileSync(summaryFile, JSON.stringify(summary, null, 2));

  console.log(`✅ Aggregated metrics for ${targetDate}`);
  console.log(`   Total invocations: ${summary.totalInvocations}`);
  console.log(`   Completed: ${summary.totalCompleted}`);
  console.log(`   Overall success rate: ${summary.overallSuccessRate}%`);
  console.log(`   File: ${summaryFile}`);
}

// Helper: Calculate overall success rate
function calculateOverallSuccessRate(skillMetrics) {
  const totals = Object.values(skillMetrics).reduce(
    (acc, m) => ({
      success: acc.success + m.successCount,
      completed: acc.completed + m.completedInvocations
    }),
    { success: 0, completed: 0 }
  );

  return totals.completed > 0
    ? (totals.success / totals.completed * 100).toFixed(1)
    : 0;
}

// Execute
try {
  aggregateMetrics();
} catch (error) {
  console.error('❌ Error aggregating metrics:', error.message);
  process.exit(1);
}
