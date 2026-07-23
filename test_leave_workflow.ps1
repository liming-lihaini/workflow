# ============================================================
# Leave Request Workflow E2E Test
# Scenario: DataModel -> Form -> Process -> Deploy -> Instances
#   A: <=3 days, direct leader approves -> COMPLETED
#   B: >3 days, direct leader + 2nd-level leader approves -> COMPLETED
#   C: Leader rejects -> rollback to applicant -> re-submit
#   D: Applicant cancels (terminate)
# ============================================================

$ErrorActionPreference = "Continue"
$base = "http://127.0.0.1:8080/api/v1"
$pass = 0; $fail = 0; $total = 0
$ts = Get-Date -Format "HHmmss"
$script:tmpBody = Join-Path $env:TEMP "leave_body_$ts.json"
$script:tmpResp = Join-Path $env:TEMP "leave_resp_$ts.json"

# Test users (from TestDataInitializer)
$applicant = "songjiang"    # user 100, dept 100
$leader    = "linchong"     # user 105, dept 101 (direct leader)
$leader2   = "liying"       # user 110, dept 110 (2nd-level leader)

$dmKey   = "leave_dm_$ts"
$formKey = "leave_form_$ts"
$procKey = "leave_proc_$ts"

function Log($s, $d) { Write-Host "`n[$s] $d" -ForegroundColor Cyan }
function Assert($c, $m) {
    $script:total++
    if ($c) { $script:pass++; Write-Host "  [PASS] $m" -ForegroundColor Green }
    else    { $script:fail++; Write-Host "  [FAIL] $m" -ForegroundColor Red }
}

# curl.exe-based POST to avoid PowerShell double-encoding
function CurlPost($url, $body) {
    $jsonStr = $body | ConvertTo-Json -Depth 20 -Compress
    [System.IO.File]::WriteAllText($script:tmpBody, $jsonStr, [System.Text.Encoding]::UTF8)
    $a = @('-s', '-X', 'POST', "$base$url",
           '-H', 'Content-Type: application/json; charset=utf-8',
           '-d', "@$($script:tmpBody)",
           '-o', $script:tmpResp)
    & curl.exe @a 2>&1 | Out-Null
    if (Test-Path $script:tmpResp) {
        return ([System.IO.File]::ReadAllText($script:tmpResp, [System.Text.Encoding]::UTF8) | ConvertFrom-Json)
    }
    return $null
}
function CurlPut($url, $body) {
    $jsonStr = $body | ConvertTo-Json -Depth 20 -Compress
    [System.IO.File]::WriteAllText($script:tmpBody, $jsonStr, [System.Text.Encoding]::UTF8)
    $a = @('-s', '-X', 'PUT', "$base$url",
           '-H', 'Content-Type: application/json; charset=utf-8',
           '-d', "@$($script:tmpBody)",
           '-o', $script:tmpResp)
    & curl.exe @a 2>&1 | Out-Null
    if (Test-Path $script:tmpResp) {
        return ([System.IO.File]::ReadAllText($script:tmpResp, [System.Text.Encoding]::UTF8) | ConvertFrom-Json)
    }
    return $null
}
function CurlGet($url) {
    $a = @('-s', "$base$url", '-o', $script:tmpResp)
    & curl.exe @a 2>&1 | Out-Null
    if (Test-Path $script:tmpResp) {
        return ([System.IO.File]::ReadAllText($script:tmpResp, [System.Text.Encoding]::UTF8) | ConvertFrom-Json)
    }
    return $null
}
function SafeCall($sb) { try { return (& $sb) } catch { return $null } }

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  LEAVE WORKFLOW E2E TEST ($ts)" -ForegroundColor Yellow
Write-Host "  Applicant=$applicant Leader=$leader Leader2=$leader2"
Write-Host "============================================" -ForegroundColor Yellow

