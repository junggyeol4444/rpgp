# RpgCore

마인크래프트 RPG 플러그인. 기획안 `rpg_plugin_design_v0.3` 과
구현 지시서 `rpg_plugin_implementation_guide_v1` 을 따른다.

현재 상태: **1~7단계** (골격과 전투 레벨 / 스탯과 전투 / 기본 직업 / 스킬 코어 / 퀘스트 / 경제 / 생활 트랙) + 지시서 3장 패키지 골격.

## Paper API 검증 결과

지시서 16장의 검증 항목을 실제 Paper 26.1.2 API 소스로 확인했다.
`repo.papermc.io` 를 못 쓰는 환경에서도 재현할 수 있도록
`tools/verify-against-paper.sh` 를 두었다. 이 스크립트는 Paper 저장소의
API 소스와 Maven Central 의 의존성을 받아 플러그인 전체를 컴파일한다.

**결과: 오류 0.** (Paper `ver/26.1.2` + VaultUnlockedAPI 기준, `-Xlint:all`)

| 16장 항목 | 결과 | 근거 |
|---|---|---|
| 1. paper-api 좌표·버전 | 확인 | Paper `ver/26.1.2` 의 `README.md` 에 실린 `io.papermc.paper:paper-api:26.1.2.build.+` 와 저장소 URL 그대로 |
| 2. plugin.yml / paper-plugin.yml | 확인 | 둘 다 있고 `PluginDescriptionFile` 은 사용 중단이 아니다. 부트스트랩 기능을 안 쓰므로 plugin.yml 유지 |
| 3. api-version 형식 | 확인 | 패치 단위까지 올라간다. `gradle.properties` 의 `apiVersion=26.1.2`. 26.x 태그는 26.1.2 하나뿐이라 이 값이 26.x 시작점 |
| 4. Paper 점프 이벤트 | 있음 | `com.destroystokyo.paper.event.player.PlayerJumpEvent`. 조합 수는 12가 아니라 **14** |
| 5. 좌클릭 허공 감지 | 절반 | `Action.LEFT_CLICK_AIR` 상수는 있다. 실제로 매번 서버까지 오는지는 구동해 봐야 안다 |
| 6. Citizens 26.x | 확인 | Citizens2 저장소에 `v26_1_R1`, `v26_2_R1` NMS 모듈이 있다 |
| 7. 원본 Vault 26.x | 확인 | VaultAPI 는 순수 인터페이스 5개라 버전을 타지 않는다. VaultUnlocked 2.20.2 는 플러그인 이름을 `Vault` 로 등록하는 드롭인 대체품이고, 레거시 `vault.economy.Economy` 와 신규 `vault2.economy.Economy` 를 함께 제공한다. 두 저장소의 레거시 인터페이스 메서드 목록이 완전히 같다 |

### 사용 중단 API 8곳

컴파일은 통과하지만 아래는 Adventure 컴포넌트 방식으로 대체하라는
사용 중단 표시가 붙어 있다. **제거 예정(forRemoval) 표시는 없다.**

| 파일 | API |
|---|---|
| `ui/actionbar/ActionBarChannel` | `Player.sendActionBar(String)` |
| `ui/tab/TabChannel` | `Player.setPlayerListHeaderFooter(String, String)` |
| `ui/scoreboard/ScoreboardChannel` | `Scoreboard.registerNewObjective(String, String, String)` |
| `ui/gui/Gui` | `Bukkit.createInventory(holder, int, String)` |
| `ui/gui/Icons` | `ItemMeta.setDisplayName` · `setLore` |
| `binding/SkillItems` | `ItemMeta.setDisplayName` · `setLore` |

`CommandSender.sendMessage(String)` 과 `Bukkit.createBossBar` 는 사용
중단이 아니다.

### 남은 확인 항목 (서버를 띄워야 알 수 있는 것)

- 허공 좌클릭이 실제로 매번 이벤트로 오는지
- 데미지 이벤트를 취소했을 때 넉백·피격 연출이 어떻게 되는지
- Citizens 를 실제로 붙였을 때의 동작
- `VaultUnlockedAPI:2.19` 가 `repo.codemc.io` 에 실제로 올라와 있는지.
  좌표와 버전은 그 저장소 pom.xml 에서 가져왔지만 codemc 에 닿을 수 없는
  환경이라 확인하지 못했다

## 빌드

```
./gradlew build
```

- Java toolchain 25 고정 (지시서 1장·2장, Paper 26.1.2 README 와 동일)
- 인코딩 UTF-8 고정

`repo.papermc.io` 에 닿을 수 없는 환경이라면:

```
tools/verify-against-paper.sh
```

## 1단계에서 동작하는 것

- 설정 로드·검증·리로드
- 저장소 추상화 + YAML 구현, 비동기 저장, 즉시/지연 저장 정책
- 플레이어 데이터 로드·저장 (지시서 7장 스키마 전체, 알 수 없는 키 보존)
- 전투 레벨·경험치 (지수형 곡선, 상한 없음)
- `/rpg info`
- `/rpg admin` : `reload` `save` `status` `debug` `setlevel` `exp`
  `datadump` `datareset`

