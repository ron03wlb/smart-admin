# LiteFlow Rule Builder - Baseline Test

## Test Scenario: Employee Approval Workflow

**Business Requirement:**
Implement a multi-tier approval workflow for new employee onboarding with the following rules:
1. Validate employee data (name, email, salary must be present)
2. Route to appropriate approval tier based on salary:
   - Salary < 50,000: HR approval only
   - Salary 50,000-150,000: Manager approval
   - Salary > 150,000: CEO approval required
3. Send notification email to approver
4. Log approval decision
5. If approved, create user account; if rejected, send rejection email

## Expected LiteFlow DSL Output

### 1. Chain Definition

**Chain Name:** Employee Approval Workflow
**Chain Code:** `employee-approval-chain`
**Chain Type:** 2 (Conditional)

**EL Expression:**
```javascript
THEN(
  validateEmployee,
  SWITCH(getApprovalTier).to(hrApproval, managerApproval, ceoApproval),
  IF(
    isApproved,
    THEN(
      createUserAccount,
      WHEN(sendApprovalEmail, updateEmployeeStatus)
    ),
    sendRejectionEmail
  ),
  logApprovalDecision
)
```

**Database Insert Statement:**
```sql
INSERT INTO t_liteflow_chain (
  chain_name,
  chain_code,
  chain_type,
  chain_data,
  status,
  remark,
  create_user_id,
  create_user_name
) VALUES (
  '员工审批流程',
  'employee-approval-chain',
  2,
  'THEN(validateEmployee, SWITCH(getApprovalTier).to(hrApproval, managerApproval, ceoApproval), IF(isApproved, THEN(createUserAccount, WHEN(sendApprovalEmail, updateEmployeeStatus)), sendRejectionEmail), logApprovalDecision)',
  1,
  '新员工入职审批流程，根据薪资自动路由到对应审批人',
  1,
  'admin'
);
```

---

### 2. Script Nodes

#### Script 1: Validate Employee

**Script Name:** 验证员工信息
**Script Code:** `validateEmployee`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
// Get employee from context
employee = context.getData("employee");

// Validation rules
if (employee == null) {
    throw new Exception("Employee data is required");
}

if (employee.name == null || employee.name.trim().isEmpty()) {
    throw new Exception("Employee name is required");
}

if (employee.email == null || !employee.email.contains("@")) {
    throw new Exception("Valid email address is required");
}

if (employee.salary == null || employee.salary <= 0) {
    throw new Exception("Valid salary is required");
}

// Log validation success
log.info("Employee validated: " + employee.name + " (Salary: " + employee.salary + ")");

return true;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '验证员工信息',
  'validateEmployee',
  'qlexpress',
  'employee = context.getData("employee");

if (employee == null) {
    throw new Exception("Employee data is required");
}

if (employee.name == null || employee.name.trim().isEmpty()) {
    throw new Exception("Employee name is required");
}

if (employee.email == null || !employee.email.contains("@")) {
    throw new Exception("Valid email address is required");
}

if (employee.salary == null || employee.salary <= 0) {
    throw new Exception("Valid salary is required");
}

log.info("Employee validated: " + employee.name + " (Salary: " + employee.salary + ")");
return true;',
  1,
  '验证员工数据完整性和有效性'
);
```

---

#### Script 2: Get Approval Tier (Router)

**Script Name:** 获取审批层级
**Script Code:** `getApprovalTier`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
// Get employee from context
employee = context.getData("employee");
salary = employee.salary;

// Determine approval tier based on salary
if (salary < 50000) {
    log.info("Routing to HR approval (Salary: " + salary + ")");
    return "hrApproval";
} else if (salary >= 50000 && salary <= 150000) {
    log.info("Routing to Manager approval (Salary: " + salary + ")");
    return "managerApproval";
} else {
    log.info("Routing to CEO approval (Salary: " + salary + ")");
    return "ceoApproval";
}
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '获取审批层级',
  'getApprovalTier',
  'qlexpress',
  'employee = context.getData("employee");
salary = employee.salary;

if (salary < 50000) {
    log.info("Routing to HR approval (Salary: " + salary + ")");
    return "hrApproval";
} else if (salary >= 50000 && salary <= 150000) {
    log.info("Routing to Manager approval (Salary: " + salary + ")");
    return "managerApproval";
} else {
    log.info("Routing to CEO approval (Salary: " + salary + ")");
    return "ceoApproval";
}',
  1,
  '根据薪资确定审批层级（HR/Manager/CEO）'
);
```

