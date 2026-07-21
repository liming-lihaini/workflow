$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api/v1"

Write-Host "=== Step 1: Create Process Definition (with form config in nodes) ===" -ForegroundColor Cyan

$processModel = @{
    processKey = "leave-flow"
    processName = "Leave Approval"
    nodes = @(
        @{ id = "start_1"; type = "start"; name = "Start"; x = 300; y = 50; properties = @{} },
        @{ id = "userTask_1"; type = "userTask"; name = "Manager Approve"; assignee = "admin"; x = 300; y = 200; properties = @{
            formKey = "leave_form"
            formPermissions = @{
                nodePermission = "edit"
                fields = @(
                    @{ fieldKey = "reason"; permission = "readonly" },
                    @{ fieldKey = "days"; permission = "edit" },
                    @{ fieldKey = "approved"; permission = "edit" }
                )
                buttons = @(
                    @{ buttonKey = "submit"; visible = $true; enabled = $true },
                    @{ buttonKey = "reject"; visible = $true; enabled = $true }
                )
            }
        }},
        @{ id = "end_1"; type = "end"; name = "End"; x = 300; y = 400; properties = @{} }
    )
    edges = @(
        @{ id = "edge_1"; source = "start_1"; target = "userTask_1"; label = ""; condition = "" },
        @{ id = "edge_2"; source = "userTask_1"; target = "end_1"; label = ""; condition = "" }
    )
} | ConvertTo-Json -Depth 10 -Compress

$createBody = @{
    processKey = "leave-flow"
    processName = "Leave Approval"
    processJson = $processModel
} | ConvertTo-Json -Depth 3

$createBodyBytes = [System.Text.Encoding]::UTF8.GetBytes($createBody)

try {
    $result = Invoke-RestMethod -Uri "$baseUrl/process/definitions" -Method POST -ContentType "application/json; charset=utf-8" -Body $createBodyBytes
    Write-Host "Create result code: $($result.code)"
    if ($result.code -eq 0) {
        $defId = $result.data.id
        Write-Host "SUCCESS: Created definition id=$defId" -ForegroundColor Green
    } else {
        Write-Host "FAILED: $($result.message)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response: $($reader.ReadToEnd())"
    }
    exit 1
}

