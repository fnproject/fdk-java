#!/usr/bin/env bash

# Set up a local test environment in order to run integration tests,
# then execute them.

source "$(dirname "$0")/lib.sh"

set -ex

# ----------------------------------------------------------------------
# The following variables may be set to parameterise the operation of this script
# ----------------------------------------------------------------------

export SUFFIX=$(git rev-parse HEAD)
export FN_TOKEN=${FN_TOKEN:-$(cat ~/.fn-token)}


# ----------------------------------------------------------------------
# The following variables should be set in the integration environment
# ----------------------------------------------------------------------

export FN_API_URL=$(cat ~/.fn-api-url)
export COMPLETER_BASE_URL=$(cat ~/.fn-flow-base-url)


# We need to push our images into the test environment, so let's ensure that our tunnel is set up
systemctl --user restart ssh-tunnels

# Ensure we have the hooks we want in place
export PRE_BUILD_HOOK="$SCRIPT_DIR/pre-build-hook.sh"
export POST_CONFIGURE_HOOK="$SCRIPT_DIR/post-configure-hook.sh"

export HTTP_PROXY="$http_proxy"
export HTTPS_PROXY="$https_proxy"
export NO_PROXY="$no_proxy"

set +x

"$SCRIPT_DIR/run-all-tests.sh" "$@"
