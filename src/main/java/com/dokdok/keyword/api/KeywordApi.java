package com.dokdok.keyword.api;

import com.dokdok.book.entity.KeywordType;
import com.dokdok.global.response.ApiResponse;
import com.dokdok.keyword.dto.response.KeywordListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "키워드", description = "키워드 관련 API")
public interface KeywordApi {

    @Operation(
            summary = "키워드 목록 조회 (developer: 양재웅)",
            description = """
                    키워드 목록을 조회합니다.
                    - types 파라미터로 키워드 타입을 필터링할 수 있습니다. (예: BOOK, IMPRESSION)
                    - types 미전달 시 전체 키워드를 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "키워드 목록 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KeywordListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "SUCCESS",
                                      "message": "키워드 목록 조회 성공",
                                      "data": {
                                        "keywords": [
                                          { "id": 1, "name": "인간관계", "type": "BOOK", "parentId": null, "parentName": null, "level": 1, "sortOrder": 1, "isSelectable": false },
                                          { "id": 3, "name": "판타지", "type": "BOOK", "parentId": 1, "parentName": "인간관계", "level": 2, "sortOrder": 3, "isSelectable": true },
                                          { "id": 101, "name": "긍정", "type": "IMPRESSION", "parentId": null, "parentName": null, "level": 1, "sortOrder": 1, "isSelectable": false }
                                        ]
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {"code": "E000", "message": "서버 에러가 발생했습니다. 담당자에게 문의 바랍니다.", "data": null}
                                    """)))
    })
    @GetMapping(value = "/keywords", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<KeywordListResponse>> getKeywords(
            @Parameter(
                    description = "키워드 타입 필터 (예: BOOK, IMPRESSION)",
                    example = "BOOK"
            )
            @RequestParam(required = false) List<KeywordType> types
    );
}