---

#### Script 3: HR Approval

**Script Name:** HR审批
**Script Code:** `hrApproval`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");

// Simulate HR approval logic (auto-approve for demo)
approvalDecision = true;

// Set approval result to context
context.setData("approvalResult", approvalDecision);
context.setData("approver", "HR Department");
context.setData("approvalTier", "HR");

log.info("HR approval completed for: " + employee.name + " - Decision: " + (approvalDecision ? "APPROVED" : "REJECTED"));

return approvalDecision;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  'HR审批',
  'hrApproval',
  'qlexpress',
  'employee = context.getData("employee");

approvalDecision = true;

context.setData("approvalResult", approvalDecision);
context.setData("approver", "HR Department");
context.setData("approvalTier", "HR");

log.info("HR approval completed for: " + employee.name + " - Decision: " + (approvalDecision ? "APPROVED" : "REJECTED"));

return approvalDecision;',
  1,
  'HR部门审批（薪资 < 50K）'
);
```

---

#### Script 4: Manager Approval

**Script Name:** 经理审批
**Script Code:** `managerApproval`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");

// Manager approval (requires manager review)
approvalDecision = true;

context.setData("approvalResult", approvalDecision);
context.setData("approver", "Department Manager");
context.setData("approvalTier", "Manager");

log.info("Manager approval completed for: " + employee.name + " - Decision: " + (approvalDecision ? "APPROVED" : "REJECTED"));

return approvalDecision;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '经理审批',
  'managerApproval',
  'qlexpress',
  'employee = context.getData("employee");

approvalDecision = true;

context.setData("approvalResult", approvalDecision);
context.setData("approver", "Department Manager");
context.setData("approvalTier", "Manager");

log.info("Manager approval completed for: " + employee.name + " - Decision: " + (approvalDecision ? "APPROVED" : "REJECTED"));

return approvalDecision;',
  1,
  '部门经理审批（薪资 50K-150K）'
);
```

---

#### Script 5: CEO Approval

**Script Name:** CEO审批
**Script Code:** `ceoApproval`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");

// CEO approval (highest tier)
approvalDecision = true;

context.setData("approvalResult", approvalDecision);
context.setData("approver", "CEO");
context.setData("approvalTier", "CEO");

log.info("CEO approval completed for: " + employee.name + " - Decision: " + (approvalDecision ? "APPROVED" : "REJECTED"));

return approvalDecision;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  'CEO审批',
  'ceoApproval',
  'qlexpress',
  'employee = context.getData("employee");

approvalDecision = true;

context.setData("approvalResult", approvalDecision);
context.setData("approver", "CEO");
context.setData("approvalTier", "CEO");

log.info("CEO approval completed for: " + employee.name + " - Decision: " + (approvalDecision ? "APPROVED" : "REJECTED"));

return approvalDecision;',
  1,
  'CEO审批（薪资 > 150K）'
);
```

---

#### Script 6: Check If Approved (Condition)

**Script Name:** 检查是否批准
**Script Code:** `isApproved`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
approvalResult = context.getData("approvalResult");

// Return boolean for IF condition
return approvalResult == true;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '检查是否批准',
  'isApproved',
  'qlexpress',
  'approvalResult = context.getData("approvalResult");
return approvalResult == true;',
  1,
  '判断审批是否通过的条件节点'
);
```

---

#### Script 7: Create User Account

**Script Name:** 创建用户账号
**Script Code:** `createUserAccount`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");

// Get user service from Spring context
userService = context.getBean("userService");

