package com.dokdok.book.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "book")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(name = "book_name", nullable = false, length = 255)
    @NotBlank(message = "책 이름은 필수 항목입니다.")
    private String bookName;

    @Column(name = "publisher", length = 100)
    private String publisher;

    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "thumbnail", length = 500)
    private String thumbnail;

    @NotBlank(message = "isbn은 필수 항목입니다.")
    @Column(name = "isbn", length = 50)
    private String isbn;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}