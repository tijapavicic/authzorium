Kubernetes manifests for authzorium

This folder contains a minimal set of Kubernetes manifests that reproduce the docker-compose setup:
- An Oracle XE container (gvenzl/oracle-xe:18.4.0) with a PVC for persistent data.
- The authzorium Deployment and Service.

Prerequisites
- kubectl configured to a cluster (e.g., minikube, kind, k3s, or a cloud cluster).
- (Optional) kustomize (kubectl supports kustomize via `kubectl apply -k`).
- Docker (or a registry) to build/push the `authzorium:latest` image.

Steps
1) Build and publish the application image (example uses local Docker, for a cluster like minikube you may need to load the image into the cluster):

```bash
# build image locally
docker build -t authzorium:latest .

# For minikube, you can load it into the cluster
# minikube image load authzorium:latest
```

2) Apply k8s manifests (kustomize)

```bash
kubectl apply -k k8s
```

3) Check resources

```bash
kubectl get ns loket
kubectl get pods -n loket
kubectl get svc -n loket
```

Notes and caveats
- The Oracle image is relatively large and requires sufficient memory and disk. For local testing, consider using a lighter DB (H2) or running Oracle in a dedicated VM.
- The provided PVC uses an empty storageClassName by default; update it to match your cluster's storage class or leave blank for default.
- Secrets: the `oracle-secret.yaml` uses a static string (loket_pwd) for convenience; for production replace with a generated secret or use External Secret management.
- The app's `SPRING_DATASOURCE_URL` points to `jdbc:oracle:thin:@oracle-db:1521:XE` (service name `oracle-db` in the same namespace).

Optional improvements
- Add a HorizontalPodAutoscaler.
- Configure readiness/liveness probes more precisely (SQL-based probe for Oracle is complex; current probe uses HTTP or exec depending on image availability).
- Use Helm for templating and env substitution (e.g., image repository, tag, replica count).

