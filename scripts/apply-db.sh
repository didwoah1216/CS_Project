#!/usr/bin/env bash
# schema.sql 다음 dummy_data.sql 을 적용한다.
# 로컬 예시:
#   CHAGOK_DB_PASSWORD=비밀번호 ./scripts/apply-db.sh
# RDS 예시 (오늘 환경에서는 접속하지 않음):
#   CHAGOK_DB_HOST=xxxx.rds.amazonaws.com CHAGOK_DB_SSL=true \
#   CHAGOK_DB_USER=chagok_app CHAGOK_DB_PASSWORD=비밀번호 ./scripts/apply-db.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${CHAGOK_DB_HOST:-127.0.0.1}"
PORT="${CHAGOK_DB_PORT:-3306}"
USER_NAME="${CHAGOK_DB_USER:-chagok_app}"
if [[ -z "${CHAGOK_DB_PASSWORD:-}" ]]; then
  echo "CHAGOK_DB_PASSWORD 가 필요합니다. 비밀번호는 명령 기록에 남기지 말고 환경변수로 넘기세요." >&2
  exit 1
fi
SSL_ARGS=()
if [[ "${CHAGOK_DB_SSL:-false}" == "true" ]]; then
  SSL_ARGS=(--ssl-mode=REQUIRED)
fi
echo "적용 대상 ${USER_NAME}@${HOST}:${PORT}"
mysql -h "$HOST" -P "$PORT" -u "$USER_NAME" -p"$CHAGOK_DB_PASSWORD" "${SSL_ARGS[@]}" \
  --default-character-set=utf8mb4 < "$ROOT/chagok-server/src/main/resources/sql/schema.sql"
mysql -h "$HOST" -P "$PORT" -u "$USER_NAME" -p"$CHAGOK_DB_PASSWORD" "${SSL_ARGS[@]}" \
  --default-character-set=utf8mb4 < "$ROOT/chagok-server/src/main/resources/sql/dummy_data.sql"
echo "schema.sql 과 dummy_data.sql 적용을 마쳤습니다."
