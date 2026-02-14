# SmartAdmin API (Java 21 + Spring Boot 3)

SmartAdmin is a modern, enterprise-grade admin system backend based on Java 21 and Spring Boot 3.5.4. It follows a clean architecture, separating concerns into controllers, services, and DAOs, and includes robust features for security, logging, and data management.

## 🛠 Technology Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.4
- **ORM:** MyBatis Plus 3.5.12
- **Authentication:** Sa-Token 1.44.0
- **Documentation:** Knife4j 4.6.0
- **Database Connection:** Druid
- **Cache:** Redis + Redisson
- **Database:** MySQL 8.0+

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **JDK 17 or 21**: This project is built with Java 21 features.
- **Gradle 8.11+**: For dependency management and build (or use the included Gradle Wrapper).
- **MySQL 8.0+**: Database server.
- **Redis**: For caching and session management.

## ⚙️ Configuration

1.  **Database Connection:**
    Open `src/main/resources/application-dev.yml` (or the relevant profile config) and update the database settings:

    ```yaml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/smart_admin_v3?useUnicode=true&characterEncoding=utf-8&useSSL=false
        username: root
        password: your_password
    ```

2.  **Redis Connection:**
    Update the Redis configuration in the same file:

    ```yaml
    spring:
      data:
        redis:
          host: localhost
          port: 6379
          password: # leave blank if no password
          database: 1
    ```

## 🚀 Running the Application

1.  **Build the project:**

    ```bash
    ./gradlew build -Penv=dev
    ```

2.  **Run the application:**

    ```bash
    ./gradlew :sa-admin:bootRun
    ```

    Alternatively, run the jar file:
    ```bash
    java -jar sa-admin/build/libs/sa-admin-*.jar
    ```

3.  **Access the API Documentation:**
    Once running, open your browser and navigate to:
    [http://localhost:1024/swagger-ui.html](http://localhost:1024/swagger-ui.html)

## 🏗 Key Modules

-   **Auth (Sa-Token):** Handles login, permission checks (`@SaCheckPermission`), and role management.
-   **System Modules:** Users, roles, menus, departments.
-   **Support Modules:** File upload (MinIO/Local/AliYun), code generation, dictionary, configuration management.
-   **Monitor:** SQL monitoring (P6Spy), operation logs, login logs.

## 🤝 Contribution

Please follow the coding standards defined in `CLAUDE.md`. Ensure all tests pass before submitting a pull request.
