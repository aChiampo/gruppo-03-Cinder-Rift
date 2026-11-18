@echo off
setlocal DisableDelayedExpansion
call :run & if errorlevel 1 (exit /b 1) else exit /b 0

:run
rem Load the entire launcher before Git can replace this file.
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
Set-Location -LiteralPath (Split-Path -Parent $env:CINDER_LAUNCHER)
$env:PATH = "$env:ProgramFiles\Git\cmd;$env:LOCALAPPDATA\Programs\Git\cmd;$env:ProgramFiles\Docker\Docker\resources\bin;$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin;$env:PATH"
foreach ($program in @('git', 'docker')) {
    if (-not (Get-Command $program -CommandType Application -ErrorAction SilentlyContinue)) {
        throw "$program non disponibile. Esegui set-up-windows.bat e completa le installazioni."
    }
    & $program --version
    if ($LASTEXITCODE -ne 0) { throw "$program non utilizzabile. Esegui set-up-windows.bat." }
}
& docker compose version
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose non disponibile. Completa o correggi l installazione di Docker Desktop.' }

if (-not (Test-Path -LiteralPath '.git')) { throw 'Questa cartella non e un clone Git. Esegui set-up-windows.bat.' }
$origin = & git remote get-url origin
if ($LASTEXITCODE -ne 0 -or $origin -notmatch '^(https://github\.com/|git@github\.com:|ssh://git@github\.com/)aChiampo/gruppo-03-Cinder-Rift(\.git)?/?$') {
    throw 'Il repository non corrisponde a Cinder Rift. Esegui set-up-windows.bat.'
}
$changes = & git status --porcelain
if ($LASTEXITCODE -ne 0) { throw 'Impossibile verificare lo stato Git.' }
if ($changes) { throw 'Sono presenti modifiche locali. Salvale con Git o spostale prima di aggiornare. Nessuna modifica e stata eliminata.' }
Write-Host 'Aggiornamento del progetto da dev...'
& git switch dev
if ($LASTEXITCODE -ne 0) { throw 'Impossibile passare a dev. Controlla i messaggi Git.' }
& git pull --ff-only origin dev
if ($LASTEXITCODE -ne 0) { throw 'Aggiornamento non riuscito. Controlla rete, accesso GitHub ed eventuali commit divergenti.' }
if (-not (Test-Path -LiteralPath 'compose.yaml' -PathType Leaf)) { throw 'compose.yaml manca nel progetto su dev.' }

function Test-DockerReady {
    # Windows PowerShell may turn redirected native stderr into error records.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & docker info *> $null
        return $LASTEXITCODE -eq 0
    } finally { $ErrorActionPreference = $previousPreference }
}

if (-not (Test-DockerReady)) {
    Write-Host 'Avvio di Docker Desktop...'
    $dockerDesktop = @(
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe",
        "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe"
    ) | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    if (-not $dockerDesktop) { throw 'Docker Desktop non trovato. Aprilo manualmente o esegui set-up-windows.bat.' }
    Start-Process -FilePath $dockerDesktop -WindowStyle Hidden
    $deadline = (Get-Date).AddSeconds(120)
    do {
        Start-Sleep -Seconds 2
        $ready = Test-DockerReady
        if ($ready) { break }
    } while ((Get-Date) -lt $deadline)
    if (-not $ready) { throw 'Docker non pronto entro 120 secondi. Completa la configurazione in Docker Desktop e riprova.' }
}

function Ensure-JwtSecret {
    $path = Join-Path (Get-Location).Path '.env'
    $content = if (Test-Path -LiteralPath $path) { [IO.File]::ReadAllText($path) } else { '' }
    $pattern = '(?m)^[ \t]*(?:export[ \t]+)?APP_SECURITY_JWT_SECRET[ \t]*=([^\r\n]*)\r?$'
    $entries = [regex]::Matches($content, $pattern)
    if ($entries.Count -gt 1) { throw 'APP_SECURITY_JWT_SECRET e duplicata in .env. Mantieni una sola definizione.' }
    $secret = ''
    if ($entries.Count -eq 1) {
        $value = $entries[0].Groups[1].Value.Trim()
        if ($value.StartsWith('#')) { $value = '' }
        $valuePattern = '^(?:"([^"$`]*)"|''([^'']*)''|([^\s#]*))(?:[ \t]+#.*)?[ \t]*$'
        $parsed = [regex]::Match($value, $valuePattern)
        if (-not $parsed.Success) { throw 'Formato di APP_SECURITY_JWT_SECRET non supportato. Usa un valore Base64 letterale in .env.' }
        $secret = $parsed.Groups[1].Value + $parsed.Groups[2].Value + $parsed.Groups[3].Value
    }
    if ($secret) {
        try { $decoded = [Convert]::FromBase64String($secret) } catch { throw 'APP_SECURITY_JWT_SECRET non e Base64 valida. La chiave esistente non e stata modificata.' }
        if ($secret -notmatch '^[A-Za-z0-9+/]+={0,2}$' -or $decoded.Length -lt 32) {
            throw 'APP_SECURITY_JWT_SECRET deve contenere almeno 32 byte in Base64. La chiave esistente non e stata modificata.'
        }
        return
    }
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $line = 'APP_SECURITY_JWT_SECRET=' + [Convert]::ToBase64String($bytes)
    if ($entries.Count -eq 1) {
        $entry = $entries[0]
        $content = $content.Substring(0, $entry.Index) + $line + $content.Substring($entry.Index + $entry.Length)
    } else {
        if ($content.Length -gt 0 -and -not $content.EndsWith("`n")) { $content += [Environment]::NewLine }
        $content += $line + [Environment]::NewLine
    }
    [IO.File]::WriteAllText($path, $content, (New-Object Text.UTF8Encoding($false)))
    Write-Host 'Chiave JWT casuale creata nel file locale .env.'
}

Ensure-JwtSecret
# Shell variables override .env in Compose; use the local key consistently.
Remove-Item Env:APP_SECURITY_JWT_SECRET -ErrorAction SilentlyContinue
& docker compose --file compose.yaml config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Configurazione Compose non valida. Controlla .env e compose.yaml.' }
Write-Host 'Preparazione e avvio dell applicazione...'
& docker compose --file compose.yaml up --build --wait
if ($LASTEXITCODE -ne 0) { throw 'Avvio non riuscito. Controlla i messaggi Compose.' }
$url = 'http://localhost:8082'
Write-Host "Applicazione pronta: $url"
try { Start-Process $url } catch { Write-Host "Apri manualmente $url nel browser." }
