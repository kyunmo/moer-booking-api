package io.moer.booking.domain.broadcast.repository;

import io.moer.booking.domain.broadcast.Broadcast;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BroadcastRepository {
    void save(Broadcast broadcast);
    Optional<Broadcast> findById(Long id);
    List<Broadcast> findAll(@Param("offset") int offset, @Param("size") int size);
    int countAll();
    List<Broadcast> findSentBroadcasts(@Param("offset") int offset, @Param("size") int size);
}
