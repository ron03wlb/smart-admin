#!/usr/bin/env python3
"""
SmartAdmin Auto-Coding - Claude API Service
Claude API 封裝服務 - SmartAdmin 專用代碼生成

功能:
- Java 後端代碼生成（符合 SmartAdmin 規範）
- Vue 前端代碼生成（Composition API + Ant Design Vue）
- 智能需求理解和代碼優化建議

作者: SmartAdmin Auto-Coding System
版本: 1.0.0
日期: 2026-01-27
"""

import os
import re
import json
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime

# 導入 Anthropic SDK
try:
    from anthropic import Anthropic
except ImportError:
    Anthropic = None
    logging.warning("Anthropic SDK not installed. Run: pip install anthropic")

# ============================================================================
# 日誌配置
# ============================================================================

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ============================================================================
# Claude API Service
# ============================================================================

class ClaudeService:
    """
    Claude API 服務封裝 - SmartAdmin 專用代碼生成器

    功能:
    1. 生成符合 SmartAdmin 規範的 Java 後端代碼
    2. 生成符合 SmartAdmin 規範的 Vue 前端代碼
    3. 智能需求分析和優化建議
    """

    # 模型配置
    MODELS = {
        "sonnet": "claude-sonnet-4.5",
        "opus": "claude-opus-4",
        "haiku": "claude-haiku-3.5"
    }

    def __init__(
        self,
        api_key: Optional[str] = None,
        model: str = "sonnet",
        temperature: float = 0.2
    ):
        """
        初始化 Claude 服務

        Args:
            api_key: Claude API Key（如果不提供則從環境變量讀取）
            model: 模型名稱（sonnet, opus, haiku）
            temperature: 溫度參數（0.0-1.0，越低越確定性）
        """
        if Anthropic is None:
            raise RuntimeError(
                "Anthropic SDK not installed. "
                "Please run: pip install anthropic"
            )

        self.api_key = api_key or os.getenv('CLAUDE_API_KEY')
        if not self.api_key:
            raise ValueError(
                "CLAUDE_API_KEY not found. "
                "Please set it in environment or pass as parameter."
            )

        self.client = Anthropic(api_key=self.api_key)
        self.model = self.MODELS.get(model, self.MODELS["sonnet"])
        self.temperature = temperature

        logger.info(f"ClaudeService initialized with model: {self.model}")

    # ========================================================================
    # Java 後端代碼生成
    # ========================================================================

    def generate_java_backend(
        self,
        feature_spec: Dict[str, Any],
        context: Optional[Dict[str, str]] = None
    ) -> Dict[str, str]:
        """
        生成 Java 後端代碼（SmartAdmin 規範）

        Args:
            feature_spec: 功能規格
                {
                    "name": "Employee Management",
                    "entity": "Employee",
                    "endpoints": ["list", "add", "update", "delete"],
                    "fields": {
                        "name": {"type": "String", "required": True},
                        "email": {"type": "String", "required": True},
                        "departmentId": {"type": "Long", "required": True}
                    },
                    "validation": {"name": "required", "email": "email"}
                }

            context: 示例代碼上下文（可選）
                {
                    "path/to/example.java": "示例代碼內容..."
                }

        Returns:
            Dict[str, str]: {file_path: code_content}
                {
                    "sa-admin/src/.../EmployeeEntity.java": "代碼...",
                    "sa-admin/src/.../EmployeeDao.java": "代碼...",
                    ...
                }
        """
        logger.info(f"Generating Java backend for: {feature_spec['name']}")

        # 構建 Prompt
        prompt = self._build_backend_prompt(feature_spec, context or {})

        # 調用 Claude API
        try:
            response = self.client.messages.create(
                model=self.model,
                max_tokens=8000,
                temperature=self.temperature,
                messages=[{
                    "role": "user",
                    "content": prompt
                }]
            )

            # 提取文本內容
            content = response.content[0].text

            # 解析生成的代碼
            files = self._parse_generated_code(content)

            logger.info(f"Generated {len(files)} Java files")
            return files

        except Exception as e:
            logger.error(f"Claude API error: {e}")
            raise

    def _build_backend_prompt(
        self,
        feature_spec: Dict[str, Any],
        context: Dict[str, str]
    ) -> str:
        """
        構建 Java 後端代碼生成 Prompt

        這是核心 Prompt,包含 SmartAdmin 所有強制性規範
        """
        entity_name = feature_spec['entity']
        feature_name = feature_spec['name']
        endpoints = feature_spec.get('endpoints', [])
        fields = feature_spec.get('fields', {})

        # 構建字段定義
        fields_spec = self._format_fields_spec(fields)

        prompt = f"""You are an expert Java backend developer for SmartAdmin framework.

**Task**: Generate complete CRUD backend code following SmartAdmin strict conventions.

**Feature Specification**:
- Feature Name: {feature_name}
- Entity Name: {entity_name}
- Endpoints: {', '.join(endpoints)}
- Fields:
{fields_spec}

**MANDATORY SmartAdmin Patterns** (ArchUnit enforced):

1. **Layered Architecture**: Controller → Service → Manager → Dao
   - Controller: NEVER directly call Dao/Manager
   - Service: CAN call Dao for single-table CRUD, MUST call Manager when @Transactional needed
   - Manager: ONLY layer with @Transactional/@Cacheable

2. **Dependency Injection**:
   - Use `@RequiredArgsConstructor` + `private final` (NEVER @Autowired field injection)
   - Example:
   ```java
   @Service
   @RequiredArgsConstructor
   public class {entity_name}Service {{
       private final {entity_name}Dao {entity_name.lower()}Dao;
       private final {entity_name}Manager {entity_name.lower()}Manager; // if needed
   }}
   ```

3. **Return Types**:
   - Service layer: Use `io.vavr.control.Option` (NOT java.util.Optional)
   - Controller: Use `ResponseDTO.ok(data)` for all responses
   - Example:
   ```java
   // Service
   public Option<{entity_name}VO> getById(Long id) {{
       return {entity_name.lower()}Dao.selectById(id)
           .map(entity -> SmartBeanUtil.copy(entity, {entity_name}VO.class));
   }}

   // Controller
   @GetMapping("/{{id}}")
   public ResponseDTO<{entity_name}VO> getById(@PathVariable Long id) {{
       return ResponseDTO.ok({entity_name.lower()}Service.getById(id).getOrNull());
   }}
   ```

4. **Naming Conventions**:
   - Boolean fields: `deleted` NOT `isDeleted`
   - Classes: `{entity_name}Controller`, `{entity_name}Service`, `{entity_name}Manager`, `{entity_name}Dao`
   - Variables: camelCase
   - Constants: UPPER_SNAKE_CASE

5. **Transaction Management**:
   - Annotation: `@Transactional(rollbackFor = Throwable.class)`
   - Location: Manager layer ONLY
   - Example:
   ```java
   @Service
   @RequiredArgsConstructor
   public class {entity_name}Manager {{
       private final {entity_name}Dao {entity_name.lower()}Dao;

       @Transactional(rollbackFor = Throwable.class)
       public void batchInsert(List<{entity_name}Entity> entities) {{
           entities.forEach({entity_name.lower()}Dao::insert);
       }}
   }}
   ```

6. **Pagination**:
   - Use `SmartPageUtil.convert2PageQuery(form)` for queries
   - Return `PageResult<{entity_name}VO>` wrapped in `ResponseDTO.ok()`
   - Example:
   ```java
   @PostMapping("/query")
   public ResponseDTO<PageResult<{entity_name}VO>> query(@RequestBody {entity_name}QueryForm form) {{
       PageResult<{entity_name}VO> pageResult = {entity_name.lower()}Service.query(form);
       return ResponseDTO.ok(pageResult);
   }}
   ```

7. **Validation**:
   - Use `@Valid` on Controller parameters
   - Use `@NotNull`, `@NotBlank`, `@Email` etc. on Form fields
   - Example:
   ```java
   public class {entity_name}AddForm {{
       @NotBlank(message = "Name cannot be blank")
       private String name;

       @Email(message = "Invalid email format")
       private String email;
   }}
   ```

8. **Bean Conversion**:
   - Use `SmartBeanUtil.copy(source, Target.class)` or `SmartBeanUtil.copyList()`
   - NEVER manual field copying

9. **MyBatis-Plus**:
   - Entity uses `@TableName("t_{entity_name.lower()}")`
   - Dao extends `BaseMapper<{entity_name}Entity>`
   - Use `@TableId(type = IdType.AUTO)` for auto-increment
   - Use `@TableLogic` for soft delete (`deleted` field)

10. **Package Structure**:
    - Entity: `net.lab1024.sa.admin.module.business.{entity_name.lower()}.domain.entity`
    - Dao: `net.lab1024.sa.admin.module.business.{entity_name.lower()}.dao`
    - Manager: `net.lab1024.sa.admin.module.business.{entity_name.lower()}.manager`
    - Service: `net.lab1024.sa.admin.module.business.{entity_name.lower()}.service`
    - Controller: `net.lab1024.sa.admin.module.business.{entity_name.lower()}.controller`
    - Form/VO: `net.lab1024.sa.admin.module.business.{entity_name.lower()}.domain.form` / `.vo`

**Existing Code Context** (for reference):
{self._format_context(context)}

**Output Format**:
Generate ALL files using this EXACT format:

```java
// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/domain/entity/{entity_name}Entity.java
package net.lab1024.sa.admin.module.business.{entity_name.lower()}.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_{entity_name.lower()}")
public class {entity_name}Entity {{
    @TableId(type = IdType.AUTO)
    private Long id;

    // Add all fields from specification here

    @TableLogic
    private Boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}}

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/dao/{entity_name}Dao.java
// [Complete Dao code]

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/manager/{entity_name}Manager.java
// [Complete Manager code with @Transactional if needed]

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/service/{entity_name}Service.java
// [Complete Service code with io.vavr.control.Option]

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/controller/{entity_name}Controller.java
// [Complete Controller code with ResponseDTO]

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/domain/form/{entity_name}AddForm.java
// [Complete AddForm]

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/domain/form/{entity_name}UpdateForm.java
// [Complete UpdateForm]

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/domain/form/{entity_name}QueryForm.java
// [Complete QueryForm with PageParam]

// FILE: sa-admin/src/main/java/net/lab1024/sa/admin/module/business/{entity_name.lower()}/domain/vo/{entity_name}VO.java
// [Complete VO]
```

**CRITICAL REQUIREMENTS**:
1. Generate production-ready code. NO placeholders, NO TODOs.
2. Follow ALL SmartAdmin patterns exactly as specified above.
3. Use correct package names and imports.
4. Include proper Lombok annotations (@Data, @RequiredArgsConstructor).
5. Include proper validation annotations where needed.
6. Use Option instead of Optional in Service layer.
7. Use @Transactional ONLY in Manager layer.
8. Each file MUST start with `// FILE: <path>`.

Generate complete, production-ready code now."""

        return prompt

    def _format_fields_spec(self, fields: Dict[str, Any]) -> str:
        """格式化字段規格為 Prompt"""
        if not fields:
            return "  (No specific fields, use standard CRUD fields)"

        lines = []
        for field_name, field_spec in fields.items():
            field_type = field_spec.get('type', 'String')
            required = field_spec.get('required', False)
            description = field_spec.get('description', '')

            line = f"  - {field_name}: {field_type}"
            if required:
                line += " (required)"
            if description:
                line += f" - {description}"

            lines.append(line)

        return "\n".join(lines)

    def _format_context(self, context: Dict[str, str]) -> str:
        """格式化上下文示例代碼"""
        if not context:
            return "(No context provided)"

        lines = []
        for file_path, content in context.items():
            lines.append(f"File: {file_path}")
            lines.append("```java")
            # 只取前 50 行作為示例
            content_lines = content.split('\n')[:50]
            lines.extend(content_lines)
            if len(content.split('\n')) > 50:
                lines.append("... (truncated)")
            lines.append("```")
            lines.append("")

        return "\n".join(lines)

    def _parse_generated_code(self, content: str) -> Dict[str, str]:
        """
        解析 Claude 生成的代碼（FILE: 格式）

        Args:
            content: Claude 返回的文本內容

        Returns:
            Dict[str, str]: {file_path: code_content}
        """
        files = {}

        # 匹配 // FILE: path 格式
        # 正則表達式：匹配 // FILE: 開頭直到下一個 // FILE: 或字符串結尾
        pattern = r'//\s*FILE:\s*([^\n]+)\n(.*?)(?=//\s*FILE:|$)'
        matches = re.findall(pattern, content, re.DOTALL)

        logger.info(f"Found {len(matches)} file blocks in generated content")

        for file_path, code in matches:
            clean_path = file_path.strip()
            clean_code = code.strip()

            # 移除 markdown 代碼塊標記（如果存在）
            clean_code = re.sub(r'^```java\n', '', clean_code)
            clean_code = re.sub(r'^```\n', '', clean_code)
            clean_code = re.sub(r'\n```$', '', clean_code)
            clean_code = clean_code.strip()

            files[clean_path] = clean_code
            logger.debug(f"Parsed file: {clean_path} ({len(clean_code)} chars)")

        return files

    # ========================================================================
    # Vue 前端代碼生成
    # ========================================================================

    def generate_vue_frontend(
        self,
        feature_spec: Dict[str, Any],
        context: Optional[Dict[str, str]] = None
    ) -> Dict[str, str]:
        """
        生成 Vue 前端代碼（SmartAdmin 規範）

        Args:
            feature_spec: 功能規格
            context: 示例代碼上下文

        Returns:
            Dict[str, str]: {file_path: code_content}
        """
        logger.info(f"Generating Vue frontend for: {feature_spec['name']}")

        # TODO: 實現 Vue 前端代碼生成
        # 這裡先返回空字典，完整實現在後續版本
        logger.warning("Vue frontend generation not yet implemented")
        return {}

    # ========================================================================
    # 需求分析
    # ========================================================================

    def analyze_requirements(self, requirements: str) -> Dict[str, Any]:
        """
        分析自然語言需求，轉換為結構化 feature_spec

        Args:
            requirements: 自然語言需求描述

        Returns:
            Dict[str, Any]: 結構化的 feature_spec
        """
        logger.info("Analyzing requirements...")

        prompt = f"""Analyze the following feature requirements and extract structured information:

Requirements:
{requirements}

Extract and return a JSON object with this structure:
{{
    "name": "Feature name",
    "entity": "Main entity name (singular, PascalCase)",
    "endpoints": ["list", "add", "update", "delete", ...],
    "fields": {{
        "fieldName": {{
            "type": "Java type (String, Long, Integer, Boolean, LocalDateTime, etc.)",
            "required": true/false,
            "description": "Field description"
        }}
    }},
    "validation": {{
        "fieldName": "validation rule (required, email, min:5, max:100, etc.)"
    }},
    "relationships": {{
        "fieldName": {{
            "entity": "Related entity name",
            "type": "oneToMany/manyToOne/manyToMany"
        }}
    }}
}}

Return ONLY the JSON object, no additional text."""

        try:
            response = self.client.messages.create(
                model=self.model,
                max_tokens=2000,
                temperature=0.1,  # 低溫度確保一致性
                messages=[{
                    "role": "user",
                    "content": prompt
                }]
            )

            content = response.content[0].text

            # 提取 JSON
            json_match = re.search(r'\{.*\}', content, re.DOTALL)
            if json_match:
                feature_spec = json.loads(json_match.group())
                logger.info(f"Analyzed feature: {feature_spec.get('name')}")
                return feature_spec
            else:
                raise ValueError("Failed to extract JSON from response")

        except Exception as e:
            logger.error(f"Requirements analysis error: {e}")
            raise

    # ========================================================================
    # 工具方法
    # ========================================================================

    def estimate_tokens(self, text: str) -> int:
        """
        估算文本的 token 數量（粗略估算）

        Args:
            text: 文本內容

        Returns:
            int: 估算的 token 數量
        """
        # 粗略估算：1 token ≈ 4 個字符
        return len(text) // 4

    def estimate_cost(self, input_tokens: int, output_tokens: int) -> float:
        """
        估算 API 調用成本（USD）

        Args:
            input_tokens: 輸入 token 數量
            output_tokens: 輸出 token 數量

        Returns:
            float: 估算成本（美元）
        """
        # Claude Sonnet 4.5 定價（2026-01）
        # Input: $3 per million tokens
        # Output: $15 per million tokens
        input_cost = (input_tokens / 1_000_000) * 3.0
        output_cost = (output_tokens / 1_000_000) * 15.0

        return input_cost + output_cost


