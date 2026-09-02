#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 /absolute/or/relative/JerseySee-Complete.zip" >&2
  exit 64
fi

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
output_input="$1"
case "$output_input" in
  /*) output="$output_input" ;;
  *) output="$(pwd)/$output_input" ;;
esac
output_dir="$(dirname "$output")"
mkdir -p "$output_dir"
output="$(cd "$output_dir" && pwd)/$(basename "$output")"

case "$output" in
  "$repo_root"/*)
    echo "Release archive must be written outside the repository." >&2
    exit 65
    ;;
esac

if [ -e "$output" ]; then
  echo "Refusing to overwrite existing archive: $output" >&2
  exit 66
fi

stage="$(mktemp -d)"
trap 'rm -rf "$stage"' EXIT
package_root="$stage/JerseySee-Complete"
mkdir -p "$package_root"

is_excluded() {
  case "$1" in
    .git|.git/*|.idea|.idea/*|.superpowers|.superpowers/*|target|target/*|uploads|uploads/*|demo-uploads|demo-uploads/*|demo-data|demo-data/*|logs|logs/*|secrets|secrets/*)
      return 0
      ;;
    .env|application-local.properties|application-local.yml|application-local.yaml)
      return 0
      ;;
    .env.example)
      return 1
      ;;
    .env.*|*.zip|*.h2.db|*.trace.db|*.log|*.pem|*.key)
      return 0
      ;;
  esac
  return 1
}

while IFS= read -r -d '' file; do
  relative="${file#"$repo_root"/}"
  if is_excluded "$relative"; then
    continue
  fi
  destination="$package_root/$relative"
  mkdir -p "$(dirname "$destination")"
  cp -p "$file" "$destination"
done < <(find "$repo_root" -type f -print0)

(cd "$stage" && zip -q -r "$output" JerseySee-Complete)
echo "Created $output"
