package io.moer.booking.domain.notification.repository;

import io.moer.booking.domain.notification.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface NotificationRepository {

    void save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findByUserId(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    List<Notification> findUnreadByUserId(@Param("userId") Long userId,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    int countByUserId(@Param("userId") Long userId);

    int countUnreadByUserId(@Param("userId") Long userId);

    void markAsRead(@Param("id") Long id);

    void markAllAsRead(@Param("userId") Long userId);

    void delete(Long id);

    void deleteByUserId(@Param("userId") Long userId);
}
