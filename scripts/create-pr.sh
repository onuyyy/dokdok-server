#!/bin/bash

# GitHub PR 생성 자동화 스크립트
# 사용법: ./scripts/create-pr.sh <이슈번호> [PR타입] [base브랜치]

set -e

# ============================================
# 색상 정의
# ============================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================
# 함수 정의
# ============================================

print_error() {
    echo -e "${RED}❌ ERROR: $1${NC}" >&2
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

extract_repo_full_name() {
    local remote_url="$1"

    remote_url="${remote_url%.git}"

    if [[ "$remote_url" =~ ^git@github\.com:(.+/[^/]+)$ ]]; then
        echo "${BASH_REMATCH[1]}"
        return 0
    fi

    if [[ "$remote_url" =~ ^https://github\.com/(.+/[^/]+)$ ]]; then
        echo "${BASH_REMATCH[1]}"
        return 0
    fi

    return 1
}

# ============================================
# 인자 파싱
# ============================================

if [ -z "$1" ]; then
    print_error "이슈 번호가 필요합니다."
    echo "사용법: $0 <이슈번호> [PR타입] [base브랜치]"
    echo "예시: $0 21"
    echo "예시: $0 21 FEAT"
    echo "예시: $0 21 FEAT dev"
    exit 1
fi

ISSUE_NUMBER="$1"
PR_TYPE="${2:-}"
BASE_BRANCH="${3:-}"

print_info "이슈 번호: #${ISSUE_NUMBER}"

# ============================================
# Base 브랜치 결정
# ============================================

if [ -z "$BASE_BRANCH" ]; then
    # origin/dev 존재 확인
    if git show-ref --verify --quiet refs/remotes/origin/dev; then
        BASE_BRANCH="dev"
        print_info "Base 브랜치: dev (자동 감지)"
    else
        BASE_BRANCH="main"
        print_info "Base 브랜치: main (기본값)"
    fi
else
    print_info "Base 브랜치: ${BASE_BRANCH} (사용자 지정)"
fi

# ============================================
# Git 정보 수집
# ============================================

# 현재 브랜치
CURRENT_BRANCH=$(git branch --show-current)
if [ -z "$CURRENT_BRANCH" ]; then
    print_error "현재 브랜치를 확인할 수 없습니다."
    exit 1
fi
print_info "현재 브랜치: ${CURRENT_BRANCH}"

# remote fetch
print_info "Remote 정보를 업데이트 중..."
git fetch origin --quiet

ORIGIN_URL=$(git remote get-url origin 2>/dev/null || echo "")
REPO_FULL_NAME=$(extract_repo_full_name "$ORIGIN_URL" || true)

if [ -z "$REPO_FULL_NAME" ]; then
    print_error "origin remote에서 GitHub 저장소 정보를 파싱할 수 없습니다: ${ORIGIN_URL}"
    exit 1
fi

print_info "대상 저장소: ${REPO_FULL_NAME}"

# base 브랜치 존재 확인
if ! git show-ref --verify --quiet refs/remotes/origin/${BASE_BRANCH}; then
    print_error "Base 브랜치 origin/${BASE_BRANCH}가 존재하지 않습니다."
    exit 1
fi

# 커밋 메시지 목록 (origin/base..HEAD)
COMMIT_MESSAGES=$(git log origin/${BASE_BRANCH}..HEAD --pretty=format:"%s" 2>/dev/null || echo "")

if [ -z "$COMMIT_MESSAGES" ]; then
    print_warning "origin/${BASE_BRANCH}..HEAD에 커밋이 없습니다."
fi

# 변경 파일 통계
CHANGED_FILES=$(git diff --name-only origin/${BASE_BRANCH}...HEAD 2>/dev/null || echo "")
NUMSTAT=$(git diff --numstat origin/${BASE_BRANCH}...HEAD 2>/dev/null || echo "")

# ============================================
# PR 타입 추론 (다수결 또는 우선순위)
# ============================================

if [ -z "$PR_TYPE" ]; then
    print_info "PR 타입을 커밋 메시지에서 추론 중..."

    # 모든 커밋의 타입 카운트
    feat_count=0
    fix_count=0
    refactor_count=0
    docs_count=0
    chore_count=0

    while IFS= read -r commit; do
        if [[ "$commit" =~ ^feat ]]; then
            feat_count=$((feat_count + 1))
        elif [[ "$commit" =~ ^fix ]]; then
            fix_count=$((fix_count + 1))
        elif [[ "$commit" =~ ^refactor ]]; then
            refactor_count=$((refactor_count + 1))
        elif [[ "$commit" =~ ^docs ]]; then
            docs_count=$((docs_count + 1))
        elif [[ "$commit" =~ ^(chore|test) ]]; then
            chore_count=$((chore_count + 1))
        fi
    done <<< "$COMMIT_MESSAGES"

    # 가장 많은 타입 선택 (우선순위: feat > fix > refactor > docs > chore)
    max_count=0
    PR_TYPE="CHORE"

    if [ $chore_count -ge $max_count ] && [ $chore_count -gt 0 ]; then
        max_count=$chore_count
        PR_TYPE="CHORE"
    fi

    if [ $docs_count -ge $max_count ] && [ $docs_count -gt 0 ]; then
        max_count=$docs_count
        PR_TYPE="DOCS"
    fi

    if [ $refactor_count -ge $max_count ] && [ $refactor_count -gt 0 ]; then
        max_count=$refactor_count
        PR_TYPE="REFACTOR"
    fi

    if [ $fix_count -ge $max_count ] && [ $fix_count -gt 0 ]; then
        max_count=$fix_count
        PR_TYPE="FIX"
    fi

    if [ $feat_count -ge $max_count ] && [ $feat_count -gt 0 ]; then
        max_count=$feat_count
        PR_TYPE="FEAT"
    fi

    if [ $max_count -eq 0 ]; then
        print_warning "PR 타입을 추론할 수 없어 CHORE로 설정합니다."
    else
        print_info "추론된 PR 타입: ${PR_TYPE} (feat:${feat_count} fix:${fix_count} refactor:${refactor_count} docs:${docs_count} chore:${chore_count})"
    fi
else
    print_info "PR 타입: ${PR_TYPE} (사용자 지정)"
fi

# ============================================
# PR 제목 생성 (여러 커밋 종합 요약)
# ============================================

SUMMARY=""

if [ -n "$COMMIT_MESSAGES" ]; then
    # 커밋 개수 확인
    COMMIT_COUNT=$(echo "$COMMIT_MESSAGES" | wc -l | tr -d ' ')

    if [ "$COMMIT_COUNT" -eq 1 ]; then
        # 커밋 1개: 그대로 사용
        SUMMARY=$(echo "$COMMIT_MESSAGES" | sed -E 's/^[a-z]+(\([^)]+\))? *: *//')
    else
        # 커밋 2개 이상: 메시지 일부를 조합해 요약
        print_info "커밋 ${COMMIT_COUNT}개를 종합 요약 중..."

        # prefix 제거 후 중복 제거한 메시지 수집 (최신순)
        cleaned_list=""
        unique_count=0
        while IFS= read -r commit; do
            cleaned=$(echo "$commit" | sed -E 's/^[a-z]+(\([^)]+\))? *: *//')
            if ! echo "$cleaned_list" | grep -Fxq "$cleaned"; then
                cleaned_list="${cleaned_list}${cleaned}\n"
                unique_count=$((unique_count + 1))
            fi
        done <<< "$COMMIT_MESSAGES"

        first_cleaned=$(echo -e "$cleaned_list" | head -n 1)
        second_cleaned=$(echo -e "$cleaned_list" | head -n 2 | tail -n 1)

        if [ "$unique_count" -eq 2 ]; then
            SUMMARY="${first_cleaned}, ${second_cleaned}"
        else
            SUMMARY="${first_cleaned} 외 $((unique_count - 1))건"
        fi
    fi

    # 50자 제한 (자연스럽게 끊기)
    if [ ${#SUMMARY} -gt 50 ]; then
        # 45자까지 자르고 마지막 공백에서 끊기
        SUMMARY_SHORT="${SUMMARY:0:45}"
        # 마지막 공백 찾기
        if [[ "$SUMMARY_SHORT" =~ (.*)\ (.*)$ ]]; then
            SUMMARY="${BASH_REMATCH[1]}"
        else
            SUMMARY="${SUMMARY:0:47}..."
        fi
    fi
else
    # 커밋이 없으면 변경 파일 기반으로 요약 생성
    if [ -n "$CHANGED_FILES" ]; then
        TOP_DIR=$(echo "$CHANGED_FILES" | head -n 1 | cut -d'/' -f1-2)
        SUMMARY="${TOP_DIR} 변경"
    else
        SUMMARY="변경 사항"
    fi
fi

PR_TITLE="[#${ISSUE_NUMBER}][${PR_TYPE}] ${SUMMARY}"

print_success "PR 제목: ${PR_TITLE}"

# ============================================
# 체크박스 자동 선택
# ============================================

case "$PR_TYPE" in
    FEAT)
        CHECKBOX_FEAT="- [x] 기능 추가"
        CHECKBOX_FIX="- [ ] 버그 수정"
        CHECKBOX_REFACTOR="- [ ] 코드 리팩토링"
        CHECKBOX_DOCS="- [ ] 문서 수정"
        CHECKBOX_CHORE="- [ ] 기타 (설명)"
        ;;
    FIX)
        CHECKBOX_FEAT="- [ ] 기능 추가"
        CHECKBOX_FIX="- [x] 버그 수정"
        CHECKBOX_REFACTOR="- [ ] 코드 리팩토링"
        CHECKBOX_DOCS="- [ ] 문서 수정"
        CHECKBOX_CHORE="- [ ] 기타 (설명)"
        ;;
    REFACTOR)
        CHECKBOX_FEAT="- [ ] 기능 추가"
        CHECKBOX_FIX="- [ ] 버그 수정"
        CHECKBOX_REFACTOR="- [x] 코드 리팩토링"
        CHECKBOX_DOCS="- [ ] 문서 수정"
        CHECKBOX_CHORE="- [ ] 기타 (설명)"
        ;;
    DOCS)
        CHECKBOX_FEAT="- [ ] 기능 추가"
        CHECKBOX_FIX="- [ ] 버그 수정"
        CHECKBOX_REFACTOR="- [ ] 코드 리팩토링"
        CHECKBOX_DOCS="- [x] 문서 수정"
        CHECKBOX_CHORE="- [ ] 기타 (설명)"
        ;;
    CHORE)
        CHECKBOX_FEAT="- [ ] 기능 추가"
        CHECKBOX_FIX="- [ ] 버그 수정"
        CHECKBOX_REFACTOR="- [ ] 코드 리팩토링"
        CHECKBOX_DOCS="- [ ] 문서 수정"
        CHECKBOX_CHORE="- [x] 기타 (설명)"
        ;;
    *)
        CHECKBOX_FEAT="- [ ] 기능 추가"
        CHECKBOX_FIX="- [ ] 버그 수정"
        CHECKBOX_REFACTOR="- [ ] 코드 리팩토링"
        CHECKBOX_DOCS="- [ ] 문서 수정"
        CHECKBOX_CHORE="- [ ] 기타 (설명)"
        ;;
