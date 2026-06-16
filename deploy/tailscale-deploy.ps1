param(
    [Parameter(Mandatory = $true)]
    [string]$HostName,

    [string]$User = $env:USERNAME,
    [string]$RemoteDir = "/opt/lastsys",
    [string]$ServiceName = "lastsys",
    [switch]$RunTests
)

$ErrorActionPreference = "Stop"

$sshTarget = "$User@$HostName"
$gradle = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { "gradle" }

if ($RunTests) {
    & $gradle clean test bootJar
} else {
    & $gradle clean bootJar
}

$jar = Get-ChildItem ".\build\libs\*.jar" |
    Where-Object { $_.Name -notlike "*-plain.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    throw "No boot jar found under build\libs."
}

$remoteTmp = "/tmp/$($jar.Name)"

Write-Host "Uploading $($jar.FullName) to ${sshTarget}:$remoteTmp"
scp $jar.FullName "${sshTarget}:$remoteTmp"

$remoteCommand = @"
set -e
sudo mkdir -p '$RemoteDir'
sudo install -m 0644 '$remoteTmp' '$RemoteDir/app.jar'
sudo systemctl restart '$ServiceName'
sudo systemctl --no-pager --full status '$ServiceName'
"@

ssh -t $sshTarget "bash -lc '$($remoteCommand.Replace("'", "'\''"))'"
