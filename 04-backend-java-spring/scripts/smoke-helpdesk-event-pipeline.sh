#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/compose.yml"
FAILURE_LOG="${FAILURE_LOG:-/tmp/helpdesk-compose-failure.log}"
export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-1}"
export JWT_SECRET="${JWT_SECRET:-$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')}"

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  echo "ERROR: Docker Compose tidak ditemukan" >&2
  exit 1
fi

compose() {
  "${COMPOSE[@]}" -f "$COMPOSE_FILE" "$@"
}

cleanup() {
  local rc=$?
  trap - EXIT
  if (( rc != 0 )); then
    compose ps >&2 || true
    compose logs --no-color --tail=150 api worker kafka redis postgres >"$FAILURE_LOG" 2>&1 || true
    echo "FAIL: smoke test gagal; log: $FAILURE_LOG" >&2
  fi
  if [[ "${KEEP_STACK:-0}" != "1" ]]; then
    compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  fi
  exit "$rc"
}
trap cleanup EXIT

wait_http() {
  local url=$1
  local attempts=${2:-90}
  local code
  for ((i = 1; i <= attempts; i++)); do
    code=$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 3 "$url" || true)
    if [[ "$code" == "200" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "Timeout menunggu $url" >&2
  return 1
}

sql() {
  local database=$1
  local query=$2
  compose exec -T postgres psql -U helpdesk -d "$database" -Atqc "$query"
}

wait_count_at_least() {
  local database=$1
  local query=$2
  local minimum=$3
  local attempts=${4:-60}
  local count
  for ((i = 1; i <= attempts; i++)); do
    count=$(sql "$database" "$query" 2>/dev/null | tr -d '[:space:]' || true)
    if [[ "$count" =~ ^[0-9]+$ ]] && (( count >= minimum )); then
      return 0
    fi
    sleep 2
  done
  echo "Timeout menunggu query: $query" >&2
  return 1
}

json_field() {
  local field=$1
  python3 -c 'import json,sys; print(json.load(sys.stdin)[sys.argv[1]])' "$field"
}

post_json() {
  local url=$1
  local body=$2
  shift 2
  curl --silent --show-error --fail-with-body --max-time 20 \
    -H 'Content-Type: application/json' "$@" --data "$body" "$url"
}

wait_kafka() {
  for ((i = 1; i <= 60; i++)); do
    if compose exec -T kafka /opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "Timeout menunggu Kafka" >&2
  return 1
}

printf '%s\n' '[1/8] Build dan start 5-service stack'
compose down --volumes --remove-orphans >/dev/null 2>&1 || true
compose up --detach --build
wait_http http://localhost:8080/actuator/health
wait_http http://localhost:8081/actuator/health

stamp="$(date +%s)"
email="smoke-${stamp}@example.com"
password=$(python3 -c 'import secrets; print("Smoke-" + secrets.token_hex(8) + "-A9!")')

printf '%s\n' '[2/8] Registrasi dan create ticket'
auth=$(post_json http://localhost:8080/api/auth/register \
  "{\"email\":\"$email\",\"password\":\"$password\",\"fullName\":\"Smoke Test\"}")
token=$(printf '%s' "$auth" | json_field accessToken)
ticket=$(post_json http://localhost:8080/api/tickets \
  '{"title":"Pipeline smoke","description":"E2E event delivery","priority":"HIGH"}' \
  -H "Authorization: Bearer $token")
ticket_id=$(printf '%s' "$ticket" | json_field id)

printf '%s\n' '[3/8] Verifikasi outbox -> Kafka -> worker database'
wait_count_at_least helpdesk_notification \
  "SELECT count(*) FROM notification_log WHERE ticket_id = $ticket_id AND event_type = 'ticket.created.v1'" 1
published=$(sql helpdesk \
  "SELECT count(*) FROM ticket_event_outbox WHERE ticket_id = $ticket_id AND published_at IS NOT NULL")
[[ "$published" == "1" ]]

printf '%s\n' '[4/8] Replay event yang sama tetap idempotent'
event_row=$(sql helpdesk "
  SELECT event_id::text || '|' || event_type || '|' || event_version::text || '|' ||
         ticket_id::text || '|' || owner_email || '|' || status || '|' ||
         to_char(occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.US\"Z\"')
    FROM ticket_event_outbox
   WHERE ticket_id = $ticket_id
   ORDER BY occurred_at
   LIMIT 1")
IFS='|' read -r event_id event_type event_version event_ticket_id event_owner event_status event_time <<<"$event_row"
payload=$(python3 - "$event_id" "$event_type" "$event_version" "$event_ticket_id" \
  "$event_owner" "$event_status" "$event_time" <<'PY'
import json, sys
print(json.dumps({
    "eventId": sys.argv[1],
    "eventType": sys.argv[2],
    "eventVersion": int(sys.argv[3]),
    "ticketId": int(sys.argv[4]),
    "ownerEmail": sys.argv[5],
    "status": sys.argv[6],
    "occurredAt": sys.argv[7],
}, separators=(",", ":")))
PY
)
printf '%s|%s\n%s|%s\n' "$ticket_id" "$payload" "$ticket_id" "$payload" | \
  compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server kafka:9092 \
    --topic helpdesk.ticket-events.v1 \
    --property parse.key=true \
    --property 'key.separator=|'
sleep 4
duplicate_count=$(sql helpdesk_notification \
  "SELECT count(*) FROM notification_log WHERE event_id = '$event_id'")
[[ "$duplicate_count" == "1" ]]

printf '%s\n' '[5/8] Redis outage: detail ticket tetap 200 dari PostgreSQL fallback'
curl --silent --show-error --fail --max-time 10 \
  -H "Authorization: Bearer $token" \
  "http://localhost:8080/api/tickets/$ticket_id" >/dev/null
compose stop redis >/dev/null
redis_code=$(curl --silent --output /tmp/helpdesk-redis-outage-response.json \
  --write-out '%{http_code}' --max-time 15 \
  -H "Authorization: Bearer $token" \
  "http://localhost:8080/api/tickets/$ticket_id" || true)
[[ "$redis_code" == "200" ]]
compose start redis >/dev/null

printf '%s\n' '[6/8] Kafka outage: create tetap commit dan outbox tetap pending'
compose stop kafka >/dev/null
outage_ticket=$(post_json http://localhost:8080/api/tickets \
  '{"title":"Kafka outage","description":"Must commit before broker recovery","priority":"MEDIUM"}' \
  -H "Authorization: Bearer $token")
outage_ticket_id=$(printf '%s' "$outage_ticket" | json_field id)
wait_count_at_least helpdesk \
  "SELECT count(*) FROM ticket_event_outbox WHERE ticket_id = $outage_ticket_id AND published_at IS NULL" 1 15

printf '%s\n' '[7/8] Kafka pulih: pending outbox terkirim tepat sekali'
compose start kafka >/dev/null
wait_kafka
wait_count_at_least helpdesk \
  "SELECT count(*) FROM ticket_event_outbox WHERE ticket_id = $outage_ticket_id AND published_at IS NOT NULL" 1 60
wait_count_at_least helpdesk_notification \
  "SELECT count(*) FROM notification_log WHERE ticket_id = $outage_ticket_id AND event_type = 'ticket.created.v1'" 1 60
recovered_count=$(sql helpdesk_notification \
  "SELECT count(*) FROM notification_log WHERE ticket_id = $outage_ticket_id AND event_type = 'ticket.created.v1'")
[[ "$recovered_count" == "1" ]]

printf '%s\n' '[8/8] Final health dan ringkasan'
wait_http http://localhost:8080/actuator/health
wait_http http://localhost:8081/actuator/health
printf 'PASS: ticket=%s duplicate_count=%s outage_ticket=%s recovered_count=%s\n' \
  "$ticket_id" "$duplicate_count" "$outage_ticket_id" "$recovered_count"
