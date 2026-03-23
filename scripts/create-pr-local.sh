#!/bin/bash

# GitHub PR 생성 자동화 스크립트
# 사용법: ./scripts/create-pr-local.sh [이슈번호] [PR타입] [base브랜치]

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

# ============================================
# 인자 파싱
# ============================================

# 인자 규칙
# 1) 첫 번째 인자가 숫자면: [이슈번호] [PR타입] [base브랜치]
# 2) 첫 번째 인자가 숫자가 아니면: [PR타입] [base브랜치]
if [ -n "$1" ] && [[ "$1" =~ ^[0-9]+$ ]]; then
    ISSUE_NUMBER="$1"
    PR_TYPE="${2:-}"
    BASE_BRANCH="${3:-}"
    print_info "이슈 번호: #${ISSUE_NUMBER}"
else
    ISSUE_NUMBER=""
    PR_TYPE="${1:-}"
    BASE_BRANCH="${2:-}"
    print_info "이슈 번호: 없음"
fi

# ============================================
# Base 브랜치 결정
# ============================================

if [ -z "$BASE_BRANCH" ]; then
    # local/dev 존재 확인
    if git show-ref --verify --quiet refs/remotes/local/dev; then
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

# local remote에서 GitHub 저장소(owner/repo) 추출
REMOTE_URL=$(git remote get-url local 2>/dev/null || echo "")
if [ -z "$REMOTE_URL" ]; then
    print_error "local remote URL을 확인할 수 없습니다."
    exit 1
fi

REPO_SLUG=$(echo "$REMOTE_URL" | sed -E 's#^(git@github\.com:|https://github\.com/)([^/]+/[^/]+)(\.git)?$#\2#')
if [ "$REPO_SLUG" = "$REMOTE_URL" ]; then
    print_error "local remote URL에서 GitHub 저장소를 추출할 수 없습니다: ${REMOTE_URL}"
    exit 1
fi
print_info "대상 저장소: ${REPO_SLUG}"

# remote fetch
print_info "Remote 정보를 업데이트 중..."
git fetch local --quiet

# base 브랜치 존재 확인
if ! git show-ref --verify --quiet refs/remotes/local/${BASE_BRANCH}; then
    print_error "Base 브랜치 local/${BASE_BRANCH}가 존재하지 않습니다."
    exit 1
fi

# 커밋 메시지 목록 (local/base..HEAD)
COMMIT_MESSAGES=$(git log local/${BASE_BRANCH}..HEAD --pretty=format:"%s" 2>/dev/null || echo "")

if [ -z "$COMMIT_MESSAGES" ]; then
    print_warning "local/${BASE_BRANCH}..HEAD에 커밋이 없습니다."
fi

# 변경 파일 통계
CHANGED_FILES=$(git diff --name-only local/${BASE_BRANCH}...HEAD 2>/dev/null || echo "")

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

if [ -n "$ISSUE_NUMBER" ]; then
    PR_TITLE="[#${ISSUE_NUMBER}][${PR_TYPE}] ${SUMMARY}"
else
    PR_TITLE="[${PR_TYPE}] ${SUMMARY}"
fi

print_success "PR 제목: ${PR_TITLE}"

# ============================================
# 파일 저장
# ============================================

# 스크립트 디렉토리 경로
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PR_BODY_FILE="${SCRIPT_DIR}/PR_BODY.md"

echo "$PR_TITLE" > "${SCRIPT_DIR}/PR_TITLE.txt"

print_success "PR 제목이 scripts/PR_TITLE.txt에 저장되었습니다."

if [ ! -f "$PR_BODY_FILE" ]; then
    print_error "PR 본문 파일이 없습니다: scripts/PR_BODY.md"
    print_info "직접 작성한 PR 본문을 scripts/PR_BODY.md에 저장한 뒤 다시 실행해주세요."
    exit 1
fi
print_info "PR 본문 파일 사용: scripts/PR_BODY.md (직접 작성본)"

# ============================================
# PR 생성
# ============================================

if command -v gh &> /dev/null; then
    print_info "gh CLI를 사용하여 PR을 생성합니다..."

    # PR 생성
    if gh pr create \
        --repo "$REPO_SLUG" \
        --base "$BASE_BRANCH" \
        --head "$CURRENT_BRANCH" \
        --title "$PR_TITLE" \
        --body-file "$PR_BODY_FILE"; then
        print_success "PR이 성공적으로 생성되었습니다!"
    else
        print_error "PR 생성에 실패했습니다."
        exit 1
    fi
else
    print_warning "gh CLI가 설치되어 있지 않습니다."
    print_info "다음 명령어로 수동으로 PR을 생성할 수 있습니다:"
    echo ""
    echo "gh pr create --repo $REPO_SLUG --base $BASE_BRANCH --head $CURRENT_BRANCH --title \"$PR_TITLE\" --body-file scripts/PR_BODY.md"
    echo ""
fi
