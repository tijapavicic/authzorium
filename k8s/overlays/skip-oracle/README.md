skip-oracle overlay (dev / H2)

This overlay deploys only the `authzorium` application with an in-memory H2 database and omits the heavy Oracle resources. It's intended for fast local development (Kind/Minikube).

What it changes
- Uses the base `app-deployment.yaml` and `app-service.yaml` from `k8s/`.
- Patches the `Deployment` to set environment:
  - `SPRING_PROFILES_ACTIVE=localh2`
  - `SPRING_DATASOURCE_URL=jdbc:h2:mem:loketdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
  - `SPRING_DATASOURCE_USERNAME=sa` (default H2)
  - `SPRING_DATASOURCE_PASSWORD=` (empty)
- Adds small resource requests and limits for the container to make local cluster scheduling easier:
  - requests: cpu=100m, memory=128Mi
  - limits: cpu=300m, memory=256Mi

How to apply

If you already have a local cluster (kind, minikube):

```bash
# apply the skip-oracle overlay
kubectl apply -k k8s/overlays/skip-oracle
```

Or use the helper script in this repo (creates/uses kind, builds image, loads image, applies overlay):

```bash
# from repo root
./scripts/kind-setup-and-deploy.sh --skip-oracle
```

Expected logs & startup sequence
- The app pod will start quickly (no DB init wait). Look for logs similar to:

```
2026-01-04 12:34:56.789  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8082 (http) with context path ''
2026-01-04 12:34:57.123  INFO 1 --- [           main] org.loket.authN.DataInitializer         : No users found; creating demo user...
2026-01-04 12:34:57.456  INFO 1 --- [           main] o.s.b.a.ApplicationAvailability        : Application is ready to serve requests
```

- If the pod shows CrashLoopBackOff or fails liveness/readiness:
  - Run: `kubectl describe pod -n loket <pod-name>` and `kubectl logs -n loket <pod-name>`.
  - Common reasons: actuator not enabled (probe path), missing environment variable, or image failed to start.

Tips
- To run quick iterative testing, run `kubectl port-forward svc/authzorium 8082:8082 -n loket` and open http://localhost:8082/hello
- If you prefer not to build the image locally, push your image to a registry and call the script with `--image yourrepo/authzorium:tag --no-build --skip-load`.

Contact
- If you want me to tighten the probe to match your app configuration (different actuator path or port), tell me the actuator endpoint and I will update the overlay accordingly.

