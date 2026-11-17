@echo off
setlocal DisableDelayedExpansion
call :run & if errorlevel 1 (exit /b 1) else exit /b 0

:run
rem Parse this block before the embedded PowerShell payload is executed.
(
    set "CINDER_LAUNCHER=%~f0"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; try { $source = [IO.File]::ReadAllText($env:CINDER_LAUNCHER); & ([scriptblock]::Create(($source -split '(?m)^# POWERSHELL\r?$', 2)[1])); exit 0 } catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }"
    if errorlevel 1 (
        pause
        exit /b 1
    )
    pause
    exit /b 0
)
# POWERSHELL
$ErrorActionPreference = 'Stop'
$repositoryUrl = 'https://github.com/aChiampo/gruppo-03-Cinder-Rift.git'
$desktop = [Environment]::GetFolderPath('DesktopDirectory')
if (-not $desktop) { throw 'Impossibile trovare il Desktop del tuo account.' }
$projectDir = Join-Path $desktop 'gruppo-03-Cinder-Rift'
$downloads = Join-Path $env:USERPROFILE 'Downloads'

# Refresh common install paths when setup is rerun from an older terminal.
$env:PATH = "$env:ProgramFiles\Git\cmd;$env:LOCALAPPDATA\Programs\Git\cmd;$env:ProgramFiles\Docker\Docker\resources\bin;$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin;$env:PATH"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$architecture = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
switch ($architecture) {
    'AMD64' { $dockerArch = 'amd64'; $gitAssetPattern = 'Git-*-64-bit.exe' }
    'ARM64' { $dockerArch = 'arm64'; $gitAssetPattern = 'Git-*-arm64.exe' }
    default { throw "Architettura Windows non supportata: $architecture" }
}

function Save-Installer([string]$Url, [string]$Name) {
    New-Item -ItemType Directory -Force -Path $downloads | Out-Null
    $installer = Join-Path $downloads $Name
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile "$installer.partial"
    Move-Item -LiteralPath "$installer.partial" -Destination $installer -Force
    Write-Host "Installer scaricato: $installer"
}

$needsInstallation = $false
$downloadErrors = @()
$gitAvailable = $false
if (Get-Command git -CommandType Application -ErrorAction SilentlyContinue) {
    & git --version
    $gitAvailable = $LASTEXITCODE -eq 0
}
if (-not $gitAvailable) {
    $needsInstallation = $true
    try {
        Write-Host 'Git non disponibile. Scaricamento da Git for Windows...'
        $release = Invoke-RestMethod -Uri 'https://api.github.com/repos/git-for-windows/git/releases/latest'
        $asset = @($release.assets | Where-Object { $_.name -like $gitAssetPattern })
        if ($asset.Count -ne 1) { throw 'Installer Git non trovato per questa architettura.' }
        Save-Installer $asset[0].browser_download_url $asset[0].name
    } catch { $downloadErrors += "Git: $($_.Exception.Message)" }
}

$dockerDesktop = @(
    "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe",
    "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe"
) | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if (-not $dockerDesktop) {
    $needsInstallation = $true
    try {
        Write-Host 'Docker Desktop non installato. Scaricamento...'
        Save-Installer "https://desktop.docker.com/win/main/$dockerArch/Docker%20Desktop%20Installer.exe" 'Docker Desktop Installer.exe'
    } catch { $downloadErrors += "Docker Desktop: $($_.Exception.Message)" }
}
if ($downloadErrors.Count) { throw ($downloadErrors -join [Environment]::NewLine) }
if ($needsInstallation) {
    Write-Host 'Apri gli installer nella cartella Downloads e completa le installazioni.'
    Write-Host 'Per Git, abilita l utilizzo dal prompt dei comandi. Completa anche il primo avvio di Docker Desktop.'
    Write-Host 'Se richiesto, riavvia Windows. Poi rilancia questo set-up per clonare il progetto.'
    return
}

function Assert-Checkout([string]$Path) {
    if (-not (Test-Path -LiteralPath (Join-Path $Path '.git'))) {
        throw "La destinazione esiste ma non e un clone Git: $Path. Spostala o rinominala e riprova."
    }
    $root = & git -C $Path rev-parse --show-toplevel
    if ($LASTEXITCODE -ne 0 -or [IO.Path]::GetFullPath("$root") -ne [IO.Path]::GetFullPath($Path)) {
        throw "Checkout Git non valido: $Path"
    }
    $origin = & git -C $Path remote get-url origin
    if ($LASTEXITCODE -ne 0 -or $origin -notmatch '^(https://github\.com/|git@github\.com:|ssh://git@github\.com/)aChiampo/gruppo-03-Cinder-Rift(\.git)?/?$') {
        throw "La cartella contiene un altro repository: $Path"
    }
}

$scriptDir = Split-Path -Parent $env:CINDER_LAUNCHER
if (Test-Path -LiteralPath (Join-Path $scriptDir '.git')) {
    Assert-Checkout $scriptDir
    $projectDir = $scriptDir
} elseif (Test-Path -LiteralPath $projectDir) {
    Assert-Checkout $projectDir
} else {
    New-Item -ItemType Directory -Force -Path $desktop | Out-Null
    Write-Host "Clonazione di dev in $projectDir..."
    & git clone --branch dev $repositoryUrl $projectDir
    if ($LASTEXITCODE -ne 0) { throw 'Clonazione non riuscita. Controlla rete, accesso GitHub e destinazione; nessuna cartella viene eliminata automaticamente.' }
}
if (-not (Test-Path -LiteralPath (Join-Path $projectDir 'start-up-windows.bat') -PathType Leaf)) {
    throw "Il clone esiste in $projectDir, ma start-up-windows.bat manca. Verifica che i nuovi script siano disponibili su dev."
}
Write-Host "Preparazione completata. Per avviare l applicazione, apri: $projectDir\start-up-windows.bat"
