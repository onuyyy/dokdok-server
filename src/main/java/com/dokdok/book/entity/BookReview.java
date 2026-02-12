package com.dokdok.book.entity;

import com.dokdok.global.BaseTimeEntity;
import com.dokdok.history.listener.BookReviewHistoryListener;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@EntityListeners(BookReviewHistoryListener.class)
@Table(name = "book_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE book_review SET deleted_at = CURRENT_TIMESTAMP WHERE book_review_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class BookReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    @OneToMany(mappedBy = "bookReview", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookReviewKeyword> keywords = new ArrayList<>();

    public static BookReview create(Book book, User user, BigDecimal rating, List<Keyword> keywords) {
        BookReview review = BookReview.builder()
                .book(book)
                .user(user)
                .rating(rating)
                .build();
        review.replaceKeywords(keywords);
        return review;
    }


    public void updateReview(BigDecimal rating, List<Keyword> keywords) {
        boolean ratingChanged = !this.rating.equals(rating);
        boolean keywordsChanged = updateKeywords(keywords);

        if (ratingChanged) {
            this.rating = rating;
        }

        if (ratingChanged || keywordsChanged) {
            this.touch();
        }
    }

    public void deleteReview() {
        this.markDeletedNow();
    }

    private void replaceKeywords(List<Keyword> keywords) {
        this.keywords = new ArrayList<>();
        for (Keyword keyword : keywords) {
            this.keywords.add(BookReviewKeyword.create(this, keyword));
        }
    }

    /**
     * 책 리뷰 수정 시 키워드 변경 여부를 확인합니다.
     */
    private boolean updateKeywords(List<Keyword> newKeywords) {
        Set<Long> newKeywordIds = newKeywords.stream()
                .map(Keyword::getId)
                .collect(Collectors.toSet());

        Set<Long> existingKeywordIds = this.keywords.stream()
                .map(brk -> brk.getKeyword().getId())
                .collect(Collectors.toSet());

        // 변경 여부 확인
        boolean hasChanges = !newKeywordIds.equals(existingKeywordIds);

        if (hasChanges) {
            // 삭제: 기존에 있지만 새 목록에 없는 키워드
            this.keywords.removeIf(brk -> !newKeywordIds.contains(brk.getKeyword().getId()));

            // 추가: 새 목록에 있지만 기존에 없는 키워드
            for (Keyword keyword : newKeywords) {
                if (!existingKeywordIds.contains(keyword.getId())) {
                    this.keywords.add(BookReviewKeyword.create(this, keyword));
                }
            }
        }

        return hasChanges;
    }
}