esac

# ============================================
# 주요 변경 사항 생성
# ============================================

MAIN_CHANGES=""

if [ -n "$NUMSTAT" ]; then
    # 폴더별로 변경 사항 그룹화 (bash 3.2 호환)
    # 1. 폴더 추출 및 통계와 함께 임시 파일 생성
    temp_stats=$(mktemp)

    while IFS=$'\t' read -r added deleted file; do
        if [ -z "$file" ]; then
            continue
        fi

        # 상위 폴더 추출 (src/main/java, src/test/java는 더 깊게)
        if [[ "$file" == src/main/java/* ]] || [[ "$file" == src/test/java/* ]]; then
            folder=$(echo "$file" | cut -d'/' -f1-6)  # com/dokdok/meeting 레벨까지
        elif [[ "$file" == src/main/* ]] || [[ "$file" == src/test/* ]]; then
            folder=$(echo "$file" | cut -d'/' -f1-3)
        elif [[ "$file" == */* ]]; then
            folder=$(echo "$file" | cut -d'/' -f1-2)
        else
            folder=$(echo "$file" | cut -d'/' -f1)
        fi

        # numstat에서 바이너리 파일은 "-"가 들어올 수 있음
        if ! [[ "$added" =~ ^[0-9]+$ ]]; then
            added=0
        fi
        if ! [[ "$deleted" =~ ^[0-9]+$ ]]; then
            deleted=0
        fi

        # 폴더:added:deleted 형태로 저장
        echo "${folder}:${added}:${deleted}" >> "$temp_stats"
    done <<< "$NUMSTAT"

    # 2. 폴더별로 정렬 및 합산
    if [ -s "$temp_stats" ]; then
        sorted_stats=$(sort "$temp_stats")

        prev_folder=""
        total_added=0
        total_deleted=0
        file_count=0

        while IFS=':' read -r folder added deleted; do
            if [ "$folder" = "$prev_folder" ] || [ -z "$prev_folder" ]; then
                # 같은 폴더면 누적
                total_added=$((total_added + added))
                total_deleted=$((total_deleted + deleted))
                file_count=$((file_count + 1))
                prev_folder="$folder"
            else
                # 다른 폴더면 이전 폴더 출력 후 초기화
                # 커밋 메시지에서 요약 추출
                summary="요구사항 반영"
                if [ -n "$COMMIT_MESSAGES" ]; then
                    commit_summary=$(git log origin/${BASE_BRANCH}..HEAD --pretty=format:"%s" -- "$prev_folder" 2>/dev/null | head -n 2 | \
                        sed -E 's/^[a-z]+(\([^)]+\))? *: *//' || true)
                    commit_count=$(git log origin/${BASE_BRANCH}..HEAD --pretty=format:"%s" -- "$prev_folder" 2>/dev/null | wc -l | tr -d ' ')
                    if [ "$commit_count" -eq 1 ]; then
                        summary=$(echo "$commit_summary" | head -n 1)
                    elif [ "$commit_count" -gt 1 ]; then
                        summary="$(echo "$commit_summary" | head -n 1) 외 $((commit_count - 1))건"
                    fi
                fi

                MAIN_CHANGES="${MAIN_CHANGES}- \`${prev_folder}\`: +${total_added}/-${total_deleted} (${file_count} files) — ${summary}\n"

                # 새 폴더로 초기화
                prev_folder="$folder"
                total_added=$added
                total_deleted=$deleted
                file_count=1
            fi
        done <<< "$sorted_stats"

        # 마지막 폴더 출력
        if [ -n "$prev_folder" ]; then
            summary="요구사항 반영"
            if [ -n "$COMMIT_MESSAGES" ]; then
                commit_summary=$(git log origin/${BASE_BRANCH}..HEAD --pretty=format:"%s" -- "$prev_folder" 2>/dev/null | head -n 2 | \
                    sed -E 's/^[a-z]+(\([^)]+\))? *: *//' || true)
                commit_count=$(git log origin/${BASE_BRANCH}..HEAD --pretty=format:"%s" -- "$prev_folder" 2>/dev/null | wc -l | tr -d ' ')
                if [ "$commit_count" -eq 1 ]; then
                    summary=$(echo "$commit_summary" | head -n 1)
                elif [ "$commit_count" -gt 1 ]; then
                    summary="$(echo "$commit_summary" | head -n 1) 외 $((commit_count - 1))건"
                fi
            fi

            MAIN_CHANGES="${MAIN_CHANGES}- \`${prev_folder}\`: +${total_added}/-${total_deleted} (${file_count} files) — ${summary}\n"
        fi
    fi

    # 임시 파일 삭제
    rm -f "$temp_stats"
