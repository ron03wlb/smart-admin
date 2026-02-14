#!/usr/bin/env node

/**
 * Ralph Loop - Rest Strategy Calculator (Node.js version)
 *
 * Calculates optimal rest duration based on risk score.
 *
 * Usage:
 *   node rest-strategy.js
 *   node rest-strategy.js --quota-file=/path/to/quota.json
 *
 * Output: Number of minutes to rest (stdout)
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
const SCRIPT_DIR = __dirname;
const QUOTA_FILE = args['quota-file'] || path.join(SCRIPT_DIR, '../quota.json');
const THRESHOLDS_FILE = args['thresholds-file'] || path.join(SCRIPT_DIR, '../thresholds.yml');
const FORCE_RISK_SCORE = args['force-risk-score'] ? parseFloat(args['force-risk-score']) : null;

// Default rest strategy
const DEFAULT_REST_STRATEGY = {
  moderate: { min_minutes: 5, max_minutes: 10 },
  high: { min_minutes: 15, max_minutes: 30 },
  critical: { min_minutes: 45, max_minutes: 60 }
};

/**
 * Read risk score from quota.json
 */
function getRiskScore() {
  if (FORCE_RISK_SCORE !== null) {
    return FORCE_RISK_SCORE;
  }

  if (!fs.existsSync(QUOTA_FILE)) {
    return 0.5; // Default moderate risk
  }

  try {
    const content = fs.readFileSync(QUOTA_FILE, 'utf8');
    const quota = JSON.parse(content);
    return quota.risk_score || 0.5;
  } catch (err) {
    console.error(`⚠️ Warning: Could not read risk score: ${err.message}`, '\n');
    return 0.5;
  }
}

/**
 * Read rest strategy from thresholds.yml
 */
function getRestStrategy() {
  if (!fs.existsSync(THRESHOLDS_FILE)) {
    return DEFAULT_REST_STRATEGY;
  }

  try {
    const content = fs.readFileSync(THRESHOLDS_FILE, 'utf8');

    // Simple YAML parsing for rest_strategy section
    const moderateMatch = content.match(/moderate:\s*\n\s+min_minutes:\s*(\d+)\s*\n\s+max_minutes:\s*(\d+)/m);
    const highMatch = content.match(/high:\s*\n\s+min_minutes:\s*(\d+)\s*\n\s+max_minutes:\s*(\d+)/m);
    const criticalMatch = content.match(/critical:\s*\n\s+min_minutes:\s*(\d+)\s*\n\s+max_minutes:\s*(\d+)/m);

    return {
      moderate: moderateMatch ? { min_minutes: parseInt(moderateMatch[1], 10), max_minutes: parseInt(moderateMatch[2], 10) } : DEFAULT_REST_STRATEGY.moderate,
      high: highMatch ? { min_minutes: parseInt(highMatch[1], 10), max_minutes: parseInt(highMatch[2], 10) } : DEFAULT_REST_STRATEGY.high,
      critical: criticalMatch ? { min_minutes: parseInt(criticalMatch[1], 10), max_minutes: parseInt(criticalMatch[2], 10) } : DEFAULT_REST_STRATEGY.critical
    };
  } catch (err) {
    console.error(`⚠️ Warning: Could not read thresholds.yml: ${err.message}`, '\n');
    return DEFAULT_REST_STRATEGY;
  }
}

/**
 * Calculate rest duration based on risk score
 *
 * Using linear interpolation within each risk band:
 * - Low (0.0-0.3): 0 minutes
 * - Moderate (0.3-0.6): 5-10 minutes (linear)
 * - High (0.6-0.8): 15-30 minutes (linear)
 * - Critical (0.8-1.0): 45-60 minutes (linear)
 */
function calculateRestDuration(riskScore, restStrategy) {
  if (riskScore < 0.3) {
    // Low risk: no rest needed
    return 0;
  } else if (riskScore < 0.6) {
    // Moderate risk (0.3-0.6): linear interpolation
    const { min_minutes, max_minutes } = restStrategy.moderate;
    const range = max_minutes - min_minutes;
    const scoreOffset = riskScore - 0.3;
    return Math.round(min_minutes + (scoreOffset / 0.3) * range);
  } else if (riskScore < 0.8) {
    // High risk (0.6-0.8): linear interpolation
    const { min_minutes, max_minutes } = restStrategy.high;
    const range = max_minutes - min_minutes;
    const scoreOffset = riskScore - 0.6;
    return Math.round(min_minutes + (scoreOffset / 0.2) * range);
  } else {
    // Critical risk (0.8-1.0): linear interpolation
    const { min_minutes, max_minutes } = restStrategy.critical;
    const range = max_minutes - min_minutes;
    const scoreOffset = riskScore - 0.8;
    const restMinutes = Math.round(min_minutes + (scoreOffset / 0.2) * range);

    // Cap at maximum
    return Math.min(restMinutes, max_minutes);
  }
}

/**
 * Main function
 */
function main() {
  const riskScore = getRiskScore();
  const restStrategy = getRestStrategy();
  const restDuration = calculateRestDuration(riskScore, restStrategy);

  console.log(restDuration);
}

// Execute
try {
  main();
} catch (err) {
  console.error('❌ Error calculating rest duration:', err.message);
  process.exit(1);
}
