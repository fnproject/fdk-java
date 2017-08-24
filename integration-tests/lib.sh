# Bash clean-up junk --
typeset -a _deferrals
_on_exit() {
  set +x
  local idx
  for (( idx=${#_deferrals[@]}-1 ; idx>=0 ; idx-- )) ; do
    set -x
    ${_deferrals[idx]}
    set +x
  done
}
defer() {
  _deferrals+=("$*")
}
trap _on_exit EXIT TERM INT
# -- Bash clean-up junk

wait_for_http() {
  (
  set +ex
  local i
  for i in {1..15}; do
    curl --connect-timeout 5 --max-time 2 "$1" && exit
    sleep 1
  done
  exit 1
  )
}

# prefix each line, whilst evaluating --
prefix_lines() {
  local prefix="$1"
  local file="$2"
  eval awk \''{print "'"$prefix"' " $0}'\' '<<-EOF'$'\n'"$(< "$file")"$'\n'EOF
}
# -- prefix each line, whilst evaluating


line() {
  echo --------------------------------------------------------------------------------
}

error() {
    echo "$1" 1>&2
    exit "${2:-1}"
}

export SCRIPT_DIR="$( cd "$(dirname "$0")" && command pwd )"
