#!/bin/sh

fail() {
    printf '%s\n' "$1" >&2
    exit 1
}

download_docker_desktop() {
    case "$(uname -m)" in
        arm64) download_url="https://desktop.docker.com/mac/main/arm64/Docker.dmg" ;;
        x86_64) download_url="https://desktop.docker.com/mac/main/amd64/Docker.dmg" ;;
        *) fail "Architettura Mac non supportata per il download automatico di Docker Desktop." ;;
    esac

    installer="$HOME/Downloads/Docker.dmg"
    mkdir -p "$HOME/Downloads" ||
        fail "Impossibile creare la cartella Download."

    printf '%s\n' "Docker Desktop non risulta installato. Scaricamento dell'installer..."
    curl --fail --location --retry 3 "$download_url" --output "$installer" ||
        fail "Download di Docker Desktop non riuscito."

    printf 'Installer scaricato in %s\n' "$installer"
    printf '%s\n' "Apri il DMG, installa Docker Desktop e poi rilancia questo script."
    exit 0
}

ensure_jwt_secret() {
    if [ -f .env ]; then
        if grep -q '^APP_SECURITY_JWT_SECRET=[^[:space:]]' .env; then
            return
        fi

        if grep -q '^APP_SECURITY_JWT_SECRET=[[:space:]]*$' .env; then
            fail "APP_SECURITY_JWT_SECRET è vuota in .env. Correggila e rilancia lo script."
        fi

        secret=$(openssl rand -base64 32) ||
            fail "Generazione della chiave JWT non riuscita."
        printf '\nAPP_SECURITY_JWT_SECRET=%s\n' "$secret" >> .env ||
            fail "Impossibile aggiornare .env."
    else
        secret=$(openssl rand -base64 32) ||
            fail "Generazione della chiave JWT non riuscita."
        (umask 077; printf 'APP_SECURITY_JWT_SECRET=%s\n' "$secret" > .env) ||
            fail "Impossibile creare .env."
    fi

    printf '%s\n' "Chiave JWT casuale creata nel file locale .env."
}

# Load the launcher before Git can replace this file.
main() {
    cd "$(dirname "$0")" || fail "Impossibile aprire la cartella del progetto."
    export PATH="$PATH:$HOME/.docker/bin"

    printf '%s\n' "Aggiornamento del progetto da dev..."
    git switch dev && git pull --ff-only origin dev ||
        fail "Aggiornamento non riuscito. Controlla i messaggi qui sopra."

    [ -f compose.yaml ] || fail "Il file compose.yaml manca nella cartella del progetto su dev."

    if [ ! -d /Applications/Docker.app ] && [ ! -d "$HOME/Applications/Docker.app" ]; then
        download_docker_desktop
    fi

    if ! docker info >/dev/null 2>&1; then
        printf '%s\n' "Avvio di Docker Desktop..."
        docker desktop start --timeout 120 ||
            fail "Apri Docker Desktop, attendi che sia pronto e riprova."
    fi

    ensure_jwt_secret

    [ -f .env ] || fail "Il file .env non è stato creato."
    grep -q '^APP_SECURITY_JWT_SECRET=[^[:space:]]' .env ||
        fail "APP_SECURITY_JWT_SECRET manca o è vuota in .env."

    printf '%s\n' "Preparazione e avvio dell'applicazione..."
    docker compose --file compose.yaml up --build --wait ||
        fail "Avvio non riuscito. Controlla i messaggi qui sopra."

    printf '%s\n' "Applicazione pronta. Apertura del frontend nel browser..."
    open "http://localhost:8082" ||
        printf '%s\n' "Non è stato possibile aprire il browser. Visita http://localhost:8082." >&2
}

main
