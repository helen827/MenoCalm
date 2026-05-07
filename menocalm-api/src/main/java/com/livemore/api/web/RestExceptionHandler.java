package com.livemore.api.web;

import com.livemore.api.config.RequestIdFilter;
import com.livemore.api.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return ResponseEntity.status(status).body(errorBody(
                status.name().toLowerCase(),
                ex.getReason() != null ? ex.getReason() : status.name(),
                request
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var errors = ex.getBindingResult().getAllErrors();
        String message = errors.isEmpty() ? "invalid_request" : errors.get(0).getDefaultMessage();
        return errorBody("validation_failed", message != null ? message : "invalid_request", request);
    }

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnexpected(Exception ex, HttpServletRequest request) {
        return errorBody("internal_server_error", "internal_server_error", request);
    }

    private ApiErrorResponse errorBody(String code, String message, HttpServletRequest request) {
        Object rid = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        String requestId = rid instanceof String ? (String) rid : null;
        return new ApiErrorResponse(
                code,
                message,
                requestId,
                request.getRequestURI(),
                System.currentTimeMillis()
        );
    }
}
