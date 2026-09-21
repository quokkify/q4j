from __future__ import annotations

import os
import shutil
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools/scripts/ci/gradle-retry.sh"


class GradleRetryTests(unittest.TestCase):
    def run_case(self, mode: str, *, max_attempts: str = "3") -> tuple[subprocess.CompletedProcess[str], int]:
        with tempfile.TemporaryDirectory(prefix="q4j-gradle-retry-") as temporary:
            root = Path(temporary)
            fake_gradle = root / "gradlew"
            count = root / "count"
            fake_gradle.write_text(
                "#!/usr/bin/env bash\n"
                "n=0; [[ -f \"$COUNT_FILE\" ]] && n=$(<\"$COUNT_FILE\")\n"
                "n=$((n + 1)); printf '%s' \"$n\" > \"$COUNT_FILE\"\n"
                "case \"$MODE\" in\n"
                "  ordinary) echo 'compilation failed'; exit 9;;\n"
                "  not-found) echo 'Could not resolve org.example:missing:1.0'; exit 7;;\n"
                "  rate-limit) [[ $n -lt 2 ]] && { echo 'Could not GET repository, status code 429'; exit 8; }; echo success; exit 0;;\n"
                "  forbidden) [[ $n -lt 2 ]] && { echo 'Could not GET https://repo.maven.apache.org/maven2/example.pom'; echo 'Received status code 403 from server: Forbidden'; exit 13; }; echo success; exit 0;;\n"
                "  persistent-forbidden) echo 'Could not GET https://repo.maven.apache.org/maven2/example.pom'; echo 'Received status code 403 from server: Forbidden'; exit 13;;\n"
                "  server-error) echo 'Could not GET https://repo.maven.apache.org/maven2/example.pom'; echo 'Received status code 500 from server'; exit 14;;\n"
                "esac\n"
            )
            fake_gradle.chmod(fake_gradle.stat().st_mode | stat.S_IXUSR)
            env = {
                **os.environ,
                "COUNT_FILE": str(count),
                "GRADLE_RETRY_INITIAL_DELAY_SECONDS": "1",
                "GRADLE_RETRY_MAX_ATTEMPTS": max_attempts,
                "MODE": mode,
            }
            result = subprocess.run(
                ["bash", str(SCRIPT), "check"],
                cwd=root,
                env=env,
                text=True,
                capture_output=True,
                check=False,
            )
            return result, int(count.read_text())

    def test_transient_403_retries_and_recovers(self) -> None:
        result, calls = self.run_case("forbidden", max_attempts="2")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(calls, 2)

    def test_persistent_403_fails_after_limit(self) -> None:
        result, calls = self.run_case("persistent-forbidden", max_attempts="2")
        self.assertEqual(result.returncode, 13)
        self.assertEqual(calls, 2)

    def test_existing_429_retry_is_preserved(self) -> None:
        result, calls = self.run_case("rate-limit", max_attempts="2")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(calls, 2)

    def test_500_and_ordinary_failures_are_not_retried(self) -> None:
        for mode, status in (("server-error", 14), ("ordinary", 9), ("not-found", 7)):
            with self.subTest(mode=mode):
                result, calls = self.run_case(mode)
                self.assertEqual(result.returncode, status)
                self.assertEqual(calls, 1)


if __name__ == "__main__":
    unittest.main()