# ============================================================
# STEP 1: Data Model Definition
# ============================================================
Log "Step1" "Create Leave Request Data Model"
$r = CurlPost "/data-models" @{
    modelKey = $dmKey; modelName = "Leave Request Model"
    mainTable = @{
        tableName = "leave_request"; label = "Leave Request"
        fields = @(
            @{ fieldKey = "reason";     label = "Reason";     type = "text";   required = $true }
            @{ fieldKey = "start_date"; label = "Start Date"; type = "date";   required = $true }
            @{ fieldKey = "end_date";   label = "End Date";   type = "date";   required = $true }
            @{ fieldKey = "days";       label = "Days";       type = "number"; required = $true }
            @{ fieldKey = "leave_type"; label = "Leave Type"; type = "text";   required = $false }
        )
    }; subTables = @()
}
Assert ($r.code -eq 0) "1.1 Create data model code=0"
Assert ($r.data.modelKey -eq $dmKey) "1.2 Model key correct"
Assert ($r.data.mainTable.fields.Count -eq 5) "1.3 Has 5 fields"

# Publish
$r2 = CurlPost "/data-models/$dmKey/publish" $null
Assert ($r2.code -eq 0) "1.4 Publish data model code=0"

# ============================================================
# STEP 2: Form Design
# ============================================================
Log "Step2" "Create Leave Request Form"
$r = CurlPost "/forms" @{
    formKey = $formKey; formName = "Leave Request Form"; category = "leave"
    modelKey = $dmKey
    formJson = (@{ fields = @(
        @{ id="f1"; key="reason";     label="Reason";     type="textarea"; required=$true }
        @{ id="f2"; key="start_date"; label="Start Date"; type="date";     required=$true }
        @{ id="f3"; key="end_date";   label="End Date";   type="date";     required=$true }
        @{ id="f4"; key="days";       label="Days";       type="number";   required=$true }
        @{ id="f5"; key="leave_type"; label="Leave Type"; type="select" }
    ); layout = "vertical" } | ConvertTo-Json -Depth 10)
}
Assert ($r.code -eq 0) "2.1 Create form code=0"
Assert ($r.data.modelKey -eq $dmKey) "2.2 Form bound to model"

# ============================================================
# STEP 3: Process Definition (with exclusive gateway)
# ============================================================
Log "Step3" "Create Process Definition with Gateway"

$pj = @{
    processKey = $procKey; processName = "Leave Approval Process"
    nodes = @(
        @{ id="n_start";         type="start";            name="Start";                 x=100; y=250 }
        @{ id="n_submit";        type="userTask";         name="Submit Leave"
           assignee=$applicant
           properties=@{ formKey=$formKey; formPermissions=@{
               fieldPermissions=@{ reason="edit"; start_date="edit"; end_date="edit"; days="edit"; leave_type="edit" }
           }}; x=300; y=250 }
        @{ id="n_gateway";       type="exclusiveGateway"; name="Days Check";            x=500; y=250 }
        @{ id="n_approve_short"; type="userTask";         name="Leader Approve (<=3d)"
           assignee=$leader
           properties=@{ formKey=$formKey; formPermissions=@{
               fieldPermissions=@{ reason="readonly"; start_date="readonly"; end_date="readonly"; days="readonly"; leave_type="readonly" }
           }}; x=700; y=150 }
        @{ id="n_approve_long";  type="userTask";         name="Leader Approve (>3d)"
           assignee=$leader
           properties=@{ formKey=$formKey; formPermissions=@{
               fieldPermissions=@{ reason="readonly"; start_date="readonly"; end_date="readonly"; days="readonly"; leave_type="readonly" }
           }}; x=700; y=350 }
        @{ id="n_approve_long2"; type="userTask";         name="2nd Leader Approve"
           assignee=$leader2
           properties=@{ formKey=$formKey; formPermissions=@{
               fieldPermissions=@{ reason="readonly"; start_date="readonly"; end_date="readonly"; days="readonly"; leave_type="readonly" }
           }}; x=900; y=350 }
        @{ id="n_end";           type="end";              name="End";                   x=1100; y=250 }
    )
    edges = @(
        @{ id="e1"; source="n_start";         target="n_submit" }
        @{ id="e2"; source="n_submit";        target="n_gateway" }
        @{ id="e3"; source="n_gateway";       target="n_approve_short"; condition="days <= 3"; label="<=3 days" }
        @{ id="e4"; source="n_gateway";       target="n_approve_long";  condition="days > 3";  label=">3 days" }
        @{ id="e5"; source="n_approve_short"; target="n_end" }
        @{ id="e6"; source="n_approve_long";  target="n_approve_long2" }
        @{ id="e7"; source="n_approve_long2"; target="n_end" }
    )
}

