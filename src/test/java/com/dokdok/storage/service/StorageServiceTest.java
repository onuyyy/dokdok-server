package com.dokdok.storage.service;

import com.dokdok.global.util.SecurityUtil;
import com.dokdok.storage.exception.StorageErrorCode;
import com.dokdok.storage.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 프로필 이미지 업로드 실패(S001)의 원인을 좁히기 위한 테스트.
 *
 * 핵심 증명:
 *  - S001(FILE_UPLOAD_FAILED)은 파일 검증 실패가 아니라, MinIO 호출
 *    (putObject / bucketExists)이 예외를 던질 때(=스토리지 연결/인증/권한 문제) 발생한다.
 *  - 파일 검증 실패는 S001이 아닌 별도 코드(S003/S004)로 구분된다.
 *  => 따라서 사용자가 받은 S001은 클라이언트/검증 문제가 아니라
 *     "서버가 MinIO에 올리는 단계"에서 실패했다는 의미임을 증명한다.
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private MinioClient internalMinioClient;

    @Mock
    private MinioClient externalMinioClient;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        // 동일 타입 빈 2개라 @InjectMocks 이름 매칭이 불안정 → 생성자로 명확히 주입
        storageService = new StorageService(internalMinioClient, externalMinioClient);
        // @Value 로 주입되는 버킷명을 테스트에서 설정 (MinIO Args 빌드 검증 통과용)
        ReflectionTestUtils.setField(storageService, "bucket", "dokdok-storage");
    }

    private MultipartFile pngFile() {
        return new MockMultipartFile(
                "profileImage", "photo.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("MinIO putObject가 실패하면 S001(FILE_UPLOAD_FAILED)로 변환되어 던져진다 - 사용자가 본 에러의 원인")
    void uploadProfileImage_whenMinioPutObjectFails_throwsS001() throws Exception {
        // given: 버킷은 존재하지만 업로드(putObject)가 연결 실패 등으로 예외를 던지는 상황
        lenient().when(internalMinioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doThrow(new IOException("Connection refused"))
                .when(internalMinioClient).putObject(any(PutObjectArgs.class));

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

            // when & then
            assertThatThrownBy(() -> storageService.uploadProfileImage(pngFile()))
                    .isInstanceOf(StorageException.class)
                    .extracting(e -> ((StorageException) e).getErrorCode())
                    .isEqualTo(StorageErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Test
    @DisplayName("MinIO 연결 실패로 bucketExists 단계에서 예외가 나도 S001로 던져진다 - 스토리지 접근 자체가 안 되는 경우")
    void uploadProfileImage_whenMinioUnreachableAtBucketCheck_throwsS001() throws Exception {
        // given: 스토리지에 아예 접근이 안 되는 상황 (bucketExists에서 예외)
        given(internalMinioClient.bucketExists(any(BucketExistsArgs.class)))
                .willThrow(new IOException("Connection refused"));

        try (MockedStatic<SecurityUtil> mock = mockStatic(SecurityUtil.class)) {
            mock.when(SecurityUtil::getCurrentUserId).thenReturn(1L);

            assertThatThrownBy(() -> storageService.uploadProfileImage(pngFile()))
                    .isInstanceOf(StorageException.class)
                    .extracting(e -> ((StorageException) e).getErrorCode())
                    .isEqualTo(StorageErrorCode.FILE_UPLOAD_FAILED);

            // 업로드까지 가지도 못함
            verify(internalMinioClient, never()).putObject(any(PutObjectArgs.class));
        }
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 검증 단계에서 S003으로 막히고, MinIO 호출조차 하지 않는다 - S001과 구분됨")
    void uploadProfileImage_whenInvalidExtension_throwsS003_andNeverCallsMinio() throws Exception {
        // given: gif (허용: jpg/jpeg/png)
        MultipartFile gif = new MockMultipartFile(
                "profileImage", "photo.gif", "image/gif", new byte[]{1, 2, 3});

        // when & then
        assertThatThrownBy(() -> storageService.uploadProfileImage(gif))
                .isInstanceOf(StorageException.class)
                .extracting(e -> ((StorageException) e).getErrorCode())
                .isEqualTo(StorageErrorCode.INVALID_FILE_TYPE);

        verify(internalMinioClient, never()).bucketExists(any(BucketExistsArgs.class));
        verify(internalMinioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("5MB 초과 파일은 검증 단계에서 S004로 막힌다 - S001과 구분됨")
    void uploadProfileImage_whenFileTooLarge_throwsS004() {
        // given: 5MB 초과 (.png)
        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        MultipartFile bigFile = new MockMultipartFile(
                "profileImage", "big.png", "image/png", tooLarge);

        // when & then
        assertThatThrownBy(() -> storageService.uploadProfileImage(bigFile))
                .isInstanceOf(StorageException.class)
                .extracting(e -> ((StorageException) e).getErrorCode())
                .isEqualTo(StorageErrorCode.FILE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("프로필 이미지가 없으면(null) presigned URL은 null을 반환한다 - 이미지 미설정과 업로드 실패는 별개")
    void getPresignedProfileImage_whenNull_returnsNull() {
        assertThat(storageService.getPresignedProfileImage(null)).isNull();
        assertThat(storageService.getPresignedProfileImage("  ")).isNull();
    }
}
