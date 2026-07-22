# ============================================================
# E2E Test: Nested Form Designer (Section → Row → Cell → Field)
# ============================================================

$ErrorActionPreference = "Continue"
$base = "http://localhost:8080/api/v1"
$pass = 0; $fail = 0; $total = 0
$ts = Get-Date -Format "HHmmss"

$dmKey = "fn_dm_$ts"
$formKey = "fn_form_$ts"

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

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  NESTED STRUCTURE TEST ($ts)" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

# ============================================================
# T1: Setup - Create & Publish Model
# ============================================================
Log "T1" "Create & Publish Model"
$r = PostJson "/data-models" @{
    modelKey = $dmKey; modelName = "Nested Test Model"
    mainTable = @{
        tableName = "nested_form"; label = "Nested Form"
        fields = @(
            @{ fieldKey = "name"; label = "Name"; type = "text"; required = $true }
            @{ fieldKey = "email"; label = "Email"; type = "text"; required = $false }
            @{ fieldKey = "phone"; label = "Phone"; type = "text"; required = $true }
            @{ fieldKey = "address"; label = "Address"; type = "text"; required = $false }
            @{ fieldKey = "applicant"; label = "Applicant"; type = "person"; required = $true }
            @{ fieldKey = "department"; label = "Department"; type = "department"; required = $true }
        )
    }; subTables = @()
}
Assert ($r.code -eq 0) "T1.1 Create model"
$r2 = PostJson "/data-models/$dmKey/publish" $null
Assert ($r2.code -eq 0) "T1.2 Publish model"

# ============================================================
# T2: Create Form
# ============================================================
Log "T2" "Create Form"
$r = PostJson "/forms" @{
    formKey = $formKey; formName = "Nested Structure Test"
    category = "test"; modelKey = $dmKey; formJson = "{}"
}
Assert ($r.code -eq 0) "T2.1 Create form"

# ============================================================
# T3: Nested Structure - Section contains Rows, Rows contain Fields
# ============================================================
Log "T3" "Nested: Section → Row → Cell → Field"

$formJson = @{
    sections = @(
        @{
            id = "sec_1"; title = "Personal Info"
            children = @(
                @{ id="row_1"; columns=2; cells = @(
                    @{ id="cell_1"; fields = @(
                        @{ id="f1"; type="text"; field="name"; label="Name"; required=$true; modelField="name" }
                    )}
                    @{ id="cell_2"; fields = @(
                        @{ id="f2"; type="text"; field="email"; label="Email"; required=$false; modelField="email" }
                    )}
                )}
                @{ id="row_2"; columns=1; cells = @(
                    @{ id="cell_3"; fields = @(
                        @{ id="f3"; type="text"; field="address"; label="Address"; required=$false; modelField="address" }
                    )}
                )}
            )
        }
        @{
            id = "sec_2"; title = "Work Info"
            children = @(
                @{ id="row_3"; columns=2; cells = @(
                    @{ id="cell_4"; fields = @(
                        @{ id="f4"; type="user"; field="applicant"; label="Applicant"; required=$true; modelField="applicant" }
                    )}
                    @{ id="cell_5"; fields = @(
                        @{ id="f5"; type="dept"; field="department"; label="Department"; required=$true; modelField="department" }
                    )}
                )}
                @{ id="row_4"; columns=1; cells = @(
                    @{ id="cell_6"; fields = @(
                        @{ id="f6"; type="text"; field="phone"; label="Phone"; required=$true; modelField="phone" }
                    )}
                )}
            )
        }
    )
    modelKey = $dmKey
}

$r = PutJson "/forms/$formKey" @{ formKey = $formKey; formJson = ($formJson | ConvertTo-Json -Depth 20); modelKey = $dmKey }
Assert ($r.code -eq 0) "T3.1 Save nested form"

# Verify structure
$r2 = GetApi "/forms/$formKey"
$saved = $r2.data.formJson | ConvertFrom-Json

# Sections
Assert ($saved.sections.Count -eq 2) "T3.2 Has 2 sections"
Assert ($saved.sections[0].title -eq "Personal Info") "T3.3 Section 1 title"
Assert ($saved.sections[1].title -eq "Work Info") "T3.4 Section 2 title"

# Rows inside section
Assert ($saved.sections[0].children.Count -eq 2) "T3.5 Section 1 has 2 rows"
Assert ($saved.sections[0].children[0].columns -eq 2) "T3.6 Row 1: 2 columns"
Assert ($saved.sections[0].children[1].columns -eq 1) "T3.7 Row 2: 1 column"

