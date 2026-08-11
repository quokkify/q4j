#!/bin/bash
set -euo pipefail

# shellcheck source=tools/environment/scripts/infra/compose_utils.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/compose_utils.sh"

if [[ "${CI:-}" == "true" ]]; then
  # Source in the caller shell so dynamically allocated ports remain available
  # to Docker Compose interpolation during startup.
  # shellcheck source=tools/environment/scripts/infra/hooks/pre_up_ci.sh
  source ./tools/environment/scripts/infra/hooks/pre_up_ci.sh "$@"
fi
