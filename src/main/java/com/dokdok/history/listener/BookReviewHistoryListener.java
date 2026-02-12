package com.dokdok.history.listener;

import com.dokdok.book.entity.BookReview;
import com.dokdok.global.util.BeanUtils;
import com.dokdok.history.entity.BookReviewHistory;
import com.dokdok.history.entity.HistoryAction;
import com.dokdok.history.repository.BookReviewHistoryRepository;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;

public class BookReviewHistoryListener {

    @PostPersist
    public void onInsert(BookReview review) {
        saveHistory(review, HistoryAction.INSERT);
    }

    @PostUpdate
    public void onUpdate(BookReview review) {
        if (review.getDeletedAt() != null) {
            saveHistory(review, HistoryAction.DELETE);
        } else {
            saveHistory(review, HistoryAction.UPDATE);
        }
    }

    // Hard Delete 사용 시 사용.
    /*@PreRemove
    public void onDelete(BookReview review) {
        saveHistory(review, HistoryAction.DELETE);
    }*/

    private void saveHistory(BookReview review, HistoryAction action) {
        BookReviewHistoryRepository repository = BeanUtils.getBean(BookReviewHistoryRepository.class);
        repository.save(BookReviewHistory.create(review, action));
    }
}
