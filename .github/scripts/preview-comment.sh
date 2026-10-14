#!/usr/bin/env bash
set -euo pipefail

[[ "$PR_NUMBER" =~ ^[1-9][0-9]*$ ]] || { echo "Invalid PR number" >&2; exit 1; }
marker='<!-- pi-calculator-preview -->'
case "$MODE" in
  preview)
    message="[View PR preview](https://tylerprete.github.io/pi-calculator/previews/pr-$PR_NUMBER/)"
    note='Updated from the latest successful build. The link becomes available after GitHub Pages finishes deploying.'
    ;;
  cleanup)
    message='Preview removed because this pull request was closed or merged.'
    note=''
    ;;
  *) echo "Invalid preview comment mode" >&2; exit 1 ;;
esac
body=$(printf '%s\n\n%s\n\n%s' "$marker" "$message" "$note")

# Only update our marked bot comment, never a contributor's comment.
comment_id=$(gh api "repos/$GITHUB_REPOSITORY/issues/$PR_NUMBER/comments?per_page=100" \
  --paginate --slurp | jq -r \
  '[.[][] | select(.user.login == "github-actions[bot]" and
    (.body | startswith("<!-- pi-calculator-preview -->")))] | first | .id // empty')
if [[ -n "$comment_id" ]]; then
  gh api --method PATCH "repos/$GITHUB_REPOSITORY/issues/comments/$comment_id" \
    -f body="$body" --silent
elif [[ "$MODE" == preview ]]; then
  gh api --method POST "repos/$GITHUB_REPOSITORY/issues/$PR_NUMBER/comments" \
    -f body="$body" --silent
fi
