BEGIN;

WITH parents AS (
INSERT INTO keyword (keyword_type, keyword_name, parent_id, level, sort_order, is_selectable, created_at, updated_at)
VALUES
    ('BOOK',    '인간관계',  NULL, 1, 1, false, CURRENT_DATE, CURRENT_DATE),
    ('BOOK',    '개인',      NULL, 1, 2, false, CURRENT_DATE, CURRENT_DATE),
    ('BOOK',    '삶과 죽음', NULL, 1, 3, false, CURRENT_DATE, CURRENT_DATE),
    ('BOOK',    '사회',      NULL, 1, 4, false, CURRENT_DATE, CURRENT_DATE),
    ('BOOK',    '기타',      NULL, 1, 5, false, CURRENT_DATE, CURRENT_DATE),
    ('IMPRESSION', '긍정',      NULL, 1, 1, false, CURRENT_DATE, CURRENT_DATE),
    ('IMPRESSION', '감상',      NULL, 1, 2, false, CURRENT_DATE, CURRENT_DATE),
    ('IMPRESSION', '부정',      NULL, 1, 3, false, CURRENT_DATE, CURRENT_DATE)
    RETURNING keyword_id, keyword_name, keyword_type
    ),
    book_children AS (
INSERT INTO keyword (keyword_type, keyword_name, parent_id, level, sort_order, is_selectable, created_at, updated_at)
SELECT 'BOOK', v.name, p.keyword_id, 2, v.sort_order, true, CURRENT_DATE, CURRENT_DATE
FROM (VALUES
    ('인간관계','사랑',1), ('인간관계','관계',2), ('인간관계','가족',3), ('인간관계','우정',4), ('인간관계','이별',5),
    ('개인','성장',1), ('개인','자아',2), ('개인','고독',3), ('개인','선택',4), ('개인','자유',5),
    ('삶과 죽음','삶',1), ('삶과 죽음','죽음',2), ('삶과 죽음','상실',3), ('삶과 죽음','치유',4), ('삶과 죽음','기억',5),
    ('사회','사회',1), ('사회','현실',2), ('사회','역사',3), ('사회','노동',4), ('사회','여성',5), ('사회','윤리',6),
    ('기타','청춘',1), ('기타','모험',2), ('기타','판타지',3), ('기타','추리',4)
    ) AS v(parent_name, name, sort_order)
    JOIN parents p
ON p.keyword_name = v.parent_name AND p.keyword_type = 'BOOK'
    ),
    impression_children AS (
INSERT INTO keyword (keyword_type, keyword_name, parent_id, level, sort_order, is_selectable, created_at, updated_at)
SELECT 'IMPRESSION', v.name, p.keyword_id, 2, v.sort_order, true, CURRENT_DATE, CURRENT_DATE
FROM (VALUES
    ('긍정','즐거운',1), ('긍정','감동적인',2), ('긍정','위로받은',3), ('긍정','뭉클한',4), ('긍정','후련한',5),
    ('긍정','벅찬',6), ('긍정','안도한',7), ('긍정','희망이 생긴',8), ('긍정','설레는',9), ('긍정','흥미로운',10), ('긍정','빠져든',11),
    ('감상','여운이 남는',1), ('감상','먹먹한',2), ('감상','울컥한',3), ('감상','찡한',4),
    ('감상','그리운',5), ('감상','익숙한',6), ('감상','이해가 되는',7), ('감상','의문이 남는',8),
    ('부정','지루한',1), ('부정','씁쓸한',2), ('부정','허무한',3), ('부정','찝찝한',4),
    ('부정','공허한',5), ('부정','서글픈',6), ('부정','분노가 이는',7), ('부정','복잡한',8),
    ('부정','허탈한',9), ('부정','불안한',10), ('부정','괴로운',11), ('부정','안타까운',12), ('부정','답답한',13), ('부정','슬픈',14)
    ) AS v(parent_name, name, sort_order)
    JOIN parents p
ON p.keyword_name = v.parent_name AND p.keyword_type = 'IMPRESSION'
    )
SELECT 1;

COMMIT;