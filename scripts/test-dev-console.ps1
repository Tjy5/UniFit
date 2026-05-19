param(
  [ValidateSet('all', 'user', 'admin')]
  [string]$System = 'all',
  [int]$Seconds = 5
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $repoRoot 'scripts\dev-console-common.ps1')

function Assert-Contains {
  param(
    [string]$Text,
    [string]$Pattern,
    [string]$Description
  )

  if ($Text -notmatch $Pattern) {
    throw "Missing expected output: $Description"
  }
}

function Assert-PortsReleased {
  param([int[]]$Ports)

  Start-Sleep -Seconds 2
  foreach ($port in $Ports) {
    if (Test-PortListening -Port $port) {
      throw "Port $port is still listening after dev console smoke test."
    }
  }
}

function Invoke-DevConsoleSmoke {
  param(
    [string]$Name,
    [string]$WorkingDirectory,
    [string]$ScriptPath,
    [string]$FrontendUrlPattern,
    [string]$BackendUrlPattern,
    [string]$FrontendPrefixPattern,
    [string]$BackendPrefixPattern,
    [int[]]$Ports
  )

  foreach ($port in $Ports) {
    if (Test-PortListening -Port $port) {
      throw "$Name smoke test cannot start because port $port is already listening. $(Get-PortOwnerSummary -Port $port)"
    }
  }

  $env:SUIOS_DEV_CONSOLE_TEST_SECONDS = [string]$Seconds
  try {
    Push-Location $WorkingDirectory
    try {
      $output = & powershell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath 2>&1
      $exitCode = $LASTEXITCODE
    } finally {
      Pop-Location
    }
  } finally {
    Remove-Item Env:\SUIOS_DEV_CONSOLE_TEST_SECONDS -ErrorAction SilentlyContinue
  }

  $text = ($output | Out-String)
  if ($exitCode -ne 0) {
    throw "$Name smoke test failed with exit code $exitCode.`n$text"
  }

  Assert-Contains -Text $text -Pattern $FrontendUrlPattern -Description "$Name frontend URL"
  Assert-Contains -Text $text -Pattern $BackendUrlPattern -Description "$Name backend URL"
  Assert-Contains -Text $text -Pattern $FrontendPrefixPattern -Description "$Name frontend prefixed log"
  Assert-Contains -Text $text -Pattern $BackendPrefixPattern -Description "$Name backend prefixed log"
  Assert-PortsReleased -Ports $Ports

  [Console]::WriteLine("$Name dev console smoke test passed.")
}

$targets = @()
if ($System -eq 'all' -or $System -eq 'user') {
  $userRoot = Join-Path $repoRoot 'School-uniform-intelligent-ordering-system-usersystem'
  $targets += [pscustomobject]@{
    Name = 'user-system'
    WorkingDirectory = $userRoot
    ScriptPath = Join-Path $userRoot 'dev.ps1'
    FrontendUrlPattern = 'Frontend:\s+http://127\.0\.0\.1:3000'
    BackendUrlPattern = 'Backend\s+:\s+http://127\.0\.0\.1:9090'
    FrontendPrefixPattern = '\[user-frontend\]'
    BackendPrefixPattern = '\[user-backend\]'
    Ports = @(3000, 9090)
  }
}

if ($System -eq 'all' -or $System -eq 'admin') {
  $adminRoot = Join-Path $repoRoot 'School-uniform-intelligent-ordering-system-managementor'
  $targets += [pscustomobject]@{
    Name = 'admin-system'
    WorkingDirectory = $adminRoot
    ScriptPath = Join-Path $adminRoot 'dev.ps1'
    FrontendUrlPattern = 'Frontend:\s+http://127\.0\.0\.1:5173/login'
    BackendUrlPattern = 'Backend\s+:\s+http://127\.0\.0\.1:9091'
    FrontendPrefixPattern = '\[admin-frontend\]'
    BackendPrefixPattern = '\[admin-backend\]'
    Ports = @(5173, 9091)
  }
}

foreach ($target in $targets) {
  Invoke-DevConsoleSmoke `
    -Name $target.Name `
    -WorkingDirectory $target.WorkingDirectory `
    -ScriptPath $target.ScriptPath `
    -FrontendUrlPattern $target.FrontendUrlPattern `
    -BackendUrlPattern $target.BackendUrlPattern `
    -FrontendPrefixPattern $target.FrontendPrefixPattern `
    -BackendPrefixPattern $target.BackendPrefixPattern `
    -Ports $target.Ports
}