// Create user account
userAccount = userService.createAccount(employee.email, employee.name);

context.setData("userAccount", userAccount);
log.info("User account created for: " + employee.name + " (ID: " + userAccount.userId + ")");

return true;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '创建用户账号',
  'createUserAccount',
  'qlexpress',
  'employee = context.getData("employee");

userService = context.getBean("userService");
userAccount = userService.createAccount(employee.email, employee.name);

context.setData("userAccount", userAccount);
log.info("User account created for: " + employee.name + " (ID: " + userAccount.userId + ")");

return true;',
  1,
  '审批通过后创建系统用户账号'
);
```

---

#### Script 8: Send Approval Email

**Script Name:** 发送批准邮件
**Script Code:** `sendApprovalEmail`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");
approver = context.getData("approver");

emailService = context.getBean("emailService");

subject = "Welcome to the Company!";
body = "Dear " + employee.name + ", your employment has been approved by " + approver + ". Welcome aboard!";

emailService.send(employee.email, subject, body);

log.info("Approval email sent to: " + employee.email);
return true;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '发送批准邮件',
  'sendApprovalEmail',
  'qlexpress',
  'employee = context.getData("employee");
approver = context.getData("approver");

emailService = context.getBean("emailService");

subject = "Welcome to the Company!";
body = "Dear " + employee.name + ", your employment has been approved by " + approver + ". Welcome aboard!";

emailService.send(employee.email, subject, body);

log.info("Approval email sent to: " + employee.email);
return true;',
  1,
  '向员工发送审批通过的欢迎邮件'
);
```

---

#### Script 9: Update Employee Status

**Script Name:** 更新员工状态
**Script Code:** `updateEmployeeStatus`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");

employeeService = context.getBean("employeeService");
employeeService.updateStatus(employee.employeeId, "ACTIVE");

log.info("Employee status updated to ACTIVE for: " + employee.name);
return true;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '更新员工状态',
  'updateEmployeeStatus',
  'qlexpress',
  'employee = context.getData("employee");

employeeService = context.getBean("employeeService");
employeeService.updateStatus(employee.employeeId, "ACTIVE");

log.info("Employee status updated to ACTIVE for: " + employee.name);
return true;',
  1,
  '审批通过后更新员工状态为ACTIVE'
);
```

---

#### Script 10: Send Rejection Email

**Script Name:** 发送拒绝邮件
**Script Code:** `sendRejectionEmail`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");
approver = context.getData("approver");

emailService = context.getBean("emailService");

subject = "Application Status Update";
body = "Dear " + employee.name + ", unfortunately your application was not approved by " + approver + ". Please contact HR for details.";

emailService.send(employee.email, subject, body);

log.info("Rejection email sent to: " + employee.email);
return true;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '发送拒绝邮件',
  'sendRejectionEmail',
  'qlexpress',
  'employee = context.getData("employee");
approver = context.getData("approver");

emailService = context.getBean("emailService");

subject = "Application Status Update";
body = "Dear " + employee.name + ", unfortunately your application was not approved by " + approver + ". Please contact HR for details.";

emailService.send(employee.email, subject, body);

log.info("Rejection email sent to: " + employee.email);
return true;',
  1,
  '向员工发送审批拒绝通知邮件'
);
```

---

#### Script 11: Log Approval Decision

**Script Name:** 记录审批决策
**Script Code:** `logApprovalDecision`
**Script Type:** `qlexpress`

**Script Logic:**
```javascript
employee = context.getData("employee");
approvalResult = context.getData("approvalResult");
approver = context.getData("approver");
approvalTier = context.getData("approvalTier");

auditLogService = context.getBean("auditLogService");

logEntry = {
  "employeeId": employee.employeeId,
  "employeeName": employee.name,
  "salary": employee.salary,
  "approvalTier": approvalTier,
  "approver": approver,
  "decision": approvalResult ? "APPROVED" : "REJECTED",
  "timestamp": new Date()
};

auditLogService.log("EMPLOYEE_APPROVAL", logEntry);

log.info("Approval decision logged: " + employee.name + " - " + (approvalResult ? "APPROVED" : "REJECTED"));
return true;
```

