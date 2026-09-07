#!/usr/bin/env bash
#
# Membuktikan pembatasan 90 request per menit pada resource /threads.
#
#   bash uji-rate-limit.sh <DOMAIN>
#
# Mengirim 120 permintaan berurutan lalu menghitung jumlah balasan 429.
# Bila pembatasan bekerja, sebagian permintaan terakhir dibalas 429.

set -euo pipefail

DOMAIN="${1:-}"
if [[ -z "$DOMAIN" ]]; then
  echo "Penggunaan: bash uji-rate-limit.sh <DOMAIN>"
  exit 1
fi

echo "Mengirim 120 permintaan ke https://$DOMAIN/threads/uji ..."
CODES=$(for i in $(seq 1 120); do
  curl -s -o /dev/null -w "%{http_code}\n" "https://$DOMAIN/threads/uji"
done)

echo ""
echo "Rekapitulasi kode status:"
echo "$CODES" | sort | uniq -c | sort -rn

TOTAL_429=$(echo "$CODES" | grep -c '^429$' || true)
echo ""
if [[ "$TOTAL_429" -gt 0 ]]; then
  echo "BERHASIL: $TOTAL_429 permintaan ditolak dengan kode 429 (limit access aktif)."
else
  echo "PERINGATAN: tidak ada balasan 429. Periksa kembali konfigurasi limit_req."
fi
