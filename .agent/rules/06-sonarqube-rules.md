---
trigger: always_on
---

# SonarQube 規則配置

## 必須啟用的 Blocker/Critical 規則

### 安全漏洞類
| 規則 ID | 名稱              | 類型          |
| ------- | ----------------- | ------------- |
| S3649   | SQL Injection     | Vulnerability |
| S5131   | XSS Prevention    | Vulnerability |
| S2076   | Command Injection | Vulnerability |
| S5135   | Deserialization   | Vulnerability |
| S2755   | XXE Vulnerability | Vulnerability |

### Bug 類
| 規則 ID | 名稱             | 說明                  |
| ------- | ---------------- | --------------------- |
| S2259   | Null Pointer     | 潛在 NPE              |
| S2095   | Resources Closed | 資源洩漏              |
| S1143   | Jump in finally  | finally 中禁止 return |

### 代碼異味
| 規則 ID | 名稱                 | 閾值   |
| ------- | -------------------- | ------ |
| S3776   | Cognitive Complexity | ≤ 15   |
| S1192   | String Duplication   | ≥ 3 次 |
| S1481   | Unused Variables     | 0      |

## Spring 專屬規則 (2024-2025)

### 必須啟用
```yaml
rules:
  S4684: ERROR  # 禁止 Entity 作為 RequestMapping 參數
  S4288: ERROR  # 必須使用構造函數注入
  S2229: ERROR  # @Transactional 自調用問題
  S2230: ERROR  # @Transactional 方法必須 public
  S4601: ERROR  # Security URL 匹配順序
  S4602: ERROR  # 禁止默認包
```

### 代碼示例

#### S4684 - Entity 暴露
```java
// ❌ 錯誤 - Entity 直接暴露
@PostMapping("/user")
public void createUser(@RequestBody User user) { }

// ✅ 正確 - 使用 DTO
@PostMapping("/user")
public void createUser(@RequestBody UserCreateDTO dto) { }
```

#### S4288 - 構造函數注入
```java
// ❌ 錯誤 - 字段注入
@Service
public class UserService {
    @Autowired
    private UserRepository repository;
}

// ✅ 正確 - 構造函數注入
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
}
```

#### S4601 - Security URL 順序
```java
// ❌ 錯誤 - 通配符在前
http.authorizeRequests()
    .antMatchers("/admin/**").authenticated()
    .antMatchers("/admin/user").hasRole("ADMIN");

// ✅ 正確 - 具體規則在前
http.authorizeRequests()
    .antMatchers("/admin/user").hasRole("ADMIN")
    .antMatchers("/admin/**").authenticated();
```

## Quality Gate 配置
```yaml
# 新代碼標準 (Clean as You Code)
conditions:
  - metric: new_reliability_rating
    operator: GREATER_THAN
    value: "1"  # A 級 - 無新 Bug
  - metric: new_security_rating
    operator: GREATER_THAN
    value: "1"  # A 級 - 無新漏洞
  - metric: new_coverage
    operator: LESS_THAN
    value: "80" # 新代碼覆蓋率 ≥ 80%
  - metric: new_duplicated_lines_density
    operator: GREATER_THAN
    value: "3"  # 重複代碼 ≤ 3%
```