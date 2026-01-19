# SmartAdmin (Java 21 + Vue 3 Edition)

Welcome to the **SmartAdmin** workspace. This repository is configured for the latest technology stack, featuring a **Java 21** backend and a **Vue 3** frontend.

## 📂 Project Structure

This workspace focuses on the following two core projects:

### 1. Backend: [smart-admin-api-java21-springboot3](./smart-admin-api-java21-springboot3/README.md)
*   **Tech Stack:** Java 21, Spring Boot 3.5.4, MyBatis Plus, Sa-Token.
*   **Path:** `smart-admin-api-java21-springboot3/`
*   **Documentation:** [Click here to view Backend README](./smart-admin-api-java21-springboot3/README.md)

### 2. Frontend: [smart-admin-web](./smart-admin-web/README.md)
*   **Tech Stack:** Vue 3, Vite, TypeScript, Ant Design Vue.
*   **Path:** `smart-admin-web/`
*   **Documentation:** [Click here to view Frontend README](./smart-admin-web/README.md)

---

## 🚀 Quick Start Guide

To get the full system up and running, please follow these steps:

### Step 1: Start the Backend
1.  Navigate to `smart-admin-api-java21-springboot3`.
2.  Follow the [Backend Setup Instructions](./smart-admin-api-java21-springboot3/README.md#running-the-application).
3.  Ensure the API service is running on `http://localhost:1024`.

### Step 2: Start the Frontend
1.  Navigate to `smart-admin-web`.
2.  Follow the [Frontend Setup Instructions](./smart-admin-web/README.md#getting-started).
3.  The frontend will typically launch at a robust local port (e.g., `http://localhost:5173`).

---

## 🛠️ Git Sparse Checkout Configuration

To focus only on the relevant directories for this workspace (Java 21 + Vue 3), use the following commands to configure git sparse checkout:

```bash
git sparse-checkout init --cone
git sparse-checkout set .agent .github docker smart-admin-api-java21-springboot3 smart-admin-web smart-app 数据库SQL脚本
```

> **Note:** Older versions or other directories in this repository (like `smart-admin-api-java8-springboot2`) are legacy or reference implementations and are hidden by this configuration.
