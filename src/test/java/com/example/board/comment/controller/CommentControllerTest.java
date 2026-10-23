package com.example.board.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.board.comment.dto.CommentCreateRequest;
import com.example.board.comment.dto.CommentResponse;
import com.example.board.comment.dto.CommentUpdateRequest;
import com.example.board.comment.service.CommentService;
import com.example.board.global.common.PageResponse;
import com.example.board.global.error.BusinessException;
import com.example.board.global.error.ErrorCode;
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

@WebMvcTest(CommentController.class)
class CommentControllerTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 12, 0);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private CommentService commentService;

	@Test
	@DisplayName("댓글을 생성하면 201과 Location 헤더를 반환한다")
	void create() {
		given(commentService.create(eq(1L), any(CommentCreateRequest.class)))
				.willReturn(new CommentResponse(10L, 1L, "댓글", "작성자", NOW, NOW));

		assertThat(mvc.post().uri("/api/v1/posts/1/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": "댓글", "author": "작성자"}
						"""))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/v1/posts/1/comments/10")
				.bodyJson()
				.extractingPath("$.postId").isEqualTo(1);
	}

	@Test
	@DisplayName("내용이 비어 있으면 400과 필드별 에러 목록을 반환한다")
	void createWithInvalidInput() {
		assertThat(mvc.post().uri("/api/v1/posts/1/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": " ", "author": "작성자"}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.satisfies(json -> {
					assertThat(json).extractingPath("$.code").isEqualTo("INVALID_INPUT");
					assertThat(json).extractingPath("$.errors[0].field").isEqualTo("content");
				});
	}

	@Test
	@DisplayName("목록 조회 시 기본 정렬은 작성순, 기본 크기는 50이다")
	void getListWithDefaultPageable() {
		given(commentService.getList(eq(1L), any(Pageable.class))).willReturn(new PageResponse<>(
				List.of(new CommentResponse(10L, 1L, "댓글", "작성자", NOW, NOW)), 0, 50, 1, 1, false));

		assertThat(mvc.get().uri("/api/v1/posts/1/comments"))
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.content[0].content").isEqualTo("댓글");

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		then(commentService).should().getList(eq(1L), captor.capture());
		assertThat(captor.getValue().getPageSize()).isEqualTo(50);
		assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
	}

	@Test
	@DisplayName("존재하지 않는 댓글을 수정하면 404 ProblemDetail을 반환한다")
	void updateNotFound() {
		given(commentService.update(eq(1L), eq(10L), any(CommentUpdateRequest.class)))
				.willThrow(new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

		assertThat(mvc.put().uri("/api/v1/posts/1/comments/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": "수정된 댓글"}
						"""))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.extractingPath("$.code").isEqualTo("COMMENT_NOT_FOUND");
	}

	@Test
	@DisplayName("댓글을 삭제하면 204를 반환한다")
	void delete() {
		assertThat(mvc.delete().uri("/api/v1/posts/1/comments/10"))
				.hasStatus(HttpStatus.NO_CONTENT);

		then(commentService).should().delete(1L, 10L);
	}
}
