#!/usr/bin/env node

/**
 * Ralph Loop - Token Usage Tracker
 *
 * Records token usage from Claude CLI output for quota monitoring.
 *
 * Usage:
 *   echo "$OUTPUT" | node usage-tracker.js --iteration=5 --duration=45 --log-file=metrics/2026-02-10-1430.json
 *
 * Input (stdin): Claude CLI output containing token information
 * Output: JSON log entry appended to specified file
 */

const fs = require('fs');
const path = require('path');
const readline = require('readline');

// Parse command line arguments
const args = process.argv.slice(2).reduce((acc, arg) => {
  const [key, value] = arg.split('=');
  acc[key.replace('--', '')] = value;
  return acc;
}, {});

// Configuration
const ITERATION = parseInt(args.iteration || '0', 10);
const DURATION = parseInt(args.duration || '0', 10);
const LOG_FILE = args['log-file'] || path.join(__dirname, '../metrics/usage.json');

// Token extraction patterns (multiple regex for format flexibility)
const TOKEN_PATTERNS = {
  // Pattern 1: "Input tokens: 12500"
  input1: /Input tokens:\s*(\d+)/i,
  // Pattern 2: "input_tokens: 12500"
  input2: /input_tokens:\s*(\d+)/i,
  // Pattern 3: "tokens_input: 12500"
  input3: /tokens_input:\s*(\d+)/i,

  // Output token patterns
  output1: /Output tokens:\s*(\d+)/i,
  output2: /output_tokens:\s*(\d+)/i,
  output3: /tokens_output:\s*(\d+)/i,

  // Cached token patterns
  cached1: /Cached tokens:\s*(\d+)/i,
  cached2: /cached_tokens:\s*(\d+)/i,
  cached3: /cache_read_tokens:\s*(\d+)/i,
  cached4: /tokens_cached:\s*(\d+)/i,
};

// Success indicators
const SUCCESS_INDICATORS = [
  /RALPH_COMPLETE/,
  /✅/,
  /SUCCESS/i,
  /completed/i,
];

// Error indicators
const ERROR_INDICATORS = [
  /ERROR/i,
  /FAILED/i,
  /Exception/i,
  /rate.?limit/i,
  /429/,
  /⚠️/,
  /🛑/,
];

/**
 * Extract token counts from text using multiple patterns
 */
function extractTokens(text) {
  const tokens = {
    input: 0,
    output: 0,
    cached: 0,
    total: 0
  };

  // Try all input patterns
  for (const pattern of [TOKEN_PATTERNS.input1, TOKEN_PATTERNS.input2, TOKEN_PATTERNS.input3]) {
    const match = text.match(pattern);
    if (match) {
      tokens.input = parseInt(match[1], 10);
      break;
    }
  }

  // Try all output patterns
  for (const pattern of [TOKEN_PATTERNS.output1, TOKEN_PATTERNS.output2, TOKEN_PATTERNS.output3]) {
    const match = text.match(pattern);
    if (match) {
      tokens.output = parseInt(match[1], 10);
      break;
    }
  }

  // Try all cached patterns
  for (const pattern of [TOKEN_PATTERNS.cached1, TOKEN_PATTERNS.cached2, TOKEN_PATTERNS.cached3, TOKEN_PATTERNS.cached4]) {
    const match = text.match(pattern);
    if (match) {
      tokens.cached = parseInt(match[1], 10);
      break;
    }
  }

  // Calculate effective tokens (cached tokens discount: 90%)
  tokens.total = tokens.input + tokens.output - Math.floor(tokens.cached * 0.9);

  return tokens;
}

/**
 * Determine success status from output
 */
function determineSuccess(text) {
  // Check error indicators first (higher priority)
  for (const pattern of ERROR_INDICATORS) {
    if (pattern.test(text)) {
      return false;
    }
  }

  // Check success indicators
  for (const pattern of SUCCESS_INDICATORS) {
    if (pattern.test(text)) {
      return true;
    }
  }

  // Default: assume success if no explicit error
  return true;
}

/**
 * Main function: read stdin, extract data, write to log
 */
async function trackUsage() {
  // Validate required arguments
  if (!ITERATION) {
    console.error('❌ Error: --iteration is required');
    console.error('Usage: echo "$OUTPUT" | node usage-tracker.js --iteration=N --duration=S --log-file=PATH');
    process.exit(1);
  }

  // Read entire input from stdin
  let inputText = '';
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: false
  });

  for await (const line of rl) {
    inputText += line + '\n';
  }

  // Extract tokens
  const tokens = extractTokens(inputText);
  const success = determineSuccess(inputText);

  // Create log entry
  const logEntry = {
    timestamp: new Date().toISOString(),
    iteration: ITERATION,
    tokens: tokens,
    api_calls: 1, // Each iteration = 1 API call
    duration_seconds: DURATION,
    success: success
  };

  // Ensure log directory exists
  const logDir = path.dirname(LOG_FILE);
  if (!fs.existsSync(logDir)) {
    fs.mkdirSync(logDir, { recursive: true });
  }

  // Read existing logs or create new array
  let logs = [];
  if (fs.existsSync(LOG_FILE)) {
    try {
      const content = fs.readFileSync(LOG_FILE, 'utf8');
      logs = JSON.parse(content);
    } catch (err) {
      console.error(`⚠️ Warning: Could not parse existing log file, starting fresh: ${err.message}`);
      logs = [];
    }
  }

  // Append new log entry
  logs.push(logEntry);

  // Write back to file
  fs.writeFileSync(LOG_FILE, JSON.stringify(logs, null, 2));

  // Output summary
  console.log(`✅ Logged iteration #${ITERATION}`);
  console.log(`   Tokens: input=${tokens.input}, output=${tokens.output}, cached=${tokens.cached}, total=${tokens.total}`);
  console.log(`   Success: ${success}`);
  console.log(`   File: ${LOG_FILE}`);
}

// Execute
trackUsage().catch(err => {
  console.error('❌ Error tracking usage:', err.message);
  process.exit(1);
});
