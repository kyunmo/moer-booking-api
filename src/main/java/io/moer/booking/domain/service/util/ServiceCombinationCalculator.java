package io.moer.booking.domain.service.util;

import io.moer.booking.domain.service.Service;

import java.util.List;

/**
 * 시술 조합 계산 유틸리티
 * 여러 시술을 조합했을 때 총 소요 시간과 가격을 계산
 */
public class ServiceCombinationCalculator {

    /**
     * 총 소요 시간 계산 (분)
     */
    public static int calculateTotalDuration(List<Service> services) {
        return services.stream()
                .mapToInt(Service::getDuration)
                .sum();
    }

    /**
     * 총 가격 계산
     */
    public static int calculateTotalPrice(List<Service> services) {
        return services.stream()
                .mapToInt(Service::getPrice)
                .sum();
    }

    /**
     * 시간 포맷팅 (분 → "X시간 Y분")
     */
    public static String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "분";
        }

        int hours = minutes / 60;
        int mins = minutes % 60;

        if (mins == 0) {
            return hours + "시간";
        }

        return hours + "시간 " + mins + "분";
    }

    /**
     * 가격 포맷팅 (원 → "XX,XXX원")
     */
    public static String formatPrice(int price) {
        return String.format("%,d원", price);
    }

    /**
     * 조합 가능 여부 확인
     * options의 can_combine이 모두 true인지 체크
     */
    public static boolean canCombine(List<Service> services) {
        return services.stream()
                .allMatch(service -> {
                    if (service.getOptions() == null) {
                        return true;  // options가 없으면 조합 가능
                    }
                    Object canCombine = service.getOptions().get("can_combine");
                    return canCombine == null || (canCombine instanceof Boolean && (Boolean) canCombine);
                });
    }
}