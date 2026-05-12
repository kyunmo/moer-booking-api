package io.moer.booking.common.storage;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Value("${app.storage.allowed-extensions}")
    private String allowedExtensions;

    @Value("${app.storage.max-file-size}")
    private long maxFileSize;

    private List<String> allowedExtensionList;
    private Path uploadRoot; // 정규화된 절대 경로 (Path Traversal 검증용)

    @PostConstruct
    public void init() {
        allowedExtensionList = Arrays.asList(allowedExtensions.split(","));
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            // SECURITY (P0-4): 정규화된 절대 경로를 캐시하여 모든 파일 조작 시 hostname 비교
            this.uploadRoot = uploadPath.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다: " + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        // 1. 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED,
                    "파일 크기가 " + (maxFileSize / 1024 / 1024) + "MB를 초과했습니다");
        }

        // 2. 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!allowedExtensionList.contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "지원하지 않는 파일 형식입니다. 허용: " + allowedExtensions);
        }

        // 3. subDir 검증 (영문/숫자/하이픈/언더스코어만 허용 - Path Traversal 방어)
        if (!isSafeSubDir(subDir)) {
            log.warn("Invalid subDir attempt: {}", subDir);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "허용되지 않는 저장 경로입니다");
        }

        // 4. 저장 경로 생성 및 검증
        String filename = UUID.randomUUID() + "." + extension;
        Path targetDir = uploadRoot.resolve(subDir).normalize();
        if (!targetDir.startsWith(uploadRoot)) {
            log.warn("Path traversal attempt detected: subDir={}, resolved={}", subDir, targetDir);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "허용되지 않는 저장 경로입니다");
        }

        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path targetPath = targetDir.resolve(filename).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "허용되지 않는 저장 경로입니다");
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + subDir + "/" + filename;
            log.info("File stored: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            // "/uploads/profiles/xxx.jpg" -> "profiles/xxx.jpg"
            String relativePath = fileUrl.replaceFirst("^/uploads/", "");

            // SECURITY (P0-4): 정규화 후 uploadRoot 하위 경로인지 검증 (Path Traversal 방어)
            // 예: relativePath="../etc/passwd" -> 정규화하면 uploadRoot 밖으로 벗어남 -> 차단
            Path filePath = uploadRoot.resolve(relativePath).normalize();
            if (!filePath.startsWith(uploadRoot)) {
                log.warn("Path traversal attempt detected on delete: fileUrl={}, resolved={}", fileUrl, filePath);
                return; // 조용히 거부 (공격자에게 정보 노출 회피)
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("File delete failed: {}", e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * subDir 화이트리스트 검증.
     * 영문/숫자/하이픈/언더스코어/슬래시(중첩 디렉토리) 만 허용. 점(.) 금지.
     */
    private boolean isSafeSubDir(String subDir) {
        if (subDir == null || subDir.isBlank()) return false;
        return subDir.matches("[a-zA-Z0-9_\\-/]+");
    }
}
