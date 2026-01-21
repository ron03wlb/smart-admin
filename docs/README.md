# SmartAdmin Documentation

This directory contains the VitePress-powered documentation site for SmartAdmin.

## Quick Start

### Development

```bash
# Install dependencies
npm install

# Start dev server
npm run dev
```

Visit `http://localhost:5173`

### Build

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

### Docker

```bash
# Build Docker image
docker build -t smart-admin-docs:latest .

# Run container
docker run -d -p 8080:80 smart-admin-docs:latest
```

Visit `http://localhost:8080/docs/`

## Project Structure

```
docs/
├── .vitepress/
│   ├── config.ts           # VitePress configuration
│   ├── theme/              # Custom theme
│   └── public/             # Static assets
├── kafka/                  # Kafka documentation
│   ├── getting-started/
│   ├── architecture/
│   ├── guides/
│   ├── operations/
│   ├── troubleshooting/
│   ├── advanced/
│   ├── examples/
│   ├── testing/
│   ├── reference/
│   └── appendix/
├── index.md                # Documentation homepage
└── package.json
```

## Documentation Standards

- All documentation follows Markdown format
- Code examples must be tested and runnable
- Diagrams use Mermaid syntax
- Each page has clear navigation links
- Maximum line length: no strict limit (Markdown flows naturally)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test the documentation locally
5. Submit a pull request

See [Contributing Guide](/kafka/appendix/contributing.md) for details.

## Technology Stack

- **VitePress**: ^1.0.0 - Documentation framework
- **Vue**: ^3.4.0 - UI framework
- **Mermaid**: ^10.6.1 - Diagram rendering
- **Nginx**: alpine - Production web server

## License

Apache License 2.0
