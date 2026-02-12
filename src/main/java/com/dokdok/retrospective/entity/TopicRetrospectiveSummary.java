package com.dokdok.retrospective.entity;

import com.dokdok.global.BaseTimeEntity;
import com.dokdok.topic.entity.Topic;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "topic_retrospective_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE topic_retrospective_summary SET deleted_at = CURRENT_TIMESTAMP WHERE topic_retrospective_summary_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class TopicRetrospectiveSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_retrospective_summary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_points", columnDefinition = "jsonb")
    private List<KeyPoint> keyPoints;

    public void update(String summary, List<KeyPoint> keyPoints) {
        this.summary = summary;
        this.keyPoints = keyPoints;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyPoint {
        private String title;
        private List<String> details;
    }
}
