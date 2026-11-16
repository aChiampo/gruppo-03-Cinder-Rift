@echo off
setlocal DisableDelayedExpansion
call :start & if errorlevel 1 (exit /b 1) else exit /b 0

:start
rem Read the whole block before Git can replace this file.
(
    cd /d "%~dp0" || (
        echo Impossibile aprire la cartella del progetto.
        pause
        exit /b 1
    )

    echo Aggiornamento del progetto da dev...
    git switch dev && git pull --ff-only origin dev
    if errorlevel 1 (
        echo Aggiornamento non riuscito. Controlla i messaggi qui sopra.
        pause
        exit /b 1
    )

    if not exist compose.yaml (
        echo Il file compose.yaml manca nella cartella del progetto su dev.
        pause
        exit /b 1
    )

    if not exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
        if not exist "%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe" (
            echo Docker Desktop non risulta installato. Scaricamento dell'installer...
            powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; $downloads = Join-Path $env:USERPROFILE 'Downloads'; New-Item -ItemType Directory -Force -Path $downloads | Out-Null; $arch = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'arm64' } else { 'amd64' }; $url = 'https://desktop.docker.com/win/main/' + $arch + '/' + [uri]::EscapeDataString('Docker Desktop Installer.exe'); Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile (Join-Path $downloads 'Docker Desktop Installer.exe')"
            if errorlevel 1 (
                echo Download di Docker Desktop non riuscito.
                pause
                exit /b 1
            )
            echo Installer scaricato in "%USERPROFILE%\Downloads\Docker Desktop Installer.exe".
            echo Installalo e poi rilancia questo script.
            pause
            exit /b 0
        )
    )

    docker info >nul 2>&1 || (
        echo Avvio di Docker Desktop...
        docker desktop start --timeout 120 || (
            echo Apri Docker Desktop, attendi che sia pronto e riprova.
            pause
            exit /b 1
        )
    )

    powershell -NoProfile -ExecutionPolicy Bypass -Command "$path = '.env'; $content = if (Test-Path $path) { [IO.File]::ReadAllText($path) } else { '' }; if ($content -match '(?m)^APP_SECURITY_JWT_SECRET=\S') { exit 0 }; if ($content -match '(?m)^APP_SECURITY_JWT_SECRET=[ \t]*\r?$') { Write-Error 'APP_SECURITY_JWT_SECRET è vuota in .env.'; exit 1 }; $bytes = New-Object byte[] 32; $rng = [Security.Cryptography.RandomNumberGenerator]::Create(); $rng.GetBytes($bytes); $secret = [Convert]::ToBase64String($bytes); $line = 'APP_SECURITY_JWT_SECRET=' + $secret + [Environment]::NewLine; if (Test-Path $path) { if ($content.Length -gt 0 -and -not $content.EndsWith([Environment]::NewLine)) { $line = [Environment]::NewLine + $line }; [IO.File]::AppendAllText($path, $line, [Text.Encoding]::ASCII) } else { [IO.File]::WriteAllText($path, $line, [Text.Encoding]::ASCII) }"
    if errorlevel 1 (
        echo Creazione della chiave JWT non riuscita. Controlla il file .env.
        pause
        exit /b 1
    )

    if not exist .env (
        echo Il file .env non esiste dopo la configurazione.
        pause
        exit /b 1
    )
    findstr /r /c:"^APP_SECURITY_JWT_SECRET=[A-Za-z0-9+/]" .env >nul
    if errorlevel 1 (
        echo APP_SECURITY_JWT_SECRET manca o e vuota in .env.
        pause
        exit /b 1
    )

    echo Preparazione e avvio dell'applicazione...
    docker compose --file compose.yaml up --build --wait || (
        echo Avvio non riuscito. Controlla i messaggi qui sopra.
        pause
        exit /b 1
    )

    echo Applicazione pronta. Apertura del frontend nel browser...
    start "" "http://localhost:8082"
    if errorlevel 1 echo Non e stato possibile aprire il browser. Visita http://localhost:8082.
    pause
    exit /b 0
)
