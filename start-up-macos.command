#!/bin/sh

fail() {
    printf '%s\n' "$1" >&2
    exit 1
}

ensure_jwt_secret() {
    pattern='^[[:blank:]]*(export[[:blank:]]+)?APP_SECURITY_JWT_SECRET[[:blank:]]*='
    count=0
    if [ -f .env ]; then
        count=$(grep -Ec "$pattern" .env) || [ "$count" = 0 ] || fail "Impossibile leggere .env."
    elif [ -e .env ] || [ -L .env ]; then
        fail ".env non e un file leggibile."
    fi
    [ "$count" -le 1 ] || fail "APP_SECURITY_JWT_SECRET e duplicata in .env. Mantieni una sola definizione."
    secret=''
    if [ "$count" = 1 ]; then
        raw=$(grep -E "$pattern" .env | sed -E 's/^[^=]*=[[:blank:]]*//; s/\r$//; s/[[:blank:]]+#.*$//; s/[[:blank:]]+$//') || fail "Impossibile leggere la chiave JWT."
        case "$raw" in
            \#*) secret='' ;;
            \"*\") secret=${raw#\"}; secret=${secret%\"} ;;
            \'*\') secret=${raw#\'}; secret=${secret%\'} ;;
            *) secret=$raw ;;
        esac
    fi
    if [ -n "$secret" ]; then
        printf '%s\n' "$secret" | grep -Eq '^[A-Za-z0-9+/]+={0,2}$' ||
            fail "APP_SECURITY_JWT_SECRET non e Base64 valida. La chiave esistente non e stata modificata."
        [ $((${#secret} % 4)) -eq 0 ] || fail "APP_SECURITY_JWT_SECRET non e Base64 valida."
        size=$(printf '%s' "$secret" | openssl base64 -d -A | wc -c | tr -d '[:space:]') || fail "Impossibile verificare la chiave JWT."
        [ "$size" -ge 32 ] || fail "APP_SECURITY_JWT_SECRET deve contenere almeno 32 byte in Base64. La chiave esistente non e stata modificata."
        return
    fi
    secret=$(openssl rand -base64 32) || fail "Generazione della chiave JWT non riuscita."
    # Write beside .env, then replace it only after the full update succeeds.
    temp_env=$(mktemp .env.XXXXXX) || fail "Impossibile preparare .env."
    if [ -f .env ]; then
        if [ "$count" = 1 ]; then
            awk -v replacement="APP_SECURITY_JWT_SECRET=$secret" '
                /^[ \t]*(export[ \t]+)?APP_SECURITY_JWT_SECRET[ \t]*=/ { print replacement; next }
                { print }
            ' .env > "$temp_env" || { rm -f "$temp_env"; fail "Impossibile aggiornare .env."; }
        else
            cat .env > "$temp_env" && printf '\nAPP_SECURITY_JWT_SECRET=%s\n' "$secret" >> "$temp_env" ||
                { rm -f "$temp_env"; fail "Impossibile aggiornare .env."; }
        fi
    else
        printf 'APP_SECURITY_JWT_SECRET=%s\n' "$secret" > "$temp_env" ||
            { rm -f "$temp_env"; fail "Impossibile creare .env."; }
    fi
    chmod 600 "$temp_env" && mv -f "$temp_env" .env ||
        { rm -f "$temp_env"; fail "Impossibile salvare .env."; }
    printf '%s\n' "Chiave JWT casuale creata nel file locale .env."
}

# Load all functions before Git can replace this launcher.
main() {
    [ "$(uname -s)" = Darwin ] || fail "Questo script richiede macOS. Su Windows usa start-up-windows.bat."
    cd "$(dirname -- "$0")" || fail "Impossibile aprire la cartella del progetto."
    export PATH="$PATH:/usr/local/bin:/opt/homebrew/bin:$HOME/.docker/bin:/Applications/Docker.app/Contents/Resources/bin:$HOME/Applications/Docker.app/Contents/Resources/bin"
    for program in git docker openssl; do
        command -v "$program" >/dev/null 2>&1 || fail "$program non disponibile. Esegui set-up-macos.command o completa l installazione."
    done
    git --version >/dev/null 2>&1 && docker --version >/dev/null 2>&1 ||
        fail "Git o Docker non utilizzabile. Esegui set-up-macos.command."
    docker compose version || fail "Docker Compose non disponibile. Completa o correggi l installazione di Docker Desktop."
    [ -e .git ] || fail "Questa cartella non e un clone Git. Esegui set-up-macos.command."
    origin=$(git remote get-url origin) || fail "Il repository non ha un remote origin."
    printf '%s\n' "$origin" | grep -Eiq '^(https://github\.com/|git@github\.com:|ssh://git@github\.com/)aChiampo/gruppo-03-Cinder-Rift(\.git)?/?$' ||
        fail "Il repository non corrisponde a Cinder Rift. Esegui set-up-macos.command."
    # Setup may add executable bits to launchers tracked without those bits.
    # Ignore mode-only changes for these commands, without changing Git config.
    changes=$(git -c core.fileMode=false status --porcelain) || fail "Impossibile verificare lo stato Git."
    [ -z "$changes" ] || fail "Sono presenti modifiche locali. Salvale con Git o spostale prima di aggiornare. Nessuna modifica e stata eliminata."
    printf '%s\n' "Aggiornamento del progetto da dev..."
    git -c core.fileMode=false switch dev && git -c core.fileMode=false pull --ff-only origin dev ||
        fail "Aggiornamento non riuscito. Controlla rete, accesso GitHub ed eventuali commit divergenti."
    chmod +x ./start-up-macos.command || fail "Impossibile rendere eseguibile lo start-up aggiornato."
    [ -f compose.yaml ] || fail "compose.yaml manca nel progetto su dev."

    if ! docker info >/dev/null 2>&1; then
        printf '%s\n' "Avvio di Docker Desktop..."
        if [ -d /Applications/Docker.app ]; then
            open /Applications/Docker.app || fail "Impossibile aprire Docker Desktop."
        elif [ -d "$HOME/Applications/Docker.app" ]; then
            open "$HOME/Applications/Docker.app" || fail "Impossibile aprire Docker Desktop."
        else
            fail "Docker Desktop non trovato. Aprilo manualmente o esegui set-up-macos.command."
        fi
        attempts=0
        until docker info >/dev/null 2>&1; do
            [ "$attempts" -lt 60 ] || fail "Docker non pronto entro 120 secondi. Completa la configurazione in Docker Desktop e riprova."
            sleep 2
            attempts=$((attempts + 1))
        done
    fi

    ensure_jwt_secret
    # Shell variables override .env in Compose; use the local key consistently.
    unset APP_SECURITY_JWT_SECRET
    docker compose --file compose.yaml config --quiet || fail "Configurazione Compose non valida. Controlla .env e compose.yaml."
    printf '%s\n' "Preparazione e avvio dell applicazione..."
    docker compose --file compose.yaml up --build --wait || fail "Avvio non riuscito. Controlla i messaggi Compose."
    url="http://localhost:8082"
    printf 'Applicazione pronta: %s\n' "$url"
    open "$url" || printf 'Apri manualmente %s nel browser.\n' "$url" >&2
}

main
