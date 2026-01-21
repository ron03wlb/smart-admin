---
layout: home

hero:
  name: "SmartAdmin"
  text: "Enterprise Documentation"
  tagline: Modern modular monolith framework based on Spring Boot 3 and Java 21
  image:
    src: /logo.svg
    alt: SmartAdmin
  actions:
    - theme: brand
      text: Get Started
      link: /kafka/getting-started/quick-start
    - theme: alt
      text: View on GitHub
      link: https://github.com/smart-admin

features:
  - icon: 🚀
    title: Kafka Integration
    details: Enterprise-grade Kafka integration with batch processing, DLQ, error handling, and comprehensive monitoring
    link: /kafka/
    linkText: Read Kafka Docs

  - icon: ⚡
    title: Redis Support
    details: Advanced Redis integration with distributed locks, caching strategies, and pub/sub messaging
    link: /redis/
    linkText: Read Redis Docs

  - icon: 📨
    title: RocketMQ
    details: Reliable message queue integration with transactional messages and ordered delivery
    link: /rocketmq/
    linkText: Read RocketMQ Docs

  - icon: 🗄️
    title: Database
    details: MyBatis Plus integration with multi-datasource support, sharding, and read-write splitting
    link: /database/
    linkText: Read Database Docs

  - icon: 🔒
    title: Security
    details: Sa-Token based authentication and authorization with role-based access control
    link: /security/
    linkText: Read Security Docs

  - icon: 🐳
    title: Deployment
    details: Docker and Kubernetes deployment guides with production-ready configurations
    link: /deployment/
    linkText: Read Deployment Docs

---

## Quick Navigation

### Message Queue

- **[Kafka](/kafka/)** - Enterprise Kafka integration
  - [Quick Start](/kafka/getting-started/quick-start) - Get up and running in 5 minutes
  - [Quick Reference](/kafka/getting-started/quick-reference) - API cheat sheet
  - [Architecture](/kafka/architecture/overview) - Design and architecture
  - [Troubleshooting](/kafka/troubleshooting/common-issues) - Common issues and solutions

- **[RocketMQ](/rocketmq/)** - Coming soon
- **[Redis Pub/Sub](/redis/)** - Coming soon

### Data Access

- **[Database](/database/)** - Coming soon
- **[Redis](/redis/)** - Coming soon
- **[Cache](/cache/)** - Coming soon

### Infrastructure

- **[Deployment](/deployment/)** - Coming soon
- **[Monitoring](/monitoring/)** - Coming soon
- **[Security](/security/)** - Coming soon

---

## Documentation Philosophy

Our documentation is built with the following principles:

- **Quick to Find** - Get answers in under 2 minutes with quick reference cards and effective search
- **Easy to Understand** - Clear examples, diagrams, and step-by-step guides
- **Production Ready** - Real-world patterns, best practices, and troubleshooting guides
- **Always Updated** - Synchronized with code changes and version releases

---

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Language runtime |
| Spring Boot | 3.5.4 | Application framework |
| MyBatis Plus | 3.5.12 | ORM framework |
| Sa-Token | 1.44.0 | Authentication |
| Redisson | 3.50.0 | Redis client |
| Kafka | 3.x | Message queue |

---

## Getting Help

- **Issues**: [GitHub Issues](https://github.com/smart-admin/issues)
- **Discussions**: [GitHub Discussions](https://github.com/smart-admin/discussions)
- **Email**: support@smart-admin.io

---

<div style="text-align: center; margin-top: 48px; color: var(--vp-c-text-2);">
  <p>Built with ❤️ by SmartAdmin Team</p>
  <p>
    <a href="/kafka/appendix/contributing">Contributing</a> ·
    <a href="/kafka/appendix/changelog">Changelog</a> ·
    <a href="https://github.com/smart-admin">GitHub</a>
  </p>
</div>
