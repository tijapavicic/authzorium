#!/usr/bin/env bash
set -euo pipefail

# create-and-push-remote.sh
# Create a remote repository (GitHub) and push all local branches & tags.
# Usage examples:
# 1) Using gh (recommended):
#    REPO_NAME=my-repo GH_OWNER=myuser VISIBILITY=private ./scripts/create-and-push-remote.sh
# 2) Using GitHub token (no gh installed):
#    REPO_NAME=my-repo GITHUB_TOKEN=ghp_xxx GH_OWNER=myuser VISIBILITY=private ./scripts/create-and-push-remote.sh
# 3) If origin already exists and you want to overwrite:
#    FORCE=1 REPO_NAME=my-repo GH_OWNER=myuser ./scripts/create-and-push-remote.sh

#Push existing git repo on origin:
#git remote add origin <URL>
#git remote set-url origin https://<USERNAME>:<NEW_PERSONAL_ACCESS_TOKEN>@github.com/<USERNAME>/<PROJECT-NAME>
#
#Verify:
#git remote -v

REMOTE_NAME=${REMOTE_NAME:-origin}
REPO_NAME=${REPO_NAME:-}
GH_OWNER=${GH_OWNER:-}
VISIBILITY=${VISIBILITY:-private}  # or public
FORCE=${FORCE:-0}
PREFER_SSH=${PREFER_SSH:-1} # if 1 use SSH remote (git@github.com:...), else https

usage() {
  cat <<EOF
Usage: REPO_NAME=repo-name GH_OWNER=your-github-username [VISIBILITY=private|public] [FORCE=1] [PREFER_SSH=1] $0

Environment options:
  REPO_NAME    (required) repository name to create on GitHub
  GH_OWNER     (required) your GitHub username or organization
  VISIBILITY   optional: private (default) or public
  FORCE        optional: set to 1 to override existing origin remote
  PREFER_SSH   optional: 1 to set SSH remote, 0 to set HTTPS remote (default 1)

Requirements:
  - This script expects to run inside a local git repository (has .git)
  - Either the GitHub CLI (gh) installed and authenticated, OR a GITHUB_TOKEN that has 'repo' permission

Examples:
  GH_OWNER=alice REPO_NAME=authzorium VISIBILITY=private ./scripts/create-and-push-remote.sh
EOF
}

if [ -z "$REPO_NAME" ] || [ -z "$GH_OWNER" ]; then
  echo "REPO_NAME and GH_OWNER must be set"
  usage
  exit 2
fi

# Ensure we're in a git repo
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "This directory is not a git repository. Run this from the repository root."
  exit 2
fi

# Check existing remote
if git remote get-url "$REMOTE_NAME" >/dev/null 2>&1; then
  if [ "$FORCE" != "1" ]; then
    echo "Remote '$REMOTE_NAME' already exists: $(git remote get-url $REMOTE_NAME)"
    echo "If you want to overwrite it, re-run with FORCE=1"
    exit 1
  else
    echo "Overwriting existing remote '$REMOTE_NAME'"
    git remote remove "$REMOTE_NAME" || true
  fi
fi

# Create repository on GitHub
REMOTE_URL=
if command -v gh >/dev/null 2>&1; then
  echo "Creating repository using GitHub CLI..."
  # gh repo create [OWNER]/[REPO] --private --confirm
  if [ -n "$GH_OWNER" ]; then
    gh repo create "$GH_OWNER/$REPO_NAME" --${VISIBILITY} --confirm || true
    # prefer SSH or HTTPS depending on config
    if [ "$PREFER_SSH" = "1" ]; then
      REMOTE_URL="git@github.com:$GH_OWNER/$REPO_NAME.git"
    else
      REMOTE_URL="https://github.com/$GH_OWNER/$REPO_NAME.git"
    fi
  else
    echo "GH_OWNER must be set when using gh"
    exit 2
  fi
elif [ -n "${GITHUB_TOKEN:-}" ]; then
  echo "Creating repository using GitHub API (GITHUB_TOKEN detected)..."
  # Use API: POST /user/repos or /orgs/:org/repos
  if [ -n "$GH_OWNER" ]; then
    # Determine if GH_OWNER is an org by trying to GET /orgs/$GH_OWNER
    if curl -s -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/orgs/$GH_OWNER | grep -q 'Not Found'; then
      # Not an org -> create in user's account
      payload=$(jq -n --arg name "$REPO_NAME" --argjson priv $( [ "$VISIBILITY" = "private" ] && echo true || echo false ) '{name: $name, private: $priv}')
      resp=$(curl -s -H "Authorization: token $GITHUB_TOKEN" -d "$payload" https://api.github.com/user/repos)
    else
      # org
      payload=$(jq -n --arg name "$REPO_NAME" --argjson priv $( [ "$VISIBILITY" = "private" ] && echo true || echo false ) '{name: $name, private: $priv}')
      resp=$(curl -s -H "Authorization: token $GITHUB_TOKEN" -d "$payload" https://api.github.com/orgs/$GH_OWNER/repos)
    fi
    # parse ssh_url / clone_url
    REMOTE_URL=$(echo "$resp" | (jq -r '.ssh_url // empty' || sed -n 's/.*"ssh_url": "\([^"]*\)".*/\1/p'))
    if [ -z "$REMOTE_URL" ]; then
      # fallback to https
      REMOTE_URL="https://github.com/$GH_OWNER/$REPO_NAME.git"
    fi
  else
    echo "GH_OWNER must be set when using GITHUB_TOKEN"
    exit 2
  fi
else
  echo "Neither 'gh' CLI nor GITHUB_TOKEN present. Please install gh (https://cli.github.com/) and run 'gh auth login' or set GITHUB_TOKEN environment variable."
  exit 2
fi

# Add remote and push
if [ -z "$REMOTE_URL" ]; then
  echo "Failed to determine remote URL. Aborting."
  exit 2
fi

echo "Adding remote $REMOTE_NAME -> $REMOTE_URL"
git remote add "$REMOTE_NAME" "$REMOTE_URL"

echo "Pushing all branches to $REMOTE_NAME..."
# Push all refs/heads
git push --all "$REMOTE_NAME"

echo "Pushing tags to $REMOTE_NAME..."
git push --tags "$REMOTE_NAME"

# Set upstream for current branch if not set
current_branch=$(git rev-parse --abbrev-ref HEAD)
if [ -n "$current_branch" ]; then
  echo "Setting upstream for current branch '$current_branch'"
  git push -u "$REMOTE_NAME" "$current_branch"
fi

echo "Done. Remote '"$REMOTE_NAME"' points to: $(git remote get-url $REMOTE_NAME)"

echo "Remote branches:"
git ls-remote --heads "$REMOTE_NAME" | sed -n '1,200p'

exit 0

