#!/usr/bin/env bash
set -ex

source "$(dirname $0)/lib.sh"

check_command_exists jq
check_command_exists git

: "${FN_CLI_BINARY:=fn_linux}"
: "${INSTALL_DIR:=/usr/local/bin}"

FN_BINARY_LOCATION="$(\
    curl -s https://api.github.com/repos/fnproject/cli/releases/latest \
      | jq -r ".assets[] \
      | select(.name | test(\"${FN_CLI_BINARY}\")) \
      | .browser_download_url"\
)"

echo "Download fn from $FN_BINARY_LOCATION"
# --location = follow redirects
curl -f --location "$FN_BINARY_LOCATION" --output fn
chmod +x fn
./fn || true # show fn version
sudo cp fn "${INSTALL_DIR}/fn"
