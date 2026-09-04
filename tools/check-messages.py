#!/usr/bin/env python3
"""messages.yml 의 키와 코드가 실제로 쓰는 경로를 맞춰 본다.

코드에 있는데 파일에 없으면 화면에 경로 문자열이 그대로 나오고,
파일에 있는데 아무도 안 쓰면 죽은 설정이다. 둘 다 잡는다.

경로는 messages.send(...) 같은 호출에서만 오지 않는다. 상수에 담기도 하고
(`ROLE_DISPLAY` 를 넘기면서 이름 키를 따로 넘기는 식), 이어 붙이기도 한다
(`"quest.type." + type`). 그래서 호출부를 파싱하는 대신, 자바 소스의 모든
문자열 중 messages.yml 의 맨 윗줄 키로 시작하는 것을 경로 후보로 본다.

  - 점으로 끝나는 후보는 이어 붙이는 접두사로 보고, 그 아래 키를 모두 쓴 것으로 친다
  - 그 외는 키 하나를 그대로 가리키는 것으로 본다
"""
import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
MESSAGES = ROOT / "src/main/resources/messages.yml"

# 코드가 직접 부르지 않아도 되는 키. 이유를 함께 적는다.
ALLOWED_UNUSED = {
    # Messages 가 모든 문구 앞에 붙이는 값이라, 경로로 불릴 일이 없다.
    "prefix",
}

# 문구 경로와 생김새가 같지만 뜻이 다른 문자열이 있는 파일.
# 저장 파일의 키 경로("quest.active" 같은)라 messages.yml 과 상관없다.
IGNORED_FILES = {
    "player/data/PlayerDataCodec.java",
}

# 파일 하나로 자를 수 없는 개별 예외. 설정 파일 안의 경로다.
IGNORED_LITERALS = {
    # levels.yml 의 life.curve 블록을 가리키는 설정 경로.
    "life.curve",
}

STRING = re.compile(r'"([^"\\\n]*)"')


def flatten(node, prefix=""):
    if isinstance(node, dict):
        for key, value in node.items():
            yield from flatten(value, f"{prefix}{key}.")
    else:
        yield prefix[:-1]


def main():
    tree = yaml.safe_load(MESSAGES.read_text(encoding="utf-8"))
    defined = set(flatten(tree))
    roots = set(tree)

    source_root = ROOT / "src/main/java"
    exact, prefixes = set(), set()
    for path in source_root.rglob("*.java"):
        if path.relative_to(source_root).as_posix().endswith(tuple(IGNORED_FILES)):
            continue
        for literal in STRING.findall(path.read_text(encoding="utf-8")):
            if literal in IGNORED_LITERALS or literal.endswith(".yml"):
                continue
            if literal.split(".", 1)[0] not in roots:
                continue
            # 이어 붙이는 접두사("objective." 처럼)는 한 마디여도 뜻이 분명하다.
            # 그대로 가리키는 경로는 최소 두 마디여야 설정 이름과 구별된다.
            if not literal.endswith(".") and "." not in literal:
                continue
            if not re.fullmatch(r"[a-z0-9]+(?:[.\-][a-z0-9]+)*\.?", literal):
                continue
            (prefixes if literal.endswith(".") else exact).add(literal)

    missing = sorted(key for key in exact if key not in defined)
    # 접두사는 그 아래 키가 하나라도 있어야 뜻이 있다.
    dead_prefixes = sorted(
        prefix for prefix in prefixes
        if not any(key.startswith(prefix) for key in defined))

    def covered(key):
        return (key in exact
                or key in ALLOWED_UNUSED
                or any(key.startswith(prefix) for prefix in prefixes))

    unused = sorted(key for key in defined if not covered(key))

    print(f"messages.yml 키 {len(defined)}개 "
          f"/ 코드가 그대로 가리키는 경로 {len(exact)}개 "
          f"/ 이어 붙이는 접두사 {len(prefixes)}개")
    if missing:
        print(f"\n코드에 있는데 messages.yml 에 없음 ({len(missing)}):")
        for key in missing:
            print("  " + key)
    if dead_prefixes:
        print(f"\n아래에 아무 키도 없는 접두사 ({len(dead_prefixes)}):")
        for prefix in dead_prefixes:
            print("  " + prefix)
    if unused:
        print(f"\nmessages.yml 에 있는데 아무도 안 씀 ({len(unused)}):")
        for key in unused:
            print("  " + key)
    if not missing and not dead_prefixes and not unused:
        print("\n빠진 키 없음, 죽은 접두사 없음, 안 쓰는 키 없음.")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
