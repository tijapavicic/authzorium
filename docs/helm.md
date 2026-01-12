# Helm chart for authzorium

This folder contains a small Helm chart you can use to deploy `authzorium` to a Kubernetes cluster (kind, minikube, EKS, GKE, etc.). It is intentionally minimal so you can adapt it for your environment.

Chart path: `k8s/helm/loket-authn`

Quick steps

1. Build or make the container image available to your cluster
   - For kind/local clusters use `kind load docker-image` or push to a registry.

2. Install the chart with defaults:

```bash
helm upgrade --install loket-authn k8s/helm/loket-authn -n loket --create-namespace
```

3. If you want to enable the Oracle connection (production-like) set `--set oracle.enabled=true` and provide credentials:

```bash
helm upgrade --install loket-authn k8s/helm/loket-authn -n loket \
  --set oracle.enabled=true \
  --set oracle.host=oracle.example.com \
  --set oracle.port=1521 \
  --set oracle.username=system \
  --set oracle.password=secret
```

4. To run with the H2 (fast dev) profile (default values.yaml sets `spring_profiles_active: localh2`), nothing else is needed. If you want to override the JWT secret:

```bash
helm upgrade --install loket-authn k8s/helm/loket-authn -n loket --create-namespace \
  --set env.security_jwt_secret=mysupersecret
```

---
check Helm version
```bash
helm version --short || true
```
read Helm from Docker and lint the chart
```bash

docker run --rm -v "$(pwd)":/work -w /work alpine/helm:3.11.0 helm lint k8s/helm/loket-authn
docker run --rm -v "$(pwd)":/work -w /work lachlanevenson/k8s-helm:3.11.0 helm lint k8s/helm/loket-authn
docker run --rm -v "$(pwd)":/work -w /work alpine/helm:3.10.0 helm version --short || true
docker run --rm -v "$(pwd)":/work -w /work alpine/helm:3.10.0 /bin/sh -c 'which helm && helm version --short'
docker run --rm --entrypoint sh -v "$(pwd)":/work -w /work alpine/helm:3.10.0 -c 'which helm && helm version --short'
docker run --rm --entrypoint sh -v "$(pwd)":/work -w /work alpine/helm:3.10.0 -c 'helm lint k8s/helm/loket-authn'
docker run --rm --entrypoint sh -v "$(pwd)":/work -w /work alpine/helm:3.10.0 -c 'helm template loket k8s/helm/loket-authn --namespace loket > rendered-loket-authn.yaml && sed -n "1,400p" rendered-loket-authn.yaml'


docker run --rm --entrypoint sh -v "$(pwd)":/work -w /work alpine/helm:3.10.0 -c 'helm lint k8s/helm/loket-authn && helm install --dry-run --debug loket k8s/helm/loket-authn -n loket'

docker run --rm --entrypoint sh -v "$(pwd)":/work -w /work alpine/helm:3.10.0 -c 'ls -la k8s/helm/loket-authn/templates && echo "---" && sed -n "1,200p" k8s/helm/loket-authn/templates/NOTES.txt'

docker run --rm --entrypoint sh -v "$(pwd)":/work -w /work alpine/helm:3.10.0 -c 'helm lint k8s/helm/loket-authn && helm template loket k8s/helm/loket-authn --namespace loket > rendered-loket-authn.yaml && sed -n "1,300p" rendered-loket-authn.yaml'

docker run --rm --entrypoint sh -v "$(pwd)":/work -w /work alpine/helm:3.10.0 -c 'helm lint k8s/helm/loket-authn && helm template loket k8s/helm/loket-authn --namespace loket > rendered-loket-authn.yaml && sed -n "1,400p" rendered-loket-authn.yaml'


```
Local validation: `helm lint` and `helm template`

I could not run `helm` in this environment because `helm` is not installed here (you saw `command not found: helm`). Below are exact, copy-pastable steps you can run on your machine to lint the chart and render (template) the final Kubernetes YAML that Helm would install.

If you don't have Helm installed, install it first:

- macOS (Homebrew):
```bash
brew install helm
```

- Linux (script):
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

- Or see the official install docs: https://helm.sh/docs/intro/install/

Once `helm` is available, run these commands from the repository root (where `k8s/helm/loket-authn` is located):

