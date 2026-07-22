# ============================================================
# E2E Test: Process Definition Full Workflow
# 1. Create process
# 2. Configure form (select/modify)
# 3. Configure nodes (form permissions + handler types)
# 4. Process test (deploy + start + complete)
# 5. Process replay (history query)
# ============================================================

$ErrorActionPreference = "Continue"
$base = "http://localhost:8080/api/v1"
$pass = 0; $fail = 0; $total = 0
$ts = Get-Date -Format "HHmmss"

$dmKey = "pd_dm_$ts"
$formKey1 = "pd_form1_$ts"
$formKey2 = "pd_form2_$ts"
$procKey = "pd_proc_$ts"

function Log($s, $d) { Write-Host "`n[$s] $d" -ForegroundColor Cyan }
function Assert($c, $m) {
    $script:total++
    if ($c) { $script:pass++; Write-Host "  [PASS] $m" -ForegroundColor Green }
    else { $script:fail++; Write-Host "  [FAIL] $m" -ForegroundColor Red }
}
function PostJson($url, $body) {
    if ($null -eq $body) { return Invoke-RestMethod -Uri "$base$url" -Method Post -ContentType "application/json; charset=utf-8" }
    $b = [System.Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 20))
    return Invoke-RestMethod -Uri "$base$url" -Method Post -Body $b -ContentType "application/json; charset=utf-8"
}
function GetApi($url) { return Invoke-RestMethod -Uri "$base$url" -Method Get }
function PutJson($url, $body) {
    $b = [System.Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 20))
    return Invoke-RestMethod -Uri "$base$url" -Method Put -Body $b -ContentType "application/json; charset=utf-8"
}
function SafeCall($scriptBlock) { try { return (& $scriptBlock) } catch { return $null } }

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  PROCESS DEFINITION TEST ($ts)" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

# ============================================================
# T1: Setup - Create Data Model + Form1 + Form2
# ============================================================
Log "T1" "Setup: Data Model + Two Forms"

# Create model
$r = PostJson "/data-models" @{
    modelKey = $dmKey; modelName = "PD Test Model"
    mainTable = @{ tableName = "pd_form"; label = "PD Form"; fields = @(
        @{ fieldKey = "title"; label = "Title"; type = "text"; required = $true }
        @{ fieldKey = "reason"; label = "Reason"; type = "text"; required = $true }
        @{ fieldKey = "approver"; label = "Approver"; type = "person"; required = $false }
    )}; subTables = @()
}
Assert ($r.code -eq 0) "T1.1 Create model"
PostJson "/data-models/$dmKey/publish" $null | Out-Null

# Create Form 1
$r = PostJson "/forms" @{ formKey = $formKey1; formName = "Form A"; category = "test"; modelKey = $dmKey; formJson = '{"sections":[]}' }
Assert ($r.code -eq 0) "T1.2 Create Form A"

# Create Form 2
$r = PostJson "/forms" @{ formKey = $formKey2; formName = "Form B"; category = "test"; modelKey = $dmKey; formJson = '{"sections":[]}' }
Assert ($r.code -eq 0) "T1.3 Create Form B"

# ============================================================
# T2: Create Process Definition
# ============================================================
Log "T2" "Create Process Definition"

$r = PostJson "/process/definitions" @{
    processKey = $procKey; processName = "PD Test Process"
    category = "test"; processType = "approval"; createBy = "tester"
}
Assert ($r.code -eq 0) "T2.1 Create process"
$procId = $r.data.id
Assert ($r.data.status -eq 0) "T2.2 Status=draft(0)"

# ============================================================
# T3: Configure Process Form (Select + Modify)
# ============================================================
Log "T3" "Configure Process Form"

# Bind Form A
$r = PutJson "/process/definitions/$procId" @{
    processName = "PD Test Process"; processType = "approval"; category = "test"
    processJson = (@{
        processKey = $procKey; formKey = $formKey1
        nodes = @(); edges = @()
    } | ConvertTo-Json -Depth 10)
}
Assert ($r.code -eq 0) "T3.1 Bind Form A"

