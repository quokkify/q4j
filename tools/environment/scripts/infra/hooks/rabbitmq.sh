#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"

info "[infra] rabbitmq hook: bootstrap rabbitmq environment"
./tools/environment/scripts/infra/modules/rabbitmq/bootstrap_rabbitmq.sh
