package io.moer.booking.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * 파일 저장
     * @param file 업로드 파일
     * @param subDir 하위 디렉토리 (예: "profiles")
     * @return 접근 가능한 URL 경로
     */
    String store(MultipartFile file, String subDir);

    /**
     * 파일 삭제
     * @param fileUrl 저장된 파일 URL
     */
    void delete(String fileUrl);
}
