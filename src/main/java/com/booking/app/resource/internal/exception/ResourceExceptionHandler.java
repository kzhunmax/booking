package com.booking.app.resource.internal.exception;

import com.booking.app.resource.InvalidStatusTransitionException;
import com.booking.app.resource.NameAlreadyTakenException;
import com.booking.app.resource.ResourceNotFoundException;
import com.booking.app.resource.internal.api.ResourceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ResourceController.class)
public class ResourceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResourceExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(NameAlreadyTakenException.class)
    public ResponseEntity<ProblemDetail> handleNameTaken(NameAlreadyTakenException ex) {
        log.warn("Resource name already taken: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Resource Name Taken");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }
}
