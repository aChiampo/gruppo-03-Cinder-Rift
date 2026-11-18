#!/bin/sh

fail() {
    printf '%s\n' "$1" >&2
    exit 1
}

save_installer() {
    mkdir -p "$HOME/Downloads" || fail "Impossibile creare la cartella Downloads."
    installer="$HOME/Downloads/Docker.dmg"
    curl --fail --location --retry 3 "$1" --output "$installer.partial" || return 1
    mv -f "$installer.partial" "$installer" || return 1
    printf 'Installer scaricato: %s\n' "$installer"
}

assert_checkout() {
    [ -e "$1/.git" ] || fail "La destinazione esiste ma non e un clone Git: $1. Spostala o rinominala e riprova."
    root=$(git -C "$1" rev-parse --show-toplevel) || fail "Checkout Git non valido: $1"
    target=$(cd "$1" && pwd -P) || fail "Impossibile aprire $1."
    [ "$root" = "$target" ] || fail "La destinazione non e la radice del repository: $1"
    origin=$(git -C "$1" remote get-url origin) || fail "Il repository non ha un remote origin: $1"
    printf '%s\n' "$origin" | grep -Eiq '^(https://github\.com/|git@github\.com:|ssh://git@github\.com/)aChiampo/gruppo-03-Cinder-Rift(\.git)?/?$' ||
        fail "La cartella contiene un altro repository: $1"
}

# Load the complete setup before operating on a possibly existing checkout.
main() {
    [ "$(uname -s)" = Darwin ] || fail "Questo script richiede macOS. Su Windows usa set-up-windows.bat."
    project_dir="$HOME/Desktop/gruppo-03-Cinder-Rift"
    repository_url='https://github.com/aChiampo/gruppo-03-Cinder-Rift.git'
    export PATH="$PATH:/usr/local/bin:/opt/homebrew/bin:$HOME/.docker/bin:/Applications/Docker.app/Contents/Resources/bin:$HOME/Applications/Docker.app/Contents/Resources/bin"
    needs_installation=false
    installation_failed=false

    # /usr/bin/git is an Apple stub until Command Line Tools are installed.
    git_available=false
    git_path=$(command -v git 2>/dev/null || :)
    if [ "$git_path" != /usr/bin/git ] || xcode-select -p >/dev/null 2>&1; then
        if [ -n "$git_path" ] && git --version >/dev/null 2>&1; then
            git_available=true
        fi
    fi
    if [ "$git_available" = false ]; then
        needs_installation=true
        printf '%s\n' "Git non disponibile. Avvio dell'installazione Apple Command Line Tools..."
        xcode-select --install || printf '%s\n' "Se la richiesta e gia aperta, completala. Altrimenti installa Command Line Tools da Apple e riprova."
    fi

    if [ ! -d /Applications/Docker.app ] && [ ! -d "$HOME/Applications/Docker.app" ]; then
        needs_installation=true
        case "$(uname -m)" in
            arm64) docker_arch=arm64 ;;
            x86_64) docker_arch=amd64 ;;
            *) fail "Architettura Mac non supportata per Docker Desktop." ;;
        esac
        printf '%s\n' "Docker Desktop non installato. Scaricamento..."
        save_installer "https://desktop.docker.com/mac/main/$docker_arch/Docker.dmg" || installation_failed=true
        printf '%s\n' "Apri ~/Downloads/Docker.dmg, trascina Docker in Applicazioni e completa il primo avvio."
    fi
    [ "$installation_failed" = false ] || fail "Download di Docker Desktop non riuscito. Controlla la rete e rilancia il set-up."
    if [ "$needs_installation" = true ]; then
        printf '%s\n' "Completa le installazioni indicate, poi rilancia questo set-up per clonare il progetto."
        return
    fi

    script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || fail "Impossibile trovare la cartella dello script."
    if [ -e "$script_dir/.git" ]; then
        assert_checkout "$script_dir"
        project_dir=$script_dir
    elif [ -e "$project_dir" ] || [ -L "$project_dir" ]; then
        assert_checkout "$project_dir"
    else
        mkdir -p "$HOME/Desktop" || fail "Impossibile creare la cartella Desktop."
        printf 'Clonazione di dev in %s...\n' "$project_dir"
        git clone --branch dev "$repository_url" "$project_dir" ||
            fail "Clonazione non riuscita. Controlla rete, accesso GitHub e destinazione; nessuna cartella viene eliminata automaticamente."
    fi
    [ -f "$project_dir/start-up-macos.command" ] ||
        fail "Il clone esiste ma start-up-macos.command manca. Verifica che i nuovi script siano disponibili su dev."
    chmod +x "$project_dir/start-up-macos.command" || fail "Impossibile rendere eseguibile lo start-up."
    printf 'Preparazione completata. Per avviare l applicazione, apri: %s/start-up-macos.command\n' "$project_dir"
}

main
