---
description: Initialize SmartAdmin development environment
---

# SmartAdmin Development Environment Initialization

This workflow guides you through setting up the SmartAdmin (Java 17 + Spring Boot 3) development environment.

## Prerequisites Check

1. **Verify Java Version**
   ```bash
   java -version
   ```
   Expected: Java 17 or higher

// turbo
2. **Check Maven Installation**
   ```bash
   mvn -version
   ```

3. **Verify Database Connection**
   - Ensure MySQL is running on port 3306
   - Default database: `smart_admin_v3`
   - Check connection settings in: `sa-base/src/main/resources/{env}/sa-base.yaml`

4. **Verify Redis** (optional but recommended)
   ```bash
   redis-cli ping
   ```
   Expected: `PONG`

## Project Structure

Navigate to the Java 17 version:
```bash
cd smart-admin-api-java17-springboot3
```

**Key directories:**
- `sa-base/` - Shared infrastructure library (357 Java files)
  - `common/` - Core DTOs, utilities, constants
  - `config/` - Spring configurations
  - `module/support/` - 26 reusable support modules
- `sa-admin/` - Main application (202 Java files)
  - `module/business/` - Business logic modules
  - `module/system/` - System modules

## Database Setup

1. **Create Database**
   ```bash
   mysql -u root -p
   ```
   ```sql
   CREATE DATABASE smart_admin_v3 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Import SQL Scripts**
   - Navigate to project root: `cd ../`
   - Import scripts from `数据库SQL脚本/` directory:
   ```bash
   mysql -u root -p smart_admin_v3 < 数据库SQL脚本/smart-admin-v3.sql
   ```

## Configuration

1. **Update Database Configuration**
   - Edit: `sa-base/src/main/resources/dev/sa-base.yaml`
   - Update database credentials:
   ```yaml
   spring:
     datasource:
       url: jdbc:p6spy:mysql://127.0.0.1:3306/smart_admin_v3
       username: your_username
       password: your_password
   ```

2. **Update Redis Configuration** (if using)
   - Same file: `sa-base.yaml`
   ```yaml
   spring:
     data:
       redis:
         host: 127.0.0.1
         port: 6379
   ```

## Build and Run

// turbo
1. **Clean and Build Project**
   ```bash
   cd smart-admin-api-java17-springboot3
   mvn clean package -DskipTests
   ```

2. **Run Application**
   ```bash
   cd sa-admin
   mvn spring-boot:run
   ```

3. **Verify Application**
   - Application URL: http://localhost:1024
   - API Documentation: http://localhost:1024/swagger-ui.html
   - Default port: 1024
   - Look for log message: "Application started successfully"

## Frontend Setup (Optional)

If you need to run the frontend:

1. **Navigate to frontend directory**
   ```bash
   cd ../../smart-admin-web-typescript  # or smart-admin-web-javascript
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Run development server**
   ```bash
   npm run dev
   ```

## Next Steps

After successful initialization:
- Review `CLAUDE.md` for coding conventions and patterns
- Explore the support modules in `sa-base/module/support/`
- Check the business modules in `sa-admin/module/business/`
- Review API documentation at http://localhost:1024/swagger-ui.html

## Common Issues

**Issue: Port 1024 already in use**
- Solution: Change port in `sa-admin/src/main/resources/dev/application.yaml`

**Issue: Database connection failed**
- Check MySQL is running: `systemctl status mysql` (Linux) or via Activity Monitor (Mac)
- Verify credentials in `sa-base.yaml`

**Issue: Redis connection failed**
- Redis is optional for basic development
- Start Redis: `redis-server` or disable in configuration
