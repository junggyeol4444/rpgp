#!/usr/bin/env python3
"""리소스의 YAML 을 전부 열어 본다.

문법이 깨진 파일은 서버가 뜰 때야 드러난다. 그전에 잡는다.
YAML 1.1 은 on/off/yes/no 를 논릿값으로 읽으므로, 그렇게 읽힌 키가
있으면 함께 알린다. (messages.yml 의 state.on 이 실제로 그랬다)
"""
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"


def bool_keys(node, prefix=""):
    if isinstance(node, dict):
        for key, value in node.items():
            if isinstance(key, bool):
                yield f"{prefix}{key}"
            yield from bool_keys(value, f"{prefix}{key}.")


def main():
    files = sorted(RESOURCES.rglob("*.yml"))
    if not files:
        print("YAML 파일을 찾지 못했습니다.")
        return 1

    failures = 0
    for path in files:
        name = path.relative_to(RESOURCES).as_posix()
        try:
            tree = yaml.safe_load(path.read_text(encoding="utf-8"))
        except yaml.YAMLError as error:
            print(f"  깨짐  {name}: {error}")
            failures += 1
            continue
        bad = list(bool_keys(tree))
        if bad:
            print(f"  주의  {name}: 논릿값으로 읽힌 키 {bad}")
            failures += 1
            continue
        top = len(tree) if isinstance(tree, dict) else 0
        print(f"  정상  {name} (맨 윗줄 키 {top}개)")

    print()
    print(f"파일 {len(files)}개 / 문제 {failures}건")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