# 3a: Create definition
$r = CurlPost "/process/definitions" @{
    processKey = $procKey; processName = "Leave Approval Process"
    category = "leave"; processType = "approval"; createBy = "tester"
}
Assert ($r.code -eq 0) "3.1 Create process definition code=0"
$procId = $r.data.id

# 3b: Update with nodes/edges
$r = CurlPut "/process/definitions/$procId" @{
    processName = "Leave Approval Process"; processType = "approval"; category = "leave"
    processJson = ($pj | ConvertTo-Json -Depth 20)
}
Assert ($r.code -eq 0) "3.2 Update with nodes code=0"
$r2 = CurlGet "/process/definitions/$procId"
$parsed = $r2.data.processJson | ConvertFrom-Json
Assert ($parsed.nodes.Count -eq 7) "3.3 Has 7 nodes"
Assert ($parsed.edges.Count -eq 7) "3.4 Has 7 edges"
$gwEdges = $parsed.edges | Where-Object { $_.source -eq "n_gateway" }
Assert ($gwEdges.Count -eq 2) "3.5 Gateway has 2 conditional edges"

# 3c: Deploy
$r = CurlPost "/process/definitions/$procId/deploy" $null
Assert ($r.code -eq 0) "3.6 Deploy code=0"
Assert ($r.data.status -eq 1) "3.7 Status=1 (deployed)"

# ============================================================
# SCENARIO A: <=3 days, Direct Leader Approves
# ============================================================
Log "A" "Scenario A: 2-day leave, leader approves -> COMPLETED"

# A1: Start instance
$r = CurlPost "/process/instances" @{
    processKey = $procKey; businessKey = "LEAVE-A-$ts"; startUser = $applicant
    variables = @{
        applicant = $applicant; leader = $leader; leader2 = $leader2
        reason = "Family matter"; start_date = "2026-08-01"; end_date = "2026-08-02"
        days = 2; leave_type = "Personal"
    }
}
Assert ($r.code -eq 0) "A1.1 Start instance code=0"
$instA = $r.data.id
Assert ($r.data.status -eq 0) "A1.2 Instance running"

# A2: Applicant completes submit task
$r = CurlGet "/tasks/todo?userId=$applicant"
$aTask = $r.data | Where-Object { $_.processInstanceId -eq $instA -and $_.nodeId -eq "n_submit" }
$submitTaskId = if ($aTask -is [array]) { $aTask[0].id } else { $aTask.id }
Assert ($null -ne $submitTaskId) "A2.1 Applicant has submit task"

$r = CurlPost "/tasks/$submitTaskId/complete" @{ userId = $applicant; variables = @{ submitted = $true } }
Assert ($r.code -eq 0) "A2.2 Submit complete code=0"

# A3: Engine auto-passes gateway (days=2 <= 3) -> n_approve_short
$r = CurlGet "/tasks/todo?userId=$leader"
$leaderTask = $r.data | Where-Object { $_.processInstanceId -eq $instA -and $_.nodeId -eq "n_approve_short" }
$leaderTaskId = if ($leaderTask -is [array]) { $leaderTask[0].id } else { $leaderTask.id }
Assert ($null -ne $leaderTaskId) "A3.1 Leader has short-approval task"
Assert ($leaderTask.nodeName -eq "Leader Approve (<=3d)") "A3.2 Task is short-approval node"

# A4: Leader approves
$r = CurlPost "/tasks/$leaderTaskId/complete" @{ userId = $leader; variables = @{ approved = $true } }
Assert ($r.code -eq 0) "A4.1 Leader approve code=0"

# A5: Verify process COMPLETED
$r = CurlGet "/process/instances/$instA"
Assert ($r.code -eq 0) "A5.1 Get instance code=0"
Assert ($r.data.status -eq 1) "A5.2 Instance COMPLETED"
Assert ($null -ne $r.data.endTime) "A5.3 endTime is set"

# ============================================================
# SCENARIO B: >3 days, Direct Leader + 2nd-Level Leader
# ============================================================
Log "B" "Scenario B: 5-day leave, two leaders approve -> COMPLETED"

# B1: Start instance
$r = CurlPost "/process/instances" @{
    processKey = $procKey; businessKey = "LEAVE-B-$ts"; startUser = $applicant
    variables = @{
        applicant = $applicant; leader = $leader; leader2 = $leader2
        reason = "Vacation trip"; start_date = "2026-09-01"; end_date = "2026-09-05"
        days = 5; leave_type = "Annual"
    }
}
Assert ($r.code -eq 0) "B1.1 Start instance code=0"
$instB = $r.data.id

