#!/usr/bin/env bash
# 서버 없이 돌릴 수 있는 검증 프로그램을 전부 실행한다.
#
# 대상은 Bukkit API 에 닿지 않는 순수 자바 부분(설정 스키마 · 수치 계산 ·
# 저장 형식 · 편집 초안)이다. Bukkit 에 닿는 부분은
# tools/verify-against-paper.sh 로 실제 Paper API 컴파일을 확인한다.
set -uo pipefail
cd "$(dirname "$0")/.."

OUT="${TMPDIR:-/tmp}/rpgcore-checks"
rm -rf "$OUT"
mkdir -p "$OUT"

echo "[1/2] 컴파일"
# 검증 대상이 되는 순수 자바 클래스만 sourcepath 로 끌어온다.
if ! javac -proc:none -encoding UTF-8 -nowarn \
           -sourcepath src/main/java \
           -d "$OUT" \
           tools/checks/Check*.java; then
  echo "컴파일 실패."
  exit 1
fi

echo "[2/2] 실행"
total_pass=0
total_fail=0
failed_classes=""
for class in Check Check2 Check3 Check4 Check5 Check6 Check7; do
  echo
  echo "--- $class ---"
  # 하나가 깨져도 나머지를 다 돌려야 어디가 문제인지 한 번에 본다.
  output="$(java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "$OUT" "$class" 2>&1)"
  status=$?
  echo "$output"

  summary="$(printf '%s\n' "$output" | grep '^결과: ' | tail -1)"
  p="$(printf '%s\n' "$summary" | sed -nE 's/^결과: ([0-9]+) 통과.*/\1/p')"
  f="$(printf '%s\n' "$summary" | sed -nE 's/.*\/ ([0-9]+) 실패.*/\1/p')"
  total_pass=$((total_pass + ${p:-0}))
  total_fail=$((total_fail + ${f:-0}))

  # 결과 줄 자체가 안 나왔으면(예외로 중간에 죽음) 그것도 실패로 센다.
  if [ -z "$summary" ]; then
    total_fail=$((total_fail + 1))
    failed_classes="$failed_classes $class(중단)"
  elif [ "${f:-0}" -ne 0 ] || [ $status -ne 0 ]; then
    failed_classes="$failed_classes $class"
  fi
done

echo
echo "합계: $total_pass 통과 / $total_fail 실패"
if [ -n "$failed_classes" ]; then
  echo "실패한 프로그램:$failed_classes"
  exit 1
fi
