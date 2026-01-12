#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHANGELOG="$REPO_ROOT/CHANGES_PER_COMMIT.md"

# Get latest commit info
sha_full=$(git rev-parse HEAD)
sha_short=$(git rev-parse --short HEAD)
author=$(git show -s --format='%an <%ae>' HEAD)
date=$(git show -s --format='%ai' HEAD)
subject=$(git show -s --format='%s' HEAD)
body=$(git show -s --format='%b' HEAD)

# Extract checklist lines from commit body (markdown style)
# Lines like: - [x] done or - [ ] todo
checklist=$(printf "%s" "$body" | sed -n -E 's/^\s*- \[[ xX]\] .*/&/p')

if [ -z "$checklist" ]; then
  checklist='- [ ] describe done items'
fi

entry="### Commit $sha_short - $date
Author: $author

Message: $subject

$checklist

---

"

# Append to changelog
printf "%s\n" "$entry" >> "$CHANGELOG"

echo "Appended changelog entry for $sha_short to $CHANGELOG"

# optionally open in editor if requested
if [ "${1:-}" = "--edit" ]; then
  ${EDITOR:-vi} "$CHANGELOG"
fi

