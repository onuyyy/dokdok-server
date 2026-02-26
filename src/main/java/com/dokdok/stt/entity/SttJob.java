package com.dokdok.stt.entity;

import com.dokdok.global.BaseTimeEntity;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "stt_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class SttJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stt_job_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SttJobStatus status = SttJobStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "temp_file_path", length = 500)
    private String tempFilePath;

    @Column(name = "language", length = 20)
    @Builder.Default
    private String language = "ko-KR";

    public void markProcessing() {
        this.status = SttJobStatus.PROCESSING;
    }

    public void markDone() {
        this.status = SttJobStatus.DONE;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = SttJobStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
