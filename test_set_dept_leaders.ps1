# ============================================================
# Automated Test: Set Department Leaders for All Departments
# Logic:
#   1. Fetch all departments (flat list)
#   2. For each department, query direct members
#   3. If members exist -> randomly pick one as leader
#   4. If no members -> create a user in that dept, then set as leader
# Note: Uses curl.exe for POST requests to avoid PowerShell
#       Invoke-RestMethod double UTF-8 encoding issue with CJK chars
# ============================================================

$ErrorActionPreference = "Continue"
$base = "http://localhost:8080/api/v1"
$pass = 0; $fail = 0; $total = 0
$ts = Get-Date -Format "HHmmss"
$tmpBodyFile = Join-Path $env:TEMP "test_body_$ts.json"
$tmpRespFile = Join-Path $env:TEMP "test_resp_$ts.json"

function Log($s, $d) { Write-Host "`n[$s] $d" -ForegroundColor Cyan }
function Assert($c, $m) {
    $script:total++
    if ($c) { $script:pass++; Write-Host "  [PASS] $m" -ForegroundColor Green }
    else { $script:fail++; Write-Host "  [FAIL] $m" -ForegroundColor Red }
}
function GetApi($url) { return Invoke-RestMethod -Uri "$base$url" -Method Get }

# Use curl.exe for POST requests with JSON body to avoid PowerShell double UTF-8 encoding
function CurlPost($url, $body) {
    $jsonStr = $body | ConvertTo-Json -Depth 20 -Compress
    [System.IO.File]::WriteAllText($script:tmpBodyFile, $jsonStr, [System.Text.Encoding]::UTF8)
    $args = @('-s', '-X', 'POST', "$base$url",
              '-H', 'Content-Type: application/json; charset=utf-8',
              '-d', "@$($script:tmpBodyFile)",
              '-o', $script:tmpRespFile)
    & curl.exe @args 2>&1 | Out-Null
    if (Test-Path $script:tmpRespFile) {
        $raw = [System.IO.File]::ReadAllText($script:tmpRespFile, [System.Text.Encoding]::UTF8)
        return $raw | ConvertFrom-Json
    }
    return $null
}

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  SET DEPT LEADERS TEST ($ts)" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

# --- Step 1: Fetch all departments ---
Log "Step1" "Fetch all departments"
$deptsRes = GetApi "/system/depts"
Assert ($deptsRes.code -eq 0) "Step1.1 Get depts code=0"
$allDepts = $deptsRes.data
Assert ($allDepts.Count -gt 0) "Step1.2 Has departments ($($allDepts.Count) found)"
Write-Host "  Found $($allDepts.Count) departments" -ForegroundColor Gray

# --- Step 2: For each department, set leader ---
Log "Step2" "Set leaders for all departments"

$createdUsers = @()
$setLeaders = @()