# ============================================================================
# 測試主函數
# ============================================================================

if __name__ == '__main__':
    print("=" * 70)
    print("Claude Service Test")
    print("=" * 70)
    print()

    # 檢查 API Key
    api_key = os.getenv('CLAUDE_API_KEY')
    if not api_key:
        print("❌ CLAUDE_API_KEY not set in environment")
        print("   Please run: export CLAUDE_API_KEY='your-api-key'")
        exit(1)

    print("✅ CLAUDE_API_KEY found")
    print()

    # 初始化服務
    try:
        service = ClaudeService(model="sonnet", temperature=0.2)
        print(f"✅ ClaudeService initialized (model: {service.model})")
        print()
    except Exception as e:
        print(f"❌ Failed to initialize ClaudeService: {e}")
        exit(1)

    # 測試需求分析
    print("Testing requirements analysis...")
    test_requirements = """
    Create an employee management system with the following features:
    - Employee name (required)
    - Email address (required, must be valid email)
    - Department (required, foreign key to Department table)
    - Phone number (optional)
    - Hire date (required)
    - Salary (optional, decimal)
    - Status (active/inactive)

    CRUD operations: list all employees, add new employee, update employee, delete employee
    """

    try:
        feature_spec = service.analyze_requirements(test_requirements)
        print("✅ Requirements analyzed successfully")
        print(json.dumps(feature_spec, indent=2, ensure_ascii=False))
        print()
    except Exception as e:
        print(f"❌ Requirements analysis failed: {e}")

    # 估算成本
    sample_text = "This is a sample text for token estimation"
    tokens = service.estimate_tokens(sample_text)
    print(f"Sample text token estimate: {tokens}")
    print(f"Estimated cost (1000 input, 5000 output tokens): ${service.estimate_cost(1000, 5000):.4f}")
    print()

    print("=" * 70)
    print("Test completed")
    print("=" * 70)
