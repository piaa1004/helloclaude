package com.example.board.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.board.post.controller.PostController;
import com.example.board.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

// 에러 응답 형식(ProblemDetail + code)이 예외 종류와 관계없이 일관적인지 검증한다.
@WebMvcTest(PostController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PostService postService;

	@Test
	@DisplayName("JSON 형식이 잘못되면 400 INVALID_REQUEST_BODY를 반환한다")
	void malformedJson() {
		assertThat(mvc.post().uri("/api/v1/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": \"제목\","))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.satisfies(json -> {
					assertThat(json).extractingPath("$.code").isEqualTo("INVALID_REQUEST_BODY");
					assertThat(json).extractingPath("$.detail").isEqualTo(ErrorCode.INVALID_REQUEST_BODY.getMessage());
				});
	}

	@Test
	@DisplayName("경로 변수 타입이 맞지 않으면 400 INVALID_INPUT을 반환한다")
	void pathVariableTypeMismatch() {
		assertThat(mvc.get().uri("/api/v1/posts/abc"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	@DisplayName("없는 경로를 요청하면 404 RESOURCE_NOT_FOUND를 반환한다")
	void noResource() {
		assertThat(mvc.get().uri("/api/v1/unknown"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.extractingPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
	}

	@Test
	@DisplayName("지원하지 않는 HTTP 메서드면 405 METHOD_NOT_ALLOWED를 반환한다")
	void methodNotAllowed() {
		assertThat(mvc.patch().uri("/api/v1/posts/1"))
				.hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
				.bodyJson()
				.extractingPath("$.code").isEqualTo("METHOD_NOT_ALLOWED");
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type이면 415 UNSUPPORTED_MEDIA_TYPE을 반환한다")
	void unsupportedMediaType() {
		assertThat(mvc.post().uri("/api/v1/posts")
				.contentType(MediaType.TEXT_PLAIN)
				.content("title=제목"))
				.hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.bodyJson()
				.extractingPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
	}

	@Test
	@DisplayName("예상하지 못한 예외는 500 INTERNAL_ERROR를 반환하고 내부 메시지를 노출하지 않는다")
	void unexpectedException() {
		given(postService.get(1L)).willThrow(new IllegalStateException("내부 상세 정보"));

		assertThat(mvc.get().uri("/api/v1/posts/1"))
				.hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
				.bodyJson()
				.satisfies(json -> {
					assertThat(json).extractingPath("$.code").isEqualTo("INTERNAL_ERROR");
					assertThat(json).extractingPath("$.detail").isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
				});
	}
}