# Fields inside cells inside rows
Assert ($saved.sections[0].children[0].cells[0].fields[0].field -eq "name") "T3.8 Cell→Field: name"
Assert ($saved.sections[0].children[0].cells[1].fields[0].field -eq "email") "T3.9 Cell→Field: email"

# Section 2
Assert ($saved.sections[1].children.Count -eq 2) "T3.10 Section 2 has 2 rows"
Assert ($saved.sections[1].children[0].cells[0].fields[0].type -eq "user") "T3.11 User field in section 2"
Assert ($saved.sections[1].children[0].cells[1].fields[0].type -eq "dept") "T3.12 Dept field in section 2"

# ============================================================
# T4: Model Property Binding
# ============================================================
Log "T4" "Model Property Binding"

# All fields should have modelField bound
$allFields = @()
$saved.sections | ForEach-Object { $_.children | ForEach-Object { $_.cells | ForEach-Object { $_.fields | ForEach-Object { $allFields += $_ } } } }
Assert ($allFields.Count -eq 6) "T4.1 Total 6 fields"

$boundFields = $allFields | Where-Object { $_.modelField -ne $null -and $_.modelField -ne "" }
Assert ($boundFields.Count -eq 6) "T4.2 All 6 fields bound to model"

# Verify specific bindings
$nameField = $allFields | Where-Object { $_.field -eq "name" }
Assert ($nameField.modelField -eq "name") "T4.3 name → model.name"

$applicantField = $allFields | Where-Object { $_.field -eq "applicant" }
Assert ($applicantField.modelField -eq "applicant") "T4.4 applicant → model.applicant"

$deptField = $allFields | Where-Object { $_.field -eq "department" }
Assert ($deptField.modelField -eq "department") "T4.5 department → model.department"

# ============================================================
# T5: Published Model List Available
# ============================================================
Log "T5" "Published Model Selection"

$r = GetApi "/data-models?page=1&size=100"
$data = $r.data
if ($data -is [array]) { $modelRecords = $data }
elseif ($data.records) { $modelRecords = $data.records }
else { $modelRecords = @() }

$publishedModels = $modelRecords | Where-Object { $_.status -eq 1 }
Assert ($publishedModels.Count -ge 1) "T5.1 Has published models"

$ourModel = $publishedModels | Where-Object { $_.modelKey -eq $dmKey }
Assert ($null -ne $ourModel) "T5.2 Our model is published"
Assert ($ourModel.status -eq 1) "T5.3 Model status=1 (published)"

# ============================================================
# T6: Empty Section (no rows)
# ============================================================
Log "T6" "Empty Section"

$emptyFormJson = @{
    sections = @(
        @{ id="sec_empty"; title="Empty Section"; children = @() }
        @{ id="sec_with_row"; title="Has Row"; children = @(
            @{ id="row_1"; columns=1; cells = @(
                @{ id="cell_1"; fields = @(
                    @{ id="f1"; type="text"; field="test"; label="Test"; required=$false }
                )}
            )}
        )}
    )
}

$r = PutJson "/forms/$formKey" @{ formKey = $formKey; formJson = ($emptyFormJson | ConvertTo-Json -Depth 20); modelKey = $null }
Assert ($r.code -eq 0) "T6.1 Save empty section OK"

$r2 = GetApi "/forms/$formKey"
$saved = $r2.data.formJson | ConvertFrom-Json
Assert ($saved.sections.Count -eq 2) "T6.2 Both sections saved"
Assert ($saved.sections[0].children.Count -eq 0) "T6.3 Empty section has 0 rows"
Assert ($saved.sections[1].children.Count -eq 1) "T6.4 Non-empty section has 1 row"

# ============================================================
# T7: Deep Nesting - Multiple Rows per Section
# ============================================================
Log "T7" "Deep Nesting: Multiple Rows"

