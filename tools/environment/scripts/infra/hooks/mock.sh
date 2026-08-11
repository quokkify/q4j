#!/bin/bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/../compose_utils.sh"
init_compose_files

info "[infra] mock hook: upload expectations"
./tools/environment/scripts/infra/modules/mock/run_mock_server.sh
./tools/environment/scripts/infra/modules/mock/upload_expectations.sh
