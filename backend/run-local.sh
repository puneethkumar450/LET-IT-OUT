#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="$repo_dir/backend/.env"

if [[ ! -f "$env_file" ]]; then
  echo "Create backend/.env from backend/.env.example and add your Groq API key." >&2
  exit 1
fi

set -a
# This ignored file is local and user-controlled. Shell syntax allows quoted values.
source "$env_file"
set +a

if [[ -z "${GROQ_API_KEY:-}" || "$GROQ_API_KEY" == "replace-me" ]]; then
  echo "Add GROQ_API_KEY to backend/.env before starting chat." >&2
  exit 1
fi
if [[ -z "${FIREBASE_PROJECT_ID:-}" ]]; then
  echo "Add FIREBASE_PROJECT_ID to backend/.env." >&2
  exit 1
fi
if [[ ! -f "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]]; then
  echo "Set GOOGLE_APPLICATION_CREDENTIALS to the local Firebase Admin JSON file." >&2
  exit 1
fi

exec "$repo_dir/gradlew" -p "$repo_dir/backend" run
