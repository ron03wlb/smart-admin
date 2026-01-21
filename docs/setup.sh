#!/bin/bash

# SmartAdmin Documentation Setup Script
# This script helps you get started with the documentation site quickly

set -e

echo "🚀 SmartAdmin Documentation Setup"
echo "=================================="
echo ""

# Check if we're in the docs directory
if [ ! -f "package.json" ]; then
    echo "❌ Error: This script must be run from the docs/ directory"
    echo "   cd docs && ./setup.sh"
    exit 1
fi

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Error: Node.js is not installed"
    echo "   Please install Node.js 18+ from https://nodejs.org/"
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "⚠️  Warning: Node.js version $NODE_VERSION detected"
    echo "   Recommended: Node.js 18 or higher"
    echo ""
fi

echo "✅ Node.js $(node -v) detected"
echo ""

# Install dependencies
echo "📦 Installing dependencies..."
if [ -d "node_modules" ]; then
    echo "   node_modules exists, skipping npm install"
    echo "   (Delete node_modules to reinstall)"
else
    npm install
    echo "✅ Dependencies installed"
fi
echo ""

# Offer to start dev server
echo "🎯 What would you like to do?"
echo ""
echo "1) Start development server (localhost:5173)"
echo "2) Build for production"
echo "3) Preview production build"
echo "4) Build Docker image"
echo "5) Run Docker container"
echo "6) Exit"
echo ""
read -p "Enter choice [1-6]: " choice

case $choice in
    1)
        echo ""
        echo "🚀 Starting development server..."
        echo "   Visit: http://localhost:5173"
        echo "   Press Ctrl+C to stop"
        echo ""
        npm run dev
        ;;
    2)
        echo ""
        echo "🏗️  Building for production..."
        npm run build
        echo ""
        echo "✅ Build complete!"
        echo "   Output: .vitepress/dist/"
        ;;
    3)
        echo ""
        echo "👀 Building and previewing production..."
        npm run build
        npm run preview
        ;;
    4)
        echo ""
        echo "🐳 Building Docker image..."
        docker build -t smart-admin-docs:latest .
        echo ""
        echo "✅ Docker image built: smart-admin-docs:latest"
        ;;
    5)
        echo ""
        echo "🐳 Running Docker container..."
        docker run -d -p 8081:80 --name smart-admin-docs smart-admin-docs:latest
        echo ""
        echo "✅ Container started!"
        echo "   Visit: http://localhost:8081/docs/"
        echo "   Stop: docker stop smart-admin-docs"
        echo "   Remove: docker rm smart-admin-docs"
        ;;
    6)
        echo "👋 Goodbye!"
        exit 0
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac
