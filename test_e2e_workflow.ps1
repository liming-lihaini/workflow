# ============================================================
# E2E Automated Test: Complete Workflow Scenario
# Covers: Data Model -> Form -> Process -> Nodes -> Permissions
#         -> Deploy -> Instance -> Tasks -> Completion
# Plus: Negative/Edge cases (B1-B8)
# ============================================================

$ErrorActionPreference = "Continue"
$base = "http://localhost:8080/api/v1"
$pass = 0; $fail = 0; $total = 0
$ts = Get-Date -Format "HHmmss"

$dmKey = "e2e_dm_$ts"
$formKey = "e2e_form_$ts"
$procKey = "e2e_proc_$ts"

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
function SafeCall($scriptBlock) {
    try { return (& $scriptBlock) } catch { return $null }
}

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  E2E TEST SUITE ($ts)" -ForegroundColor Yellow
Write-Host "  Keys: dm=$dmKey form=$formKey proc=$procKey"
Write-Host "============================================" -ForegroundColor Yellow

# ============================================================
# PART A: HAPPY PATH (12 Steps)
# ============================================================

# --- A1: Data Model Definition ---
Log "A1" "Create Data Model (5 fields)"
$r = PostJson "/data-models" @{
    modelKey = $dmKey; modelName = "E2E Leave Model"
    mainTable = @{
        tableName = "leave"; label = "Leave Form"
        fields = @(
            @{ fieldKey = "reason"; label = "Reason"; type = "text"; required = $true }
            @{ fieldKey = "start_date"; label = "Start"; type = "date"; required = $true }
            @{ fieldKey = "end_date"; label = "End"; type = "date"; required = $true }
            @{ fieldKey = "days"; label = "Days"; type = "number"; required = $true }
            @{ fieldKey = "leave_type"; label = "Type"; type = "text"; required = $false }
        )
    }; subTables = @()
}
Assert ($r.code -eq 0) "A1.1 Create returns code=0"
Assert ($r.data.modelKey -eq $dmKey) "A1.2 Model key correct"
Assert ($r.data.mainTable.fields.Count -eq 5) "A1.3 Has 5 fields"
Assert ((GetApi "/data-models/$dmKey").code -eq 0) "A1.4 Get by key works"

# --- A2: Form Design ---
Log "A2" "Create Form (bound to data model)"
$r = PostJson "/forms" @{
    formKey = $formKey; formName = "E2E Leave Form"; category = "leave"
    modelKey = $dmKey
    formJson = (@{ fields = @(
        @{ id = "f1"; key = "reason"; label = "Reason"; type = "textarea"; required = $true }
        @{ id = "f2"; key = "start_date"; label = "Start"; type = "date"; required = $true }
        @{ id = "f3"; key = "end_date"; label = "End"; type = "date"; required = $true }
        @{ id = "f4"; key = "days"; label = "Days"; type = "number"; required = $true }
        @{ id = "f5"; key = "leave_type"; label = "Type"; type = "select" }
    ); layout = "vertical" } | ConvertTo-Json -Depth 10)
}
Assert ($r.code -eq 0) "A2.1 Create form code=0"
Assert ($r.data.modelKey -eq $dmKey) "A2.2 Form bound to model"
Assert ((GetApi "/forms/$formKey").code -eq 0) "A2.3 Get form by key"

# --- A3: Process Definition ---
Log "A3" "Create Process Definition"
$r = PostJson "/process/definitions" @{
    processKey = $procKey; processName = "E2E Leave Approval"
    category = "leave"; processType = "approval"; createBy = "tester"
}
Assert ($r.code -eq 0) "A3.1 Create code=0"
$procId = $r.data.id
Assert ($r.data.status -eq 0) "A3.2 Status=0 (draft)"

