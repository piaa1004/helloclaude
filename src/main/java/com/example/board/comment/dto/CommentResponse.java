package com.example.board.comment.dto;

import com.example.board.comment.domain.Comment;
import java.time.LocalDateTime;

public record CommentResponse(
		Long id,
		Long postId,
		String content,
		String author,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	public static CommentResponse from(Comment comment) {
		return new CommentResponse(
				comment.getId(),
				// 프록시의 id 조회는 초기화를 일으키지 않는다.
				comment.getPost().getId(),
				comment.getContent(),
				comment.getAuthor(),
				comment.getCreatedAt(),
				comment.getUpdatedAt()
		);
	}
}