else
    MAIN_CHANGES="- 변경 사항 없음"
fi

# ============================================
# PR 본문 생성
# ============================================

PR_BODY=$(cat <<EOF
## PR 요약
> 이 PR이 어떤 변경을 하는지 간단히 설명하고, 체크 표시는 괄호 사이에 소문자 'x'를 삽입하세요.

${CHECKBOX_FEAT}
${CHECKBOX_FIX}
${CHECKBOX_REFACTOR}
${CHECKBOX_DOCS}
${CHECKBOX_CHORE}

---

## 이슈 번호
- Closes #${ISSUE_NUMBER}

---

## 주요 변경 사항
> 주요 파일, 로직, 컴포넌트 등을 구체적으로 적어주세요.

$(echo -e "$MAIN_CHANGES")

---

## 참고 사항
> 리뷰어가 알아야 할 추가 정보, 테스트 방법 등을 작성해주세요.

예:
- 테스트 계정 정보
- 관련 API 엔드포인트
- 로컬 테스트 방법

---
EOF
)

# ============================================
# 파일 저장
# ============================================

# 스크립트 디렉토리 경로
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "$PR_TITLE" > "${SCRIPT_DIR}/PR_TITLE.txt"
print_success "PR 제목이 scripts/PR_TITLE.txt에 저장되었습니다."

