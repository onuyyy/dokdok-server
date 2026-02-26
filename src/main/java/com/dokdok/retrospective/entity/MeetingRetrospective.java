package com.dokdok.retrospective.entity;

import com.dokdok.global.BaseTimeEntity;
import com.dokdok.meeting.entity.Meeting;
import com.dokdok.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "meeting_retrospective")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE meeting_retrospective SET deleted_at = CURRENT_TIMESTAMP WHERE meeting_retrospective_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class MeetingRetrospective extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_retrospective_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    public static MeetingRetrospective of(Meeting meeting, User user, String comment){
        return MeetingRetrospective.builder()
                .meeting(meeting)
                .createdBy(user)
                .comment(comment)
                .build();
    }
}