# --- A4: Define Nodes + Form Binding + Permissions ---
Log "A4" "Define Nodes, Bind Form, Set Permissions"
$pj = @{
    processKey = $procKey; processName = "E2E Leave Approval"
    nodes = @(
        @{ id = "n_start"; type = "start"; name = "Start"; x=100; y=200 }
        @{ id = "n_submit"; type = "userTask"; name = "Submit"; assignee = "applicant"
           properties = @{ formKey = $formKey; formPermissions = @{
               fieldPermissions = @{ reason="edit"; start_date="edit"; end_date="edit"; days="edit"; leave_type="edit" }
               buttonPermissions = @{ submit=@{visible=$true;enabled=$true}; reject=@{visible=$false;enabled=$false} }
           }}; x=300; y=200 }
        @{ id = "n_approve"; type = "userTask"; name = "Approve"; assignee = "manager"; candidateUsers = "manager"
           properties = @{ formKey = $formKey; formPermissions = @{
               fieldPermissions = @{ reason="readonly"; start_date="readonly"; end_date="readonly"; days="readonly"; leave_type="readonly" }
               buttonPermissions = @{ submit=@{visible=$true;enabled=$true}; reject=@{visible=$true;enabled=$true}; transfer=@{visible=$true;enabled=$true} }
           }}; x=500; y=200 }
        @{ id = "n_end"; type = "end"; name = "End"; x=700; y=200 }
    )
    edges = @(
        @{ id="e1"; source="n_start"; target="n_submit" }
        @{ id="e2"; source="n_submit"; target="n_approve" }
        @{ id="e3"; source="n_approve"; target="n_end" }
    )
}
$r = PutJson "/process/definitions/$procId" @{ processName="E2E Leave Approval"; processType="approval"; category="leave"; processJson=($pj | ConvertTo-Json -Depth 20) }
Assert ($r.code -eq 0) "A4.1 Update with nodes code=0"
$r2 = GetApi "/process/definitions/$procId"
$parsed = $r2.data.processJson | ConvertFrom-Json
Assert ($parsed.nodes.Count -eq 4) "A4.2 4 nodes"
Assert ($parsed.edges.Count -eq 3) "A4.3 3 edges"
Assert ($parsed.nodes[1].properties.formKey -eq $formKey) "A4.4 Submit node formKey bound"
Assert ($parsed.nodes[2].properties.formKey -eq $formKey) "A4.5 Approve node formKey bound"
Assert ($parsed.nodes[1].properties.formPermissions.fieldPermissions.reason -eq "edit") "A4.6 Submit: reason=edit"
Assert ($parsed.nodes[2].properties.formPermissions.fieldPermissions.reason -eq "readonly") "A4.7 Approve: reason=readonly"

# --- A5: Deploy Process ---
Log "A5" "Deploy Process"
$r = PostJson "/process/definitions/$procId/deploy" $null
Assert ($r.code -eq 0) "A5.1 Deploy code=0"
Assert ($r.data.status -eq 1) "A5.2 Status=1 (deployed)"
$r2 = GetApi "/process/definitions/key/$procKey"
Assert ($r2.data.status -eq 1) "A5.3 GetByKey confirms deployed"

# --- A6: Start Process Instance ---
Log "A6" "Start Process Instance"
$r = PostJson "/process/instances" @{
    processKey = $procKey; businessKey = "LEAVE-$ts"; startUser = "applicant"
    variables = @{ applicant="applicant"; manager="manager"; reason="Vacation"; start_date="2026-08-01"; end_date="2026-08-05"; days=5; leave_type="Annual" }
}
Assert ($r.code -eq 0) "A6.1 Start code=0"
$instanceId = $r.data.id
Assert ($r.data.processKey -eq $procKey) "A6.2 ProcessKey matches"
Assert ($r.data.status -eq 0) "A6.3 Instance status=0 (running)"

# --- A7: Verify Variables ---
Log "A7" "Verify Instance Variables"
$r = GetApi "/process/instances/$instanceId/variables"
Assert ($r.code -eq 0) "A7.1 Get variables code=0"
Assert ($r.data.applicant -eq "applicant") "A7.2 applicant=applicant"
Assert ($r.data.reason -eq "Vacation") "A7.3 reason=Vacation"
Assert ($r.data.days -eq 5) "A7.4 days=5"

