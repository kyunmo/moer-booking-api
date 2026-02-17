package io.moer.booking.domain.user.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.dto.CustomerProfileResponse;
import io.moer.booking.domain.user.dto.CustomerProfileUpdateRequest;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerProfileService {

    private final UserRepository userRepository;

    public CustomerProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다: " + userId));

        int reservationCount = userRepository.countReservationsByUserId(userId);
        int reviewCount = userRepository.countReviewsByUserId(userId);

        return CustomerProfileResponse.from(user, reservationCount, reviewCount);
    }

    @Transactional
    public CustomerProfileResponse updateProfile(Long userId, CustomerProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다: " + userId));

        String name = request.getName() != null ? request.getName() : user.getName();
        String phone = request.getPhone() != null ? request.getPhone() : user.getPhone();
        String marketingAgree = request.getMarketingAgree() != null ? request.getMarketingAgree() : user.getMarketingAgree();

        userRepository.updateCustomerProfile(userId, name, phone, marketingAgree);

        // 업데이트 후 다시 조회
        return getProfile(userId);
    }
}