$deepFormJson = @{
    sections = @(
        @{ id="sec1"; title="Complex Form"; children = @(
            @{ id="r1"; columns=2; cells = @(
                @{ id="c1"; fields = @(@{ id="f1"; type="text"; field="a"; label="A"; required=$false; modelField=$null }) }
                @{ id="c2"; fields = @(@{ id="f2"; type="text"; field="b"; label="B"; required=$false; modelField=$null }) }
            )}
            @{ id="r2"; columns=2; cells = @(
                @{ id="c3"; fields = @(@{ id="f3"; type="date"; field="c"; label="C"; required=$false; modelField=$null }) }
                @{ id="c4"; fields = @(@{ id="f4"; type="date"; field="d"; label="D"; required=$false; modelField=$null }) }
            )}
            @{ id="r3"; columns=1; cells = @(
                @{ id="c5"; fields = @(
                    @{ id="f5"; type="textarea"; field="e"; label="E"; required=$false; modelField=$null }
                    @{ id="f6"; type="select"; field="f"; label="F"; required=$false; optionsSource="manual"; optionsText="1:Opt1`n2:Opt2"; modelField=$null }
                )}
            )}
            @{ id="r4"; columns=1; cells = @(
                @{ id="c6"; fields = @(@{ id="f7"; type="file"; field="g"; label="G"; required=$false; modelField=$null }) }
            )}
        )}
    )
}

$r = PutJson "/forms/$formKey" @{ formKey = $formKey; formJson = ($deepFormJson | ConvertTo-Json -Depth 20); modelKey = $null }
Assert ($r.code -eq 0) "T7.1 Save deep form"

$r2 = GetApi "/forms/$formKey"
$saved = $r2.data.formJson | ConvertFrom-Json
Assert ($saved.sections[0].children.Count -eq 4) "T7.2 Section has 4 rows"
Assert ($saved.sections[0].children[0].columns -eq 2) "T7.3 Row 1: 2-col"
Assert ($saved.sections[0].children[1].columns -eq 2) "T7.4 Row 2: 2-col"
Assert ($saved.sections[0].children[2].columns -eq 1) "T7.5 Row 3: 1-col"
Assert ($saved.sections[0].children[2].cells[0].fields.Count -eq 2) "T7.6 Row 3 cell has 2 fields"
Assert ($saved.sections[0].children[3].cells[0].fields[0].type -eq "file") "T7.7 Row 4: file field"

# ============================================================
# T8: User/Dept Selector + Model Binding
# ============================================================
Log "T8" "User/Dept with Model Binding"

$udFormJson = @{
    sections = @(
        @{ id="sec1"; title="Selectors"; children = @(
            @{ id="r1"; columns=2; cells = @(
                @{ id="c1"; fields = @(
                    @{ id="f1"; type="user"; field="reviewer"; label="Reviewer"; required=$true; modelField="applicant" }
                )}
                @{ id="c2"; fields = @(
                    @{ id="f2"; type="dept"; field="review_dept"; label="Review Dept"; required=$true; modelField="department" }
                )}
            )}
        )}
    )
    modelKey = $dmKey
}

$r = PutJson "/forms/$formKey" @{ formKey = $formKey; formJson = ($udFormJson | ConvertTo-Json -Depth 20); modelKey = $dmKey }
Assert ($r.code -eq 0) "T8.1 Save user/dept form"

$r2 = GetApi "/forms/$formKey"
$saved = $r2.data.formJson | ConvertFrom-Json
$udFields = $saved.sections[0].children[0].cells | ForEach-Object { $_.fields } | ForEach-Object { $_ }
Assert ($udFields[0].type -eq "user") "T8.2 User type"
Assert ($udFields[0].modelField -eq "applicant") "T8.3 User bound to model.applicant"
Assert ($udFields[1].type -eq "dept") "T8.4 Dept type"
Assert ($udFields[1].modelField -eq "department") "T8.5 Dept bound to model.department"

# Verify APIs work for selectors
$r3 = GetApi "/system/users/page?keyword=&page=1&size=5"
Assert ($r3.code -eq 0) "T8.6 User search API OK"
$r4 = GetApi "/system/depts/tree"
Assert ($r4.code -eq 0) "T8.7 Dept tree API OK"

# ============================================================
# SUMMARY
# ============================================================
Write-Host "`n============================================" -ForegroundColor Yellow
Write-Host "  RESULTS: $pass/$total passed, $fail failed" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "  T1: Model Setup"
Write-Host "  T2: Form Creation"
Write-Host "  T3: Nested Structure (Section→Row→Cell→Field)"
Write-Host "  T4: Model Property Binding"
Write-Host "  T5: Published Model Available"
Write-Host "  T6: Empty Section"
Write-Host "  T7: Deep Nesting (4 rows/section)"
Write-Host "  T8: User/Dept + Model Binding"
Write-Host "============================================`n" -ForegroundColor Yellow

if ($fail -eq 0) { Write-Host "ALL TESTS PASSED!" -ForegroundColor Green; exit 0 }
else { Write-Host "SOME TESTS FAILED!" -ForegroundColor Red; exit 1 }
