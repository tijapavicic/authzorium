# Request tracing and flow logs

This document explains how to correlate a single HTTP request across the application using the provided request-flow logs and `X-Request-ID` header.

Quick summary

- The app sets (or echoes) the `X-Request-ID` response header for every request. You can supply your own `X-Request-ID` header to correlate logs for a request.
- Flow-specific traces are written to a dedicated file `logs/request-flow.log` (rolling, daily, size-limited) and contain only entries marked with the `FLOW` marker.
- General logs still go to the console and `logs/application.log`.

Where to find logs

- Default logs location: `./logs` (relative to the process working directory).
- You can change it by setting the environment variable `LOG_HOME` before starting the app. Example:

```bash
export LOG_HOME=/var/log/loket-authn
java -jar target/authzorium.jar
```

Key concepts

- X-Request-ID
  - If the incoming request contains `X-Request-ID`, the service echoes it back in the response header so callers can correlate client/server logs.
  - If the header is missing, the service generates a UUID and sets it as the value of `X-Request-ID`.

- MDC (Mapped Diagnostic Context)
  - `MdcFilter` sets two MDC keys for every request: `requestId` and `remoteIp`.
  - Log patterns include these MDC keys so record lines show `[requestId,remoteIp]`.

- FLOW marker & `request-flow.log`
  - Flow-level messages (start / end request, request/response bodies for error flows, validation events, etc.) are logged with the SLF4J marker named `FLOW`.
  - The logback config writes only `FLOW`-marked events into `logs/request-flow.log`, keeping that file focused and easy to grep for a single request.

How to correlate a request

1. Send a request and either include your own request id or read the response header:

```bash
curl -i -H "X-Request-ID: my-trace-123" http://localhost:8080/admin/hello
# or without header - read response X-Request-ID
curl -i http://localhost:8080/hello
```

2. Search the flow log for that request id:

```bash
grep my-trace-123 logs/request-flow.log
```

You should see a short trace of the request like:

```
2026-01-10 12:00:00.000 [http-nio-8081-exec-1] DEBUG ... - [my-trace-123,127.0.0.1] Start request: [my-trace-123] GET /admin/hello from 127.0.0.1
2026-01-10 12:00:00.010 [http-nio-8081-exec-1] DEBUG ... - [my-trace-123,127.0.0.1] Unauthorized response [my-trace-123] -> {"timestamp":...,"status":401,...}
2026-01-10 12:00:00.011 [http-nio-8081-exec-1] DEBUG ... - [my-trace-123,127.0.0.1] End request: [my-trace-123] GET /admin/hello -> status=401 (from 127.0.0.1)
```

Enabling and controlling verbosity

- Flow traces are produced using `logger.debug(FLOW, ...)` and are captured by the `REQUEST_FLOW` appender regardless of the root log level (because filtering is done by marker). However, whether a debug invocation actually executes depends on the logger's level. If you don't see flow entries, two things to check:
  - Confirm flow code logs use the `FLOW` marker (they do by default).
  - Make sure your logger's effective level allows debug messages for the classes you care about. To enable debug for flow logs globally you can set the root logger to DEBUG, or set package-specific levels in `application.properties` or `logback-spring.xml`.

Example (application.properties):

```properties
logging.level.org.loket.authN=DEBUG
```

Or change the root level in `logback-spring.xml` (not usually recommended for production).

MDC and asynchronous propagation

- The `MdcFilter` sets MDC keys for synchronous request handling. If your request processing submits work to background threads or ExecutorServices, MDC will not be propagated automatically. Use a decorator or library to copy MDC into worker threads if needed.

Troubleshooting

- `request-flow.log` is empty
  - Ensure the application process can write to `LOG_HOME` (default `./logs`).
  - Ensure your flow-level logger is enabled at DEBUG for the package (or raise only the specific classes to DEBUG) so the debug calls are emitted.

- No `X-Request-ID` header in responses
  - Verify `MdcFilter` is registered and not excluded in test profiles. In tests we sometimes disable filters; run the app normally or enable filters in tests when required.

- Flow file is too large / rotate differently
  - Edit `src/main/resources/logback-spring.xml` and adjust `maxFileSize` and `maxHistory` to match your retention and storage policy.

Extras and recommendations

- When troubleshooting production issues, ask clients to send a unique `X-Request-ID` so you can immediately grep `logs/request-flow.log` and attach correlated events to an incident report.
- For long-lived investigations, forward `request-flow.log` to your central log system (ELK/EFK/Datadog) and index by `requestId` to enable fast cross-system correlation.

That's it — if you want I can also commit a small `scripts/collect-flow.sh` helper that accepts a request id and tails the most recent flow log entries for it.

