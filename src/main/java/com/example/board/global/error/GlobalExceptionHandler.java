package com.example.board.global.error;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String CODE = "code";

	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("Business exception: code={}, message={}", errorCode, e.getMessage());

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), e.getMessage());
		problem.setTitle(errorCode.getMessage());
		problem.setProperty(CODE, errorCode.name());
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpectedException(Exception e) {
		log.error("Unexpected exception", e);

		ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
		problem.setProperty(CODE, errorCode.name());
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
		problem.setProperty(CODE, ErrorCode.INVALID_INPUT.name());
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	// 잘못된 JSON, 타입이 맞지 않는 값 등. 기본 detail("Failed to read request")은 원인을 알기 어려워 교체한다.
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		log.warn("Unreadable request body: {}", ex.getMessage());

		ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, errorCode.getMessage());
		problem.setProperty(CODE, errorCode.name());
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	// 프레임워크 기본 예외(404, 405, 415, 타입 불일치 등)에도 code를 채워 응답 형식을 통일한다.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		// 405, 415, 404 등은 body 없이 호출되므로 예외가 가진 기본 ProblemDetail을 사용한다.
		if (body == null && ex instanceof ErrorResponse errorResponse) {
			body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
		}
		if (body instanceof ProblemDetail problem
				&& (problem.getProperties() == null || !problem.getProperties().containsKey(CODE))) {
			problem.setProperty(CODE, toErrorCode(statusCode).name());
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	private ErrorCode toErrorCode(HttpStatusCode statusCode) {
		if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
			return ErrorCode.RESOURCE_NOT_FOUND;
		}
		if (statusCode.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
			return ErrorCode.METHOD_NOT_ALLOWED;
		}
		if (statusCode.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
			return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
		}
		return statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_INPUT;
	}

	public record FieldErrorDetail(String field, String message) {
	}
}