**Database Insert:**
```sql
INSERT INTO t_liteflow_script (
  script_name,
  script_code,
  script_type,
  script_data,
  status,
  remark
) VALUES (
  '记录审批决策',
  'logApprovalDecision',
  'qlexpress',
  'employee = context.getData("employee");
approvalResult = context.getData("approvalResult");
approver = context.getData("approver");
approvalTier = context.getData("approvalTier");

auditLogService = context.getBean("auditLogService");

logEntry = {
  "employeeId": employee.employeeId,
  "employeeName": employee.name,
  "salary": employee.salary,
  "approvalTier": approvalTier,
  "approver": approver,
  "decision": approvalResult ? "APPROVED" : "REJECTED",
  "timestamp": new Date()
};

auditLogService.log("EMPLOYEE_APPROVAL", logEntry);

log.info("Approval decision logged: " + employee.name + " - " + (approvalResult ? "APPROVED" : "REJECTED"));
return true;',
  1,
  '记录审批决策到审计日志系统'
);
```

---

## Integration Code

### Service Layer

```java
package net.lab1024.sa.admin.module.system.employee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.system.employee.domain.form.EmployeeApprovalForm;
import net.lab1024.sa.admin.module.system.employee.domain.vo.EmployeeApprovalResultVO;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowExecutionForm;
import net.lab1024.sa.base.module.support.liteflow.domain.vo.LiteFlowExecutionResultVO;
import net.lab1024.sa.base.module.support.liteflow.service.LiteFlowExecutionService;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.base.common.code.UserErrorCode;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeApprovalService {

    private final LiteFlowExecutionService liteFlowExecutionService;

    public ResponseDTO<EmployeeApprovalResultVO> approveEmployee(EmployeeApprovalForm form) {
        // Execute LiteFlow chain
        LiteFlowExecutionForm executionForm = new LiteFlowExecutionForm();
        executionForm.setChainCode("employee-approval-chain");
        executionForm.setInputParams(Map.of(
            "employee", form  // Pass employee data to flow
        ));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(executionForm);

        if (!result.getOk()) {
            log.error("Employee approval flow failed: {}", result.getMsg());
            return ResponseDTO.error(UserErrorCode.BUSINESS_ERROR, result.getMsg());
        }

        // Extract results from flow execution
        LiteFlowExecutionResultVO executionResult = result.getData();
        Map<String, Object> outputResult = executionResult.getOutputResult();

        EmployeeApprovalResultVO approvalResult = new EmployeeApprovalResultVO();
        approvalResult.setApproved((Boolean) outputResult.get("approvalResult"));
        approvalResult.setApprover((String) outputResult.get("approver"));
        approvalResult.setApprovalTier((String) outputResult.get("approvalTier"));
        approvalResult.setExecutionTime(executionResult.getExecutionTime());

        log.info("Employee approval completed: {} - Decision: {}",
            form.getName(), approvalResult.getApproved() ? "APPROVED" : "REJECTED");

        return ResponseDTO.ok(approvalResult);
    }
}
```

---

## Test Execution

### Step 1: Load Flow Definitions

```bash
# Execute SQL inserts to create chain and all scripts
psql -U postgres -d smartadmin -f employee-approval-flow.sql
```

### Step 2: Reload Flow Engine

```bash
POST http://localhost:1024/liteflow/chain/reloadAll
```

**Expected Response:**
```json
{
  "ok": true,
  "code": 1,
  "msg": "操作成功",
  "data": "Flow engine reloaded successfully. Chains loaded: 1, Scripts loaded: 11"
}
```

### Step 3: Execute Test Cases

#### Test Case 1: HR Approval (Salary < 50K)

```bash
POST http://localhost:1024/liteflow/execution/execute
Content-Type: application/json

{
  "chainCode": "employee-approval-chain",
  "inputParams": {
    "employee": {
      "employeeId": 1001,
      "name": "Alice Johnson",
      "email": "alice@example.com",
      "salary": 45000
    }
  }
}
```

