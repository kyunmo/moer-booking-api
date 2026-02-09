package io.moer.booking.domain.superadmin.dto;

import io.moer.booking.domain.business.BusinessStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 매장 상태 일괄 변경 요청
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusUpdateRequest {
    @NotNull(message = "매장 ID 목록을 입력해주세요")
    private List<Long> businessIds;

    @NotNull(message = "변경할 상태를 선택해주세요")
    private BusinessStatus status;
}
