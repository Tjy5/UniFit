$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $root
. (Join-Path $repoRoot 'scripts\dev-console-common.ps1')

$frontendDir = Join-Path $root 'user-vue'
$frontendServeScript = Join-Path $frontendDir 'node_modules\vite\bin\vite.js'
$script:LogDir = Join-Path ([System.IO.Path]::GetTempPath()) 'suios-usersystem-dev-logs'
$script:DevConsoleJobName = 'suios-usersystem-dev'
$testSeconds = Get-DevConsoleTestSeconds

function Get-ReusableUserBackend {
  param([int]$Port = 9090)

  $process = Get-ListeningProcess -Port $Port
  if (-not $process) {
    return $null
  }

  $commandLine = Get-ProcessCommandLine -ProcessId $process.Id
  if ($process.ProcessName -eq 'java' -and $commandLine -match 'com\.authguard\.usersystem\.UsersystemApplication') {
    return $process
  }

  return $null
}

function Get-ReusableUserFrontend {
  param([int]$Port = 3000)

  $process = Get-ListeningProcess -Port $Port
  if (-not $process) {
    return $null
  }

  $commandLine = Get-ProcessCommandLine -ProcessId $process.Id
  if ($process.ProcessName -eq 'node' -and
      $commandLine -match 'vite(\.js)?' -and
      $commandLine -match 'user-vue' -and
      $commandLine -match '--port 3000') {
    return $process
  }

  return $null
}

[void](Ensure-MySqlListening -SystemName 'User' -Port 3306)

if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
  [Console]::WriteLine('Installing user frontend dependencies...')
  Push-Location $frontendDir
  try {
    & npm.cmd install
    if ($LASTEXITCODE -ne 0) {
      throw 'Failed to install user frontend dependencies.'
    }
  } finally {
    Pop-Location
  }
}

if (-not (Test-Path $frontendServeScript)) {
  throw "Cannot find Vite dev server entry at $frontendServeScript"
}

$startedProcesses = @()
$watchedProcesses = @()

try {
  $backend = Get-ReusableUserBackend -Port 9090
  if ($backend) {
    [Console]::WriteLine("Reusing existing user-backend on port 9090 (PID=$($backend.Id)).")
  } elseif (Test-PortListening -Port 9090) {
    throw "user-backend $(Get-PortOwnerSummary -Port 9090) Please stop the existing process first."
  } else {
    $backend = Start-StreamingProcess `
      -Name 'user-backend' `
      -FilePath (Join-Path $root 'mvnw.cmd') `
      -Arguments @('spring-boot:run') `
      -WorkingDirectory $root `
      -Port 9090

    $startedProcesses += $backend
    Register-ManagedProcessWithJob -Entry $backend
  }

  $watchedProcesses += $backend

  $frontend = Get-ReusableUserFrontend -Port 3000
  if ($frontend) {
    [Console]::WriteLine("Reusing existing user-frontend on port 3000 (PID=$($frontend.Id)).")
  } elseif (Test-PortListening -Port 3000) {
    throw "user-frontend $(Get-PortOwnerSummary -Port 3000) Please stop the existing process first."
  } else {
    $frontend = Start-StreamingProcess `
      -Name 'user-frontend' `
      -FilePath 'node.exe' `
      -Arguments @($frontendServeScript, '--host', '127.0.0.1', '--port', '3000') `
      -WorkingDirectory $frontendDir `
      -Port 3000

    $startedProcesses += $frontend
    Register-ManagedProcessWithJob -Entry $frontend
  }

  $watchedProcesses += $frontend

  Invoke-HttpReadinessCheck -Name 'user frontend' -Url 'http://127.0.0.1:3000/login'
  Invoke-HttpReadinessCheck -Name 'user backend' -Url 'http://127.0.0.1:9090/api/s-schools/selectList'

  [Console]::WriteLine('')
  [Console]::WriteLine('User system dev mode is attached to this terminal.')
  [Console]::WriteLine('Frontend: http://127.0.0.1:3000')
  [Console]::WriteLine('Backend : http://127.0.0.1:9090')
  [Console]::WriteLine('Press Ctrl+C to stop the attached frontend/backend services. MySQL will remain running.')
  [Console]::WriteLine('')

  Watch-DevProcesses -WatchedProcesses $watchedProcesses -StartedProcesses $startedProcesses -TestSeconds $testSeconds
} finally {
  Drain-ProcessLogs -Processes $startedProcesses
  Stop-ProcessTree -Processes $startedProcesses
}
