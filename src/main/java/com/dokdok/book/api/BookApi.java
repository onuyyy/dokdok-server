package com.dokdok.book.api;

import com.dokdok.book.dto.request.BookCreateRequest;
import com.dokdok.book.dto.response.*;
import com.dokdok.book.entity.BookReadingStatus;
import com.dokdok.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@Tag(name = "책 관리", description = "책 검색 및 내 책장 관리 API")
@RequestMapping("/api/book")
public interface BookApi {

    @Operation(
            summary = "외부 책 API 조회 (developer: 권우희)",
            description = """
                    검색어로 책 정보를 조회합니다.
                    - cursorPage/size 파라미터로 다음 페이지를 조회합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "책 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KakaoBookResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "책 정보 조회 성공",
                                              "data": {
                                                "items": [
                                                  {
                                                    "title": "예제 도서명",
                                                    "contents": "책 소개",
                                                    "authors": ["저자A", "저자B"],
                                                    "publisher": "출판사",
                                                    "isbn": "9788994757254",
                                                    "thumbnail": "https://example.com/thumb.jpg"
                                                  }
                                                ],
                                                "pageSize": 10,
                                                "hasNext": true,
                                                "nextCursor": {
                                                  "page": 2
                                                },
                                                "totalCount": 25
                                              }
                                            }
                                            """
                            ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G002",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "책을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "B001",
                                              "message": "책을 찾을 수 없습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "E000",
                                              "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/search")
    ResponseEntity<ApiResponse<CursorPageResponse<KakaoBookResponse.Document, BookSearchCursor>>> searchBook(
            @Parameter(description = "책 제목, 내용 등에 사용할 검색어", required = true)
            @RequestParam String query,
            @Parameter(description = "다음 페이지 커서 (nextCursor.page 값)")
            @RequestParam(required = false) Integer cursorPage,
            @Parameter(description = "한 페이지당 아이템 수", example = "10")
            @RequestParam(required = false) Integer size
    );


