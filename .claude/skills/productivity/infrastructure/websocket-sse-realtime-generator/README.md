# WebSocket/SSE Real-time Generator - Quick Reference

**Sprint:** 5 (Weeks 16-18) | **Priority:** P1 - Roadmap #4 | **Status:** 🚧 Skeleton

## One-Line Summary
Generate WebSocket/SSE real-time communication with STOMP, authentication, and Redis Pub/Sub scaling.

## When to Use
- Real-time dashboard updates
- Live notifications
- Chat functionality
- Order/status tracking
- System monitoring dashboards

## What It Generates
✅ WebSocket endpoint (STOMP)
✅ SSE endpoint (server-to-client)
✅ Sa-Token authentication
✅ Broadcasting patterns (topic, user, role)
✅ Heartbeat and reconnection
✅ Backpressure handling
✅ Vue client integration (SockJS)
✅ Redis Pub/Sub for scaling

## Quick Example
```
User: "Add WebSocket for real-time order status updates"
→ Generates WebSocket endpoint with Vue component in minutes
```

## Success Metric
**Real-time updates with < 500ms latency**

## Integration
- Works with: `smartadmin-vue-crud`, `message-queue-pattern-generator`
- Uses: Spring WebSocket, SockJS
- Compatible: All real-time features

---
**Full Documentation:** [SKILL.md](SKILL.md)
**Version:** 1.0.0-SKELETON
**Created:** 2026-01-26
