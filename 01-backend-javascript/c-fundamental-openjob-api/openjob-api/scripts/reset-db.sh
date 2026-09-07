#!/usr/bin/env bash
# Menyiapkan basis data bersih sebelum pengujian dijalankan.
set -e
dropdb --if-exists openjob
createdb openjob
npm run migrate up > /dev/null
redis-cli flushall > /dev/null
echo "Basis data dan cache telah direset."