**Expected Output:**
- Validation passes
- Routes to `hrApproval`
- Approval decision: `true`
- User account created
- Approval email sent
- Employee status updated to ACTIVE
- Audit log created

**Expected Response:**
```json
{
  "ok": true,
  "code": 1,
  "msg": "操作成功",
  "data": {
    "executionStatus": 1,
    "executionTime": 85,
    "outputResult": {
      "approvalResult": true,
      "approver": "HR Department",
      "approvalTier": "HR"
    }
  }
}
```

---

#### Test Case 2: Manager Approval (Salary 50K-150K)

```bash
POST http://localhost:1024/liteflow/execution/execute
Content-Type: application/json

{
  "chainCode": "employee-approval-chain",
  "inputParams": {
    "employee": {
      "employeeId": 1002,
      "name": "Bob Smith",
      "email": "bob@example.com",
      "salary": 95000
    }
  }
}
```

**Expected Output:**
- Routes to `managerApproval`
- Approver: "Department Manager"
- Approval tier: "Manager"

---

#### Test Case 3: CEO Approval (Salary > 150K)

```bash
POST http://localhost:1024/liteflow/execution/execute
Content-Type: application/json

{
  "chainCode": "employee-approval-chain",
  "inputParams": {
    "employee": {
      "employeeId": 1003,
      "name": "Carol Williams",
      "email": "carol@example.com",
      "salary": 180000
    }
  }
}
```

**Expected Output:**
- Routes to `ceoApproval`
- Approver: "CEO"
- Approval tier: "CEO"

---

#### Test Case 4: Validation Failure (Invalid Email)

```bash
POST http://localhost:1024/liteflow/execution/execute
Content-Type: application/json

{
  "chainCode": "employee-approval-chain",
  "inputParams": {
    "employee": {
      "employeeId": 1004,
      "name": "David Brown",
      "email": "invalid-email",
      "salary": 60000
    }
  }
}
```

**Expected Output:**
- Validation fails with error: "Valid email address is required"
- Chain execution stops
- No approval process triggered

**Expected Response:**
```json
{
  "ok": false,
  "code": -1,
  "msg": "Valid email address is required",
  "data": null
}
```

---

## Success Criteria

### Functional Requirements
- ✅ All test cases execute successfully
- ✅ Correct approval tier routing based on salary
- ✅ Validation catches invalid input
- ✅ User account created on approval
- ✅ Correct emails sent (approval/rejection)
- ✅ Audit logs created with complete data

### Performance Requirements
- ✅ Execution time < 100ms (P95)
- ✅ Chain and scripts loaded from database
- ✅ Hot-reload works without server restart

### Quality Requirements
- ✅ No errors in execution logs
- ✅ All scripts execute without syntax errors
- ✅ Context data passed correctly between nodes
- ✅ Spring beans accessible from QLExpress scripts

---

## Visual Flow Diagram

```
[START]
   ↓
[validateEmployee]
   ↓
[getApprovalTier] ─────┬───────> [hrApproval] (Salary < 50K)
                       ├───────> [managerApproval] (50K-150K)
                       └───────> [ceoApproval] (> 150K)
                                      ↓
                                [isApproved]
                                      ↓
                     ┌────────────────┴────────────────┐
                     ▼                                 ▼
                 [APPROVED]                       [REJECTED]
                     ↓                                 ↓
            [createUserAccount]              [sendRejectionEmail]
                     ↓
        ┌────────────┴────────────┐
        ▼                         ▼
  [sendApprovalEmail]    [updateEmployeeStatus]
        └────────────┬────────────┘
                     ↓
            [logApprovalDecision]
                     ↓
                  [END]
```

---

## Version Information

**Test Version:** 1.0.0
**Created:** 2026-01-25
**SmartAdmin Version:** v4.0.0+
**LiteFlow Version:** 2.15.3

**Test Maintainer:** SmartAdmin AI Team
