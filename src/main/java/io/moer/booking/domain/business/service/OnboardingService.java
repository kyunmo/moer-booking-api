package io.moer.booking.domain.business.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.dto.OnboardingStatusResponse;
import io.moer.booking.domain.business.dto.OnboardingStep;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final BusinessSettingsRepository settingsRepository;

    /**
     * 온보딩 상태 조회
     */
    public OnboardingStatusResponse getOnboardingStatus(Long businessId) {
        BusinessSettings settings = settingsRepository.findByBusinessId(businessId).orElse(null);

        if (settings == null) {
            // 설정이 없으면 미완료 상태
            return buildDefaultStatus();
        }

        boolean completed = "Y".equals(settings.getOnboardingCompleted());
        boolean skipped = "Y".equals(settings.getOnboardingSkipped());

        List<OnboardingStep> steps = new ArrayList<>();
        steps.add(OnboardingStep.builder()
                .step("BUSINESS_INFO").label("매장 정보 설정")
                .completed(true) // 매장 생성 시 자동 완료
                .build());
        steps.add(OnboardingStep.builder()
                .step("SERVICES").label("서비스 등록")
                .completed("Y".equals(settings.getOnboardingStepService()))
                .build());
        steps.add(OnboardingStep.builder()
                .step("STAFFS").label("스태프 등록")
                .completed("Y".equals(settings.getOnboardingStepStaff()))
                .build());
        steps.add(OnboardingStep.builder()
                .step("FIRST_RESERVATION").label("첫 예약 등록")
                .completed("Y".equals(settings.getOnboardingStepReservation()))
                .build());

        int completedCount = (int) steps.stream().filter(OnboardingStep::getCompleted).count();

        // 현재 스텝 결정
        String currentStep = null;
        if (!completed && !skipped) {
            for (OnboardingStep step : steps) {
                if (!step.getCompleted()) {
                    currentStep = step.getStep();
                    break;
                }
            }
        }

        return OnboardingStatusResponse.builder()
                .completed(completed)
                .skipped(skipped)
                .steps(steps)
                .currentStep(currentStep)
                .completedSteps(completedCount)
                .totalSteps(steps.size())
                .build();
    }

    /**
     * 온보딩 건너뛰기
     */
    @Transactional
    public void skipOnboarding(Long businessId) {
        BusinessSettings settings = settingsRepository.findByBusinessId(businessId).orElse(null);
        if (settings != null) {
            if ("Y".equals(settings.getOnboardingCompleted())) {
                throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
            }
            if ("Y".equals(settings.getOnboardingSkipped())) {
                throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_SKIPPED);
            }
        }
        settingsRepository.skipOnboarding(businessId);
        log.info("Onboarding skipped: businessId={}", businessId);
    }

    /**
     * 온보딩 스텝 자동 완료 (내부 호출용)
     */
    @Transactional
    public void markStepComplete(Long businessId, String step) {
        BusinessSettings settings = settingsRepository.findByBusinessId(businessId).orElse(null);
        if (settings == null) return;
        if ("Y".equals(settings.getOnboardingCompleted()) || "Y".equals(settings.getOnboardingSkipped())) return;

        String column = switch (step) {
            case "service" -> "onboarding_step_service";
            case "staff" -> "onboarding_step_staff";
            case "reservation" -> "onboarding_step_reservation";
            default -> null;
        };

        if (column == null) return;

        settingsRepository.updateOnboardingStep(businessId, column, "Y");
        log.debug("Onboarding step completed: businessId={}, step={}", businessId, step);

        // 모든 스텝 완료 시 자동으로 온보딩 완료
        checkAndCompleteOnboarding(businessId);
    }

    private void checkAndCompleteOnboarding(Long businessId) {
        BusinessSettings settings = settingsRepository.findByBusinessId(businessId).orElse(null);
        if (settings == null) return;

        boolean allDone = "Y".equals(settings.getOnboardingStepService())
                && "Y".equals(settings.getOnboardingStepStaff())
                && "Y".equals(settings.getOnboardingStepReservation());

        if (allDone && !"Y".equals(settings.getOnboardingCompleted())) {
            settingsRepository.completeOnboarding(businessId);
            log.info("Onboarding auto-completed: businessId={}", businessId);
        }
    }

    private OnboardingStatusResponse buildDefaultStatus() {
        List<OnboardingStep> steps = List.of(
                OnboardingStep.builder().step("BUSINESS_INFO").label("매장 정보 설정").completed(true).build(),
                OnboardingStep.builder().step("SERVICES").label("서비스 등록").completed(false).build(),
                OnboardingStep.builder().step("STAFFS").label("스태프 등록").completed(false).build(),
                OnboardingStep.builder().step("FIRST_RESERVATION").label("첫 예약 등록").completed(false).build()
        );
        return OnboardingStatusResponse.builder()
                .completed(false).skipped(false).steps(steps)
                .currentStep("SERVICES").completedSteps(1).totalSteps(4)
                .build();
    }
}
