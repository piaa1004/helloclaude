package com.example.board.comment.service;

import com.example.board.comment.domain.Comment;
import com.example.board.comment.dto.CommentCreateRequest;
import com.example.board.comment.dto.CommentResponse;
import com.example.board.comment.dto.CommentUpdateRequest;
import com.example.board.comment.repository.CommentRepository;
import com.example.board.global.common.PageResponse;
import com.example.board.global.error.BusinessException;
import com.example.board.global.error.ErrorCode;
import com.example.board.post.domain.Post;
import com.example.board.post.service.PostService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

	private static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "createdAt");

	private final CommentRepository commentRepository;
	private final PostService postService;

	@Transactional
	public CommentResponse create(Long postId, CommentCreateRequest request) {
		Post post = postService.findPost(postId);
		Comment comment = Comment.create(post, request.content(), request.author());
		return CommentResponse.from(commentRepository.save(comment));
	}

	public PageResponse<CommentResponse> getList(Long postId, Pageable pageable) {
		validateSort(pageable.getSort());
		// 게시글이 없으면 빈 목록 대신 404를 반환한다.
		postService.findPost(postId);
		return PageResponse.from(commentRepository.findByPostId(postId, pageable), CommentResponse::from);
	}

	@Transactional
	public CommentResponse update(Long postId, Long commentId, CommentUpdateRequest request) {
		Comment comment = findComment(postId, commentId);
		comment.update(request.content());
		// 응답에 갱신된 updatedAt을 담기 위해 auditing이 동작하도록 먼저 flush한다.
		commentRepository.flush();
		return CommentResponse.from(comment);
	}

	@Transactional
	public void delete(Long postId, Long commentId) {
		commentRepository.delete(findComment(postId, commentId));
	}

	// 다른 게시글의 댓글 id로 접근하는 경우도 존재하지 않는 댓글로 처리한다.
	private Comment findComment(Long postId, Long commentId) {
		return commentRepository.findByIdAndPostId(commentId, postId)
				.orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
	}

	private void validateSort(Sort sort) {
		for (Sort.Order order : sort) {
			if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "정렬할 수 없는 필드입니다: " + order.getProperty());
			}
		}
	}
}