# B2: Applicant submits
$r = CurlGet "/tasks/todo?userId=$applicant"
$bTask = $r.data | Where-Object { $_.processInstanceId -eq $instB -and $_.nodeId -eq "n_submit" }
$submitBId = if ($bTask -is [array]) { $bTask[0].id } else { $bTask.id }
$r = CurlPost "/tasks/$submitBId/complete" @{ userId = $applicant; variables = @{ submitted = $true } }
Assert ($r.code -eq 0) "B2.1 Submit code=0"

# B3: Gateway routes to n_approve_long (days=5 > 3)
$r = CurlGet "/tasks/todo?userId=$leader"
$longTask = $r.data | Where-Object { $_.processInstanceId -eq $instB -and $_.nodeId -eq "n_approve_long" }
$longTaskId = if ($longTask -is [array]) { $longTask[0].id } else { $longTask.id }
Assert ($null -ne $longTaskId) "B3.1 Leader has long-approval task"
Assert ($longTask.nodeName -eq "Leader Approve (>3d)") "B3.2 Task is long-approval node"

# B4: Direct leader approves
$r = CurlPost "/tasks/$longTaskId/complete" @{ userId = $leader; variables = @{ approved = $true } }
Assert ($r.code -eq 0) "B4.1 Leader approve code=0"

# B5: 2nd-level leader gets task
$r = CurlGet "/tasks/todo?userId=$leader2"
$long2Task = $r.data | Where-Object { $_.processInstanceId -eq $instB -and $_.nodeId -eq "n_approve_long2" }
$long2TaskId = if ($long2Task -is [array]) { $long2Task[0].id } else { $long2Task.id }
Assert ($null -ne $long2TaskId) "B5.1 2nd-level leader has task"
Assert ($long2Task.nodeName -eq "2nd Leader Approve") "B5.2 Task is 2nd-level node"

# B6: 2nd-level leader approves
$r = CurlPost "/tasks/$long2TaskId/complete" @{ userId = $leader2; variables = @{ approved = $true } }
Assert ($r.code -eq 0) "B6.1 2nd leader approve code=0"

# B7: Verify process COMPLETED
$r = CurlGet "/process/instances/$instB"
Assert ($r.code -eq 0) "B7.1 Get instance code=0"
Assert ($r.data.status -eq 1) "B7.2 Instance COMPLETED"

# ============================================================
# SCENARIO C: Leader Rejects -> Rollback to Applicant
# ============================================================
Log "C" "Scenario C: Leader rejects, applicant re-submits"

# C1: Start instance (3 days)
$r = CurlPost "/process/instances" @{
    processKey = $procKey; businessKey = "LEAVE-C-$ts"; startUser = $applicant
    variables = @{
        applicant = $applicant; leader = $leader; leader2 = $leader2
        reason = "Sick leave"; start_date = "2026-10-01"; end_date = "2026-10-03"
        days = 3; leave_type = "Sick"
    }
}
Assert ($r.code -eq 0) "C1.1 Start instance code=0"
$instC = $r.data.id

# C2: Applicant submits
$r = CurlGet "/tasks/todo?userId=$applicant"
$cTask = $r.data | Where-Object { $_.processInstanceId -eq $instC -and $_.nodeId -eq "n_submit" }
$submitCId = if ($cTask -is [array]) { $cTask[0].id } else { $cTask.id }
$r = CurlPost "/tasks/$submitCId/complete" @{ userId = $applicant; variables = @{ submitted = $true } }
Assert ($r.code -eq 0) "C2.1 Submit code=0"

# C3: Leader rejects
$r = CurlGet "/tasks/todo?userId=$leader"
$rejectTask = $r.data | Where-Object { $_.processInstanceId -eq $instC }
$rejectTaskId = if ($rejectTask -is [array]) { $rejectTask[0].id } else { $rejectTask.id }
Assert ($null -ne $rejectTaskId) "C3.1 Leader has task to reject"

$r = CurlPost "/tasks/$rejectTaskId/reject" @{
    userId = $leader; comment = "Need more details"; targetNodeId = "n_submit"
}
Assert ($r.code -eq 0) "C3.2 Reject code=0"
Assert ($r.data.taskAction -eq 2) "C3.3 Task action=REJECTED"

