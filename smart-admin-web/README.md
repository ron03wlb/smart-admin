# SmartAdmin Web (Vue 3 + Vite + TypeScript)

SmartAdmin Web is a modern, responsive admin dashboard frontend built with Vue 3, Vite, and TypeScript. It features a rich set of components based on Ant Design Vue and is designed for high performance and developer experience.

## 🛠 Technology Stack

- **Framework:** Vue 3.4+
- **Build Tool:** Vite 5.2+
- **Language:** TypeScript 5.6+
- **UI Component Library:** Ant Design Vue 4.2+
- **State Management:** Pinia
- **Routing:** Vue Router 4.x
- **HTTP Client:** Axios
- **Charts:** ECharts 5.x

## 📋 Prerequisites

- **Node.js**: Version 18.0.0 or higher.
- **Package Manager**: npm, yarn, or pnpm.

## 🚀 Getting Started

1.  **Install Dependencies:**

    ```bash
    npm install
    # or
    yarn install
    ```

2.  **Start Development Server:**

    ```bash
    npm run dev
    ```

    The application will start on user defined port (default 1024 or random available port).
    Open your browser and navigate to the local URL shown in the terminal.

    > Note: `npm run localhost` is also available if specifically configured for localhost binding.

## 📦 Build Script

-   **Development Build:**
    ```bash
    npm run build:test
    ```

-   **Pre-production Build:**
    ```bash
    npm run build:pre
    ```

-   **Production Build:**
    ```bash
    npm run build:prod
    ```

    The build artifacts will be generated in the `dist/` directory (or configured output dir).

## 🧩 Project Structure

-   `src/api`: API definition and HTTP request modules.
-   `src/assets`: Static assets (images, styles).
-   `src/components`: Reusable Vue components.
-   `src/views`: Page components and route views.
-   `src/store`: Pinia state management modules.
-   `src/router`: Vue router configuration.
-   `src/utils`: Utility functions.

## 🤝 Contribution

Please ensure your code follows the project's eslint and prettier configurations.
