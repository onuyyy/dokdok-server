package com.dokdok.history.entity;

import com.dokdok.book.entity.BookReview;
import com.dokdok.history.dto.BookReviewSnapshot;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_review_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_review_id", nullable = false)
    private Long bookReviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private HistoryAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", columnDefinition = "json")
    private BookReviewSnapshot snapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static BookReviewHistory create(BookReview review, HistoryAction action) {

        return BookReviewHistory.builder()
                .bookReviewId(review.getId())
                .userId(review.getUser().getId())
                .action(action)
                .snapshot(BookReviewSnapshot.from(review))
                .createdAt(LocalDateTime.now())
                .build();
    }

}
