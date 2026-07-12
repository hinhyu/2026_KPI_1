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

## 실행 방법

Gradle이 설치되어 있다면:

```bash
gradle bootRun --args='--repoPath=/Users/me/work/my-project'
```

출력 경로를 바꾸려면:

```bash
gradle bootRun --args='--repoPath=/Users/me/work/my-project --outputDir=/Users/me/Desktop/doc-output'
```

## 현재 가능한 기능

- Java 파일 스캔
- Controller / Service / Repository / Entity / DTO / Config 계층 분류
- Controller 기반 API endpoint 추출
- application.yml / properties key 추출
- README.md / docs/*.md 목록 수집
- 문서 생성용 prompt 생성

## 다음 개발 후보

1. `@RequestMapping(method = RequestMethod.GET)` 정교화
2. Service 호출 관계 분석
3. DTO 필드 분석
4. Git diff 기반 변경 영향도 분석
5. API 시나리오 YAML 자동 추천
6. AI API 연동으로 `onboarding.md` 자동 생성
