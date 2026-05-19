$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $root
. (Join-Path $repoRoot 'scripts\dev-console-common.ps1')

$frontendDir = Join-Path $root 'admin-web'
$frontendServeScript = Join-Path $frontendDir 'node_modules\vite\bin\vite.js'
$wrapper = Join-Path $repoRoot 'School-uniform-intelligent-ordering-system-usersystem\mvnw.cmd'
$parentPom = Join-Path $root 'pom.xml'
$backendWorkDir = Join-Path $root 'admin-boot'
$backendJar = Join-Path $backendWorkDir 'target\admin-boot-0.0.1-SNAPSHOT.jar'
$script:LogDir = Join-Path ([System.IO.Path]::GetTempPath()) 'suios-admin-dev-logs'
$script:DevConsoleJobName = 'suios-admin-dev'
$testSeconds = Get-DevConsoleTestSeconds

function Get-ReusableAdminBackend {
  param([int]$Port = 9091)

  $process = Get-ListeningProcess -Port $Port
  if (-not $process) {
    return $null
  }

  $commandLine = Get-ProcessCommandLine -ProcessId $process.Id
  if ($process.ProcessName -eq 'java' -and (
      $commandLine -match 'admin-boot-0\.0\.1-SNAPSHOT\.jar' -or
      $commandLine -match 'com\.suios\.admin\.AdminApplication')) {
    return $process
  }

  return $null
}

function Get-ReusableAdminFrontend {
  param([int]$Port = 5173)

  $process = Get-ListeningProcess -Port $Port
  if (-not $process) {
    return $null
  }

  $commandLine = Get-ProcessCommandLine -ProcessId $process.Id
  if ($process.ProcessName -eq 'node' -and
      $commandLine -match 'vite(?:\.js)?' -and
      $commandLine -match 'admin-web' -and
      $commandLine -match '--port 5173') {
    return $process
  }

  return $null
}

[void](Ensure-MySqlListening -SystemName 'Admin' -Port 3306)

if (-not (Test-Path $wrapper)) {
  throw "Cannot find Maven wrapper at $wrapper"
}

if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
  [Console]::WriteLine('Installing admin frontend dependencies...')
  Push-Location $frontendDir
  try {
    & npm.cmd install
    if ($LASTEXITCODE -ne 0) {
      throw 'Failed to install admin frontend dependencies.'
    }
  } finally {
    Pop-Location
  }
}

if (-not (Test-Path $frontendServeScript)) {
  throw "Cannot find Vite CLI at $frontendServeScript"
}

$startedProcesses = @()
$watchedProcesses = @()

try {
  $backend = Get-ReusableAdminBackend -Port 9091
  if ($backend) {
    [Console]::WriteLine("Reusing existing admin-backend on port 9091 (PID=$($backend.Id)).")
  } elseif (Test-PortListening -Port 9091) {
    throw "admin-backend $(Get-PortOwnerSummary -Port 9091) Please stop the existing process first."
  } else {
    [Console]::WriteLine('Packaging admin backend...')
    & $wrapper '-f' $parentPom '-pl' 'admin-boot' '-am' 'package' '-DskipTests'
    if ($LASTEXITCODE -ne 0) {
      throw 'Failed to package admin backend.'
    }

    if (-not (Test-Path $backendJar)) {
      throw "Cannot find packaged admin jar at $backendJar"
    }

    $backend = Start-StreamingProcess `
      -Name 'admin-backend' `
      -FilePath 'java.exe' `
      -Arguments @('-jar', $backendJar) `
      -WorkingDirectory $backendWorkDir `
      -Port 9091

    $startedProcesses += $backend
    Register-ManagedProcessWithJob -Entry $backend
  }

  $watchedProcesses += $backend

  $frontend = Get-ReusableAdminFrontend -Port 5173
  if ($frontend) {
    [Console]::WriteLine("Reusing existing admin-frontend on port 5173 (PID=$($frontend.Id)).")
  } elseif (Test-PortListening -Port 5173) {
    throw "admin-frontend $(Get-PortOwnerSummary -Port 5173) Please stop the existing process first."
  } else {
    $frontend = Start-StreamingProcess `
      -Name 'admin-frontend' `
      -FilePath 'node.exe' `
      -Arguments @($frontendServeScript, '--host', '0.0.0.0', '--port', '5173', '--force') `
      -WorkingDirectory $frontendDir `
      -Port 5173

    $startedProcesses += $frontend
    Register-ManagedProcessWithJob -Entry $frontend
  }

  $watchedProcesses += $frontend

  Invoke-HttpReadinessCheck -Name 'admin frontend' -Url 'http://127.0.0.1:5173/login'
  Invoke-HttpReadinessCheck -Name 'admin backend' -Url 'http://127.0.0.1:9091/v3/api-docs'

  [Console]::WriteLine('')
  [Console]::WriteLine('Admin system dev mode is attached to this terminal.')
  [Console]::WriteLine('Frontend: http://127.0.0.1:5173/login')
  [Console]::WriteLine('Backend : http://127.0.0.1:9091')
  [Console]::WriteLine('Press Ctrl+C to stop the attached admin frontend/backend services. MySQL will remain running.')
  [Console]::WriteLine('')

  Watch-DevProcesses -WatchedProcesses $watchedProcesses -StartedProcesses $startedProcesses -TestSeconds $testSeconds
} finally {
  Drain-ProcessLogs -Processes $startedProcesses
  Stop-ProcessTree -Processes $startedProcesses
}
