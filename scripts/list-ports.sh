#!/usr/bin/env bash
set -euo pipefail

print_help() {
  cat <<EOF
Usage: $(basename "$0") [ports...]
List listening TCP ports. If ports are provided, shows listeners only for those ports.
Examples:
  $(basename "$0")         # list all listening TCP ports
  $(basename "$0") 8082    # show listener for port 8082
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  print_help
  exit 0
fi

if command -v lsof >/dev/null 2>&1; then
  LIST_CMD=(lsof -iTCP -sTCP:LISTEN -P -n)
else
  LIST_CMD=(sh -c "netstat -anv | grep LISTEN")
fi

if [[ $# -gt 0 ]]; then
  for p in "$@"; do
    echo "----- Port $p -----"
    if "${LIST_CMD[@]}" | egrep ":$p\\b" || true; then
      :
    else
      echo "No listener on port $p"
    fi
  done
else
  echo "Listening TCP ports:"
  "${LIST_CMD[@]}"
fi