# Verify form binding
$r2 = GetApi "/process/definitions/$procId"
$parsed = $r2.data.processJson | ConvertFrom-Json
Assert ($parsed.formKey -eq $formKey1) "T3.2 Form A bound"

# Modify to Form B
$r = PutJson "/process/definitions/$procId" @{
    processName = "PD Test Process"; processType = "approval"; category = "test"
    processJson = (@{
        processKey = $procKey; formKey = $formKey2
        nodes = @(); edges = @()
    } | ConvertTo-Json -Depth 10)
}
Assert ($r.code -eq 0) "T3.3 Modify to Form B"

$r2 = GetApi "/process/definitions/$procId"
$parsed = $r2.data.processJson | ConvertFrom-Json
Assert ($parsed.formKey -eq $formKey2) "T3.4 Form B now bound"

# ============================================================
# T4: Configure Nodes with Form Permissions + Handler Types
# ============================================================
Log "T4" "Configure Nodes (Permissions + Handler Types)"

$processJson = @{
    processKey = $procKey
    formKey = $formKey2
    nodes = @(
        @{ id="n_start"; type="start"; name="Start"; x=100; y=200 }
        # Node 1: Handler = direct user, permissions = edit
        @{ id="n_submit"; type="userTask"; name="Submit Application"
           assignee = "applicant"; candidateUsers = $null
           properties = @{
               formKey = $formKey2
               assigneeType = "user"
               formPermissions = @{
                   fieldPermissions = @{ title="edit"; reason="edit"; approver="edit" }
                   buttonPermissions = @{ submit=@{visible=$true;enabled=$true} }
               }
           }; x=300; y=200
        }
        # Node 2: Handler = role, permissions = readonly + approve button
        @{ id="n_review"; type="userTask"; name="Manager Review"
           assignee = $null; candidateUsers = "manager,reviewer"
           properties = @{
               formKey = $formKey2
               assigneeType = "role"
               assigneeRoleKey = "manager"
               formPermissions = @{
                   fieldPermissions = @{ title="readonly"; reason="readonly"; approver="hide" }
                   buttonPermissions = @{ submit=@{visible=$true;enabled=$true}; reject=@{visible=$true;enabled=$true} }
               }
           }; x=500; y=200
        }
        # Node 3: Handler = dept leader, permissions = readonly
        @{ id="n_approve"; type="userTask"; name="Dept Leader Approve"
           assignee = $null; candidateUsers = $null
           properties = @{
               formKey = $formKey2
               assigneeType = "deptLeader"
               formPermissions = @{
                   fieldPermissions = @{ title="readonly"; reason="readonly"; approver="readonly" }
                   buttonPermissions = @{ submit=@{visible=$true;enabled=$true}; reject=@{visible=$true;enabled=$true} }
               }
           }; x=700; y=200
        }
        # Node 4: Handler = form field person, permissions = hide
        @{ id="n_notify"; type="userTask"; name="Notify Approver"
           assignee = $null; candidateUsers = $null
           properties = @{
               formKey = $formKey2
               assigneeType = "formField"
               assigneeFormField = "approver"
               formPermissions = @{
                   fieldPermissions = @{ title="hide"; reason="hide"; approver="hide" }
                   buttonPermissions = @{ submit=@{visible=$true;enabled=$true} }
               }
           }; x=900; y=200
        }
        @{ id="n_end"; type="end"; name="End"; x=1100; y=200 }
    )
    edges = @(
        @{ id="e1"; source="n_start"; target="n_submit" }
        @{ id="e2"; source="n_submit"; target="n_review" }
        @{ id="e3"; source="n_review"; target="n_approve" }
        @{ id="e4"; source="n_approve"; target="n_notify" }
        @{ id="e5"; source="n_notify"; target="n_end" }
    )
}

$r = PutJson "/process/definitions/$procId" @{
    processName = "PD Test Process"; processType = "approval"; category = "test"
    processJson = ($processJson | ConvertTo-Json -Depth 20)
}
Assert ($r.code -eq 0) "T4.1 Save nodes with permissions"

# Verify node structure
$r2 = GetApi "/process/definitions/$procId"
$parsed = $r2.data.processJson | ConvertFrom-Json
Assert ($parsed.nodes.Count -eq 6) "T4.2 Has 6 nodes (start+4userTask+end)"
Assert ($parsed.edges.Count -eq 5) "T4.3 Has 5 edges"

