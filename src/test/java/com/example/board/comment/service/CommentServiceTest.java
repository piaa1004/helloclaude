package com.example.board.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.board.comment.domain.Comment;
import com.example.board.comment.dto.CommentCreateRequest;
import com.example.board.comment.dto.CommentResponse;
import com.example.board.comment.dto.CommentUpdateRequest;
import com.example.board.comment.repository.CommentRepository;
import com.example.board.global.error.BusinessException;
import com.example.board.global.error.ErrorCode;
import com.example.board.post.domain.Post;
import com.example.board.post.service.PostService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

	@Mock
	private CommentRepository commentRepository;

	@Mock
	private PostService postService;

	@InjectMocks
	private CommentService commentService;

	private final Post post = Post.create("제목", "본문", "작성자");

	@Test
	@DisplayName("게시글에 댓글을 생성한다")
	void create() {
		given(postService.findPost(1L)).willReturn(post);
		given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

		CommentResponse response = commentService.create(1L, new CommentCreateRequest("댓글", "작성자"));

		assertThat(response.content()).isEqualTo("댓글");
		assertThat(response.author()).isEqualTo("작성자");
	}

	@Test
	@DisplayName("존재하지 않는 게시글에 댓글을 생성하면 POST_NOT_FOUND 예외가 발생한다")
	void createWithPostNotFound() {
		given(postService.findPost(1L)).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

		assertThatThrownBy(() -> commentService.create(1L, new CommentCreateRequest("댓글", "작성자")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.POST_NOT_FOUND);
		then(commentRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("댓글 내용을 수정한다")
	void update() {
		Comment comment = Comment.create(post, "댓글", "작성자");
		given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(comment));

		CommentResponse response = commentService.update(1L, 10L, new CommentUpdateRequest("수정된 댓글"));

		assertThat(response.content()).isEqualTo("수정된 댓글");
		assertThat(comment.getContent()).isEqualTo("수정된 댓글");
	}

	@Test
	@DisplayName("게시글에 속하지 않은 댓글을 수정하면 COMMENT_NOT_FOUND 예외가 발생한다")
	void updateNotFound() {
		given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.update(1L, 10L, new CommentUpdateRequest("수정된 댓글")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
	}

	@Test
	@DisplayName("댓글을 삭제한다")
	void delete() {
		Comment comment = Comment.create(post, "댓글", "작성자");
		given(commentRepository.findByIdAndPostId(10L, 1L)).willReturn(Optional.of(comment));

		commentService.delete(1L, 10L);

		then(commentRepository).should().delete(comment);
	}

	@Test
	@DisplayName("허용되지 않은 필드로 정렬하면 INVALID_INPUT 예외가 발생한다")
	void getListWithInvalidSort() {
		Pageable pageable = PageRequest.of(0, 50, Sort.by("content"));

		assertThatThrownBy(() -> commentService.getList(1L, pageable))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);
		then(commentRepository).should(never()).findByPostId(anyLong(), any(Pageable.class));
	}
}
