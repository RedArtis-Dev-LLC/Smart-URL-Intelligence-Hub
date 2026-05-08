package com.smarturl.hub.common.error;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationException_returns400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "password", "size must be between 8 and 64"));

        Method method = Sample.class.getDeclaredMethod("handle", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/register");
        ResponseEntity<ProblemDetail> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("Bad Request");
        assertThat(body.getDetail()).isEqualTo("Request validation failed");
        assertThat(body.getInstance()).hasToString("/auth/register");
        Object errors = body.getProperties().get("errors");
        assertThat(errors).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> errorMap = (Map<String, String>) errors;
        assertThat(errorMap)
                .containsEntry("email", "must not be blank")
                .containsEntry("password", "size must be between 8 and 64");
    }

    @Test
    void accessDeniedException_returns403() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/links/abc");
        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(
                new AccessDeniedException("nope"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getTitle()).isEqualTo("Forbidden");
        assertThat(body.getInstance()).hasToString("/links/abc");
    }

    @Test
    void unhandledException_returns500WithoutLeakingStackTrace() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        ResponseEntity<ProblemDetail> response = handler.handleAny(
                new ConstraintViolationException("boom internal db detail", null), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getTitle()).isEqualTo("Internal Server Error");
        assertThat(body.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(body.getDetail()).doesNotContain("boom internal db detail");
        assertThat(body.getProperties()).satisfiesAnyOf(
                props -> assertThat(props).isNull(),
                props -> assertThat(props).doesNotContainKey("stackTrace"));
    }

    @SuppressWarnings("unused")
    private static class Sample {
        public void handle(String value) {}
    }
}