# Verify handler types in properties
$submitNode = $parsed.nodes | Where-Object { $_.id -eq "n_submit" }
Assert ($submitNode.properties.assigneeType -eq "user") "T4.4 n_submit: assigneeType=user"
Assert ($submitNode.assignee -eq "applicant") "T4.5 n_submit: assignee=applicant"

$reviewNode = $parsed.nodes | Where-Object { $_.id -eq "n_review" }
Assert ($reviewNode.properties.assigneeType -eq "role") "T4.6 n_review: assigneeType=role"
Assert ($reviewNode.properties.assigneeRoleKey -eq "manager") "T4.7 n_review: role=manager"

$approveNode = $parsed.nodes | Where-Object { $_.id -eq "n_approve" }
Assert ($approveNode.properties.assigneeType -eq "deptLeader") "T4.8 n_approve: assigneeType=deptLeader"

$notifyNode = $parsed.nodes | Where-Object { $_.id -eq "n_notify" }
Assert ($notifyNode.properties.assigneeType -eq "formField") "T4.9 n_notify: assigneeType=formField"
Assert ($notifyNode.properties.assigneeFormField -eq "approver") "T4.10 n_notify: field=approver"

# ============================================================
# T5: Verify Form Permissions Configuration
# ============================================================
Log "T5" "Verify Form Permissions"

# Submit node: edit permissions
Assert ($submitNode.properties.formPermissions.fieldPermissions.title -eq "edit") "T5.1 submit: title=edit"
Assert ($submitNode.properties.formPermissions.fieldPermissions.reason -eq "edit") "T5.2 submit: reason=edit"

# Review node: readonly + hide
Assert ($reviewNode.properties.formPermissions.fieldPermissions.title -eq "readonly") "T5.3 review: title=readonly"
Assert ($reviewNode.properties.formPermissions.fieldPermissions.approver -eq "hide") "T5.4 review: approver=hide"

# Notify node: all hide
Assert ($notifyNode.properties.formPermissions.fieldPermissions.title -eq "hide") "T5.5 notify: title=hide"
Assert ($notifyNode.properties.formPermissions.fieldPermissions.reason -eq "hide") "T5.6 notify: reason=hide"

# Button permissions
Assert ($submitNode.properties.formPermissions.buttonPermissions.submit.visible -eq $true) "T5.7 submit btn: visible"
Assert ($reviewNode.properties.formPermissions.buttonPermissions.reject.enabled -eq $true) "T5.8 reject btn: enabled"

# ============================================================
# T6: Deploy Process
# ============================================================
Log "T6" "Deploy Process"

$r = PostJson "/process/definitions/$procId/deploy" $null
Assert ($r.code -eq 0) "T6.1 Deploy success"
Assert ($r.data.status -eq 1) "T6.2 Status=deployed(1)"

# ============================================================
# T7: Process Test - Start Instance & Complete Tasks
# ============================================================
Log "T7" "Process Test: Start + Complete Tasks"

$r = PostJson "/process/instances" @{
    processKey = $procKey; businessKey = "PD_TEST_$ts"; startUser = "applicant"
    variables = @{ title="Test Request"; reason="Testing"; approver="notifier" }
}
Assert ($r.code -eq 0) "T7.1 Start instance"
$instanceId = $r.data.id
Assert ($r.data.status -eq 0) "T7.2 Instance status=running(0)"

# Verify applicant task created
$r = GetApi "/tasks/todo?userId=applicant"
$aTask = $r.data | Where-Object { $_.processInstanceId -eq $instanceId }
$applicantTaskId = if ($aTask -is [array]) { $aTask[0].id } else { $aTask.id }
Assert ($null -ne $applicantTaskId) "T7.3 Applicant has task"

# Complete applicant task
$r = PostJson "/tasks/$applicantTaskId/complete" @{ userId="applicant"; variables=@{submitted=$true} }
Assert ($r.code -eq 0) "T7.4 Complete submit task"
Assert ($r.data.status -eq 2) "T7.5 Task status=completed(2)"

