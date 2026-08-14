$svc = Get-Service | Where-Object { $_.Name -like "*mysql*" -or $_.DisplayName -like "*mysql*" }
if ($svc) {
  foreach ($s in $svc) {
    Write-Output ("service name={0} status={1}" -f $s.Name, $s.Status)
    if ($s.Status -ne "Running") { Start-Service -Name $s.Name -ErrorAction SilentlyContinue; Write-Output ("started " + $s.Name) }
  }
} else {
  Write-Output "no mysql service found"
}