foreach ($dept in $allDepts) {
    $deptId = $dept.id
    $deptName = $dept.deptName
    Write-Host "`n  --- Dept: $deptName (id=$deptId) ---" -ForegroundColor DarkCyan

    # Query direct members of this department
    $membersRes = $null
    try {
        $membersRes = GetApi "/system/users/page?deptId=$deptId&page=1&size=50"
    } catch {
        Write-Host "    [WARN] Failed to query members for dept $deptName" -ForegroundColor Yellow
    }

    $members = @()
    if ($null -ne $membersRes -and $membersRes.code -eq 0) {
        $d = $membersRes.data
        $members = if ($d -is [array]) { $d } else {
            if ($null -ne $d.records) { $d.records }
            elseif ($null -ne $d.list) { $d.list }
            else { @() }
        }
    }

    $leaderId = $null
    $leaderName = $null

    if ($members.Count -gt 0) {
        # Randomly pick one member as leader
        $randomIndex = Get-Random -Minimum 0 -Maximum $members.Count
        $chosen = $members[$randomIndex]
        $leaderId = $chosen.id
        $leaderName = if ($chosen.realName) { $chosen.realName } else { $chosen.username }
        Write-Host "    Random member chosen: $leaderName (id=$leaderId)" -ForegroundColor Gray
    } else {
        # No members -> create a user in this department
        $userTs = Get-Date -Format "HHmmss"
        $randomSuffix = Get-Random -Minimum 100 -Maximum 999
        $newUsername = "leader_${deptId}_${userTs}_${randomSuffix}"
        $newRealName = "${deptName}领导"

        Write-Host "    No members found. Creating user: $newUsername ($newRealName)" -ForegroundColor Yellow

        $createRes = $null
        try {
            $createRes = CurlPost "/system/users" @{
                username = $newUsername
                password = "pwd123"
                realName = $newRealName
                deptId   = $deptId
            }
        } catch {
            Write-Host "    [ERROR] Failed to create user for dept $deptName : $_" -ForegroundColor Red
        }

        if ($null -ne $createRes -and $createRes.code -eq 0) {
            $leaderId = $createRes.data.id
            $leaderName = $newRealName
            $createdUsers += $newUsername
            Write-Host "    Created user: $newRealName (id=$leaderId)" -ForegroundColor Green
        } else {
            $errMsg = if ($null -ne $createRes) { "code=$($createRes.code) msg=$($createRes.msg)" } else { "null response" }
            Write-Host "    [ERROR] Create user failed: $errMsg" -ForegroundColor Red
            Assert $false "Create user for dept '$deptName'"
            continue
        }
    }

    # Set the leader (use curl.exe + JSON body to avoid encoding issues)
    if ($null -ne $leaderId -and $null -ne $leaderName) {
        $setRes = $null
        try {
            $setRes = CurlPost "/system/depts/$deptId/leader" @{ leaderId = $leaderId; leaderName = "$leaderName" }
        } catch {
            Write-Host "    [ERROR] Failed to set leader for dept $deptName : $_" -ForegroundColor Red
        }

        if ($null -ne $setRes -and $setRes.code -eq 0) {
            $setLeaders += $deptName
            Assert $true "Set leader for '$deptName' -> $leaderName"
        } else {
            $errMsg = if ($null -ne $setRes) { "code=$($setRes.code) msg=$($setRes.msg)" } else { "null response" }
            Assert $false "Set leader for '$deptName' ($errMsg)"
        }
    }
}

# --- Step 3: Verify leaders are set ---
Log "Step3" "Verify leaders via dept tree"
$treeRes = GetApi "/system/depts/tree"
Assert ($treeRes.code -eq 0) "Step3.1 Get tree code=0"

function Count-Leaders($nodes) {
    $count = 0
    foreach ($node in $nodes) {
        if ($node.leaderName) { $count++ }
        if ($node.children -and $node.children.Count -gt 0) {
            $count += Count-Leaders $node.children
        }
    }
    return $count
}

$leaderCount = Count-Leaders $treeRes.data
Assert ($leaderCount -gt 0) "Step3.2 Tree has leaders ($leaderCount found)"
Write-Host "  Total departments with leaders in tree: $leaderCount" -ForegroundColor Gray

# --- Step 4: Spot-check encoding via curl.exe ---
Log "Step4" "Spot-check leader name encoding (dept 100)"
$spotFile = Join-Path $env:TEMP "test_spot_$ts.json"
& curl.exe -s "http://127.0.0.1:8080/api/v1/system/depts/100" -o $spotFile 2>&1 | Out-Null
$spotRaw = [System.IO.File]::ReadAllText($spotFile, [System.Text.Encoding]::UTF8)
$spotJson = $spotRaw | ConvertFrom-Json
$spotLeader = $spotJson.data.leaderName
# Check that the leader name is valid (not empty, not containing typical garbled chars like Ã)
Assert ($spotLeader -and $spotLeader.Length -gt 0) "Step4.1 Dept 100 has leader: $spotLeader"
Assert ($spotLeader -notmatch '[ÃÂ]') "Step4.2 Leader name has no double-encoding artifacts"

# Cleanup temp files
Remove-Item $tmpBodyFile -ErrorAction SilentlyContinue
Remove-Item $tmpRespFile -ErrorAction SilentlyContinue
Remove-Item $spotFile -ErrorAction SilentlyContinue

# ============================================================
# SUMMARY
# ============================================================
Write-Host "`n============================================" -ForegroundColor Yellow
Write-Host "  RESULTS: $pass/$total passed, $fail failed" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  Departments processed: $($allDepts.Count)"
Write-Host "  Leaders set:           $($setLeaders.Count)"
Write-Host "  Users created:         $($createdUsers.Count)"
if ($createdUsers.Count -gt 0) {
    Write-Host "  Created usernames:     $($createdUsers -join ', ')" -ForegroundColor Gray
}
Write-Host "============================================`n" -ForegroundColor Yellow

if ($fail -eq 0) { Write-Host "ALL TESTS PASSED!" -ForegroundColor Green; exit 0 }
else { Write-Host "SOME TESTS FAILED!" -ForegroundColor Red; exit 1 }