Write-Host ""
Write-Host "=== Step 2: Verify Process Definition (GET by ID) ===" -ForegroundColor Cyan
$getResult = Invoke-RestMethod -Uri "$baseUrl/process/definitions/$defId" -Method GET
if ($getResult.code -eq 0) {
    Write-Host "SUCCESS: Got definition: key=$($getResult.data.processKey), name=$($getResult.data.processName), status=$($getResult.data.status)" -ForegroundColor Green
    $savedJson = $getResult.data.processJson | ConvertFrom-Json
    Write-Host "  Nodes count: $($savedJson.nodes.Count)"
    Write-Host "  Edges count: $($savedJson.edges.Count)"
    Write-Host "  Node types: $($savedJson.nodes | ForEach-Object { $_.type })"
} else {
    Write-Host "FAILED: $($getResult.message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Step 3: Update Process Definition (simulate second save) ===" -ForegroundColor Cyan
$updatedModel = @{
    processKey = "leave-flow"
    processName = "Leave Approval V2"
    nodes = @(
        @{ id = "start_1"; type = "start"; name = "Start"; x = 300; y = 50; properties = @{} },
        @{ id = "userTask_1"; type = "userTask"; name = "Manager Approve"; assignee = "admin"; x = 300; y = 200; properties = @{
            formKey = "leave_form"
            formPermissions = @{
                nodePermission = "edit"
                fields = @(
                    @{ fieldKey = "reason"; permission = "readonly" },
                    @{ fieldKey = "days"; permission = "edit" }
                )
                buttons = @(
                    @{ buttonKey = "submit"; visible = $true; enabled = $true }
                )
            }
        }},
        @{ id = "end_1"; type = "end"; name = "End"; x = 300; y = 400; properties = @{} }
    )
    edges = @(
        @{ id = "edge_1"; source = "start_1"; target = "userTask_1"; label = ""; condition = "" },
        @{ id = "edge_2"; source = "userTask_1"; target = "end_1"; label = ""; condition = "" }
    )
} | ConvertTo-Json -Depth 10 -Compress

$updateBody = @{
    processName = "Leave Approval V2"
    processJson = $updatedModel
} | ConvertTo-Json -Depth 3

$updateBodyBytes = [System.Text.Encoding]::UTF8.GetBytes($updateBody)

try {
    $updateResult = Invoke-RestMethod -Uri "$baseUrl/process/definitions/$defId" -Method PUT -ContentType "application/json; charset=utf-8" -Body $updateBodyBytes
    if ($updateResult.code -eq 0) {
        Write-Host "SUCCESS: Updated definition, name=$($updateResult.data.processName)" -ForegroundColor Green
    } else {
        Write-Host "FAILED: $($updateResult.message)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Step 4: Deploy Process Definition ===" -ForegroundColor Cyan
$deployResult = Invoke-RestMethod -Uri "$baseUrl/process/definitions/$defId/deploy" -Method POST
if ($deployResult.code -eq 0) {
    Write-Host "SUCCESS: Deployed, status=$($deployResult.data.status), deploymentId=$($deployResult.data.deploymentId)" -ForegroundColor Green
} else {
    Write-Host "FAILED: $($deployResult.message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Step 4: Start Process Instance ===" -ForegroundColor Cyan
$startBody = @{
    processKey = "leave-flow"
    businessKey = "BIZ_LEAVE_001"
    startUser = "admin"
    variables = @{ reason = "annual leave"; days = 3 }
} | ConvertTo-Json -Depth 3

$startBodyBytes = [System.Text.Encoding]::UTF8.GetBytes($startBody)

try {
    $startResult = Invoke-RestMethod -Uri "$baseUrl/process/instances" -Method POST -ContentType "application/json; charset=utf-8" -Body $startBodyBytes
    if ($startResult.code -eq 0) {
        $instanceId = $startResult.data.id
        Write-Host "SUCCESS: Started instance id=$instanceId, status=$($startResult.data.status), currentNode=$($startResult.data.currentNodeId)" -ForegroundColor Green
    } else {
        Write-Host "FAILED: $($startResult.message)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response: $($reader.ReadToEnd())"
    }
    exit 1
}

Write-Host ""
Write-Host "=== Step 5: Verify Instance & Task Created ===" -ForegroundColor Cyan
$instanceResult = Invoke-RestMethod -Uri "$baseUrl/process/instances/$instanceId" -Method GET
if ($instanceResult.code -eq 0) {
    Write-Host "Instance: id=$($instanceResult.data.id), status=$($instanceResult.data.status), currentNode=$($instanceResult.data.currentNodeId)" -ForegroundColor Green
}

# Check tasks via todo list
$taskResult = Invoke-RestMethod -Uri "$baseUrl/tasks/todo?userId=admin" -Method GET
if ($taskResult.code -eq 0) {
    Write-Host "Todo tasks for admin: $($taskResult.data.Count)" -ForegroundColor Green
    $taskResult.data | ForEach-Object {
        Write-Host "  Task: id=$($_.id), node=$($_.nodeName), assignee=$($_.assignee), status=$($_.status), instanceId=$($_.processInstanceId)"
    }
}

Write-Host ""
Write-Host "=== Step 6: Verify Form Permissions ===" -ForegroundColor Cyan
if ($taskResult.data.Count -gt 0) {
    $taskId = $taskResult.data[0].id
    $formPermResult = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/form-permissions" -Method GET
    if ($formPermResult.code -eq 0) {
        Write-Host "SUCCESS: Form permissions retrieved" -ForegroundColor Green
        Write-Host "  formKey: $($formPermResult.data.formKey)"
        Write-Host "  nodePermission: $($formPermResult.data.nodePermission)"
        Write-Host "  fieldPermissions count: $($formPermResult.data.fieldPermissions.Count)"
        $formPermResult.data.fieldPermissions | ForEach-Object {
            Write-Host "    field: $($_.fieldKey) => $($_.permission)"
        }
    } else {
        Write-Host "FAILED: $($formPermResult.message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Step 7: Create Data Model (Form Definition) ===" -ForegroundColor Cyan
$dataModelBody = @{
    modelKey = "leave_form"
    modelName = "Leave Form"
    mainTable = @{
        tableName = "leave_main"
        label = "Leave Info"
        fields = @(
            @{ fieldKey = "reason"; fieldName = "Reason"; fieldType = "string"; required = $true },
            @{ fieldKey = "days"; fieldName = "Days"; fieldType = "number"; required = $true },
            @{ fieldKey = "startDate"; fieldName = "Start Date"; fieldType = "date"; required = $false }
        )
    }
} | ConvertTo-Json -Depth 5

$dataModelBodyBytes = [System.Text.Encoding]::UTF8.GetBytes($dataModelBody)

try {
    $dmResult = Invoke-RestMethod -Uri "$baseUrl/data-models" -Method POST -ContentType "application/json; charset=utf-8" -Body $dataModelBodyBytes
    if ($dmResult.code -eq 0) {
        Write-Host "SUCCESS: Data model created, key=$($dmResult.data.modelKey), status=$($dmResult.data.status)" -ForegroundColor Green
    } else {
        Write-Host "FAILED: $($dmResult.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Step 8: Complete Task (Approve) ===" -ForegroundColor Cyan
if ($taskResult.data.Count -gt 0) {
    $taskId = $taskResult.data[0].id
    $completeBody = @{ userId = "admin"; variables = @{ approved = $true } } | ConvertTo-Json -Depth 3
    $completeBodyBytes = [System.Text.Encoding]::UTF8.GetBytes($completeBody)
    $completeResult = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/complete" -Method POST -ContentType "application/json; charset=utf-8" -Body $completeBodyBytes
    if ($completeResult.code -eq 0) {
        Write-Host "SUCCESS: Task completed, status=$($completeResult.data.status)" -ForegroundColor Green
    } else {
        Write-Host "FAILED: $($completeResult.message)" -ForegroundColor Red
    }

    # Verify instance completed
    $finalInstance = Invoke-RestMethod -Uri "$baseUrl/process/instances/$instanceId" -Method GET
    Write-Host "Final instance status: $($finalInstance.data.status) (1=completed)" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== ALL STEPS PASSED ===" -ForegroundColor Green
