#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"

info "[infra] testplanit hook: bootstrap TestPlanIt environment"
./tools/environment/scripts/infra/modules/testplanit/bootstrap_testplanit.sh
