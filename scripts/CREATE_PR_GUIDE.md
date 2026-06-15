# PR 생성 자동화 가이드

GitHub PR을 자동으로 생성하는 CLI 스크립트입니다.

## 사용법

```bash
./scripts/create-pr.sh <이슈번호> [PR타입] [base브랜치]
```

## 파라미터

### 필수 파라미터

- `<이슈번호>`: GitHub 이슈 번호
  - 예: `21`

### 선택 파라미터

- `[PR타입]`: PR 타입 지정
  - 가능한 값: `FEAT`, `FIX`, `REFACTOR`, `DOCS`, `CHORE`
  - 미입력 시 커밋 메시지 prefix에서 자동 추론
    - `feat` → `FEAT`
    - `fix` → `FIX`
    - `refactor` → `REFACTOR`
    - `docs` → `DOCS`
    - `chore`, `test` → `CHORE`
    - 추론 불가 → `CHORE`

- `[base브랜치]`: PR의 target 브랜치
  - 가능한 값: `dev`, `main`
  - 미입력 시 자동 결정
    - `origin/dev` 존재 시 → `dev`
    - 존재하지 않으면 → `main`

## 사용 예시

### 기본 사용 (이슈번호만)
```bash
./scripts/create-pr.sh 21
```
- PR 타입: 커밋 메시지에서 자동 추론
- base 브랜치: origin/dev 존재 여부로 자동 결정

### PR 타입 지정
```bash
./scripts/create-pr.sh 21 FEAT
```
- PR 타입: FEAT로 고정
- base 브랜치: 자동 결정

### 모든 파라미터 지정
```bash
./scripts/create-pr.sh 21 FEAT dev
```
- PR 타입: FEAT
- base 브랜치: dev

## PR 제목 규칙

자동 생성되는 PR 제목 포맷:
```
[#<이슈번호>][<TYPE>] <SUMMARY>
```

### 예시
```
[#21][FEAT] 주제 제안 및 투표 기능 구현
```

### SUMMARY 생성 규칙

1. **커밋이 있는 경우**
   - 가장 대표적인 커밋 제목 사용
   - 커밋 prefix (`feat:`, `fix:` 등) 제거
   - scope (`feat(auth):` 등) 제거
   - 35자 이내로 제한 (초과 시 `...`으로 truncate)

2. **커밋이 없는 경우**
   - 변경된 파일의 상위 폴더 기반으로 생성
   - 예: `src/main 변경`

## 자동 생성 기능

### 1. Git 정보 수집
- 현재 브랜치명
- base 브랜치 대비 커밋 메시지 목록 (`origin/base..HEAD`)
- base 브랜치 대비 변경 파일 목록 및 diff 통계 (`origin/base...HEAD`)

### 2. PR 템플릿 자동 채우기

#### 체크박스 자동 선택
- `FEAT` → "기능 추가" 체크
- `FIX` → "버그 수정" 체크
- `REFACTOR` → "코드 리팩토링" 체크
- `DOCS` → "문서 수정" 체크
- `CHORE` → "기타" 체크

#### 이슈 번호 자동 입력
```markdown
## 이슈 번호
- Closes #21
```
> `Closes #<이슈번호>` 키워드로 PR이 머지되면 해당 이슈가 자동으로 닫힙니다.

#### 주요 변경 사항 자동 생성
- 변경된 파일을 상위 폴더 1-2레벨로 그룹화
- 각 폴더별 통계 표시
  - 추가된 라인 수 (`+n`)
  - 삭제된 라인 수 (`-n`)
  - 변경된 파일 개수

포맷 예시:
```markdown
- `src/main`: +150/-20 (5 files) — 주제 제안 및 투표 기능 구현
- `src/test`: +80/-10 (3 files) — 주제 제안 및 투표 기능 구현
```

### 3. 파일 저장
- `PR_TITLE.txt`: PR 제목 (매 실행마다 자동 생성/갱신)
- `PR_BODY.md`: PR 본문 (**사용자가 직접 작성**, 아래 참고)

### 4. PR 본문 작성 방식 (2단계)

PR 본문은 스크립트가 정확하게 만들 수 없으므로 **사용자가 직접 작성**합니다.

