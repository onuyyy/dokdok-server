package com.dokdok.book.entity;

import com.dokdok.user.entity.User;
import com.dokdok.gathering.entity.Gathering;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal_book")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE personal_book SET deleted_at = CURRENT_TIMESTAMP WHERE personal_book_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PersonalBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id")
    private Gathering gathering;

    @Enumerated(EnumType.STRING)
    private BookReadingStatus readingStatus;


    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static PersonalBook create(User user, Book book, BookReadingStatus readingStatus) {
        return create(user, book, readingStatus, null);
    }

    public static PersonalBook create(User user, Book book, BookReadingStatus readingStatus, Gathering gathering) {
        if (user == null || book == null) {
            throw new EntityNotFoundException("Entity를 찾을 수 없습니다.");
        }
        return PersonalBook.builder()
                .user(user)
                .book(book)
                .gathering(gathering)
                .readingStatus(readingStatus)
                .addedAt(LocalDateTime.now())
                .build();
    }

    public void updateReadingStatus() {

        if (readingStatus == BookReadingStatus.READING) {
            readingStatus = BookReadingStatus.COMPLETED;
        } else  if (readingStatus == BookReadingStatus.COMPLETED) {
            readingStatus = BookReadingStatus.READING;
        }
    }
}
