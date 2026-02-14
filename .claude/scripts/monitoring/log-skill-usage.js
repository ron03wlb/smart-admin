#!/usr/bin/env node

/**
 * SmartAdmin Skills Monitoring - Usage Logger
 *
 * Logs skill invocations to JSON files for later aggregation and analysis.
 *
 * Usage:
 *   node log-skill-usage.js --event=start --skill=archunit-test-generator --trigger="add architecture rule"
 *   node log-skill-usage.js --event=end --skill=archunit-test-generator --success=true --duration=245
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Parse command line arguments
const args = process.argv.slice(2).reduce((acc, arg) => {
  const [key, value] = arg.split('=');
  acc[key.replace('--', '')] = value;
  return acc;
}, {});

// Configuration
const METRICS_DIR = path.join(__dirname, '../../metrics/raw');
const SESSION_ID = process.env.CLAUDE_SESSION_ID || generateSessionId();

// Ensure metrics directory exists
if (!fs.existsSync(METRICS_DIR)) {
  fs.mkdirSync(METRICS_DIR, { recursive: true });
}

// Main logging function
function logSkillUsage() {
  const { event, skill, trigger, success, duration, metadata } = args;

  // Validate required fields
  if (!event || !skill) {
    console.error('❌ Error: --event and --skill are required');
    console.error('Usage: node log-skill-usage.js --event=start|end --skill=<skill-name> [options]');
    process.exit(1);
  }

  // Create log entry
  const logEntry = {
    timestamp: new Date().toISOString(),
    event, // 'start' or 'end'
    skill,
    sessionId: hashSessionId(SESSION_ID),
    trigger: trigger || 'unknown',
    success: success === 'true' ? true : success === 'false' ? false : null,
    duration: duration ? parseInt(duration, 10) : null,
    metadata: metadata ? JSON.parse(metadata) : {}
  };

  // Determine log file (one file per day)
  const today = new Date().toISOString().split('T')[0];
  const logFile = path.join(METRICS_DIR, `${today}.json`);

  // Read existing logs or create new array
  let logs = [];
  if (fs.existsSync(logFile)) {
    const content = fs.readFileSync(logFile, 'utf8');
    logs = JSON.parse(content);
  }

  // Append new log entry
  logs.push(logEntry);

  // Write back to file
  fs.writeFileSync(logFile, JSON.stringify(logs, null, 2));

  console.log(`✅ Logged ${event} event for skill: ${skill}`);
  console.log(`   File: ${logFile}`);
  console.log(`   Session: ${logEntry.sessionId.substring(0, 8)}...`);
}

// Helper: Generate session ID if not provided
function generateSessionId() {
  return `session-${Date.now()}-${Math.random().toString(36).substring(7)}`;
}

// Helper: Hash session ID for privacy (SHA-256)
function hashSessionId(sessionId) {
  return crypto.createHash('sha256').update(sessionId).digest('hex');
}

// Execute
try {
  logSkillUsage();
} catch (error) {
  console.error('❌ Error logging skill usage:', error.message);
  process.exit(1);
}
