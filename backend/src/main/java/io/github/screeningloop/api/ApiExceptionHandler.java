package io.github.screeningloop.api;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import io.github.screeningloop.access.ForbiddenActionException;
import io.github.screeningloop.cases.InvalidTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidTransitionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ProblemDetail handleForbidden(ForbiddenActionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(ResourceVersionConflictException.class)
    public ProblemDetail handleConflict(ResourceVersionConflictException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The case was changed by someone else. Reload it and try again.");
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidReference(InvalidRequestException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request refers to a resource that does not exist or is not valid.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested resource does not exist.");
    }
}
