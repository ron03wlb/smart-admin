#!/usr/bin/env node

/**
 * SmartAdmin Skills Monitoring - Weekly Report Generator
 *
 * Generates weekly markdown reports from aggregated metrics.
 *
 * Usage:
 *   node generate-weekly-report.js [--week=YYYY-WW]
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
const AGGREGATED_DIR = path.join(__dirname, '../../metrics/aggregated');
const REPORTS_DIR = path.join(__dirname, '../../metrics/reports');

// Ensure reports directory exists
if (!fs.existsSync(REPORTS_DIR)) {
  fs.mkdirSync(REPORTS_DIR, { recursive: true });
}

// Main report generation function
function generateWeeklyReport() {
  // Determine week range (last 7 days from today)
  const endDate = new Date();
  const startDate = new Date(endDate);
  startDate.setDate(startDate.getDate() - 6);

  const weekLabel = formatWeekLabel(startDate, endDate);
  console.log(`📊 Generating weekly report for ${weekLabel}...`);

  // Collect daily summaries for the week
  const dailySummaries = [];
  for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
    const dateStr = d.toISOString().split('T')[0];
    const summaryFile = path.join(AGGREGATED_DIR, `${dateStr}-summary.json`);

    if (fs.existsSync(summaryFile)) {
      const summary = JSON.parse(fs.readFileSync(summaryFile, 'utf8'));
      dailySummaries.push(summary);
    }
  }

  if (dailySummaries.length === 0) {
    console.log('ℹ️  No data available for this week');
    return;
  }

  // Aggregate week-level metrics
  const weeklyMetrics = aggregateWeekly(dailySummaries);

  // Generate markdown report
  const markdown = generateMarkdownReport(weekLabel, weeklyMetrics, dailySummaries);

  // Write report file
  const reportFile = path.join(REPORTS_DIR, `weekly-${startDate.toISOString().split('T')[0]}.md`);
  fs.writeFileSync(reportFile, markdown);

  console.log(`✅ Weekly report generated: ${reportFile}`);
  console.log(`   Total invocations: ${weeklyMetrics.totalInvocations}`);
  console.log(`   Overall success rate: ${weeklyMetrics.overallSuccessRate}%`);
}

// Aggregate weekly metrics from daily summaries
function aggregateWeekly(dailySummaries) {
  const skillMetrics = {};
  let totalInvocations = 0;
  let totalCompleted = 0;
  let totalSuccess = 0;

  dailySummaries.forEach(day => {
    totalInvocations += day.totalInvocations || 0;
    totalCompleted += day.totalCompleted || 0;

    Object.entries(day.skills || {}).forEach(([skill, metrics]) => {
      if (!skillMetrics[skill]) {
        skillMetrics[skill] = {
          totalInvocations: 0,
          successCount: 0,
          failureCount: 0,
          totalDuration: 0,
          completedInvocations: 0
        };
      }

      skillMetrics[skill].totalInvocations += metrics.totalInvocations || 0;
      skillMetrics[skill].successCount += metrics.successCount || 0;
      skillMetrics[skill].failureCount += metrics.failureCount || 0;
      skillMetrics[skill].totalDuration += metrics.totalDuration || 0;
      skillMetrics[skill].completedInvocations += metrics.completedInvocations || 0;

      totalSuccess += metrics.successCount || 0;
    });
  });

  // Calculate averages
  Object.keys(skillMetrics).forEach(skill => {
    const m = skillMetrics[skill];
    m.avgDuration = m.completedInvocations > 0
      ? Math.round(m.totalDuration / m.completedInvocations)
      : 0;
    m.successRate = m.completedInvocations > 0
      ? (m.successCount / m.completedInvocations * 100).toFixed(1)
      : '0.0';
  });

  return {
    totalInvocations,
    totalCompleted,
    totalSuccess,
    overallSuccessRate: totalCompleted > 0
      ? (totalSuccess / totalCompleted * 100).toFixed(1)
      : '0.0',
    skills: skillMetrics,
    daysWithData: dailySummaries.length
  };
}

// Generate markdown report
function generateMarkdownReport(weekLabel, weeklyMetrics, dailySummaries) {
  const { totalInvocations, totalCompleted, overallSuccessRate, skills } = weeklyMetrics;

  // Sort skills by invocation count
  const sortedSkills = Object.entries(skills).sort((a, b) => b[1].totalInvocations - a[1].totalInvocations);

  let md = `# SmartAdmin Skills Usage Report\n\n`;
  md += `**Week**: ${weekLabel}\n`;
  md += `**Generated**: ${new Date().toISOString().split('T')[0]}\n\n`;

  md += `## 📊 Executive Summary\n\n`;
  md += `- **Total Invocations**: ${totalInvocations}\n`;
  md += `- **Completed**: ${totalCompleted}\n`;
  md += `- **Overall Success Rate**: ${overallSuccessRate}%\n`;
  md += `- **Most Used Skill**: ${sortedSkills[0] ? sortedSkills[0][0] : 'N/A'}\n`;
  md += `- **Days with Data**: ${weeklyMetrics.daysWithData}/7\n\n`;

  md += `## 🎯 Skill Performance\n\n`;

  sortedSkills.forEach(([skillName, metrics]) => {
    md += `### ${skillName}\n\n`;
    md += `| Metric | Value |\n`;
    md += `|--------|-------|\n`;
    md += `| Invocations | ${metrics.totalInvocations} |\n`;
    md += `| Success Rate | ${metrics.successRate}% |\n`;
    md += `| Avg Duration | ${metrics.avgDuration}s |\n`;
    md += `| Failures | ${metrics.failureCount} |\n\n`;
  });

  md += `## 📈 Daily Breakdown\n\n`;
  md += `| Date | Invocations | Success Rate |\n`;
  md += `|------|-------------|-------------|\n`;

  dailySummaries.forEach(day => {
    md += `| ${day.date} | ${day.totalInvocations || 0} | ${day.overallSuccessRate || '0.0'}% |\n`;
  });

  md += `\n## 💡 Recommendations\n\n`;

  // Generate recommendations based on metrics
  sortedSkills.forEach(([skillName, metrics]) => {
    const successRate = parseFloat(metrics.successRate);

    if (successRate < 85) {
      md += `- ⚠️ **${skillName}**: Success rate (${metrics.successRate}%) below target (85%). Review failure modes and update skill documentation.\n`;
    }

    if (metrics.failureCount > 0) {
      md += `- 🔍 **${skillName}**: ${metrics.failureCount} failure(s) detected. Investigate and add edge cases to skill.\n`;
    }
  });

  if (totalInvocations === 0) {
    md += `- ℹ️ No skill invocations this week. Skills may need better triggering keywords.\n`;
  }

  md += `\n---\n\n`;
  md += `*Generated by SmartAdmin Skills Monitoring System v1.0*\n`;

  return md;
}

// Helper: Format week label
function formatWeekLabel(start, end) {
  return `${start.toISOString().split('T')[0]} to ${end.toISOString().split('T')[0]}`;
}

// Execute
try {
  generateWeeklyReport();
} catch (error) {
  console.error('❌ Error generating weekly report:', error.message);
  console.error(error.stack);
  process.exit(1);
}