# --- A8: Verify Applicant Task ---
Log "A8" "Verify Applicant Task"
$r = GetApi "/tasks/todo?userId=applicant"
Assert ($r.code -eq 0) "A8.1 Get todo code=0"
$aTask = $r.data | Where-Object { $_.processInstanceId -eq $instanceId }
$applicantTaskId = if ($aTask -is [array]) { $aTask[0].id } else { $aTask.id }
Assert ($null -ne $applicantTaskId) "A8.2 Applicant has task"
Assert ($aTask.status -eq 0) "A8.3 Task status=0 (PENDING)"

# --- A9: Complete Applicant Task ---
Log "A9" "Complete Applicant Task"
$r = PostJson "/tasks/$applicantTaskId/complete" @{ userId="applicant"; variables=@{submitted=$true} }
Assert ($r.code -eq 0) "A9.1 Complete code=0"
Assert ($r.data.status -eq 2) "A9.2 Task status=2 (COMPLETED)"

# --- A10: Verify Manager Task + Form Permissions ---
Log "A10" "Verify Manager Task + Form Permissions"
$r = GetApi "/tasks/todo?userId=manager"
Assert ($r.code -eq 0) "A10.1 Get manager todo code=0"
$mTask = $r.data | Where-Object { $_.processInstanceId -eq $instanceId }
$managerTaskId = if ($mTask -is [array]) { $mTask[0].id } else { $mTask.id }
Assert ($null -ne $managerTaskId) "A10.2 Manager has task"
Assert ($mTask.status -eq 0) "A10.3 Task status=0 (PENDING)"

$r2 = GetApi "/tasks/$managerTaskId/form-permissions"
Assert ($r2.code -eq 0) "A10.4 Get permissions code=0"
Assert ($null -ne $r2.data.fieldPermissions) "A10.5 Has fieldPermissions"
Assert ($r2.data.fieldPermissions.Count -ge 5) "A10.6 Has >=5 field permissions"
# Verify readonly on reason field
$reasonPerm = $r2.data.fieldPermissions | Where-Object { $_.fieldKey -eq "reason" }
Assert ($null -ne $reasonPerm) "A10.7 reason field present"
Assert ($reasonPerm.permission -eq "readonly") "A10.8 reason=readonly for manager"

# --- A11: Manager Approves ---
Log "A11" "Manager Approves"
$r = PostJson "/tasks/$managerTaskId/complete" @{ userId="manager"; variables=@{approved=$true} }
Assert ($r.code -eq 0) "A11.1 Approve code=0"
Assert ($r.data.status -eq 2) "A11.2 Task status=2 (COMPLETED)"

# --- A12: Verify Process Completed ---
Log "A12" "Verify Process Completed"
$r = GetApi "/process/instances/$instanceId"
Assert ($r.code -eq 0) "A12.1 Get instance code=0"
Assert ($r.data.status -eq 1) "A12.2 Instance status=1 (COMPLETED)"
Assert ($null -ne $r.data.endTime) "A12.3 endTime is set"

$r2 = GetApi "/tasks/done?userId=applicant"
$doneApplicant = $r2.data | Where-Object { $_.processInstanceId -eq $instanceId }
Assert ($null -ne $doneApplicant) "A12.4 Applicant task in done list"

$r3 = GetApi "/tasks/done?userId=manager"
$doneManager = $r3.data | Where-Object { $_.processInstanceId -eq $instanceId }
Assert ($null -ne $doneManager) "A12.5 Manager task in done list"

# ============================================================
# PART B: NEGATIVE / EDGE CASES
# ============================================================

# --- B1: Data model without mainTable ---
Log "B1" "Negative: Data model without mainTable"
$r = SafeCall { PostJson "/data-models" @{ modelKey="neg_no_main_$ts"; modelName="bad"; subTables=@() } }
Assert ($r.code -eq 1019) "B1.1 Missing mainTable -> code=1019"

# --- B2: Data model key duplicate ---
Log "B2" "Negative: Data model key duplicate"
$r = SafeCall { PostJson "/data-models" @{
    modelKey = $dmKey; modelName = "dup"
    mainTable = @{ tableName="t"; label="t"; fields=@(@{fieldKey="f";label="f";type="text"}) }; subTables=@()
} }
Assert ($r.code -eq 1016) "B2.1 Duplicate key -> code=1016"

