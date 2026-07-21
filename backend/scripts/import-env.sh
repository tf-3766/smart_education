#!/usr/bin/env bash
# Load backend/.env into the CURRENT shell (macOS/Linux, bash or zsh).
#
# Usage (must be *sourced*, not executed):
#   cd backend
#   source scripts/import-env.sh          # loads ../.env relative to this script
#   source scripts/import-env.sh /path/.env
#
# Why a dedicated loader: Spring Boot does not read .env automatically, and a
# plain `source .env` breaks on values containing shell metacharacters such as
# the `&` inside DB_URL. This reads each KEY=VALUE line literally and exports it.

# Resolve this script's directory in both bash and zsh.
if [ -n "${ZSH_VERSION:-}" ]; then
  _self="${(%):-%x}"
else
  _self="${BASH_SOURCE[0]}"
fi
_dir="$(cd "$(dirname "$_self")/.." >/dev/null 2>&1 && pwd)"
ENV_FILE="${1:-$_dir/.env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "import-env: .env not found at $ENV_FILE" >&2
  echo "import-env: copy .env.example to .env and fill DASHSCOPE_API_KEY first." >&2
  return 1 2>/dev/null || exit 1
fi

_count=0
while IFS= read -r _line || [ -n "$_line" ]; do
  _line="${_line%$'\r'}"                 # strip trailing CR (Windows-edited files)
  case "$_line" in
    ''|'#'*) continue ;;                 # skip blank lines and comments
  esac
  _key="${_line%%=*}"
  _val="${_line#*=}"
  _key="$(printf '%s' "$_key" | tr -d '[:space:]')"
  [ -z "$_key" ] && continue
  export "$_key=$_val"                    # literal assignment: no re-parsing of & etc.
  _count=$((_count + 1))
done < "$ENV_FILE"

echo "import-env: loaded $_count variables from $ENV_FILE"
unset _self _dir _line _key _val _count
