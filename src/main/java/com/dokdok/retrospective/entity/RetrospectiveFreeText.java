package com.dokdok.retrospective.entity;

import com.dokdok.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "retrospective_free_text")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class RetrospectiveFreeText extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "free_text_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_meeting_retrospective_id", nullable = false)
    private PersonalMeetingRetrospective personalMeetingRetrospective;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    public static RetrospectiveFreeText of(
            PersonalMeetingRetrospective personalMeetingRetrospective,
            String title,
            String content
    ) {
        return RetrospectiveFreeText.builder()
                .personalMeetingRetrospective(personalMeetingRetrospective)
                .title(title)
                .content(content)
                .build();
    }

    // 연관관계 편의 메서드
    protected void setPersonalMeetingRetrospective(PersonalMeetingRetrospective personalMeetingRetrospective) {
        this.personalMeetingRetrospective = personalMeetingRetrospective;
    }

    public void softDelete() {
        if (!isDeleted()) {
            markDeletedNow();
        }
    }

}