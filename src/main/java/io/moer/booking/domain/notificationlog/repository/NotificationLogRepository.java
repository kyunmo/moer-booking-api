package io.moer.booking.domain.notificationlog.repository;

import io.moer.booking.domain.notificationlog.NotificationLog;
import io.moer.booking.domain.notificationlog.NotificationLogStatus;
import io.moer.booking.domain.notificationlog.dto.NotificationLogSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface NotificationLogRepository {

    void save(NotificationLog notificationLog);

    Optional<NotificationLog> findById(Long id);

    List<NotificationLog> findByCondition(NotificationLogSearchCondition condition);

    int countByCondition(NotificationLogSearchCondition condition);

    void updateStatus(@Param("id") Long id,
                      @Param("status") NotificationLogStatus status,
                      @Param("errorMessage") String errorMessage);
}
