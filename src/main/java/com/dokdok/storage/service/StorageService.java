package com.dokdok.storage.service;

import com.dokdok.global.util.SecurityUtil;
import com.dokdok.storage.exception.StorageErrorCode;
import com.dokdok.storage.exception.StorageException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

	private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
			".jpg", ".jpeg", ".png"
	);
	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

	private final MinioClient internalMinioClient;
	private final MinioClient externalMinioClient;

	@Value("${minio.bucket}")
	private String bucket;

	/**
	 * 프로필 이미지를 미니오 스토리지에 저장 후 저장 경로를 반환합니다.
	 */
	public String uploadProfileImage(MultipartFile file) {

		validateImageFile(file);

		String extension = getExtension(file);
		String fileName = "profiles/" + SecurityUtil.getCurrentUserId() + "/" + UUID.randomUUID() + extension;

		try {
			// 버킷이 없을 경우 생성합니다.
			if (!internalMinioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
				internalMinioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
			}

			// 파일 업로드
			internalMinioClient.putObject(PutObjectArgs.builder()
					.bucket(bucket)
					.object(fileName)
					.stream(file.getInputStream(), file.getSize(), -1)
					.contentType(file.getContentType())
					.build());

			return fileName;
		} catch (Exception e) {
            log.debug("파일 업로드 실패 {}", e.getMessage());
			throw new StorageException(StorageErrorCode.FILE_UPLOAD_FAILED, e);
		}
	}

    public String getPresignedProfileImage(String profileImageUrl) {

        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return null;
        }

        try {
            return externalMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(profileImageUrl)
                            .expiry(5, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("PresignedUrl 생성 실패: {}", e.getMessage(), e);
            throw new StorageException(StorageErrorCode.PRESIGNED_URL_GENERATION_FAILED, e);
        }
    }

    public void deleteProfileImage(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }

        try {
            internalMinioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectPath)
                            .build()
            );
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage(), e);
            throw new StorageException(StorageErrorCode.FILE_DELETE_FAILED, e);
        }
    }

	private void validateImageFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new StorageException(StorageErrorCode.INVALID_FILE_TYPE);
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			throw new StorageException(StorageErrorCode.FILE_SIZE_EXCEEDED);
		}

		String extension = getExtension(file);
		if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
			throw new StorageException(StorageErrorCode.INVALID_FILE_TYPE);
		}
	}

	private String getExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();

		if (originalFilename != null && originalFilename.contains(".")) {
			return originalFilename.substring(originalFilename.lastIndexOf("."));
		}
		return "";
	}
}