## 2단계에서 동작하는 것

- 스탯 포인트 분배·회수, 능력치 → 파생 수치 환산 (`stats.yml`)
- 커스텀 데미지 파이프라인. 바닐라 데미지 이벤트를 취소하고 대체한다
- 내부 HP ↔ 하트 20칸 환산. 바닐라 최대 체력은 20으로 고정
- 스탯 초기화 (특수 재화 비용, 횟수마다 배수)
- 스탯 분배 GUI (`gui.yml` 로 제목·크기·아이콘 위치 지정)
- 상시 표시 기본형: 액션바 · 스코어보드 · 탭 · 보스바
- `/rpg stat`, `/rpg admin statpoint`, `/rpg admin statreset`

## 3단계에서 동작하는 것

- `jobs.yml` 해석. 기본 직업 7개와 1차·2차 분기 트리를 읽는다
  (분기는 읽어만 두고 쓰지 않는다. 전직은 8·9단계)
- 3레벨 기본 직업 선택. 되돌릴 수 없고 즉시 저장된다
- 직업 선택 GUI
- 직업별 레벨업 스탯 보정. 레벨이 오르면 파생 수치를 다시 계산한다
- `/rpg job`, `/rpg admin setjob`, `/rpg admin jobreset`

### 확인이 필요한 밸런스 판단

직업 보정을 **전투 레벨 전체**에 곱한다
(`statBonusPerLevel × level`). 직업은 3레벨에 고르므로 고르기 전
레벨분도 함께 들어간다. 기획서에 기준이 없어 가장 단순한 쪽을 택했다.
선택 이후 레벨만 세는 쪽이 맞다면 `StatService.jobBonus` 만 고치면 된다.

## 4단계에서 동작하는 것

- `skills.yml` 해석. `skills/` 디렉터리 스캔도 지원한다 (지시서 6장)
- 분기 트리 해금. 같은 분기에서 하나를 고르면 나머지는 영구 잠금
- 스킬 레벨 투자 (포인트 1개당 1레벨, 상한 9999, 고레벨 감쇠)
- 마나(초당 회복) · 쿨타임
- 효과 실행기 4종: `DAMAGE_CONE` `DAMAGE_TARGET` `DAMAGE_AREA` `HEAL_SELF`
- 스킬 아이템 슬롯 (시작 2칸, 전직마다 +1). 아이템은 버리기·이동 금지
- 키 조합 바인딩. 유지 상태(웅크림·달리기) + 순간 입력
- 스킬트리 GUI (분기 확정 시 한 번 더 확인), 스킬 등록 GUI
- 기본 직업 7개분 스킬 42개
- `/rpg skill`, `/rpg bind`, `/rpg admin skillpoint|skill|bindreset`

### skills.yml 은 밸런스 미확정

수치는 지시서 8장 예시값을 그대로 늘려 쓴 골격이다. 맞춰 본 값이
아니다. 파일 머리말에 조정 대상을 적어 두었다.

펫(tamer)과 설치물(trapper)은 해당 효과 타입이 아직 없어서 위력이
나가는 자리만 잡아 둔 상태다. 효과 타입이 생기면 그 직업의 `effects`
를 바꿔야 한다.

## 5단계에서 동작하는 것

- `quests.yml` 해석. `quests/` 디렉터리 스캔도 지원한다 (지시서 6장)
- 목표 판정 계층. 퀘스트 코드가 이벤트를 직접 듣지 않는다 (지시서 11장)
- 목표 4종: `KILL` `COLLECT` `REACH` `TALK`
- 수주 · 진행 · 자동 완료 · 보상 지급
- 일일 · 주간 리셋 (접속할 때 주기 확인)
- 퀘스트 GUI (수락 가능 / 진행 중 / 완료 탭)
- 지역 (`regions.yml`). `REACH` 목표가 쓴다
- `/rpg quest`, `/rpg admin quest|questreset|questcycle`

### TALK 목표는 아직 진행되지 않는다

지시서 16장 6번(Citizens 의 26.x 구동)이 확인되지 않아 NPC 연동
구현을 만들지 않았다. `NpcBridge` 인터페이스와 아무 것도 하지 않는
`NoNpcBridge` 만 있다. 확인되면 `npc/citizens` 에 구현을 넣고
부팅할 때 갈아끼우면 된다.

`quests.yml` 의 `example_quest` 는 지시서 8장 예시 그대로라 `TALK` 이
들어 있고, 지금 상태로는 끝낼 수 없다. 나머지 다섯 개는 NPC 없이
수주부터 보상까지 돌아간다.

### 리셋 기준

기획서에 리셋 기준 시각이 없어서, 정해진 시각 대신 "마지막 리셋으로부터
24시간 / 7일"로 판단한다. 서버 시간대를 가정하지 않기 위해서다.

## 6단계에서 동작하는 것

- Vault 어댑터. 원본 Vault 와 VaultUnlocked 어느 쪽이 있어도 붙고,
  둘 다 없어도 서버가 뜬다 (지시서 0장 6번)
