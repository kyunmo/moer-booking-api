package io.moer.booking.domain.bookmark.repository;

import io.moer.booking.domain.bookmark.CustomerBookmark;
import io.moer.booking.domain.bookmark.dto.BookmarkResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CustomerBookmarkRepository {
    void save(CustomerBookmark bookmark);
    void deleteByUserIdAndBusinessId(@Param("userId") Long userId, @Param("businessId") Long businessId);
    boolean existsByUserIdAndBusinessId(@Param("userId") Long userId, @Param("businessId") Long businessId);
    List<BookmarkResponse> findByUserId(Long userId);
    Optional<CustomerBookmark> findByUserIdAndBusinessId(@Param("userId") Long userId, @Param("businessId") Long businessId);
}
