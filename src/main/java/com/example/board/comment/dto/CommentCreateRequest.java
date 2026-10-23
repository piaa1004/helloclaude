package com.example.board.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
		@NotBlank @Size(max = 1000) String content,
		@NotBlank @Size(max = 50) String author
) {
}