1. **1차 실행 — 초안 생성**: `scripts/PR_BODY.md`가 없으면, 스크립트가 자동 생성 본문을 *초안*으로 저장하고 **PR을 만들지 않고 종료**합니다.
   ```
   ⚠️  PR 본문 초안을 scripts/PR_BODY.md에 생성했습니다.
   ℹ️  내용을 직접 작성/수정한 뒤, 같은 명령어로 스크립트를 다시 실행하면 PR이 생성됩니다.
   ```
2. **본문 편집**: `scripts/PR_BODY.md`를 열어 내용을 작성/수정합니다.
3. **2차 실행 — PR 생성**: `scripts/PR_BODY.md`가 존재하면, 그 내용을 **그대로 사용**해 PR을 생성합니다.
   - 생성에 성공하면 `scripts/PR_BODY.md`는 정리(삭제)되어, 다음 PR은 다시 새 초안부터 시작합니다.

### 5. PR 자동 생성 (gh CLI 사용)
스크립트가 자동으로 다음 명령을 실행:
```bash
gh pr create --base <base> --head <현재브랜치> --title "<PR제목>" --body-file PR_BODY.md
```

## 요구사항

### 필수
- **Git**: 버전 관리
- **GitHub CLI (`gh`)**: PR 자동 생성

### GitHub CLI 설치 및 설정

#### macOS
```bash
brew install gh
```

#### Linux
```bash
# Debian/Ubuntu
sudo apt install gh

# Fedora/CentOS/RHEL
sudo dnf install gh
```

#### 인증
```bash
gh auth login
```

인증 후 GitHub 계정 선택 및 토큰 생성을 진행합니다.

## 출력 예시

```
ℹ️  이슈 번호: #21
ℹ️  Base 브랜치: dev (자동 감지)
ℹ️  현재 브랜치: feat/topic-voting
ℹ️  Remote 정보를 업데이트 중...
ℹ️  PR 타입을 커밋 메시지에서 추론 중...
ℹ️  추론된 PR 타입: FEAT
✅ PR 제목: [#21][FEAT] 주제 제안 및 투표 기능 구현
✅ PR 제목이 scripts/PR_TITLE.txt에 저장되었습니다.
✅ 작성된 scripts/PR_BODY.md를 그대로 사용해 PR을 생성합니다.
ℹ️  gh CLI를 사용하여 PR을 생성합니다...
✅ PR이 성공적으로 생성되었습니다!
ℹ️  scripts/PR_BODY.md를 정리했습니다. (다음 PR은 새 초안부터 시작)
```
> `scripts/PR_BODY.md`가 없는 1차 실행에서는 초안만 생성하고 종료하며 PR을 만들지 않습니다.

## 문제 해결

### gh CLI가 설치되지 않은 경우
스크립트는 수동 PR 생성 명령어를 출력합니다:
```
⚠️  gh CLI가 설치되어 있지 않습니다.
ℹ️  다음 명령어로 수동으로 PR을 생성할 수 있습니다:

gh pr create --base dev --head feat/topic-voting --title "[#21][FEAT] 주제 제안 및 투표 기능 구현" --body-file PR_BODY.md
```

### base 브랜치가 존재하지 않는 경우
```
❌ ERROR: Base 브랜치 origin/dev가 존재하지 않습니다.
```
→ 올바른 base 브랜치를 명시적으로 지정하세요.

### 커밋이 없는 경우
```
⚠️  origin/dev..HEAD에 커밋이 없습니다.
```
→ 변경 사항이 있는지 확인하세요. 스크립트는 계속 진행되지만 제목이 제한적일 수 있습니다.

## 주의사항

1. **Remote 업데이트**: 스크립트는 자동으로 `git fetch origin`을 실행합니다.
2. **PR 제목**: `PR_TITLE.txt`는 매 실행마다 자동으로 갱신됩니다.
3. **PR 본문**: `PR_BODY.md`는 직접 작성합니다. 파일이 있으면 그대로 사용하고, 없으면 초안만 생성 후 종료합니다(2단계 흐름 참고). 따라서 본문을 다듬는 중이라면 같은 명령을 다시 실행해야 PR이 생성됩니다.
4. **브랜치 확인**: 현재 브랜치가 PR을 생성할 올바른 브랜치인지 확인하세요.
5. **이슈 존재 확인**: 지정한 이슈 번호가 GitHub에 실제로 존재하는지 확인하세요.