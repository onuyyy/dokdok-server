package com.dokdok.user.api;

import com.dokdok.global.response.ApiResponse;
import com.dokdok.user.dto.request.OnboardRequest;
import com.dokdok.user.dto.request.UpdateUserInfoRequest;
import com.dokdok.user.dto.response.UserDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/users")
public interface UserApi {

    @Operation(
            summary = "사용자 온보딩 (developer: 조건희)",
            description = """
                    신규 사용자의 닉네임과 프로필 이미지를 설정합니다.
                    - 인증된 사용자만 접근 가능
                    - 닉네임 유효성 검사 및 중복 확인 후 SecurityContext 업데이트
                    - 프로필 이미지는 선택사항 (jpg, jpeg, png만 허용, 최대 5MB)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "온보딩 완료",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"code\":\"SUCCESS\",\"message\":\"온보딩 완료\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검사 실패)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "닉네임 필수",
                                            description = "닉네임이 null이거나 빈 문자열인 경우",
                                            value = "{\"code\":\"U003\",\"message\":\"닉네임은 필수 입력 항목입니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "닉네임 길이 오류",
                                            description = "닉네임이 2자 미만 또는 20자 초과인 경우",
                                            value = "{\"code\":\"U004\",\"message\":\"닉네임은 2자 이상 20자 이하로 입력해주세요.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "닉네임 형식 오류",
                                            description = "특수문자, 공백, 이모지 등이 포함된 경우",
                                            value = "{\"code\":\"U005\",\"message\":\"닉네임은 한글, 영문, 숫자만 사용 가능합니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "파일 형식 오류",
                                            description = "지원하지 않는 이미지 형식인 경우",
                                            value = "{\"code\":\"S003\",\"message\":\"지원하지 않는 파일 형식입니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "파일 크기 초과",
                                            description = "파일 크기가 5MB를 초과한 경우",
                                            value = "{\"code\":\"S004\",\"message\":\"파일 크기가 제한을 초과했습니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G001\",\"message\":\"인증되지 않은 사용자입니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U001\",\"message\":\"존재하지 않는 사용자입니디.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U002\",\"message\":\"이미 존재하는 사용자 닉네임입니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류 (파일 업로드 실패 등)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"S001\",\"message\":\"파일 업로드에 실패했습니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @PatchMapping(value = "/onboarding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> onboard(
            @Parameter(description = "온보딩 요청 정보 (JSON)", required = true)
            @Valid @RequestPart("request") OnboardRequest request,
            @Parameter(description = "프로필 이미지 (jpg, jpeg, png / 최대 5MB)", schema = @Schema(type = "string", format = "binary"))
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    );

    @Operation(
            summary = "닉네임 중복 확인 (developer: 조건희)",
            description = "닉네임 사용 가능 여부를 확인합니다. 유효성 검사(null, 길이, 형식) 및 중복 여부를 검증합니다.",
            parameters = {
                    @Parameter(
                            name = "nickname",
                            description = "확인할 닉네임 (2~20자, 한글/영문/숫자만 허용)",
                            in = ParameterIn.QUERY,
                            required = true,
                            example = "테스트닉네임"
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용 가능한 닉네임",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"code\":\"SUCCESS\",\"message\":\"사용 가능한 닉네임입니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검사 실패)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "닉네임 필수",
                                            description = "닉네임이 null이거나 빈 문자열인 경우",
                                            value = "{\"code\":\"U003\",\"message\":\"닉네임은 필수 입력 항목입니다.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "닉네임 길이 오류",
                                            description = "닉네임이 2자 미만 또는 20자 초과인 경우",
                                            value = "{\"code\":\"U004\",\"message\":\"닉네임은 2자 이상 20자 이하로 입력해주세요.\",\"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "닉네임 형식 오류",
                                            description = "특수문자, 공백, 이모지 등이 포함된 경우",
                                            value = "{\"code\":\"U005\",\"message\":\"닉네임은 한글, 영문, 숫자만 사용 가능합니다.\",\"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U002\",\"message\":\"이미 존재하는 사용자 닉네임입니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @GetMapping(value = "/check-nickname", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> checkNickname(
            @RequestParam("nickname") String nickname
    );

    @Operation(
            summary = "현재 사용자 정보 조회 (developer: 조건희)",
            description = "현재 인증된 사용자의 프로필 정보를 조회합니다. SecurityContext에서 사용자 정보를 가져옵니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDetailResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "프로필 조회 성공",
                                              "data": {
                                                "userId": 1,
                                                "nickname": "테스트닉네임",
                                                "email": "test@example.com",
                                                "profileImageUrl": "https://example.com/profile.jpg",
                                                "createdAt": "2024-01-13T10:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G001\",\"message\":\"인증되지 않은 사용자입니다.\",\"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserDetailResponse>> getCurrentUser();

    @Operation(
            summary = "사용자 정보 수정 (developer: 조건희)",
            description = "현재 사용자의 프로필 정보를 수정합니다. 현재는 닉네임만 수정 가능하며, 유효성 검사 및 중복 확인을 수행합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDetailResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "프로필 수정 성공",
                                              "data": {
                                                "userId": 1,
                                                "nickname": "변경된닉네임",
                                                "email": "test@example.com",
                                                "profileImageUrl": "https://example.com/profile.jpg",
                                                "createdAt": "2024-01-13T10:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검사 실패)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "닉네임 필수",
                                            description = "닉네임이 null이거나 빈 문자열인 경우",
                                            value = "{\"code\":\"U003\",\"message\":\"닉네임은 필수 입력 항목입니다.\", \"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "닉네임 길이 오류",
                                            description = "닉네임이 2자 미만 또는 20자 초과인 경우",
                                            value = "{\"code\":\"U004\",\"message\":\"닉네임은 2자 이상 20자 이하로 입력해주세요.\", \"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "닉네임 형식 오류",
                                            description = "특수문자, 공백, 이모지 등이 포함된 경우",
                                            value = "{\"code\":\"U005\",\"message\":\"닉네임은 한글, 영문, 숫자만 사용 가능합니다.\", \"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G001\",\"message\":\"인증되지 않은 사용자입니다.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U001\",\"message\":\"존재하지 않는 사용자입니디.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U002\",\"message\":\"이미 존재하는 사용자 닉네임입니다.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserDetailResponse>> updateUserInfo(
            @Valid @RequestBody UpdateUserInfoRequest request
    );

    @Operation(
            summary = "회원 탈퇴 (developer: 조건희)",
            description = "현재 인증된 사용자를 소프트 삭제합니다. 처리 후 인증 세션이 해제됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"code\":\"DELETED\",\"message\":\"회원 탈퇴가 완료되었습니다.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G102\",\"message\":\"인증이 필요합니다.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U001\",\"message\":\"존재하지 않는 사용자입니디.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"E000\",\"message\":\"서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.\",\"data\":null}"
                            )
                    )
            )
    })
    @DeleteMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deleteCurrentUser(
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "프로필 이미지 변경 (developer: 조건희)",
            description = """
                    현재 사용자의 프로필 이미지를 변경합니다.
                    - 기존 이미지가 있으면 삭제 후 새 이미지로 교체
                    - jpg, jpeg, png 형식만 허용
                    - 최대 5MB
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 이미지 변경 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDetailResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "프로필 이미지가 변경되었습니다.",
                                              "data": {
                                                "userId": 1,
                                                "nickname": "테스트닉네임",
                                                "email": "test@example.com",
                                                "profileImageUrl": "https://example.com/new-profile.jpg",
                                                "createdAt": "2024-01-13T10:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "파일 형식 오류",
                                            description = "지원하지 않는 이미지 형식인 경우",
                                            value = "{\"code\":\"S003\",\"message\":\"지원하지 않는 파일 형식입니다.\", \"data\":null}"
                                    ),
                                    @ExampleObject(
                                            name = "파일 크기 초과",
                                            description = "파일 크기가 5MB를 초과한 경우",
                                            value = "{\"code\":\"S004\",\"message\":\"파일 크기가 제한을 초과했습니다.\", \"data\":null}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G001\",\"message\":\"인증되지 않은 사용자입니다.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U001\",\"message\":\"존재하지 않는 사용자입니디.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"S001\",\"message\":\"파일 업로드에 실패했습니다.\", \"data\":null}"
                            )
                    )
            )
    })
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserDetailResponse>> updateProfileImage(
            @Parameter(description = "프로필 이미지 (jpg, jpeg, png / 최대 5MB)", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestPart("profileImage") MultipartFile profileImage
    );

    @Operation(
            summary = "프로필 이미지 삭제 (developer: 조건희)",
            description = "현재 사용자의 프로필 이미지를 삭제합니다. 삭제 후 기본 이미지(null)로 설정됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 이미지 삭제 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"code\":\"SUCCESS\",\"message\":\"프로필 이미지가 삭제되었습니다.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"G001\",\"message\":\"인증되지 않은 사용자입니다.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"U001\",\"message\":\"존재하지 않는 사용자입니디.\", \"data\":null}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = "{\"code\":\"S002\",\"message\":\"파일 삭제에 실패했습니다.\", \"data\":null}"
                            )
                    )
            )
    })
    @DeleteMapping(value = "/me/profile-image", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deleteProfileImage();
}
