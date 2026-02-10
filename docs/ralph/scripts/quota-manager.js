#!/usr/bin/env node

/**
 * Ralph Loop - Quota Manager (Node.js version)
 *
 * Aggregates token usage from the last 30 minutes and calculates quota status.
 *
 * Usage:
 *   node quota-manager.js > ../quota.json
 *   node quota-manager.js --window-minutes=60 --metrics-dir=/path/to/metrics
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
const METRICS_DIR = args['metrics-dir'] || path.join(SCRIPT_DIR, '../metrics');
const THRESHOLDS_FILE = path.join(SCRIPT_DIR, '../thresholds.yml');
const WINDOW_MINUTES = parseInt(args['window-minutes'] || '30', 10);

// Default tier limits (tier-4)
const DEFAULT_TIER = 'tier-4';
const DEFAULT_INPUT_TPM = 400000;

/**
 * Read tier configuration from thresholds.yml
 */
function readTierConfig() {
  if (!fs.existsSync(THRESHOLDS_FILE)) {
    return { tier: DEFAULT_TIER, input_tpm: DEFAULT_INPUT_TPM };
  }

  try {
    const content = fs.readFileSync(THRESHOLDS_FILE, 'utf8');

    // Simple YAML parsing (for basic key-value pairs)
    const tierMatch = content.match(/api_tier:\s*"?([^"\n]+)"?/);
    const tier = tierMatch ? tierMatch[1] : DEFAULT_TIER;

    // Find tier limits section
    const tierRegex = new RegExp(`${tier}:\\s*\\n\\s+input_tpm:\\s*(\\d+)`, 'm');
    const limitsMatch = content.match(tierRegex);
    const input_tpm = limitsMatch ? parseInt(limitsMatch[1], 10) : DEFAULT_INPUT_TPM;

    return { tier, input_tpm };
  } catch (err) {
    console.error(`⚠️ Warning: Could not read thresholds.yml, using defaults: ${err.message}`, '\n');
    return { tier: DEFAULT_TIER, input_tpm: DEFAULT_INPUT_TPM };
  }
}

/**
 * Find metrics files modified within the time window
 */
function findRecentMetricsFiles() {
  if (!fs.existsSync(METRICS_DIR)) {
    return [];
  }

  const now = Date.now();
  const windowStart = now - (WINDOW_MINUTES * 60 * 1000);

  const files = fs.readdirSync(METRICS_DIR)
    .filter(file => file.endsWith('.json'))
    .map(file => path.join(METRICS_DIR, file))
    .filter(filePath => {
      try {
        const stats = fs.statSync(filePath);
        return stats.mtimeMs >= windowStart;
      } catch (err) {
        return false;
      }
    });

  return files;
}

/**
 * Aggregate data from metrics files
 */
function aggregateMetrics(files) {
  const aggregated = {
    input_tokens: 0,
    output_tokens: 0,
    cached_tokens: 0,
    effective_tokens: 0,
    api_calls: 0,
    success_count: 0,
    failure_count: 0
  };

  for (const file of files) {
    try {
      const content = fs.readFileSync(file, 'utf8');
      const logs = JSON.parse(content);

      for (const log of logs) {
        aggregated.input_tokens += log.tokens?.input || 0;
        aggregated.output_tokens += log.tokens?.output || 0;
        aggregated.cached_tokens += log.tokens?.cached || 0;
        aggregated.api_calls += log.api_calls || 1;

        if (log.success === true) {
          aggregated.success_count++;
        } else if (log.success === false) {
          aggregated.failure_count++;
        }
      }
    } catch (err) {
      console.error(`⚠️ Warning: Could not read ${file}: ${err.message}`, '\n');
    }
  }

  // Calculate effective tokens (cached discount: 90%)
  aggregated.effective_tokens = aggregated.input_tokens + aggregated.output_tokens -
    Math.floor(aggregated.cached_tokens * 0.9);

  return aggregated;
}

/**
 * Calculate risk score
 */
