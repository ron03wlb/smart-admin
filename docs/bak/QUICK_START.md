# Quick Start - SmartAdmin Documentation

## 🚀 Get Started in 3 Steps

### Step 1: Install Dependencies

```bash
cd docs
npm install
```

### Step 2: Start Dev Server

```bash
npm run dev
```

### Step 3: Open Browser

Visit **http://localhost:5173**

---

## 🐳 Docker Quick Start

```bash
# From docs directory
docker build -t smart-admin-docs:latest .
docker run -d -p 8081:80 smart-admin-docs:latest
```

Visit **http://localhost:8081/docs/**

---

## 📚 What's Available

### ✅ Complete Sections

- **Getting Started** - Quick start, quick reference, hello world
- **Architecture** - System overview (partial)

### ⏳ Coming Soon (Placeholders Created)

- User Guides (6 docs)
- Operations (5 docs)
- Troubleshooting (4 docs)
- Advanced Topics (5 docs)
- Examples (5 docs)
- Testing (4 docs)
- Reference (4 docs)
- Appendix (4 docs)

---

## 🛠️ Commands

```bash
# Development
npm run dev          # Start dev server

# Production
npm run build        # Build static site
npm run preview      # Preview production build

# Docker
docker build -t smart-admin-docs .
docker run -p 8081:80 smart-admin-docs

# Docker Compose
cd ../docker
docker compose up -d docs
```

---

## 📖 Key Documents

- `README.md` - Project overview
- `DEVELOPER_GUIDE.md` - How to write docs
- `IMPLEMENTATION_PROGRESS.md` - Detailed status
- `PHASE1_COMPLETE_SUMMARY.md` - Phase 1 summary

---

## 🎯 Next Steps

1. Review Getting Started docs
2. Test local dev server
3. Start Phase 2: Complete architecture docs
4. Migrate existing Kafka documentation

---

**Last Updated**: 2026-01-21
