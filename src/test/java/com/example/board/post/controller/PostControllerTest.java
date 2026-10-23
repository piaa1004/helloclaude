package com.example.board.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.board.global.common.PageResponse;
import com.example.board.global.error.BusinessException;
import com.example.board.global.error.ErrorCode;
import com.example.board.post.dto.PostCreateRequest;
import com.example.board.post.dto.PostResponse;
import com.example.board.post.dto.PostSummaryResponse;
import com.example.board.post.service.PostService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PostController.class)
class PostControllerTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 12, 0);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private PostService postService;

	@Test
	@DisplayName("게시글을 생성하면 201과 Location 헤더를 반환한다")
	void create() {
		given(postService.create(any(PostCreateRequest.class)))
				.willReturn(new PostResponse(1L, "제목", "본문", "작성자", NOW, NOW));

		assertThat(mvc.post().uri("/api/v1/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "제목", "content": "본문", "author": "작성자"}
						"""))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/v1/posts/1")
				.bodyJson()
				.extractingPath("$.id").isEqualTo(1);
	}

	@Test
	@DisplayName("필수 값이 없으면 400과 필드별 에러 목록을 반환한다")
	void createWithInvalidInput() {
		assertThat(mvc.post().uri("/api/v1/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "", "content": "본문", "author": "작성자"}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.satisfies(json -> {
					assertThat(json).extractingPath("$.code").isEqualTo("INVALID_INPUT");
					assertThat(json).extractingPath("$.errors[0].field").isEqualTo("title");
				});
	}

	@Test
	@DisplayName("존재하지 않는 게시글을 조회하면 404 ProblemDetail을 반환한다")
	void getNotFound() {
		given(postService.get(1L)).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

		assertThat(mvc.get().uri("/api/v1/posts/1"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.extractingPath("$.code").isEqualTo("POST_NOT_FOUND");
	}

	@Test
	@DisplayName("목록 조회 시 기본 정렬은 생성일 내림차순, 기본 크기는 20이다")
	void getListWithDefaultPageable() {
		given(postService.getList(any(Pageable.class))).willReturn(new PageResponse<>(
				List.of(new PostSummaryResponse(1L, "제목", "작성자", NOW)), 0, 20, 1, 1, false));

		assertThat(mvc.get().uri("/api/v1/posts"))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.content[0].title").isEqualTo("제목");

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		then(postService).should().getList(captor.capture());
		assertThat(captor.getValue().getPageSize()).isEqualTo(20);
		assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	@Test
	@DisplayName("게시글을 삭제하면 204를 반환한다")
	void delete() {
		assertThat(mvc.delete().uri("/api/v1/posts/1"))
				.hasStatus(HttpStatus.NO_CONTENT);

		then(postService).should().delete(eq(1L));
	}
}
