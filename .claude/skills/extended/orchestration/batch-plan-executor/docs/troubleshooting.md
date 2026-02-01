## Troubleshooting

### Issue 1: Skill Mapping Failures

**症狀**: 很多方案顯示 "Requires Manual Execution"。

**可能原因**:
- 方案缺少明確的類型或模組信息
- 內容關鍵詞不足以推斷 Skill
- 自定義方案類型未配置映射

**解決方案**:

1. **添加明確的 Skill 字段**:
   ```yaml
   ---
   skill: smartadmin-crud-generator
   ---
   ```

2. **配置模組到 Skill 的映射**:
   ```yaml
   # config.yml
   skill_mapping:
     custom_mappings:
       'docs/plans/custom-module/*.md': 'smartadmin-crud-generator'
   ```

3. **降低信心閾值**:
   ```yaml
   skill_mapping:
     auto_mapping:
       confidence_threshold: 0.5  # Default: 0.7
   ```

### Issue 2: File Conflict Detection Misses

**症狀**: Dry-run 顯示無衝突，但實際執行時出現合併衝突。

**可能原因**:
- 方案中未明確記錄文件修改
- 文件路徑格式不規範
- 推斷算法失敗

**解決方案**:

1. **明確記錄文件修改**:
   ```markdown
   ## Files to Modify
   - src/main/java/.../ProductController.java
   - src/main/java/.../ProductService.java
   ```

2. **使用代碼塊文件註釋**:
   ```markdown
   ```java
   // File: src/main/java/.../ProductController.java
   @RestController
   public class ProductController { ... }
   ```
   ```

3. **手動審查衝突**:
   ```bash
   /batch-execute --dry-run --show-conflicts
   ```

### Issue 3: Execution Timeout

**症狀**: 方案執行超時（默認 60 分鐘）。

**可能原因**:
- 方案過於複雜
- Skill 執行時間過長
- 系統資源不足

**解決方案**:

1. **增加超時時間**:
   ```yaml
   execution:
     timeout:
       per_plan_seconds: 7200  # 120 minutes
   ```

2. **拆分大型方案**:
   - 將單個大方案拆分為多個小階段
   - 每個階段獨立執行

3. **手動執行超時方案**:
   ```bash
   # 查看執行報告，找到超時方案
   # 手動執行該方案（不使用 batch executor）
   ```

### Issue 4: Quality Gate Failures

**症狀**: ArchUnit 測試失敗，阻止後續方案執行。

**可能原因**:
- 方案生成的代碼違反架構規則
- Skill 實現不符合 SmartAdmin 規範

**解決方案**:

1. **查看詳細錯誤信息**:
   ```bash
   # 查看執行日誌
   cat .claude/metrics/batch-executions/batch-exec-*.log
   ```

2. **修復架構違規**:
   - 檢查生成的代碼
   - 修正違反的規則（如使用 `@Autowired` 字段注入）

3. **臨時禁用 Quality Gate** (不推薦):
   ```yaml
   execution:
     quality_gates:
       enabled: false  # Only for testing
   ```

### Issue 5: Inaccurate Time Estimates

**症狀**: 預估時間與實際執行時間差距大（> 50%）。

**可能原因**:
- 默認時間估算不準確
- 方案複雜度差異大
- 系統資源波動

**解決方案**:

1. **校準時間估算**:
   ```yaml
   # config.yml - Add custom estimates
   time_estimation:
     plan_type_estimates:
       crud: 30  # Adjust from default 25
       testing: 25  # Adjust from default 20
   ```

2. **提供方案大小提示**:
   ```yaml
   ---
   estimated_duration_minutes: 45
   ---
   ```

3. **使用歷史數據**:
   - 執行幾次後，系統會自動調整估算

---

