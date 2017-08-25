check_command_exists() {
    local command="$1"; shift
    if ! command -v "$command" 2>&1 > /dev/null; then
        echo "'$command' is not installed, please install it" >&2
    fi
}
