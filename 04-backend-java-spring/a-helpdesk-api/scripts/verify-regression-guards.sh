#!/usr/bin/env bash
# Verifikasi bahwa test keamanan benar-benar menangkap regresi.
# Tiap bug lama disuntikkan ulang; build WAJIB gagal. Jika lulus, test tidak bernilai.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

SEC=src/main/java/com/arirahmat/helpdesk/config/SecurityConfig.java
FILTER=src/main/java/com/arirahmat/helpdesk/security/JwtAuthenticationFilter.java
POM=pom.xml
BK=/tmp/helpdesk-guard-backup
mkdir -p "$BK"
cp "$SEC" "$BK/SecurityConfig.java"
cp "$FILTER" "$BK/JwtAuthenticationFilter.java"
cp "$POM" "$BK/pom.xml"

restore() { cp "$BK/SecurityConfig.java" "$SEC"; cp "$BK/JwtAuthenticationFilter.java" "$FILTER"; cp "$BK/pom.xml" "$POM"; }
trap restore EXIT

RESULT=0
report() { # nama, exit_code_build
  if [ "$2" -ne 0 ]; then
    echo "PASS  $1 -> build GAGAL seperti yang diharapkan"
  else
    echo "FAIL  $1 -> build LULUS padahal bug disuntikkan; test tidak menjaga apa pun"
    RESULT=1
  fi
}

echo "### Bug #2: hapus AuthenticationEntryPoint (anonim jadi 403, bukan 401)"
python3 - "$SEC" <<'PY'
import sys
p=sys.argv[1]; s=open(p).read()
s=s.replace(".exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))","")
open(p,"w").write(s)
PY
./mvnw -B -q test -Dtest='SecurityMatrixMockMvcTest$Unauthorized' >/tmp/guard2.log 2>&1
report "entry-point-401" $?
restore

echo "### Bug #3: filter kembali melewati ERROR dispatch (403 jadi 401)"
python3 - "$FILTER" <<'PY'
import sys
p=sys.argv[1]; s=open(p).read()
s=s.replace("protected boolean shouldNotFilterErrorDispatch() {\n        return false;","protected boolean shouldNotFilterErrorDispatch() {\n        return true;")
open(p,"w").write(s)
PY
./mvnw -B -q verify -Dtest='SecurityMatrixMockMvcTest$Forbidden' -Dit.test=SecurityMatrixHttpIT -DfailIfNoSpecifiedTests=false -Djacoco.skip=true >/tmp/guard3.log 2>&1
report "error-dispatch-403" $?
restore

echo "### Bug #1: jjwt-impl dideklarasikan ulang sebagai test-only (app gagal boot)"
python3 - "$POM" <<'PY'
import sys
p=sys.argv[1]; s=open(p).read()
dup = """\t\t<dependency>
\t\t\t<groupId>io.jsonwebtoken</groupId>
\t\t\t<artifactId>jjwt-impl</artifactId>
\t\t\t<version>${jjwt.version}</version>
\t\t\t<scope>test</scope>
\t\t</dependency>
\t</dependencies>"""
s=s.replace("\t</dependencies>", dup, 1)
open(p,"w").write(s)
PY
./mvnw -B -q verify -Dit.test=SecurityMatrixHttpIT -DskipTests=false -Dsurefire.skip=true -Djacoco.skip=true >/tmp/guard1.log 2>&1
report "jjwt-runtime-scope" $?
restore

echo
if [ "$RESULT" -eq 0 ]; then
  echo "SEMUA GUARD BEKERJA: ketiga regresi terdeteksi oleh suite."
else
  echo "ADA GUARD YANG BOCOR. Periksa /tmp/guard*.log"
fi
exit "$RESULT"
