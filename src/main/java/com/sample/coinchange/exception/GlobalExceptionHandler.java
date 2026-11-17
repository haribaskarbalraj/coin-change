package com.sample.coinchange.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * One place that turns exceptions into HTTP error responses, for every controller.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** 400: the number is valid JSON but not an accepted bill (business rule). */
    @ExceptionHandler(InvalidBillException.class)
    public ProblemDetail handleInvalidBill(InvalidBillException ex) {
        log.warn("Rejected request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid bill");
        return problem;
    }

    /**
     * 409 Conflict: the request is fine, but the machine's current state can't fulfil it.
     */
    @ExceptionHandler(InsufficientCoinsException.class)
    public ProblemDetail handleInsufficientCoins(InsufficientCoinsException ex) {
        log.warn("Cannot fulfil request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Insufficient coins");
        return problem;
    }

    /**
     * 400: @Valid failed (e.g. "bill" missing or null).
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", detail);
        ex.getBody().setTitle("Invalid request");
        ex.getBody().setDetail(detail);
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    private String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
