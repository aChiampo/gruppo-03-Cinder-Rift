#!/bin/sh

fail() {
    printf '%s\n' "$1" >&2
    exit 1
}

# Load the launcher before Git can replace this file.
main() {
    cd "$(dirname "$0")" || fail "Impossibile aprire la cartella del progetto."
    export PATH="$PATH:$HOME/.docker/bin"

    printf '%s\n' "Aggiornamento del progetto da dev..."
    git switch dev && git pull --ff-only origin dev ||
        fail "Aggiornamento non riuscito. Controlla i messaggi qui sopra."

    [ -f compose.yaml ] || fail "Il file compose.yaml manca nella cartella del progetto su dev."

    if ! docker info >/dev/null 2>&1; then
        printf '%s\n' "Avvio di Docker Desktop..."
        docker desktop start --timeout 120 ||
            fail "Apri Docker Desktop, attendi che sia pronto e riprova."
    fi

    printf '%s\n' "Preparazione e avvio dell'applicazione..."
    docker compose --file compose.yaml up --build --wait ||
        fail "Avvio non riuscito. Controlla i messaggi qui sopra."

    printf '%s\n' "Applicazione pronta. Apri http://localhost:8082 nel browser."
}

main
