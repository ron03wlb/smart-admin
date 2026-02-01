# Example 1: Approval Workflow with LiteFlow

## Scenario
生成請假審批流程的 LiteFlow 規則（員工 → 主管 → 經理）

## Input
```bash
User: "Generate LiteFlow rule for leave approval workflow"
```

## Generated Output
```xml
<!-- leave-approval.el.xml -->
<flow>
  <chain name="leaveApprovalChain">
    THEN(
      validateLeaveRequest,
      checkEmployeeQuota,
      IF(
        needManagerApproval,
        THEN(supervisorApproval, managerApproval),
        supervisorApproval
      ),
      updateLeaveBalance,
      sendNotification
    )
  </chain>
</flow>
```

## Expected Result
- 自動化審批流程
- 支持條件分支（審批層級）
- 易於維護和擴展
