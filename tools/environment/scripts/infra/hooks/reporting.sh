#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"

info "[infra] reporting hook: bootstrap report portal environment"
./tools/environment/scripts/infra/modules/reportportal/bootstrap_reportportal.sh
