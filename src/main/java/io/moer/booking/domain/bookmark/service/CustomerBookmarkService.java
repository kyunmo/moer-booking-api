package io.moer.booking.domain.bookmark.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.bookmark.CustomerBookmark;
import io.moer.booking.domain.bookmark.dto.BookmarkResponse;
import io.moer.booking.domain.bookmark.repository.CustomerBookmarkRepository;
import io.moer.booking.domain.business.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerBookmarkService {

    private final CustomerBookmarkRepository bookmarkRepository;
    private final BusinessRepository businessRepository;

    @Transactional
    public void addBookmark(Long userId, Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }
        if (bookmarkRepository.existsByUserIdAndBusinessId(userId, businessId)) {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
        CustomerBookmark bookmark = CustomerBookmark.builder()
                .userId(userId)
                .businessId(businessId)
                .build();
        bookmarkRepository.save(bookmark);
        log.info("Bookmark added: userId={}, businessId={}", userId, businessId);
    }

    @Transactional
    public void removeBookmark(Long userId, Long businessId) {
        if (!bookmarkRepository.existsByUserIdAndBusinessId(userId, businessId)) {
            throw new EntityNotFoundException(ErrorCode.BOOKMARK_NOT_FOUND);
        }
        bookmarkRepository.deleteByUserIdAndBusinessId(userId, businessId);
        log.info("Bookmark removed: userId={}, businessId={}", userId, businessId);
    }

    public List<BookmarkResponse> getBookmarks(Long userId) {
        return bookmarkRepository.findByUserId(userId);
    }

    public Map<String, Boolean> checkBookmark(Long userId, Long businessId) {
        boolean bookmarked = bookmarkRepository.existsByUserIdAndBusinessId(userId, businessId);
        return Map.of("bookmarked", bookmarked);
    }
}
