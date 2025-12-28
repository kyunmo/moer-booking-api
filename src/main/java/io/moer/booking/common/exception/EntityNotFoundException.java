package io.moer.booking.common.exception;

public class EntityNotFoundException extends BaseException {

    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EntityNotFoundException(String entityName, Object identifier) {
        super(ErrorCode.ENTITY_NOT_FOUND,
                String.format("%s를 찾을 수 없습니다. (식별자: %s)", entityName, identifier));
    }
}