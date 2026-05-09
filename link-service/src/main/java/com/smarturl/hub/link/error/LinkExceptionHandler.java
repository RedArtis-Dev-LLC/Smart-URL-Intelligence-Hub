package com.smarturl.hub.link.error;

import com.smarturl.hub.common.error.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LinkExceptionHandler {

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            LinkNotFoundException ex, HttpServletRequest request) {
        return ProblemDetails.response(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(LinkGoneException.class)
    public ResponseEntity<ProblemDetail> handleGone(
            LinkGoneException ex, HttpServletRequest request) {
        return ProblemDetails.response(HttpStatus.GONE, "Gone", ex.getMessage(), request);
    }

    @ExceptionHandler(LinkAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            LinkAccessDeniedException ex, HttpServletRequest request) {
        return ProblemDetails.response(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(ShortCodeUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleShortCodeUnavailable(
            ShortCodeUnavailableException ex, HttpServletRequest request) {
        log.warn("Short code unavailable [uri={}, reason={}]", request.getRequestURI(), ex.getMessage());
        return ProblemDetails.response(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }
}
