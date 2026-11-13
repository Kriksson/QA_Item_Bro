#!/usr/bin/env bash
set -euo pipefail

: "${THREADS:=1}"
: "${CI_PREFIX:=qa-item-bro-ci}"
case "$THREADS" in 1|2|3|4) ;; *) echo 'THREADS должен быть 1, 2, 3 или 4' >&2; exit 2 ;; esac
if [[ ! "$CI_PREFIX" =~ ^[a-z0-9][a-z0-9-]*$ ]]; then
    echo 'CI_PREFIX должен содержать только строчные латинские буквы, цифры и дефис' >&2
    exit 2
fi
export CI_PREFIX
compose=(docker compose -p "$CI_PREFIX" -f ci/compose.workers.yaml)
profiles=()
for ((worker=1; worker<=THREADS; worker++)); do
    profiles+=(--profile "worker$worker")
done

case "${1:-}" in
    up)
        "${compose[@]}" "${profiles[@]}" up -d --wait --wait-timeout 240
        ;;
    logs)
        "${compose[@]}" --profile '*' logs --no-color --tail 150
        ;;
    down)
        "${compose[@]}" --profile '*' down --volumes --remove-orphans
        ;;
    *) echo 'Использование: bash ci/workers.sh up|logs|down' >&2; exit 2 ;;
esac
