package com.example.board.global.error;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("Business exception: code={}, message={}", errorCode, e.getMessage());

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), e.getMessage());
		problem.setTitle(errorCode.getMessage());
		problem.setProperty("code", errorCode.name());
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpectedException(Exception e) {
		log.error("Unexpected exception", e);

		ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
		problem.setProperty("code", errorCode.name());
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
				.toList();

		ProblemDetail problem = ex.getBody();
		problem.setDetail(ErrorCode.INVALID_INPUT.getMessage());
		problem.setProperty("code", ErrorCode.INVALID_INPUT.name());
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	public record FieldErrorDetail(String field, String message) {
	}
}