# C4: Applicant gets a new submit task (rollback created)
$r = CurlGet "/tasks/todo?userId=$applicant"
$resubmitTask = $r.data | Where-Object { $_.processInstanceId -eq $instC -and $_.nodeId -eq "n_submit" }
$resubmitTaskId = if ($resubmitTask -is [array]) { $resubmitTask[0].id } else { $resubmitTask.id }
Assert ($null -ne $resubmitTaskId) "C4.1 Applicant has new submit task after reject"

# C5: Applicant re-submits
$r = CurlPost "/tasks/$resubmitTaskId/complete" @{ userId = $applicant; variables = @{ submitted = $true; revised = $true } }
Assert ($r.code -eq 0) "C5.1 Re-submit code=0"

# C6: Leader approves this time
$r = CurlGet "/tasks/todo?userId=$leader"
$approveTask = $r.data | Where-Object { $_.processInstanceId -eq $instC }
$approveTaskId = if ($approveTask -is [array]) { $approveTask[0].id } else { $approveTask.id }
Assert ($null -ne $approveTaskId) "C6.1 Leader has approval task"

$r = CurlPost "/tasks/$approveTaskId/complete" @{ userId = $leader; variables = @{ approved = $true } }
Assert ($r.code -eq 0) "C6.2 Leader approve code=0"

# C7: Verify COMPLETED
$r = CurlGet "/process/instances/$instC"
Assert ($r.data.status -eq 1) "C7.1 Instance COMPLETED after re-submit"

# ============================================================
# SCENARIO D: Applicant Cancels (Terminate)
# ============================================================
Log "D" "Scenario D: Applicant cancels process"

# D1: Start instance
$r = CurlPost "/process/instances" @{
    processKey = $procKey; businessKey = "LEAVE-D-$ts"; startUser = $applicant
    variables = @{
        applicant = $applicant; leader = $leader; leader2 = $leader2
        reason = "Personal"; start_date = "2026-11-01"; end_date = "2026-11-02"
        days = 1; leave_type = "Personal"
    }
}
Assert ($r.code -eq 0) "D1.1 Start instance code=0"
$instD = $r.data.id

# D2: Applicant submits
$r = CurlGet "/tasks/todo?userId=$applicant"
$dTask = $r.data | Where-Object { $_.processInstanceId -eq $instD -and $_.nodeId -eq "n_submit" }
$submitDId = if ($dTask -is [array]) { $dTask[0].id } else { $dTask.id }
$r = CurlPost "/tasks/$submitDId/complete" @{ userId = $applicant }
Assert ($r.code -eq 0) "D2.1 Submit code=0"

# D3: Applicant terminates (cancels)
$r = CurlPost "/process/instances/$instD/terminate" $null
Assert ($r.code -eq 0) "D3.1 Terminate code=0"
Assert ($r.data.status -eq 3) "D3.2 Instance TERMINATED"

# D4: Verify no more pending tasks for this instance
$r = CurlGet "/tasks/todo?userId=$leader"
$pendingD = $r.data | Where-Object { $_.processInstanceId -eq $instD }
Assert ($null -eq $pendingD) "D4.1 No pending tasks after terminate"

# ============================================================
# CLEANUP & SUMMARY
# ============================================================
Log "Cleanup" "SKIPPED - test data preserved"

# Cleanup temp files
if (Test-Path $script:tmpBody) { Remove-Item $script:tmpBody -Force }
if (Test-Path $script:tmpResp) { Remove-Item $script:tmpResp -Force }

Write-Host "`n============================================" -ForegroundColor Yellow
Write-Host "  RESULTS: $pass/$total passed, $fail failed" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  Step1-3: Setup (DataModel + Form + Process)"
Write-Host "  A: <=3 days direct leader approval"
Write-Host "  B: >3 days two-level approval"
Write-Host "  C: Reject + re-submit + approve"
Write-Host "  D: Applicant cancel (terminate)"
Write-Host "============================================`n" -ForegroundColor Yellow

if ($fail -eq 0) { Write-Host "ALL TESTS PASSED!" -ForegroundColor Green; exit 0 }
else { Write-Host "SOME TESTS FAILED!" -ForegroundColor Red; exit 1 }