# ============================================
# PR 본문: 사용자가 직접 작성한 내용을 사용 (2단계)
# ============================================
#   1) scripts/PR_BODY.md 가 없으면 → 자동 생성 본문을 '초안'으로 저장하고 종료한다.
#      사용자가 내용을 다듬은 뒤 같은 명령어로 다시 실행하면 그 본문으로 PR을 생성한다.
#   2) scripts/PR_BODY.md 가 있으면 → 그 내용을 그대로 사용해 PR을 생성한다.
if [ ! -s "${SCRIPT_DIR}/PR_BODY.md" ]; then
    echo "$PR_BODY" > "${SCRIPT_DIR}/PR_BODY.md"
    print_warning "PR 본문 초안을 scripts/PR_BODY.md에 생성했습니다."
    print_info "내용을 직접 작성/수정한 뒤, 같은 명령어로 스크립트를 다시 실행하면 PR이 생성됩니다."
    exit 0
fi

print_success "작성된 scripts/PR_BODY.md를 그대로 사용해 PR을 생성합니다."

# ============================================
# PR 생성
# ============================================

if command -v gh &> /dev/null; then
    print_info "gh CLI를 사용하여 PR을 생성합니다..."

    # PR 생성
    if gh pr create \
        --repo "$REPO_FULL_NAME" \
        --base "$BASE_BRANCH" \
        --head "$CURRENT_BRANCH" \
        --title "$PR_TITLE" \
        --body-file "${SCRIPT_DIR}/PR_BODY.md"; then
        print_success "PR이 성공적으로 생성되었습니다!"
        # 다음 PR 작성 시 이전 본문이 그대로 재사용되지 않도록 정리한다.
        # (작성한 본문은 이미 생성된 PR에 반영되어 있다.)
        rm -f "${SCRIPT_DIR}/PR_BODY.md"
        print_info "scripts/PR_BODY.md를 정리했습니다. (다음 PR은 새 초안부터 시작)"
    else
        print_error "PR 생성에 실패했습니다."
        exit 1
    fi
else
    print_warning "gh CLI가 설치되어 있지 않습니다."
    print_info "다음 명령어로 수동으로 PR을 생성할 수 있습니다:"
    echo ""
    echo "gh pr create --repo $REPO_FULL_NAME --base $BASE_BRANCH --head $CURRENT_BRANCH --title \"$PR_TITLE\" --body-file scripts/PR_BODY.md"
    echo ""
fi
