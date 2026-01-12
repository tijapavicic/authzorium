#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOOKS_DIR="$REPO_ROOT/.githooks"
GIT_HOOKS_DIR="$REPO_ROOT/.git/hooks"

mkdir -p "$HOOKS_DIR"

# Create a post-commit hook that calls our recorder
cat > "$HOOKS_DIR/post-commit" <<'HOOK'
#!/usr/bin/env bash
# Simple post-commit hook to append commit info to CHANGES_PER_COMMIT.md
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
"$REPO_ROOT/scripts/record-commit-change.sh"
HOOK

chmod +x "$HOOKS_DIR/post-commit"

# Configure git to use the local hooks directory
git config core.hooksPath "$HOOKS_DIR"

echo "Configured git hooks path to $HOOKS_DIR and created post-commit hook."