# Verify review task created for candidates
$r = GetApi "/tasks/todo?userId=manager"
$mTask = $r.data | Where-Object { $_.processInstanceId -eq $instanceId }
$managerTaskId = if ($mTask -is [array]) { $mTask[0].id } else { $mTask.id }
Assert ($null -ne $managerTaskId) "T7.6 Manager has review task"

# Get form permissions for review task
$r = GetApi "/tasks/$managerTaskId/form-permissions"
Assert ($r.code -eq 0) "T7.7 Get form permissions"
Assert ($null -ne $r.data.fieldPermissions) "T7.8 Has field permissions"
$titlePerm = $r.data.fieldPermissions | Where-Object { $_.fieldKey -eq "title" }
Assert ($null -ne $titlePerm) "T7.9 Title field present in permissions"

# Complete review task
$r = PostJson "/tasks/$managerTaskId/complete" @{ userId="manager"; variables=@{reviewed=$true} }
Assert ($r.code -eq 0) "T7.10 Complete review task"

# Verify instance still running (more nodes to go)
$r = GetApi "/process/instances/$instanceId"
Assert ($r.data.status -eq 0) "T7.11 Instance still running"

# ============================================================
# T8: Process Replay - Query History
# ============================================================
Log "T8" "Process Replay / History Query"

# Get instance details
$r = GetApi "/process/instances/$instanceId"
Assert ($r.code -eq 0) "T8.1 Get instance detail"
Assert ($r.data.processKey -eq $procKey) "T8.2 Process key matches"
Assert ($null -ne $r.data.startTime) "T8.3 Has startTime"

# Get variables
$r = GetApi "/process/instances/$instanceId/variables"
Assert ($r.code -eq 0) "T8.4 Get variables"
Assert ($r.data.title -eq "Test Request") "T8.5 Variable title preserved"
Assert ($r.data.submitted -eq $true) "T8.6 Submitted flag set"

# Get done tasks for applicant
$r = GetApi "/tasks/done?userId=applicant"
$doneTask = $r.data | Where-Object { $_.processInstanceId -eq $instanceId }
Assert ($null -ne $doneTask) "T8.7 Applicant done task exists"

# Get done tasks for manager
$r = GetApi "/tasks/done?userId=manager"
$doneMgr = $r.data | Where-Object { $_.processInstanceId -eq $instanceId }
Assert ($null -ne $doneMgr) "T8.8 Manager done task exists"

# List all process instances
$r = GetApi "/process/instances?processKey=$procKey"
Assert ($r.code -eq 0) "T8.9 List instances by processKey"
Assert ($r.data.Count -ge 1) "T8.10 Has at least 1 instance"

# ============================================================
# T9: Export Process Definition
# ============================================================
Log "T9" "Export Process"

$r = GetApi "/process/definitions/$procId/export"
Assert ($r.code -eq 0) "T9.1 Export success"
Assert ($null -ne $r.data.processJson) "T9.2 Export has processJson"

$exportedJson = $r.data.processJson | ConvertFrom-Json
Assert ($exportedJson.nodes.Count -eq 6) "T9.3 Exported nodes count"
Assert ($exportedJson.formKey -eq $formKey2) "T9.4 Exported formKey"

# ============================================================
# SUMMARY
# ============================================================
Write-Host "`n============================================" -ForegroundColor Yellow
Write-Host "  RESULTS: $pass/$total passed, $fail failed" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  T1: Setup (Model + Forms)"
Write-Host "  T2: Create Process Definition"
Write-Host "  T3: Configure Form (Select + Modify)"
Write-Host "  T4: Node Config (Permissions + Handler Types)"
Write-Host "  T5: Form Permissions Verification"
Write-Host "  T6: Deploy Process"
Write-Host "  T7: Process Test (Start + Complete)"
Write-Host "  T8: Process Replay / History"
Write-Host "  T9: Export Process"
Write-Host "============================================`n" -ForegroundColor Yellow

if ($fail -eq 0) { Write-Host "ALL TESTS PASSED!" -ForegroundColor Green; exit 0 }
else { Write-Host "SOME TESTS FAILED!" -ForegroundColor Red; exit 1 }
