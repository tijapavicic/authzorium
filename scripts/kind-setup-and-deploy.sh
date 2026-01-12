#!/usr/bin/env bash
set -euo pipefail

# kind-setup-and-deploy.sh
# Automates: install kind (via Homebrew on macOS), create a kind cluster, build the Docker image,
# load the image into kind, and apply k8s manifests (kustomize)

# Usage:
#   ./scripts/kind-setup-and-deploy.sh [--cluster-name name] [--image NAME[:TAG]] [--kustomize-path PATH]
# Examples:
#   ./scripts/kind-setup-and-deploy.sh
#   ./scripts/kind-setup-and-deploy.sh --cluster-name loket-cluster --image myrepo/authzorium:dev

CLUSTER_NAME="loket-cluster"
IMAGE_NAME="authzorium:latest"
KUSTOMIZE_PATH="k8s"
DO_BUILD=1
SKIP_LOAD=0
KIND_BINARY="kind"
SKIP_ORACLE=0
WAIT_FOR_READY=0
TINY_DEV=0

print_help() {
  cat <<EOF
Usage: $0 [options]

Options:
  --cluster-name NAME   Name for the kind cluster (default: ${CLUSTER_NAME})
  --image NAME[:TAG]    Docker image name to build/use (default: ${IMAGE_NAME})
  --kustomize-path PATH Path to kustomize dir to apply (default: ${KUSTOMIZE_PATH})
  --no-build            Skip building the Docker image
  --skip-load           Skip loading image into kind (assumes image available to cluster)
  --skip-oracle         Deploy only the application using an H2 profile (no Oracle resources)
  --tiny-dev            Use the tiny-dev overlay (minimal resources, remove probes)
  --wait                Wait for the loket deployment to rollout and tail logs until ready
  -h, --help            Show this help
EOF
}

# Simple logger
log() { echo "[kind-setup] $*"; }
err() { echo "[kind-setup] ERROR: $*" >&2; }

# Check a command exists
has_cmd() { command -v "$1" >/dev/null 2>&1; }

# Install kind via Homebrew (macOS) if available
install_kind_brew() {
  if has_cmd brew; then
    log "Installing kind via Homebrew..."
    brew install kind
  else
    err "Homebrew not found. Please install Homebrew (https://brew.sh/) or install 'kind' manually."
    exit 2
  fi
}

create_kind_cluster() {
  if ! has_cmd "$KIND_BINARY"; then
    log "'kind' not found. Attempting to install..."
    install_kind_brew
  fi

  # check Docker
  if ! has_cmd docker; then
    err "Docker CLI not found. Please install and start Docker first."
    exit 2
  fi

  # If cluster exists, skip creation
  if "$KIND_BINARY" get clusters | grep -q "^${CLUSTER_NAME}$"; then
    log "Kind cluster '${CLUSTER_NAME}' already exists. Skipping creation."
  else
    log "Creating kind cluster '${CLUSTER_NAME}'..."
    "$KIND_BINARY" create cluster --name "${CLUSTER_NAME}"
  fi
}

build_image() {
  if [ "$DO_BUILD" -ne 1 ]; then
    log "Skipping image build as requested"
    return
  fi

  if ! has_cmd docker; then
    err "Docker CLI not found. Please install and start Docker first."
    exit 2
  fi

  log "Building Docker image '${IMAGE_NAME}'..."
  docker build -t "${IMAGE_NAME}" .
}

load_image_into_kind() {
  if [ "$SKIP_LOAD" -eq 1 ]; then
    log "Skipping kind image load as requested"
    return
  fi

  log "Loading image '${IMAGE_NAME}' into kind cluster '${CLUSTER_NAME}'..."
  "$KIND_BINARY" load docker-image "${IMAGE_NAME}" --name "${CLUSTER_NAME}"
}

apply_kustomize() {
  if ! has_cmd kubectl; then
    err "kubectl CLI not found. Please install kubectl and configure access to the cluster."
    exit 2
  fi

  if [ ! -d "${KUSTOMIZE_PATH}" ]; then
    err "Kustomize path '${KUSTOMIZE_PATH}' not found"
    exit 2
  fi

  log "Applying kustomize manifests from '${KUSTOMIZE_PATH}' to cluster '${CLUSTER_NAME}'..."
  kubectl apply -k "${KUSTOMIZE_PATH}"
}

# Parse args
while [ "$#" -gt 0 ]; do
  case "$1" in
    --cluster-name)
      CLUSTER_NAME="$2"; shift 2;;
    --image)
      IMAGE_NAME="$2"; shift 2;;
    --kustomize-path)
      KUSTOMIZE_PATH="$2"; shift 2;;
    --no-build)
      DO_BUILD=0; shift;;
    --skip-load)
      SKIP_LOAD=1; shift;;
    --skip-oracle)
      SKIP_ORACLE=1; shift;;
    --tiny-dev)
      TINY_DEV=1; shift;;
    --wait)
      WAIT_FOR_READY=1; shift;;
    -h|--help)
      print_help; exit 0;;
    *)
      err "Unknown argument: $1"; print_help; exit 2;;
  esac
done

# Adjust kustomize path based on overlays
if [ "$TINY_DEV" -eq 1 ]; then
  KUSTOMIZE_PATH="k8s/overlays/tiny-dev"
elif [ "$SKIP_ORACLE" -eq 1 ]; then
  KUSTOMIZE_PATH="k8s/overlays/skip-oracle"
fi

# AuthZoriumApplication flow
log "Starting kind setup and deploy"
log "Cluster: ${CLUSTER_NAME}, Image: ${IMAGE_NAME}, Kustomize: ${KUSTOMIZE_PATH}, SKIP_ORACLE=${SKIP_ORACLE}"

create_kind_cluster
build_image
load_image_into_kind
apply_kustomize

if [ "$WAIT_FOR_READY" -eq 1 ]; then
  log "Waiting for authzorium deployment to rollout in namespace 'loket'..."
  kubectl rollout status deployment/authzorium -n loket --watch
  log "Rollout finished. Tailing logs from authzorium pods (press Ctrl+C to stop)"
  kubectl logs -n loket -l app=authzorium -f
fi

log "Done. Use 'kubectl get pods -n loket' to check pod status and 'kubectl get svc -n loket' for services."

exit 0