- Lint the chart (static checks):
```bash
helm lint k8s/helm/loket-authn
```

- Render the chart to plain YAML (dry-run, no cluster changes):
```bash
# Render using a release name of your choice (e.g., 'loket') and namespace 'loket'
helm template loket k8s/helm/loket-authn --namespace loket > rendered-loket-authn.yaml

# Inspect the first resources
sed -n '1,200p' rendered-loket-authn.yaml
```

- Render with custom values (example: set JWT secret and image tag):
```bash
helm template loket k8s/helm/loket-authn --namespace loket \
  --set env.security_jwt_secret=mysecret \
  --set image.repository=yourrepo/authzorium \
  --set image.tag=1.2.3 > rendered-custom.yaml
```

- Simulate an install without applying (also shows hooks):
```bash
helm install --dry-run --debug loket k8s/helm/loket-authn -n loket
```

Optional: run Helm from Docker (if you don't want to install Helm locally)

If you have Docker but not Helm locally, you can run Helm in a container. Example (adjust image tag if needed):

```bash
# Mount repo into container and run helm lint
docker run --rm -v "$(pwd)":/work -w /work alpine/helm:3.11.0 helm lint k8s/helm/loket-authn

docker run --rm -v "$(pwd)":/work -w /work alpine/helm:3.11.0 helm template loket k8s/helm/loket-authn --namespace loket > rendered-loket-authn.yaml
```

Note: the exact container image name `alpine/helm:3.11.0` may vary; if that image does not exist in your environment, install Helm natively (Homebrew or the script shown above) — that is the most reliable approach.


Sample: what `helm template` will produce (short excerpt)

When you run `helm template ...` you should see YAML with the Deployment, Service and (optional) Ingress/Secret resources. Below is a small *representative* excerpt of the expected output (this is a compact, hand-crafted example based on the chart templates and default values):

```yaml
# Deployment (excerpt)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: loket-authn-loket
  labels:
    app.kubernetes.io/name: loket-authn
    app.kubernetes.io/instance: loket
spec:
  replicas: 1
  template:
    metadata:
      labels:
        app.kubernetes.io/name: loket-authn
        app.kubernetes.io/instance: loket
    spec:
      containers:
      - name: loket-authn
        image: "authzorium:latest"
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "localh2"
        - name: SECURITY_JWT_SECRET
          value: "changeit-changeit-changeit-changeit"

---
# Service (excerpt)
apiVersion: v1
kind: Service
metadata:
  name: loket-authn-loket
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: http
    protocol: TCP
  selector:
    app.kubernetes.io/name: loket-authn
    app.kubernetes.io/instance: loket
```

That output will be longer and include the Secret resource for the JWT secret and, if enabled, an Ingress resource.


Detailed developer instructions (added checklist)

- Install Helm (Homebrew / script)
- Lint the chart: `helm lint k8s/helm/loket-authn`
- Render the chart and inspect YAML: `helm template loket k8s/helm/loket-authn --namespace loket > rendered.yaml`
- Validate the rendered YAML via kubectl dry-run if desired:
```bash
kubectl apply --dry-run=client -f rendered.yaml
```
- Deploy to a cluster:
```bash
helm upgrade --install loket-authn k8s/helm/loket-authn -n loket --create-namespace
```
- Uninstall:
```bash
helm uninstall loket-authn -n loket
```


Where I couldn't run commands here

- I attempted to run `helm` in the environment but it was not installed (zsh: command not found: helm). I therefore could not execute `helm lint` and `helm template` in this environment to capture the *real* rendered YAML. The instructions above are the exact commands you should run locally; they will produce the true rendered YAML given your environment (Helm version and chart values).

If you want, I can:
- (A) Try running Helm inside Docker on your behalf (I can attempt `docker run` with a Helm image), and if that image exists in your environment I will run `helm lint` and `helm template` and return the actual rendered YAML; or
- (B) You can run the exact commands above locally and paste the generated `rendered-loket-authn.yaml` here and I will review it for correctness and suggest any fixes.

Tell me whether you want me to attempt the Docker-based Helm run (A) or you prefer to run Helm locally and share the rendered YAML (B). If you choose (A) I'll try running the Docker commands now.
