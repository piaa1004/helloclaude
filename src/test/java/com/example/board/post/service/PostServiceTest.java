package com.example.board.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.board.global.error.BusinessException;
import com.example.board.global.error.ErrorCode;
import com.example.board.post.domain.Post;
import com.example.board.post.dto.PostCreateRequest;
import com.example.board.post.dto.PostResponse;
import com.example.board.post.dto.PostUpdateRequest;
import com.example.board.post.repository.PostRepository;
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
class PostServiceTest {

	@Mock
	private PostRepository postRepository;

	@InjectMocks
	private PostService postService;

	@Test
	@DisplayName("게시글을 생성한다")
	void create() {
		given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

		PostResponse response = postService.create(new PostCreateRequest("제목", "본문", "작성자"));

		assertThat(response.title()).isEqualTo("제목");
		assertThat(response.content()).isEqualTo("본문");
		assertThat(response.author()).isEqualTo("작성자");
	}

	@Test
	@DisplayName("존재하지 않는 게시글을 조회하면 POST_NOT_FOUND 예외가 발생한다")
	void getNotFound() {
		given(postRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> postService.get(1L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.POST_NOT_FOUND);
	}

	@Test
	@DisplayName("게시글의 제목과 본문을 수정한다")
	void update() {
		Post post = Post.create("제목", "본문", "작성자");
		given(postRepository.findById(1L)).willReturn(Optional.of(post));

		PostResponse response = postService.update(1L, new PostUpdateRequest("새 제목", "새 본문"));

		assertThat(response.title()).isEqualTo("새 제목");
		assertThat(response.content()).isEqualTo("새 본문");
		assertThat(post.getTitle()).isEqualTo("새 제목");
	}

	@Test
	@DisplayName("게시글을 삭제한다")
	void delete() {
		Post post = Post.create("제목", "본문", "작성자");
		given(postRepository.findById(1L)).willReturn(Optional.of(post));

		postService.delete(1L);

		then(postRepository).should().delete(post);
	}

	@Test
	@DisplayName("존재하지 않는 게시글을 삭제하면 POST_NOT_FOUND 예외가 발생한다")
	void deleteNotFound() {
		given(postRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> postService.delete(1L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.POST_NOT_FOUND);
		then(postRepository).should(never()).delete(any());
	}

	@Test
	@DisplayName("허용되지 않은 필드로 정렬하면 INVALID_INPUT 예외가 발생한다")
	void getListWithInvalidSort() {
		Pageable pageable = PageRequest.of(0, 20, Sort.by("content"));

		assertThatThrownBy(() -> postService.getList(pageable))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);
		then(postRepository).should(never()).findAll(any(Pageable.class));
	}
}
