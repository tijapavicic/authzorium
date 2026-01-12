# Running logging tests

This project contains logging-focused tests that verify the request-flow logging pipeline (the `FLOW` marker goes to `request-flow.log`) and the application logging (e.g., `application.log`). These tests write into a temporary directory during test runs so they don't pollute your working tree.

Quick guide

- Run the two logging tests locally (fast):

```bash
mvn -Dtest=logging.org.authzorium.RequestFlowLoggingTest,logging.org.authzorium.RequestFlowIntegrationTest -Djacoco.skip=true test
```

- Run all tests (skip JaCoCo locally to avoid instrumentation issues on newer JDKs):

```bash
mvn -Djacoco.skip=true test
```

- Run tests with JaCoCo enabled (recommended only on Java 17 in CI):

```bash
# Ensure you use Java 17 locally (e.g. via SDKMAN/Homebrew/IDE) before running this
mvn -Djacoco.skip=false test verify
```

Notes / troubleshooting

- JaCoCo instrumentation can fail on newer JDKs (class file version mismatch). The project defaults to having JaCoCo skipped to avoid local developer pain. The CI workflow included in `.github/workflows/ci.yml` explicitly uses Java 17 so coverage runs reliably.

- If you want to run the logging integration test in a way that ensures `LOG_HOME` is set before Spring Boot logging initializes, run the test via Maven (above) — the test suite uses a TestExecutionListener to set the temporary `LOG_HOME` before the context starts.

- Test logs and coverage artifacts
  - Temporary logs are created under the OS temp directory and cleaned up after the tests. If you need to inspect them, run the tests with a custom `LOG_HOME`:

```bash
# Create a folder for logs
export LOG_HOME=$(pwd)/build/test-logs
mkdir -p "$LOG_HOME"
# Run the integration test; it will write logs into your chosen folder
mvn -Dtest=logging.org.authzorium.RequestFlowIntegrationTest -Djacoco.skip=true test
ls -la "$LOG_HOME"
```

- CI (GitHub Actions) will upload the JaCoCo report and surefire reports as artifacts; see `.github/workflows/ci.yml` for details.

If you want me to change the CI to publish coverage to Codecov or Coveralls, I can add a secure step that requires a token (set as a repository secret).
