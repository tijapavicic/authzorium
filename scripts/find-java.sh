#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS]
Find running Java processes in a portable and robust way.

Options:
  --full           (ignored) kept for compatibility — the script always shows the command line when possible
  --jps            Prefer using the JDK's `jps` tool if available (falls back if not present)
  --pattern <RE>   Filter results by the given extended-regular-expression
  -h, --help       Show this help

Exit codes:
  0  -> at least one match found
  1  -> no matches found
  2  -> invalid usage or error

Examples:
  $(basename "$0")
  $(basename "$0") --pattern "MyApp"
  $(basename "$0") --jps
EOF
}

pattern=""
use_jps=0

# Simple arg parsing
while [[ $# -gt 0 ]]; do
  case "$1" in
    --full)
      # kept for CLI compatibility; we always attempt to show full args
      shift
      ;;
    --jps)
      use_jps=1
      shift
      ;;
    --pattern)
      if [[ $# -lt 2 ]]; then
        echo "Missing argument for --pattern" >&2
        usage >&2
        exit 2
      fi
      pattern="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

# Helper: run a command and capture output, tolerant to non-zero exit
run_capture() {
  # usage: run_capture cmd... -> prints stdout to stdout and returns code
  ("$@")
}

output=""

# 1) Prefer jps if requested and available
if [[ "$use_jps" -eq 1 ]] && command -v jps >/dev/null 2>&1; then
  # jps prints "<pid> <mainclass>" and -v shows JVM args; -l shows full class name
  # use -v to be informative
  if output=$(jps -v 2>/dev/null || true); then
    :
  fi
fi

# 2) Try pgrep (matches process name or full cmdline with -f)
if [[ -z "$output" ]] && command -v pgrep >/dev/null 2>&1; then
  # Use -a to show the command line and -f to match the full command line
  output=$(pgrep -a -f java 2>/dev/null || true)

  # Some `pgrep` implementations (or usage without -a) may output only PIDs.
  # If output contains only numeric PIDs, expand them to full ps lines so
  # subsequent pattern filtering (e.g. --pattern) works reliably.
  if [[ -n "$output" ]]; then
    all_numeric=1
    while IFS= read -r _line; do
      # empty lines skip
      if [[ -z "$_line" ]]; then
        continue
      fi
      if [[ ! "$_line" =~ ^[0-9]+( .*)?$ ]]; then
        all_numeric=0
        break
      fi
      # if line contains spaces after PID, it's already full; treat as non-numeric
      if [[ "$_line" =~ ^[0-9]+\ .+ ]]; then
        all_numeric=0
        break
      fi
    done <<< "$output"

    if [[ "$all_numeric" -eq 1 ]]; then
      expanded=""
      while IFS= read -r pid; do
        [[ -z "$pid" ]] && continue
        # ps -p may differ between platforms; request user,pid,args to get consistent columns
        if ps -p "$pid" -o user=,pid=,args= >/dev/null 2>&1; then
          ps_line=$(ps -p "$pid" -o user=,pid=,args= 2>/dev/null || true)
          if [[ -n "$ps_line" ]]; then
            expanded+="$ps_line\n"
          fi
        fi
      done <<< "$output"
      # remove trailing newline
      output=$(printf '%b' "$expanded")
    fi
  fi
fi

# 3) Fallback to ps + grep
if [[ -z "$output" ]]; then
  # Prefer ps auxww (works on Linux and macOS) to get wide command lines
  if ps auxww >/dev/null 2>&1; then
    output=$(ps auxww | grep -E '[j]ava' || true)
  else
    output=$(ps -ef | grep -E '[j]ava' || true)
  fi
fi

# Exclude this script/process from the results to avoid self-matches
script_pid=$$
script_base=$(basename "$0")
if [[ -n "$output" ]]; then
  output=$(printf '%s\n' "$output" | awk -v pid="$script_pid" -v base="$script_base" '
  {
    pidfield=0;
    if ($1 ~ /^[0-9]+$/) pidfield=1;
    else if ($2 ~ /^[0-9]+$/) pidfield=2;
    if (pidfield && $(pidfield) == pid) next;
    if (index($0, base) > 0) next;
    print
  }')
fi

# Apply optional pattern filter
if [[ -n "$pattern" ]]; then
  filtered=$(printf '%s\n' "$output" | grep -E -- "$pattern" || true)
else
  filtered="$output"
fi

# Trim leading/trailing newlines and decide exit code
if [[ -n "$filtered" ]]; then
  # Print non-empty results
  printf '%s\n' "$filtered"
  exit 0
else
  # No matches: silent by default, exit 1
  exit 1
fi

