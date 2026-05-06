#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

rm -f /tmp/live-more-api.pid /tmp/live-more-api.log /tmp/an.json

mvn -q -DskipTests package

(mvn -q spring-boot:run > /tmp/live-more-api.log 2>&1 & echo $! > /tmp/live-more-api.pid)
PID="$(cat /tmp/live-more-api.pid)"

for _ in $(seq 1 60); do
  if curl -sS -m 1 http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 0.25
done

echo "health: $(curl -sS -m 2 http://127.0.0.1:8080/actuator/health)"

TOKEN="$(
  curl -sS -X POST http://127.0.0.1:8080/api/v1/auth/test-account/login \
    -H "Content-Type: application/json" \
    -H "X-Test-Account-Secret: local-test-account-secret-2026" \
    -d '{"phone":"15121150684"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])'
)"

curl -sS -X POST "http://127.0.0.1:8080/api/v1/conversations/messages?userId=phone_15121150684" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"chat_phone_15121150684_main","role":"user","content":"test"}' \
  >/dev/null

CODE="$(
  curl -sS -o /tmp/an.json -w "%{http_code}" \
    -X POST "http://127.0.0.1:8080/api/v1/conversations/insight/chat_phone_15121150684_main/analyze?userId=phone_15121150684" \
    -H "Authorization: Bearer $TOKEN"
)"

echo "analyze_http: $CODE"
cat /tmp/an.json

kill "$PID"
sleep 0.5
tail -n 30 /tmp/live-more-api.log || true

