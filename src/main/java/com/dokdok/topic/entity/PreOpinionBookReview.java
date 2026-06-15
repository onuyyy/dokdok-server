package com.dokdok.topic.entity;

import com.dokdok.book.entity.Book;
import com.dokdok.global.BaseTimeEntity;
import com.dokdok.keyword.entity.Keyword;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "pre_opinion_book_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE pre_opinion_book_review SET deleted_at = CURRENT_TIMESTAMP WHERE pre_opinion_book_review_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PreOpinionBookReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_opinion_book_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    @OneToMany(mappedBy = "preOpinionBookReview", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PreOpinionBookReviewKeyword> keywords = new ArrayList<>();

    public static PreOpinionBookReview create(
            Meeting meeting,
            Book book,
            User user,
            BigDecimal rating,
            List<Keyword> keywords
    ) {
        PreOpinionBookReview review = PreOpinionBookReview.builder()
                .meeting(meeting)
                .book(book)
                .user(user)
                .rating(rating)
                .build();
        review.replaceKeywords(keywords);
        return review;
    }

    public void updateReview(BigDecimal rating, List<Keyword> keywords) {
        boolean ratingChanged = !Objects.equals(this.rating, rating);
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
            this.keywords.add(PreOpinionBookReviewKeyword.create(this, keyword));
        }
    }

    private boolean updateKeywords(List<Keyword> newKeywords) {
        Set<Long> newKeywordIds = newKeywords.stream()
                .map(Keyword::getId)
                .collect(Collectors.toSet());

        Set<Long> existingKeywordIds = this.keywords.stream()
                .map(reviewKeyword -> reviewKeyword.getKeyword().getId())
                .collect(Collectors.toSet());

        boolean hasChanges = !newKeywordIds.equals(existingKeywordIds);

        if (hasChanges) {
            this.keywords.removeIf(reviewKeyword -> !newKeywordIds.contains(reviewKeyword.getKeyword().getId()));

            for (Keyword keyword : newKeywords) {
                if (!existingKeywordIds.contains(keyword.getId())) {
                    this.keywords.add(PreOpinionBookReviewKeyword.create(this, keyword));
                }
            }
        }

        return hasChanges;
    }
}
