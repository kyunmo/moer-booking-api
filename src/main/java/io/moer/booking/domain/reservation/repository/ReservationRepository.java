package io.moer.booking.domain.reservation.repository;

import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.dto.ReservationSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ReservationRepository {

    // 생성
    void save(Reservation reservation);

    // 조회
    Optional<Reservation> findById(Long id);
    Optional<Reservation> findByReservationNumber(String reservationNumber);
    List<Reservation> findByBusinessId(Long businessId);
    List<Reservation> findByCustomerId(Long customerId);
    List<Reservation> findByStaffId(Long staffId);
    List<Reservation> findByBusinessIdAndDate(@Param("businessId") Long businessId,
                                              @Param("reservationDate") LocalDate reservationDate);
    List<Reservation> findByCondition(ReservationSearchCondition condition);

    // 시간 겹침 확인
    List<Reservation> findOverlappingReservations(
            @Param("businessId") Long businessId,
            @Param("staffId") Long staffId,
            @Param("reservationDate") LocalDate reservationDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    // 수정
    void update(Reservation reservation);
    void updateStatus(@Param("id") Long id, @Param("status") ReservationStatus status);

    // 삭제
    void delete(Long id);

    // 검증
    boolean existsById(Long id);
    boolean existsByBusinessIdAndId(@Param("businessId") Long businessId, @Param("id") Long id);

    // 예약 번호 생성용
    Long getNextReservationNumber();
}