#!/usr/bin/env bash
# Smoke test end-to-end Forum API lewat HTTPS produksi.
set -uo pipefail
B="https://aconk-forumapi.duckdns.org"
U="aconk$RANDOM$RANDOM"
ok=0; fail=0
chk() { # nama, aktual, harapan
  if [ "$2" = "$3" ]; then echo "  PASS  $1 ($2)"; ok=$((ok+1));
  else echo "  FAIL  $1 (dapat $2, harap $3)"; fail=$((fail+1)); fi
}

r=$(curl -s -w '\n%{http_code}' -X POST "$B/users" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$U\",\"password\":\"rahasia123\",\"fullname\":\"Aconk Uji\"}")
chk "POST /users" "$(tail -1 <<<"$r")" "201"

r=$(curl -s -w '\n%{http_code}' -X POST "$B/authentications" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$U\",\"password\":\"rahasia123\"}")
chk "POST /authentications" "$(tail -1 <<<"$r")" "201"
AT=$(sed '$d' <<<"$r" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')
RT=$(sed '$d' <<<"$r" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["refreshToken"])')
H="Authorization: Bearer $AT"

r=$(curl -s -w '\n%{http_code}' -X POST "$B/threads" -H "$H" -H 'Content-Type: application/json' \
  -d '{"title":"Uji produksi","body":"Isi thread uji end-to-end."}')
chk "POST /threads" "$(tail -1 <<<"$r")" "201"
TID=$(sed '$d' <<<"$r" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["addedThread"]["id"])')

r=$(curl -s -w '\n%{http_code}' -X POST "$B/threads/$TID/comments" -H "$H" -H 'Content-Type: application/json' -d '{"content":"komentar uji"}')
chk "POST /threads/{id}/comments" "$(tail -1 <<<"$r")" "201"
CID=$(sed '$d' <<<"$r" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["addedComment"]["id"])')

r=$(curl -s -w '\n%{http_code}' -X POST "$B/threads/$TID/comments/$CID/replies" -H "$H" -H 'Content-Type: application/json' -d '{"content":"balasan uji"}')
chk "POST .../replies" "$(tail -1 <<<"$r")" "201"

r=$(curl -s -w '\n%{http_code}' -X PUT "$B/threads/$TID/comments/$CID/likes" -H "$H")
chk "PUT .../likes (suka)" "$(tail -1 <<<"$r")" "200"

r=$(curl -s -w '\n%{http_code}' "$B/threads/$TID")
chk "GET /threads/{id}" "$(tail -1 <<<"$r")" "200"
LC=$(sed '$d' <<<"$r" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["thread"]["comments"][0]["likeCount"])')
chk "likeCount muncul" "$LC" "1"

r=$(curl -s -w '\n%{http_code}' -X PUT "$B/threads/$TID/comments/$CID/likes" -H "$H")
chk "PUT .../likes (batal)" "$(tail -1 <<<"$r")" "200"
LC=$(curl -s "$B/threads/$TID" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["thread"]["comments"][0]["likeCount"])')
chk "likeCount kembali 0" "$LC" "0"

r=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$B/threads" -H 'Content-Type: application/json' -d '{"title":"x","body":"y"}')
chk "POST /threads tanpa token ditolak" "$r" "401"

r=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$B/authentications" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$RT\"}")
chk "PUT /authentications (refresh)" "$r" "200"

r=$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$B/authentications" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$RT\"}")
chk "DELETE /authentications (logout)" "$r" "200"

echo ""
echo "TOTAL: $ok lulus, $fail gagal"
[ "$fail" -eq 0 ]
