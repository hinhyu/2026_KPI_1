# Doc Insight MVP

문서 자동화 기능을 시작하기 위한 최소 프로젝트입니다.

## 목표

로컬 Spring Boot/Java repository를 스캔해서 아래 파일을 생성합니다.

- `outputs/project-summary.json`
- `outputs/api-endpoints.json`
- `outputs/onboarding-prompt.md`

생성된 `onboarding-prompt.md` 내용을 AI 모델에 입력하면 온보딩 문서 초안을 만들 수 있습니다.

## 기술 스택

- Java 21
- Spring Boot 3.3.5
- Gradle Kotlin DSL
- JavaParser
- Jackson YAML

현재 버전은 별도의 DB, Redis, Kafka, Docker, Node.js 설치가 필요하지 않습니다.

## 프로젝트 구조

```text
doc-insight-mvp/
├── gradle/
│   └── wrapper/
├── sample-target/              # 실행 검증용 샘플 프로젝트
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── outputs/                    # 실행 결과 생성 위치
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

## 실행 전 확인

Java 버전을 확인합니다.

```bash
java -version
```

Java 21이 표시되어야 합니다.

Gradle Wrapper 동작 여부를 확인합니다.

```bash
./gradlew --version
```

## 빌드

```bash
./gradlew clean build
```

## 샘플 프로젝트 분석

프로젝트에 포함된 `sample-target`을 분석합니다.

```bash
./gradlew bootRun --args='--repoPath=sample-target'
```

## 실제 프로젝트 분석

`repoPath`에는 분석할 Java/Spring 프로젝트의 로컬 경로를 입력합니다.

절대 경로 예시:

```bash
./gradlew bootRun \
  --args='--repoPath=/Users/user/workspace/target-project'
```

상대 경로 예시:

```bash
./gradlew bootRun \
  --args='--repoPath=../target-project'
```

현재 버전은 로컬 폴더 경로만 지원합니다.

지원하지 않는 입력:

- Git 저장소 URL
- 웹 서비스 URL
- ZIP 파일 URL
- S3 URL

Git 저장소를 분석하려면 먼저 로컬에 Clone한 뒤 해당 경로를 입력해야 합니다.

## 출력 경로 지정

기본 출력 경로는 프로젝트 루트의 `outputs` 폴더입니다.

다른 경로를 지정하려면 `outputDir`을 전달합니다.

```bash
./gradlew bootRun \
  --args='--repoPath=sample-target --outputDir=my-outputs'
```

## 생성되는 산출물

실행이 완료되면 기본적으로 `outputs` 폴더에 아래 파일이 생성됩니다.

### `project-summary.json`

분석 대상 프로젝트의 전체 구조 요약입니다.

포함 정보:

- Java 파일/타입 수
- 패키지명
- 클래스/인터페이스/Enum 정보
- 클래스 Annotation
- 주요 메서드
- 설정 파일 Key
- Markdown 파일 목록

### `api-endpoints.json`

Controller에서 추출한 API 목록입니다.

포함 정보:

- Controller 클래스
- Java 메서드명
- HTTP Method (`@RequestMapping(method = ...)` 포함)
- API Path (`path`/`value` 배열 조합 지원)
- RequestBody / Response 타입
- PathVariable / RequestParam 이름
- 소스 파일 경로

### `onboarding-prompt.md`

분석 결과를 기반으로 AI에 전달할 온보딩 문서 생성용 프롬프트입니다.

현재 버전에서는 AI API를 직접 호출하지 않습니다.

```text
프로젝트 소스 분석
→ JSON 산출물 생성
→ onboarding-prompt.md 생성
→ AI에 프롬프트 입력
→ 최종 온보딩 문서 작성
```

## 실행 결과 확인

```bash
ls -al outputs
```

JSON 결과 확인:

```bash
cat outputs/project-summary.json
cat outputs/api-endpoints.json
```

JSON을 보기 좋게 출력하려면:

```bash
cat outputs/project-summary.json | python3 -m json.tool
```

## 현재 제한 사항

- 로컬 폴더만 분석 가능
- 상수/변수로 조합된 동적 URL은 정확도가 낮을 수 있음
- 설정 파일은 값이 아닌 Key 중심으로 수집
- AI API 직접 연동은 미구현
- DB 저장 및 RAG 기능은 미구현

## 개발 예정 항목

- Git URL 입력 및 자동 Clone
- 응답 타입 제네릭/래퍼 분석 고도화
- 환경설정 값 마스킹 및 프로파일별 비교
- AI API 연동
- 온보딩 문서 자동 생성
- 분석 결과 DB 저장
- 문서 검색 및 RAG 기능
- 웹 UI 제공

## TODO

```diff
- 기타 등등의 규칙 및 개발 예정은 회의를 통해 확정... 
```