- 특수 재화(`CurrencyService`). 경제 플러그인과 무관하게 항상 동작한다
- 스탯 초기화 비용과 퀘스트 보상이 `CurrencyService` 를 거친다
- `/rpg admin currency`, `/rpg admin status` 에 경제 연동 표시

### 의존성 하나로 양쪽을 덮는 이유

`VaultUnlockedAPI` 하나만 `compileOnly` 로 건다. 이 API 가 레거시
`net.milkbowl.vault.economy.Economy` 를 그대로 담고 있고, 원본 VaultAPI
저장소의 같은 인터페이스와 메서드 목록이 **완전히 일치**하는 것을
확인했다. 새 `net.milkbowl.vault2.economy.Economy` 도 함께 들어 있다.

원본 VaultAPI 는 Maven Central 에 없고(404) GitHub Packages 로만
배포되어 의존성으로 걸기 번거롭다. VaultUnlockedAPI 는 `repo.codemc.io`
에 있다.

찾는 순서는 `economy.yml` 의 `vault.preferUnlocked` 가 정한다.
Vault 클래스는 `Vault` 라는 이름의 플러그인이 실제로 있을 때만 건드리고,
없을 때 나는 `LinkageError` 까지 잡는다. VaultUnlocked 는 플러그인
이름을 `Vault` 로 등록하는 드롭인 대체품이라 이름 검사 하나로 덮인다.

## 7단계에서 동작하는 것

- 생활 · 채광 · 제작 · 연금 4트랙. 전투 레벨과 따로 오른다
- 획득원 이벤트 연결: 블록 파괴 · 낚시 · 조리 · 제작 · 양조
- 레벨업 보상 두 가지 (기획서 3장): 효율 상승과 해금
- 해금은 레벨을 건너뛰어도 이하 전부를 다시 훑어 채운다.
  접속할 때도 한 번 훑으므로 설정이 바뀌어 열릴 것이 늘어도 반영된다
- `/rpg life`, `/rpg admin life`

### 양조 경험치를 주는 시점

`BrewEvent` 는 `BlockEvent` 라 누가 만들었는지 알 수 없다.
(Paper 26.1.2 API 로 확인) 그래서 양조대에서 **결과물을 꺼내는 시점**에
꺼낸 사람에게 준다. 조리도 같은 이유로 화로에서 꺼낼 때 준다.

### 효율값은 계산과 표시까지만

`efficiency` 는 값을 계산해 `/rpg life` 에 보여주는 데까지만 되어 있다.
그 값을 실제 채집 속도에 어떻게 먹일지(신속 효과인지 자체 계산인지)는
기획서에 없어 정하지 않았다. 해금은 실제로 적용된다.

### 표시 API 주의

액션바·스코어보드·탭·보스바는 26.x 에서 확인되지 않은 API 를 쓴다.
채널마다 파일 하나에 몰아두었고, `config.yml` 의 `ui.channels` 에서
개별로 끌 수 있다. 한 채널이 연속으로 터지면 그 채널만 자동으로 꺼진다.

## 아직 없는 것

지시서 14장의 2단계 이후. 각 패키지의 `package-info.java` 에 어느
단계에서 무엇이 들어오는지 적어두었다.

## 알려진 제약

- `/rpg admin setjob` 은 `base` 만 받는다. 1차·2차는 8·9단계에서 넓힌다.
- `jobs.yml` 의 기본 직업 7개 중 `swordsman` 외에는 `statBonusPerLevel`
  이 비어 있다. 밸런스 미정이라 채우지 않았다.
- `/rpg admin questcycle` 은 접속 중인 플레이어만 리셋한다.
- 제작은 시프트 클릭으로 여러 개를 만들어도 한 번으로 센다.
  정확한 개수는 인벤토리 여유까지 봐야 나오는데, 그 계산을 클릭 이벤트
  안에서 하는 것은 지시서 11장 [주의]에 어긋난다.
- 플레이어가 직접 놓은 블록을 되캐는 것을 걸러내지 않는다.
  설치 이력을 따로 들고 있어야 하는데 기획서에 언급이 없다.
- 관리자 명령은 접속 중인 플레이어만 대상으로 한다. 오프라인 대상
  조작은 저장소 비동기 읽기가 필요하며 아직 넣지 않았다.
- 평타 위력에 무기 기여분이 없다. 아이템 체계가 생기는 단계에서 붙인다.
- 바닐라 인챈트 보정(지시서 9장 3번)과 지역·몬스터 보정(6번)은
  자리만 있고 1.0 을 돌려준다.
- 몬스터는 아직 바닐라 체력을 쓴다. 커스텀 몬스터 단계에서 내부 HP 로 옮긴다.
- 몬스터 처치 경험치는 `mobs.yml` 이 붙는 단계에서 연결한다.
- 데미지 이벤트를 취소하므로 넉백·피격 연출이 사라진다. 실제 구동으로
  확인한 뒤 `CombatListener` 에서 보완한다.
