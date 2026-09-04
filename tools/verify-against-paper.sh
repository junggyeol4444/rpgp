#!/usr/bin/env bash
# ================================================================
# 실제 Paper API 로 컴파일해 보는 검증 스크립트
#
# repo.papermc.io 에서 paper-api 를 받을 수 없는 환경에서도
# 검증할 수 있도록, Paper 저장소의 API 소스를 직접 받아 컴파일한다.
# 정상적으로 paper-api 를 받을 수 있는 환경이라면 ./gradlew build 를 쓰면 된다.
#
# 남의 코드를 이 저장소에 복사해 두지 않는다. 실행할 때 받아서 쓰고,
# 결과만 남긴다.
#
# 사용법:  tools/verify-against-paper.sh [작업디렉터리]
# 기본 작업디렉터리: .verify (git 에서 무시된다)
# ================================================================
set -euo pipefail

PAPER_REF="${PAPER_REF:-ver/26.1.2}"
WORK="${1:-.verify}"
LIB="$WORK/lib"
MAVEN="https://repo1.maven.org/maven2"

mkdir -p "$LIB"

fetch() { # 그룹경로 아티팩트 버전
  local file="$2-$3.jar"
  [ -f "$LIB/$file" ] && return 0
  local i
  for i in 1 2 3 4; do
    if curl -fsS -m 120 -o "$LIB/$file" "$MAVEN/$1/$2/$3/$file"; then
      echo "  받음 $file"; return 0
    fi
    rm -f "$LIB/$file"; sleep $((i * 3))
  done
  echo "  실패 $file" >&2; return 1
}

echo "[1/4] Paper API 소스 ($PAPER_REF)"
if [ ! -d "$WORK/paper/.git" ]; then
  git clone --depth 1 --branch "$PAPER_REF" --filter=blob:none --sparse \
      https://github.com/PaperMC/Paper.git "$WORK/paper"
  git -C "$WORK/paper" sparse-checkout set paper-api
fi

echo "[2/4] brigadier 소스 (Maven Central 에 없음)"
[ -d "$WORK/brigadier/.git" ] || \
  git clone --depth 1 https://github.com/Mojang/brigadier.git "$WORK/brigadier"

echo "[3/4] paper-api 의존성 (Maven Central)"
fetch org/jetbrains annotations 26.0.2
fetch org/checkerframework checker-qual 3.49.2
fetch org/jspecify jspecify 1.0.0
fetch com/google/guava guava 33.5.0-jre
fetch com/google/code/gson gson 2.13.2
fetch org/yaml snakeyaml 2.2
fetch org/joml joml 1.10.8
fetch it/unimi/dsi fastutil 8.5.18
fetch org/apache/logging/log4j log4j-api 2.25.2
fetch org/slf4j slf4j-api 2.0.17
fetch org/apache/commons commons-lang3 3.20.0
fetch net/md-5 bungeecord-chat 1.21-R0.4
for artifact in adventure-api adventure-key adventure-text-minimessage \
                adventure-text-serializer-gson adventure-text-serializer-json \
                adventure-text-serializer-legacy adventure-text-serializer-plain \
                adventure-text-logger-slf4j; do
  fetch net/kyori "$artifact" 4.26.1
done
fetch net/kyori examination-api 1.3.0
fetch net/kyori option 1.1.0
for artifact in maven-resolver-api maven-resolver-util maven-resolver-spi \
                maven-resolver-impl maven-resolver-connector-basic \
                maven-resolver-transport-http; do
  fetch org/apache/maven/resolver "$artifact" 1.9.18
done
fetch org/apache/maven maven-resolver-provider 3.9.6

echo "[4/4] 컴파일"
API="$WORK/paper/paper-api/src/main/java"
GEN="$WORK/paper/paper-api/src/generated/java"
BRIG="$WORK/brigadier/src/main/java"
OUT="$WORK/classes"
rm -rf "$OUT"; mkdir -p "$OUT"

javac -Xlint:all -proc:none -encoding UTF-8 \
      -cp "$LIB/*" \
      -sourcepath "$API:$GEN:$BRIG:src/main/java" \
      -d "$OUT" \
      $(find src/main/java -name '*.java')

echo
echo "컴파일 통과. 실제 Paper API($PAPER_REF) 기준 오류 0."
