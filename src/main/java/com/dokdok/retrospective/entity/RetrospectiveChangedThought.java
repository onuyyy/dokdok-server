package com.dokdok.retrospective.entity;

import com.dokdok.global.BaseTimeEntity;
import com.dokdok.topic.entity.Topic;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "retrospective_changed_thought")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class RetrospectiveChangedThought extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "changed_thought_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_meeting_retrospective_id", nullable = false)
    private PersonalMeetingRetrospective personalMeetingRetrospective;

    @Column(name = "key_issue", length = 255, nullable = false)
    private String keyIssue;

    @Column(name = "pre_opinion", columnDefinition = "TEXT")
    private String preOpinion;

    @Column(name = "post_opinion", columnDefinition = "TEXT")
    private String postOpinion;

    public static RetrospectiveChangedThought create(
            Topic topic,
            PersonalMeetingRetrospective personalMeetingRetrospective,
            String keyIssue,
            String preOpinion,
            String postOpinion
    ) {
        return RetrospectiveChangedThought.builder()
                .topic(topic)
                .personalMeetingRetrospective(personalMeetingRetrospective)
                .keyIssue(keyIssue)
                .preOpinion(preOpinion)
                .postOpinion(postOpinion)
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