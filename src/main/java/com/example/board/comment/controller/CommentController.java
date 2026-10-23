package com.example.board.comment.controller;

import com.example.board.comment.dto.CommentCreateRequest;
import com.example.board.comment.dto.CommentResponse;
import com.example.board.comment.dto.CommentUpdateRequest;
import com.example.board.comment.service.CommentService;
import com.example.board.global.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentController {

	private final CommentService commentService;

	@PostMapping
	public ResponseEntity<CommentResponse> create(
			@PathVariable Long postId, @Valid @RequestBody CommentCreateRequest request) {
		CommentResponse response = commentService.create(postId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{commentId}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(location).body(response);
	}

	@GetMapping
	public PageResponse<CommentResponse> getList(
			@PathVariable Long postId,
			@PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
		return commentService.getList(postId, pageable);
	}

	@PutMapping("/{commentId}")
	public CommentResponse update(
			@PathVariable Long postId, @PathVariable Long commentId, @Valid @RequestBody CommentUpdateRequest request) {
		return commentService.update(postId, commentId, request);
	}

	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> delete(@PathVariable Long postId, @PathVariable Long commentId) {
		commentService.delete(postId, commentId);
		return ResponseEntity.noContent().build();
	}
}
