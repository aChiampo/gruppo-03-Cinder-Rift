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

    docker info >nul 2>&1 || (
        echo Avvio di Docker Desktop...
        docker desktop start --timeout 120 || (
            echo Apri Docker Desktop, attendi che sia pronto e riprova.
            pause
            exit /b 1
        )
    )

    echo Preparazione e avvio dell'applicazione...
    docker compose --file compose.yaml up --build --wait || (
        echo Avvio non riuscito. Controlla i messaggi qui sopra.
        pause
        exit /b 1
    )

    echo Applicazione pronta. Apri http://localhost:8082 nel browser.
    pause
    exit /b 0
)
