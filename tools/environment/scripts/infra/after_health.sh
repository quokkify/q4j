#!/bin/bash
set -euo pipefail

# shellcheck source=tools/environment/scripts/infra/compose_utils.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/compose_utils.sh"

for profile in "$@"; do
  profile="$(echo "$profile" | xargs)"
  case "$profile" in
    mock)
      ./tools/environment/scripts/infra/hooks/mock.sh
      ;;
    web)
      ./tools/environment/scripts/infra/hooks/web.sh
      ;;
    storage)
      ./tools/environment/scripts/infra/hooks/storage.sh
      ;;
    redis)
      ./tools/environment/scripts/infra/hooks/redis.sh
      ;;
    reporting)
      ./tools/environment/scripts/infra/hooks/reporting.sh
      ;;
    messaging)
      ./tools/environment/scripts/infra/hooks/messaging.sh
      ;;
    rabbitmq)
      ./tools/environment/scripts/infra/hooks/rabbitmq.sh
      ;;
    "")
      ;;
    *)
      warning "[infra] unknown profile hook skipped: ${profile}"
      ;;
  esac
done
