import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'

// https://vitepress.dev/reference/site-config
export default withMermaid(
  defineConfig({
    title: 'SmartAdmin Documentation',
    description: 'Enterprise-grade modular monolith framework based on Spring Boot 3 and Java 21',
    base: '/docs/',

    head: [
      ['link', { rel: 'icon', href: '/docs/favicon.ico' }],
      ['meta', { name: 'theme-color', content: '#3c8772' }]
    ],

    themeConfig: {
      logo: '/logo.svg',

      nav: [
        { text: 'Home', link: '/' },
        { text: 'Kafka', link: '/kafka/' },
        { text: 'MinIO', link: '/minio/' },
        { text: 'Redis', link: '/redis/' },
        { text: 'RocketMQ', link: '/rocketmq/' },
        { text: 'Database', link: '/database/' },
        { text: 'Deployment', link: '/deployment/' }
      ],

      sidebar: {
        '/kafka/': [
          {
            text: 'Getting Started',
            collapsed: false,
            items: [
              { text: 'Quick Start', link: '/kafka/getting-started/quick-start' },
              { text: 'Quick Reference', link: '/kafka/getting-started/quick-reference' },
              { text: 'Hello World', link: '/kafka/getting-started/hello-world' }
            ]
          },
          {
            text: 'Architecture',
            collapsed: false,
            items: [
              { text: 'Overview', link: '/kafka/architecture/overview' },
              { text: 'Module Structure', link: '/kafka/architecture/module-structure' },
              { text: 'Message Flow', link: '/kafka/architecture/message-flow' },
              { text: 'Batch Processing', link: '/kafka/architecture/batch-processing' },
              { text: 'Dead Letter Queue', link: '/kafka/architecture/dead-letter-queue' }
            ]
          },
          {
            text: 'User Guides',
            collapsed: false,
            items: [
              { text: 'Producer Guide', link: '/kafka/guides/producer-guide' },
              { text: 'Consumer Guide', link: '/kafka/guides/consumer-guide' },
              { text: 'Batch Operations', link: '/kafka/guides/batch-operations' },
              { text: 'Error Handling', link: '/kafka/guides/error-handling' },
              { text: 'Configuration', link: '/kafka/guides/configuration' },
              { text: 'Best Practices', link: '/kafka/guides/best-practices' }
            ]
          },
          {
            text: 'Operations',
            collapsed: true,
            items: [
              { text: 'Deployment', link: '/kafka/operations/deployment' },
              { text: 'Monitoring', link: '/kafka/operations/monitoring' },
              { text: 'Health Checks', link: '/kafka/operations/health-checks' },
              { text: 'Performance Tuning', link: '/kafka/operations/performance-tuning' },
              { text: 'Backup & Recovery', link: '/kafka/operations/backup-recovery' }
            ]
          },
          {
            text: 'Troubleshooting',
            collapsed: true,
            items: [
              { text: 'Common Issues', link: '/kafka/troubleshooting/common-issues' },
              { text: 'Diagnostic Guide', link: '/kafka/troubleshooting/diagnostic-guide' },
              { text: 'FAQ', link: '/kafka/troubleshooting/faq' },
              { text: 'Debugging Tips', link: '/kafka/troubleshooting/debugging-tips' }
            ]
          },
          {
            text: 'Advanced Topics',
            collapsed: true,
            items: [
              { text: 'Idempotency', link: '/kafka/advanced/idempotency' },
              { text: 'Transactions', link: '/kafka/advanced/transactions' },
              { text: 'Schema Registry', link: '/kafka/advanced/schema-registry' },
              { text: 'Custom Listeners', link: '/kafka/advanced/custom-listeners' },
              { text: 'Extending Framework', link: '/kafka/advanced/extending-framework' }
            ]
          },
          {
            text: 'Examples',
            collapsed: true,
            items: [
              { text: 'Basic Example', link: '/kafka/examples/basic-example' },
              { text: 'Batch Processing', link: '/kafka/examples/batch-example' },
              { text: 'DLQ Handling', link: '/kafka/examples/dlq-example' },
              { text: 'Message Aggregation', link: '/kafka/examples/aggregator-example' },
              { text: 'Docker Compose', link: '/kafka/examples/docker-compose-example' }
            ]
          },
          {
            text: 'Testing',
            collapsed: true,
            items: [
              { text: 'Testing Strategy', link: '/kafka/testing/testing-strategy' },
              { text: 'Unit Testing', link: '/kafka/testing/unit-testing' },
              { text: 'Integration Testing', link: '/kafka/testing/integration-testing' },
              { text: 'Verification Framework', link: '/kafka/testing/verification-framework' }
            ]
          },
          {
            text: 'Reference',
            collapsed: true,
            items: [
              { text: 'API Reference', link: '/kafka/reference/api-reference' },
              { text: 'Configuration Reference', link: '/kafka/reference/configuration-reference' },
              { text: 'Error Codes', link: '/kafka/reference/error-codes' },
              { text: 'Migration Guide', link: '/kafka/reference/migration-guide' }
            ]
          },
          {
            text: 'Appendix',
            collapsed: true,
            items: [
              { text: 'Glossary', link: '/kafka/appendix/glossary' },
              { text: 'Changelog', link: '/kafka/appendix/changelog' },
              { text: 'Contributing', link: '/kafka/appendix/contributing' },
              { text: 'Resources', link: '/kafka/appendix/resources' }
            ]
          }
        ],
        '/minio/': [
          {
            text: 'Getting Started',
            collapsed: false,
            items: [
              { text: 'Overview', link: '/minio/' },
              { text: 'Quick Start', link: '/minio/01-quick-start' },
              { text: 'Configuration', link: '/minio/02-configuration' }
            ]
          },
          {
            text: 'Usage Guides',
            collapsed: false,
            items: [
              { text: 'API Usage', link: '/minio/04-api-usage' },
              { text: 'Security Best Practices', link: '/minio/03-security' }
            ]
          },
          {
            text: 'Operations',
            collapsed: false,
            items: [
              { text: 'Monitoring & Observability', link: '/minio/07-monitoring' },
              { text: 'Troubleshooting', link: '/minio/05-troubleshooting' }
            ]
          },
          {
            text: 'Advanced Topics',
            collapsed: true,
            items: [
              { text: 'Architecture Design', link: '/minio/06-architecture' }
            ]
          }
        ]
      },

      socialLinks: [
        { icon: 'github', link: 'https://github.com/smart-admin' }
      ],

      search: {
        provider: 'local',
        options: {
          translations: {
            button: {
              buttonText: 'Search',
              buttonAriaLabel: 'Search'
            },
            modal: {
              noResultsText: 'No results found',
              resetButtonTitle: 'Clear search',
              footer: {
                selectText: 'to select',
                navigateText: 'to navigate',
                closeText: 'to close'
              }
            }
          }
        }
      },

      editLink: {
        pattern: 'https://github.com/smart-admin/smart-admin/edit/master/docs/:path',
        text: 'Edit this page on GitHub'
      },

      lastUpdated: {
        text: 'Last updated',
        formatOptions: {
          dateStyle: 'short',
          timeStyle: 'short'
        }
      },

      footer: {
        message: 'Released under the Apache 2.0 License.',
        copyright: 'Copyright © 2024-present SmartAdmin Team'
      }
    },

    markdown: {
      lineNumbers: true,
      theme: {
        light: 'github-light',
        dark: 'github-dark'
      }
    },

    // Mermaid configuration
    mermaid: {
      theme: 'default'
    }
  })
)