    @Operation(
            summary = "내 책장에 책 등록 (developer: 권우희)",
            description = "조회한 책을 내 책장에 등록합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "책장에 책 등록 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalBookCreateResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "CREATED",
                                              "message": "내 책장에 책 등록 성공",
                                              "data": {
                                                "isbn": "9788994757254",
                                                "readingStatus": "READING",
                                                "addedAt": "2026-01-13T08:36:03.043Z"
                                              }
                                            }
                                            """
                            ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G002",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "책을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "B001",
                                              "message": "책을 찾을 수 없습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "E000",
                                              "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping
    ResponseEntity<ApiResponse<PersonalBookCreateResponse>> createBook(@Valid @RequestBody BookCreateRequest bookCreateRequest);

    @Operation(
            summary = "내 책장 목록 조회 (developer: 권우희)",
            description = """
                    내 책장에 등록된 책을 커서 기반으로 조회합니다.
                    - 로그인한 사용자 기준으로 조회합니다.
                    - cursorAddedAt/cursorBookId/size 파라미터로 다음 페이지를 조회합니다.
                    - 독서 상태 필터 (ENUM: READING/COMPLETED/PENDING)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "책 리스트 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalBookListResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "책 리스트 조회 성공",
                                              "data": {
                                                "items": [
                                                  {
                                                    "bookId": 1,
                                                    "title": "예제 도서명",
                                                    "publisher": "예제 출판사",
                                                    "authors": "저자A, 저자B",
                                                    "bookReadingStatus": "READING",
                                                    "thumbnail": "https://example.com/thumb.jpg",
                                                    "gatheringName": "예제 모임"
                                                  }
                                                ],
                                                "pageSize": 10,
                                                "hasNext": true,
                                                "nextCursor": {
                                                  "addedAt": "2026-01-22T10:25:40Z",
                                                  "bookId": 127
                                                },
                                                "totalCount": 25
                                              }
                                            }
                                            """
                            ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G002",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G102",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "E000",
                                              "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<CursorPageResponse<PersonalBookListResponse, BookListCursor>>> getMyBooks(
            @RequestParam(required = false) BookReadingStatus readingStatus,
            @RequestParam(required = false) Long gatheringId,
            @Parameter(
                    description = "커서 - 마지막 아이템 addedAt (ISO 8601, cursorBookId와 함께 전달)"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime cursorAddedAt,
            @Parameter(description = "커서 - 마지막 아이템 bookId (cursorAddedAt과 함께 전달)")
            @RequestParam(required = false) Long cursorBookId,
            @Parameter(description = "한 페이지당 아이템 수", example = "10")
            @RequestParam(required = false) Integer size
    );

    @Operation(
            summary = "내 책장 단일 조회 (developer: 권우희)",
            description = """
                    내 책장에 등록된 책 한 권의 상세 정보를 조회합니다.
                    - 로그인한 사용자 소유의 책만 조회됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "책 상세 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalBookDetailResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "책 상세 정보 조회 성공",
                                              "data": {
                                                "personalBookId": 100,
                                                "bookId": 1,
                                                "title": "예제 도서명",
                                                "publisher": "예제 출판사",
                                                "authors": "저자A, 저자B",
                                                "thumbnail": "https://kakaoapi.com/afd123sdfs",
                                                "bookReadingStatus": "READING"
                                              }
                                            }
                                            """
                            ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G002",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G102",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "책을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "B003",
                                              "message": "책장에 해당 책이 존재하지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "E000",
                                              "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/{bookId}")
    ResponseEntity<ApiResponse<PersonalBookDetailResponse>> getMyBook(
            @Parameter(description = "조회할 책 ID (book 테이블 PK)", required = true, example = "10")
            @PathVariable Long bookId
    );

    @Operation(
            summary = "내 책장에서 책 삭제 (developer: 권우희)",
            description = """
                    내 책장에 등록된 책을 삭제합니다.
                    - 로그인한 사용자 소유의 책만 삭제할 수 있습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "책 삭제 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Void.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "DELETED",
                                              "message": "책 삭제 성공",
                                              "data": null
                                            }
                                            """
                            ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G002",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G102",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "책을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "B003",
                                              "message": "책장에 해당 책이 존재하지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "E000",
                                              "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @DeleteMapping("/{bookId}")
    ResponseEntity<ApiResponse<Void>> deleteMyBook(
            @Parameter(description = "삭제할 책 ID (book 테이블 PK)", required = true, example = "10")
            @PathVariable Long bookId
    );

    @Operation(
            summary = "읽고 있는 책 목록 조회 (developer: 권우희)",
            description = """
                    읽고 있는 책(READING 상태)만 조회합니다.
                    - 로그인한 사용자 기준으로 조회합니다.
                    - page/size/sort 파라미터로 페이징과 정렬을 제어할 수 있습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "읽고 있는 책 리스트 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalBookListResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "읽고 있는 책 리스트 조회 성공",
                                              "data": {
                                                "items": [
                                                  {
                                                    "bookId": 1,
                                                    "title": "예제 도서명",
                                                    "publisher": "예제 출판사",
                                                    "authors": "저자A, 저자B",
                                                    "bookReadingStatus": "READING",
                                                    "thumbnail": "https://example.com/thumb.jpg",
                                                    "gatheringName": "예제 모임"
                                                  }
                                                ],
                                                "totalCount": 1,
                                                "currentPage": 0,
                                                "pageSize": 10,
                                                "totalPages": 1
                                              }
                                            }
                                            """
                            ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G002",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G102",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "E000",
                                              "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/reading")
    ResponseEntity<ApiResponse<PageResponse<PersonalBookListResponse>>> getMyReadingBooks(
            @ParameterObject
            @Parameter(
                    description = "페이징 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 기준)",
                    example = "page=0&size=10&sort=addedAt,desc"
            )
            @PageableDefault(
                    size = 10,
                    sort = "addedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    );

    @Operation(
            summary = "내 책장 읽기 상태 토글 (developer: 권우희)",
            description = """
                    내 책장의 읽기 상태를 토글합니다.
                    - READING ↔ COMPLETED 상태가 전환됩니다.
                    - 로그인한 사용자 소유의 책만 변경됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "읽는 상태 업데이트 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PersonalBookDetailResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "SUCCESS",
                                              "message": "읽는 상태 업데이트 성공",
                                              "data": {
                                                "personalBookId": 100,
                                                "bookId": 1,
                                                "title": "예제 도서명",
                                                "publisher": "예제 출판사",
                                                "authors": "저자A, 저자B",
                                                "thumbnail": "https://kakaoapi.com/afd123sdfs",
                                                "bookReadingStatus": "COMPLETED"
                                              }
                                            }
                                            """
                            ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G002",
                                              "message": "입력값이 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인이 필요합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "G102",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "책을 찾을 수 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "B003",
                                              "message": "책장에 해당 책이 존재하지 않습니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "E000",
                                              "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping("/{bookId}/isReading")
    ResponseEntity<ApiResponse<PersonalBookDetailResponse>> updateReadingBook(
            @Parameter(description = "상태 변경 대상 책 ID (book 테이블 PK)", required = true, example = "10")
            @PathVariable Long bookId,
            @Parameter(description = "내 책장 항목 ID (personal_book PK)", required = true, example = "100")
            @RequestParam Long personalBookId
    );
}
