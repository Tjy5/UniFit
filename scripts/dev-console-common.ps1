$script:ProcessJob = [IntPtr]::Zero
$script:DevConsoleJobName = 'suios-dev-console'

function Get-DevConsoleTestSeconds {
  $testSeconds = 0
  if ($env:SUIOS_DEV_CONSOLE_TEST_SECONDS) {
    [void][int]::TryParse($env:SUIOS_DEV_CONSOLE_TEST_SECONDS, [ref]$testSeconds)
  }

  return $testSeconds
}

function Test-PortListening {
  param([int]$Port)

  return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Get-ListeningProcess {
  param([int]$Port)

  $connection = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
    Select-Object -First 1

  if (-not $connection) {
    return $null
  }

  try {
    return Get-Process -Id $connection.OwningProcess -ErrorAction Stop
  } catch {
    return $null
  }
}

function Get-ProcessCommandLine {
  param([int]$ProcessId)

  try {
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction Stop
    return $processInfo.CommandLine
  } catch {
    return $null
  }
}

function Get-PortOwnerSummary {
  param([int]$Port)

  $process = Get-ListeningProcess -Port $Port
  if (-not $process) {
    return "port $Port is already in use."
  }

  $commandLine = Get-ProcessCommandLine -ProcessId $process.Id
  if ($commandLine) {
    return "port $Port is already in use by PID=$($process.Id) ($($process.ProcessName)): $commandLine"
  }

  return "port $Port is already in use by PID=$($process.Id) ($($process.ProcessName))."
}

function Get-ManagedProcess {
  param($Entry)

  if ($Entry -is [System.Diagnostics.Process]) {
    return $Entry
  }

  if ($Entry -and $Entry.PSObject.Properties['Process']) {
    return $Entry.Process
  }

  return $null
}

function Initialize-KillOnCloseJob {
  param([string]$JobName = $script:DevConsoleJobName)

  if ($script:ProcessJob -ne [IntPtr]::Zero) {
    return $true
  }

  if (-not ([System.Management.Automation.PSTypeName]'SuiosDevConsoleJobObject').Type) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class SuiosDevConsoleJobObject
{
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern IntPtr CreateJobObject(IntPtr lpJobAttributes, string lpName);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool SetInformationJobObject(IntPtr hJob, int jobObjectInfoClass, IntPtr lpJobObjectInfo, uint cbJobObjectInfoLength);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool AssignProcessToJobObject(IntPtr hJob, IntPtr hProcess);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern IntPtr OpenProcess(uint dwDesiredAccess, bool bInheritHandle, int dwProcessId);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool CloseHandle(IntPtr hObject);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool IsProcessInJob(IntPtr ProcessHandle, IntPtr JobHandle, out bool Result);

    [StructLayout(LayoutKind.Sequential)]
    public struct JOBOBJECT_BASIC_LIMIT_INFORMATION
    {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSetSize;
        public UIntPtr MaximumWorkingSetSize;
        public uint ActiveProcessLimit;
        public long Affinity;
        public uint PriorityClass;
        public uint SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct IO_COUNTERS
    {
        public ulong ReadOperationCount;
        public ulong WriteOperationCount;
        public ulong OtherOperationCount;
        public ulong ReadTransferCount;
        public ulong WriteTransferCount;
        public ulong OtherTransferCount;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct JOBOBJECT_EXTENDED_LIMIT_INFORMATION
    {
        public JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
        public IO_COUNTERS IoInfo;
        public UIntPtr ProcessMemoryLimit;
        public UIntPtr JobMemoryLimit;
        public UIntPtr PeakProcessMemoryUsed;
        public UIntPtr PeakJobMemoryUsed;
    }

    public const int JobObjectExtendedLimitInformation = 9;
    public const uint JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000;
    public const uint PROCESS_TERMINATE = 0x0001;
    public const uint PROCESS_SET_QUOTA = 0x0100;
    public const uint PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
    public const int ERROR_ACCESS_DENIED = 5;
}
'@
  }

  $job = [SuiosDevConsoleJobObject]::CreateJobObject([IntPtr]::Zero, $JobName)
  if ($job -eq [IntPtr]::Zero) {
    Write-Warning "Failed to create the $JobName process job. Hard terminal closes may leave child processes running."
    return $false
  }

  $info = New-Object SuiosDevConsoleJobObject+JOBOBJECT_EXTENDED_LIMIT_INFORMATION
  $info.BasicLimitInformation.LimitFlags = [SuiosDevConsoleJobObject]::JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
  $length = [System.Runtime.InteropServices.Marshal]::SizeOf([type]'SuiosDevConsoleJobObject+JOBOBJECT_EXTENDED_LIMIT_INFORMATION')
  $buffer = [System.Runtime.InteropServices.Marshal]::AllocHGlobal($length)

  try {
    [System.Runtime.InteropServices.Marshal]::StructureToPtr($info, $buffer, $false)
    if (-not [SuiosDevConsoleJobObject]::SetInformationJobObject(
          $job,
          [SuiosDevConsoleJobObject]::JobObjectExtendedLimitInformation,
          $buffer,
          [uint32]$length)) {
      Write-Warning "Failed to enable kill-on-close for the $JobName process job. Hard terminal closes may leave child processes running."
      return $false
    }
  } finally {
    [System.Runtime.InteropServices.Marshal]::FreeHGlobal($buffer)
  }

  $script:ProcessJob = $job
  return $true
}

function Register-ManagedProcessWithJob {
  param(
    $Entry,
    [string]$JobName = $script:DevConsoleJobName
  )

  $proc = Get-ManagedProcess -Entry $Entry
  if (-not $proc -or $proc.HasExited) {
    return
  }

  if (-not (Initialize-KillOnCloseJob -JobName $JobName)) {
    return
  }

  $processAccess = [SuiosDevConsoleJobObject]::PROCESS_TERMINATE -bor
    [SuiosDevConsoleJobObject]::PROCESS_SET_QUOTA -bor
    [SuiosDevConsoleJobObject]::PROCESS_QUERY_LIMITED_INFORMATION
  $processHandle = [SuiosDevConsoleJobObject]::OpenProcess($processAccess, $false, $proc.Id)
  if ($processHandle -eq [IntPtr]::Zero) {
    $errorCode = [System.Runtime.InteropServices.Marshal]::GetLastWin32Error()
    Write-Warning "Failed to open PID=$($proc.Id) for $JobName job-object attachment (Win32=$errorCode). Hard terminal closes may not stop this process automatically."
    return
  }

  try {
    if ([SuiosDevConsoleJobObject]::AssignProcessToJobObject($script:ProcessJob, $processHandle)) {
      return
    }

    $errorCode = [System.Runtime.InteropServices.Marshal]::GetLastWin32Error()
    $isInJob = $false
    if ($errorCode -eq [SuiosDevConsoleJobObject]::ERROR_ACCESS_DENIED -and
        [SuiosDevConsoleJobObject]::IsProcessInJob($processHandle, [IntPtr]::Zero, [ref]$isInJob) -and
        $isInJob) {
      Write-Verbose "PID=$($proc.Id) already belongs to a Windows job, so this session cannot add the $JobName kill-on-close job. Normal script-exit cleanup remains active."
      return
    }

    Write-Warning "Failed to attach PID=$($proc.Id) to the $JobName job object (Win32=$errorCode). Hard terminal closes may not stop this process automatically."
  } finally {
    [void][SuiosDevConsoleJobObject]::CloseHandle($processHandle)
  }
}

function Wait-Port {
  param(
    [int]$Port,
    [int]$TimeoutSeconds = 120
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-PortListening -Port $Port) {
      return $true
    }
    Start-Sleep -Seconds 2
  }

  return (Test-PortListening -Port $Port)
}

function Invoke-HttpReadinessCheck {
  param(
    [string]$Name,
    [string]$Url,
    [int[]]$AcceptStatusCodes = @(200),
    [int]$TimeoutSeconds = 60
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  $lastError = $null
  while ((Get-Date) -lt $deadline) {
    try {
      $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
      if ($AcceptStatusCodes -contains [int]$response.StatusCode) {
        [Console]::WriteLine("$Name readiness OK: $Url")
        return $true
      }
      $lastError = "HTTP $($response.StatusCode)"
    } catch {
      $lastError = $_.Exception.Message
      $response = $_.Exception.Response
      if ($response -and ($AcceptStatusCodes -contains [int]$response.StatusCode)) {
        [Console]::WriteLine("$Name readiness OK: $Url")
        return $true
      }
    }

    Start-Sleep -Seconds 2
  }

  throw "$Name readiness check failed for $Url. Last error: $lastError"
}

function Convert-ArgumentsToString {
  param([string[]]$Arguments)

  return ($Arguments | ForEach-Object {
      if ($_ -match '[\s"]') {
        '"' + ($_ -replace '"', '\"') + '"'
      } else {
        $_
      }
    }) -join ' '
}

function Resolve-MySqlInstallRoot {
  if ($env:SUIOS_MYSQL_ROOT -and (Test-Path $env:SUIOS_MYSQL_ROOT)) {
    return $env:SUIOS_MYSQL_ROOT
  }

  $mysqld = Get-Command 'mysqld.exe' -ErrorAction SilentlyContinue
  if (-not $mysqld) {
    return $null
  }

  return Split-Path -Parent (Split-Path -Parent $mysqld.Source)
}

function Ensure-MySqlListening {
  param(
    [string]$SystemName,
    [int]$Port = 3306,
    [int]$TimeoutSeconds = 30
  )

  if (Test-PortListening -Port $Port) {
    return $true
  }

  $installRoot = Resolve-MySqlInstallRoot
  if (-not $installRoot) {
    Write-Warning "MySQL is not listening on $Port and no local mysqld.exe was found on PATH. $SystemName backend startup may fail."
    return $false
  }

  $mysqld = Join-Path $installRoot 'bin\mysqld.exe'
  $config = Join-Path $installRoot 'my.ini'

  if (-not (Test-Path $mysqld) -or -not (Test-Path $config)) {
    Write-Warning "MySQL is not listening on $Port and the local install at $installRoot is missing mysqld.exe or my.ini. $SystemName backend startup may fail."
    return $false
  }

  [Console]::WriteLine("Starting local MySQL from $installRoot ...")

  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $mysqld
  $psi.Arguments = Convert-ArgumentsToString -Arguments @("--defaults-file=$config")
  $psi.WorkingDirectory = $installRoot
  $psi.UseShellExecute = $false
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.CreateNoWindow = $true

  $proc = New-Object System.Diagnostics.Process
  $proc.StartInfo = $psi

  if (-not $proc.Start()) {
    Write-Warning "Failed to start local MySQL from $installRoot. $SystemName backend startup may fail."
    return $false
  }

  if (-not (Wait-Port -Port $Port -TimeoutSeconds $TimeoutSeconds)) {
    $logFile = Join-Path $installRoot 'logs\mysqld.err.log'
    if (Test-Path $logFile) {
      Write-Warning "MySQL did not begin listening on $Port. Check $logFile for details."
    } else {
      Write-Warning "MySQL did not begin listening on $Port. $SystemName backend startup may fail."
    }
    return $false
  }

  [Console]::WriteLine("MySQL is now listening on $Port.")
  return $true
}

function New-ProcessLogPath {
  param(
    [string]$Name,
    [string]$Stream,
    [string]$LogDir = $script:LogDir
  )

  if (-not (Test-Path $LogDir)) {
    [void](New-Item -ItemType Directory -Path $LogDir -Force)
  }

  $timestamp = Get-Date -Format 'yyyyMMdd-HHmmssfff'
  return Join-Path $LogDir "$Name.$timestamp.$Stream.log"
}

function Emit-NewLogLines {
  param(
    [pscustomobject]$Entry,
    [string]$Path,
    [string]$Prefix,
    [string]$CounterProperty
  )

  if (-not $Entry -or -not $Path -or -not (Test-Path $Path)) {
    return
  }

  $lines = @(Get-Content -Path $Path -ErrorAction SilentlyContinue)
  $startIndex = [int]$Entry.$CounterProperty

  for ($index = $startIndex; $index -lt $lines.Count; $index++) {
    if (-not [string]::IsNullOrWhiteSpace($lines[$index])) {
      [Console]::WriteLine(("[{0}] {1}" -f $Prefix, $lines[$index]))
    }
  }

  $Entry.$CounterProperty = $lines.Count
}

function Drain-ProcessLogs {
  param([object[]]$Processes)

  foreach ($entry in $Processes) {
    if (-not ($entry -is [pscustomobject])) {
      continue
    }

    Emit-NewLogLines -Entry $entry -Path $entry.StdOutPath -Prefix $entry.Name -CounterProperty 'StdOutLineCount'
    Emit-NewLogLines -Entry $entry -Path $entry.StdErrPath -Prefix "$($entry.Name):err" -CounterProperty 'StdErrLineCount'
  }
}

function Start-StreamingProcess {
  param(
    [string]$Name,
    [string]$FilePath,
    [string[]]$Arguments,
    [string]$WorkingDirectory,
    [int]$Port
  )

  if (Test-PortListening -Port $Port) {
    throw "$Name $(Get-PortOwnerSummary -Port $Port) Please stop the existing process first."
  }

  $stdoutPath = New-ProcessLogPath -Name $Name -Stream 'stdout'
  $stderrPath = New-ProcessLogPath -Name $Name -Stream 'stderr'

  $proc = Start-Process `
    -FilePath $FilePath `
    -ArgumentList $Arguments `
    -WorkingDirectory $WorkingDirectory `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath `
    -NoNewWindow `
    -PassThru

  if (-not $proc) {
    throw "Failed to start $Name"
  }

  if (-not (Wait-Port -Port $Port -TimeoutSeconds 120)) {
    try {
      if (-not $proc.HasExited) {
        $proc.Kill($true)
      }
    } catch {
    }
    throw "$Name failed to start on port $Port"
  }

  return [pscustomobject]@{
    Name = $Name
    Process = $proc
    StdOutPath = $stdoutPath
    StdErrPath = $stderrPath
    StdOutLineCount = 0
    StdErrLineCount = 0
  }
}

function Stop-ProcessTree {
  param([object[]]$Processes)

  foreach ($entry in $Processes) {
    $proc = Get-ManagedProcess -Entry $entry
    if ($proc -and -not $proc.HasExited) {
      try {
        & taskkill.exe /F /T /PID $proc.Id | Out-Null
      } catch {
        try {
          $proc.Kill($true)
        } catch {
        }
      }
    }
  }
}

function Watch-DevProcesses {
  param(
    [object[]]$WatchedProcesses,
    [object[]]$StartedProcesses,
    [int]$TestSeconds = 0
  )

  $startedAt = Get-Date
  while ($true) {
    Drain-ProcessLogs -Processes $StartedProcesses

    foreach ($entry in $WatchedProcesses) {
      $proc = Get-ManagedProcess -Entry $entry
      if ($proc -and $proc.HasExited) {
        throw "A dev process exited unexpectedly. PID=$($proc.Id) ExitCode=$($proc.ExitCode)"
      }
    }

    if ($TestSeconds -gt 0 -and ((Get-Date) - $startedAt).TotalSeconds -ge $TestSeconds) {
      break
    }

    Start-Sleep -Milliseconds 300
  }
}
