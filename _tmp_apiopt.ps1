$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api/v1'
# login
curl.exe -s -o '_tmp_login.json' -X POST "$base/auth/login" -H 'Content-Type: application/json' -d '@_tmp_login_body.json'
$login = [System.IO.File]::ReadAllText('_tmp_login.json', [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$token = $login.data.token
if (-not $token) { Write-Output ('LOGIN_FAIL: ' + $login); exit 1 }
Write-Output 'LOGIN_OK'
# dict items endpoint structure check (api options data source)
curl.exe -s -o '_tmp_dict.json' "$base/system/dict/items/code/moni_entrust_source" -H "Authorization: Bearer $token"
$dict = [System.IO.File]::ReadAllText('_tmp_dict.json', [System.Text.Encoding]::UTF8) | ConvertFrom-Json
Write-Output ('code=' + $dict.code)
Write-Output ('dataIsArray=' + ($dict.data -is [array]))
Write-Output ('count=' + $dict.data.Count)
$first = $dict.data | Select-Object -First 1
Write-Output ('first.itemValue=' + $first.itemValue + ' first.itemText.len=' + ($first.itemText | Out-String).Trim().Length)
