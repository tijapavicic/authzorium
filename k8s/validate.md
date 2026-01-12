validating scripts
```bash


cd /authzorium && bash -n scripts/k8s-cheatsheet.sh && echo 'syntax OK' && ./scripts/k8s-cheatsheet.sh --wait-timeout 120s --smoke-timeout 5 --smoke-retry 3 --smoke-retry-delay 2 --smoke-retry-backoff 2.0 smoke-test && ./scripts/k8s-cheatsheet.sh --smoke-jsonpath '$.data.id' smoke-test

cd /authzorium && bash -n scripts/k8s-cheatsheet.sh && echo 'syntax OK' && ./scripts/k8s-cheatsheet.sh smoke-test && ./scripts/k8s-cheatsheet.sh --smoke-jsonpath '$.data.id' smoke-test
```

```bash

cd /authzorium && ./scripts/kind-setup-and-deploy.sh --skip-oracle

```

```bash
 cd /authzorium && ./scripts/kind-setup-and-deploy.sh --skip-oracle

```

```bash
 cd /authzorium && kubectl apply -f k8s/base/namespace.yaml && kubectl apply -k k8s/overlays/skip-oracle && kubectl get pods -n loket -o wide && kubectl get svc -n loket


```