function calculateRiskScore(aggregated, tierConfig, windowMinutes) {
  const { input_tpm } = tierConfig;

  // Calculate usage percentages
  const maxTokensInWindow = input_tpm * windowMinutes;
  const tokenUsagePct = (aggregated.input_tokens * 100.0) / maxTokensInWindow;

  const totalCalls = aggregated.success_count + aggregated.failure_count;
  const failureRate = totalCalls > 0 ? aggregated.failure_count / totalCalls : 0.0;

  const apiCallRate = aggregated.api_calls / windowMinutes;
  // Normalize to percentage (assume max 50 calls per minute for tier-4)
  const apiCallRatePct = Math.min((apiCallRate * 100.0) / 50, 100.0);

  // 5-hour proximity (placeholder - will be calculated in threshold-checker)
  const fiveHourProximity = 0.0;

  // Calculate risk score using weighted formula
  const riskScore =
    (tokenUsagePct * 0.40 / 100) +
    (apiCallRatePct * 0.30 / 100) +
    (failureRate * 0.15) +
    (fiveHourProximity * 0.15);

  return {
    token_usage_pct: tokenUsagePct,
    api_call_rate_pct: apiCallRatePct,
    failure_rate: failureRate,
    five_hour_proximity: fiveHourProximity,
    risk_score: riskScore
  };
}

/**
 * Determine recommendation based on risk score
 */
function getRecommendation(riskScore) {
  if (riskScore < 0.3) return 'continue';
  if (riskScore < 0.6) return 'monitor';
  if (riskScore < 0.8) return 'rest_soon';
  return 'rest_now';
}

/**
 * Main function
 */
function main() {
  const now = new Date();
  const windowStart = new Date(now.getTime() - (WINDOW_MINUTES * 60 * 1000));

  // Read tier configuration
  const tierConfig = readTierConfig();

  // Find recent metrics files
  const recentFiles = findRecentMetricsFiles();

  // Aggregate metrics
  const aggregated = aggregateMetrics(recentFiles);

  // Calculate risk score
  const riskComponents = calculateRiskScore(aggregated, tierConfig, WINDOW_MINUTES);

  // Determine recommendation
  const recommendation = getRecommendation(riskComponents.risk_score);

  // Calculate remaining quota
  const maxTokensInWindow = tierConfig.input_tpm * WINDOW_MINUTES;
  const remainingInputPct = 100.0 - riskComponents.token_usage_pct;

  // Output quota status as JSON
  const quotaStatus = {
    window_start: windowStart.toISOString(),
    window_end: now.toISOString(),
    accumulated: {
      input_tokens: aggregated.input_tokens,
      output_tokens: aggregated.output_tokens,
      cached_tokens: aggregated.cached_tokens,
      effective_tokens: aggregated.effective_tokens,
      api_calls: aggregated.api_calls
    },
    failure_count: aggregated.failure_count,
    success_count: aggregated.success_count,
    estimated_quota: {
      tier: tierConfig.tier,
      input_tpm: tierConfig.input_tpm,
      max_tokens_in_window: maxTokensInWindow,
      token_usage_pct: parseFloat(riskComponents.token_usage_pct.toFixed(2)),
      remaining_input_pct: parseFloat(remainingInputPct.toFixed(2))
    },
    risk_components: {
      token_usage_pct: parseFloat(riskComponents.token_usage_pct.toFixed(2)),
      api_call_rate_pct: parseFloat(riskComponents.api_call_rate_pct.toFixed(2)),
      failure_rate: parseFloat(riskComponents.failure_rate.toFixed(4)),
      five_hour_proximity: parseFloat(riskComponents.five_hour_proximity.toFixed(2))
    },
    risk_score: parseFloat(riskComponents.risk_score.toFixed(4)),
    recommendation: recommendation
  };

  console.log(JSON.stringify(quotaStatus, null, 2));
}

// Execute
try {
  main();
} catch (err) {
  console.error('❌ Error calculating quota:', err.message);
  process.exit(1);
}
