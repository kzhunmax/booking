package com.booking.app.booking.internal.exception;

import com.booking.app.booking.BookingAlreadyCompletedException;
import com.booking.app.booking.BookingNotFoundException;
import com.booking.app.booking.BookingSlotAlreadyTakenException;
import com.booking.app.booking.CancellationTooLateException;
import com.booking.app.booking.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(1)
@RestControllerAdvice
public class BookingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BookingExceptionHandler.class);

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(BookingNotFoundException ex) {
        log.warn("Booking not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Booking Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(CancellationTooLateException.class)
    public ResponseEntity<ProblemDetail> handleCancellation(CancellationTooLateException ex) {
        log.warn("Cancellation done too late: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Cancellation Too Late");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(BookingAlreadyCompletedException.class)
    public ResponseEntity<ProblemDetail> handleBookingCompleted(BookingAlreadyCompletedException ex) {
        log.warn("Booking already completed: {}", ex.getMessage());
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problemDetail.setTitle("Booking Already Completed");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problemDetail);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ProblemDetail> handleStatusTransition(InvalidStatusTransitionException ex) {
        log.warn("Booking status transition failed: {}", ex.getMessage());
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problemDetail.setTitle("Booking Status Invalid Transition");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problemDetail);
    }

    @ExceptionHandler(BookingSlotAlreadyTakenException.class)
    public ResponseEntity<ProblemDetail> handleSlotTaken(BookingSlotAlreadyTakenException ex) {
        log.warn("Booking slot already taken: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Booking Slot Taken");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }
}
