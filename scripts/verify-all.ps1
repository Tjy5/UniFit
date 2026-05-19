param(
  [switch]$IncludeFullChainE2E,
  [switch]$SkipDevConsole
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

function Invoke-Step {
  param(
    [string]$Name,
    [string]$WorkingDirectory,
    [string]$FilePath,
    [string[]]$Arguments
  )

  [Console]::WriteLine("")
  [Console]::WriteLine("==> $Name")
  [Console]::WriteLine("    cwd: $WorkingDirectory")
  [Console]::WriteLine("    cmd: $FilePath $($Arguments -join ' ')")

  Push-Location $WorkingDirectory
  try {
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
      throw "$Name failed with exit code $LASTEXITCODE."
    }
  } finally {
    Pop-Location
  }
}

$userRoot = Join-Path $repoRoot 'School-uniform-intelligent-ordering-system-usersystem'
$adminRoot = Join-Path $repoRoot 'School-uniform-intelligent-ordering-system-managementor'
$userFrontendRoot = Join-Path $userRoot 'user-vue'
$adminFrontendRoot = Join-Path $adminRoot 'admin-web'

Invoke-Step -Name 'User backend Maven tests' -WorkingDirectory $userRoot -FilePath (Join-Path $userRoot 'mvnw.cmd') -Arguments @('test')
Invoke-Step -Name 'Admin backend Maven tests' -WorkingDirectory $adminRoot -FilePath (Join-Path $userRoot 'mvnw.cmd') -Arguments @('-f', (Join-Path $adminRoot 'pom.xml'), 'test')
Invoke-Step -Name 'User frontend deep selector guard' -WorkingDirectory $userFrontendRoot -FilePath 'npm.cmd' -Arguments @('run', 'lint:deep-selectors')
Invoke-Step -Name 'User frontend production build' -WorkingDirectory $userFrontendRoot -FilePath 'npm.cmd' -Arguments @('run', 'build')
Invoke-Step -Name 'Admin frontend production build' -WorkingDirectory $adminFrontendRoot -FilePath 'npm.cmd' -Arguments @('run', 'build')
Invoke-Step -Name 'Playwright mocked smoke tests' -WorkingDirectory $repoRoot -FilePath 'npm.cmd' -Arguments @('run', 'test:smoke')

if (-not $SkipDevConsole) {
  Invoke-Step -Name 'User and admin dev-console smoke tests' -WorkingDirectory $repoRoot -FilePath 'npm.cmd' -Arguments @('run', 'test:dev-console', '--', '-System', 'all')
}

if ($IncludeFullChainE2E) {
  Invoke-Step -Name 'Full-chain API E2E' -WorkingDirectory $repoRoot -FilePath 'npm.cmd' -Arguments @('run', 'test:e2e:full-chain')
} else {
  [Console]::WriteLine("")
  [Console]::WriteLine("Skipped full-chain API E2E. Re-run with -IncludeFullChainE2E after setting the MySQL and backend environment variables.")
}