# --- B3: Process key duplicate ---
Log "B3" "Negative: Process key duplicate"
$r = SafeCall { PostJson "/process/definitions" @{ processKey=$procKey; processName="dup" } }
Assert ($r.code -eq 1002) "B3.1 Duplicate key -> code=1002"

# --- B4: Start instance on undeployed process ---
Log "B4" "Negative: Start on undeployed process"
$r2 = PostJson "/process/definitions" @{ processKey="neg_undep_$ts"; processName="undeployed" }
$undepId = $r2.data.id
$r = SafeCall { PostJson "/process/instances" @{ processKey="neg_undep_$ts"; startUser="x" } }
Assert ($r.code -eq 1009) "B4.1 Undeployed -> code=1009"

# --- B5: Update deployed process definition ---
Log "B5" "Negative: Update deployed definition"
$r = SafeCall { PutJson "/process/definitions/$procId" @{ processName="Modified" } }
Assert ($r.code -eq 1007) "B5.1 Update deployed -> code=1007"

# --- B6: Complete non-existent task ---
Log "B6" "Negative: Complete non-existent task"
$r = SafeCall { PostJson "/tasks/99999/complete" @{ userId="nobody" } }
Assert ($r.code -eq 1011) "B6.1 Not found -> code=1011"

# --- B7: Form key duplicate ---
Log "B7" "Negative: Form key duplicate"
$r = SafeCall { PostJson "/forms" @{ formKey=$formKey; formName="dup" } }
Assert ($r.code -eq 1051) "B7.1 Duplicate -> code=1051"

# --- B8: Suspend and Resume instance ---
Log "B8" "Edge: Suspend/Resume (need new instance)"
# Start a new instance for this test
$r = PostJson "/process/instances" @{ processKey=$procKey; businessKey="SUSPEND-$ts"; startUser="applicant"; variables=@{applicant="applicant";manager="manager"} }
$suspendInstId = $r.data.id
# Suspend
$r2 = SafeCall { PostJson "/process/instances/$suspendInstId/suspend" $null }
Assert ($r2.code -eq 0) "B8.1 Suspend code=0"
Assert ($r2.data.status -eq 2) "B8.2 Status=2 (SUSPENDED)"
# Resume
$r3 = SafeCall { PostJson "/process/instances/$suspendInstId/resume" $null }
Assert ($r3.code -eq 0) "B8.3 Resume code=0"
Assert ($r3.data.status -eq 0) "B8.4 Status=0 (RUNNING again)"
# Terminate
$r4 = SafeCall { PostJson "/process/instances/$suspendInstId/terminate" $null }
Assert ($r4.code -eq 0) "B8.5 Terminate code=0"
Assert ($r4.data.status -eq 3) "B8.6 Status=3 (TERMINATED)"

# ============================================================
# CLEANUP
# ============================================================
Log "Cleanup" "Remove all test data"

function TryDelete($method, $url) {
    try { Invoke-RestMethod -Uri "$base$url" -Method $method | Out-Null; return $true }
    catch { return $false }
}

# Delete process definitions (all with our key prefix)
$defs = (GetApi "/process/definitions").data | Where-Object { $_.processKey -like "e2e_proc_$ts*" -or $_.processKey -like "neg_*_$ts*" }
foreach ($d in $defs) { TryDelete "Delete" "/process/definitions/$($d.id)" | Out-Null }
Assert ($true) "C.1 Process definitions cleaned"

# Delete form
TryDelete "Delete" "/forms/$formKey" | Out-Null
Assert ($true) "C.2 Form cleaned"

# Delete data model
TryDelete "Delete" "/data-models/$dmKey" | Out-Null
Assert ($true) "C.3 Data model cleaned"

# ============================================================
# SUMMARY
# ============================================================
Write-Host "`n============================================" -ForegroundColor Yellow
Write-Host "  RESULTS: $pass/$total passed, $fail failed" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  A (Happy Path): Steps A1-A12"
Write-Host "  B (Negative):   B1-B8"
Write-Host "============================================`n" -ForegroundColor Yellow

if ($fail -eq 0) { Write-Host "ALL TESTS PASSED!" -ForegroundColor Green; exit 0 }
else { Write-Host "SOME TESTS FAILED!" -ForegroundColor Red; exit 1 }
