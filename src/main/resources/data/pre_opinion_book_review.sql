CREATE TABLE IF NOT EXISTS pre_opinion_book_review (
    pre_opinion_book_review_id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating NUMERIC(2, 1),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_pre_opinion_book_review_meeting
        FOREIGN KEY (meeting_id) REFERENCES meeting (meeting_id),
    CONSTRAINT fk_pre_opinion_book_review_book
        FOREIGN KEY (book_id) REFERENCES book (book_id),
    CONSTRAINT fk_pre_opinion_book_review_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX IF NOT EXISTS idx_pre_opinion_book_review_meeting_user
    ON pre_opinion_book_review (meeting_id, user_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS pre_opinion_book_review_keyword (
    pre_opinion_book_review_keyword_id BIGSERIAL PRIMARY KEY,
    pre_opinion_book_review_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pre_opinion_book_review_keyword_review
        FOREIGN KEY (pre_opinion_book_review_id)
        REFERENCES pre_opinion_book_review (pre_opinion_book_review_id),
    CONSTRAINT fk_pre_opinion_book_review_keyword_keyword
        FOREIGN KEY (keyword_id) REFERENCES keyword (keyword_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pre_opinion_book_review_keyword
    ON pre_opinion_book_review_keyword (pre_opinion_book_review_id, keyword_id);
