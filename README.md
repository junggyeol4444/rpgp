# RpgCore

마인크래프트 RPG 플러그인. 기획안 `rpg_plugin_design_v0.3` 과
구현 지시서 `rpg_plugin_implementation_guide_v1` 을 따른다.

현재 상태: **1단계 (골격과 전투 레벨)** + 지시서 3장 패키지 골격.

## 착수 전에 반드시 채워야 하는 값

지시서 16장의 검증 항목 중 아래 세 가지는 값이 비어 있어서
**지금 상태로는 빌드도, 서버 로드도 되지 않는다.** 실제로 확인한 값을
넣어야 한다. 기억으로 적지 말 것.

| 항목 | 위치 | 지시서 |
|---|---|---|
| Paper 저장소 URL | `build.gradle` `repositories` | 16장 1번 |
| paper-api 좌표·버전 문자열 | `build.gradle` `dependencies` | 16장 1번 |
| plugin.yml / paper-plugin.yml 중 권장 방식 | `src/main/resources/plugin.yml` | 16장 2번 |
| api-version 값 | `src/main/resources/plugin.yml` | 16장 3번 |

나머지 검증 항목(점프 이벤트, 좌클릭 허공 감지, Citizens, 원본 Vault)은
해당 기능을 만드는 단계에서 확인한다. 코드에 `[확인 필요]` 주석으로
표시해 두었다.

## 빌드

```
./gradlew build
```

- Java toolchain 25 고정 (지시서 1장·2장)
- 인코딩 UTF-8 고정

## 1단계에서 동작하는 것

- 설정 로드·검증·리로드 (`config.yml`, `levels.yml`, `messages.yml`)
- 저장소 추상화 + YAML 구현, 비동기 저장, 즉시/지연 저장 정책
- 플레이어 데이터 로드·저장 (지시서 7장 스키마 전체, 알 수 없는 키 보존)
- 전투 레벨·경험치 (지수형 곡선, 상한 없음)
- `/rpg info`
- `/rpg admin` : `reload` `save` `status` `debug` `setlevel` `exp`
  `datadump` `datareset`

## 아직 없는 것

지시서 14장의 2단계 이후. 각 패키지의 `package-info.java` 에 어느
단계에서 무엇이 들어오는지 적어두었다.

## 알려진 제약

- 관리자 명령은 접속 중인 플레이어만 대상으로 한다. 오프라인 대상
  조작은 저장소 비동기 읽기가 필요하며 아직 넣지 않았다.
- `skills.yml` / `quests.yml` 의 디렉터리 스캔(지시서 6장)은 해당
  단계(4·5단계)에서 붙인다.
