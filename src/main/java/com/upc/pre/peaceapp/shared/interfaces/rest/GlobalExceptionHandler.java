package com.upc.pre.peaceapp.shared.interfaces.rest;

import com.upc.pre.peaceapp.profile.ProfileNotFoundException;
import com.upc.pre.peaceapp.reports.domain.exceptions.InvalidReportStatusTransitionException;
import com.upc.pre.peaceapp.reports.domain.exceptions.ReportNotFoundException;
import com.upc.pre.peaceapp.alerts.domain.exceptions.AlertNotFoundException;
import com.upc.pre.peaceapp.community.domain.exceptions.AbuseFlagAlreadyResolvedException;
import com.upc.pre.peaceapp.community.domain.exceptions.AbuseFlagNotFoundException;
import com.upc.pre.peaceapp.community.domain.exceptions.CommentNotFoundException;
import com.upc.pre.peaceapp.community.domain.exceptions.DuplicateAbuseFlagException;
import com.upc.pre.peaceapp.notifications.domain.exceptions.NotificationNotFoundException;
import com.upc.pre.peaceapp.security.iam.domain.exceptions.*;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResource> handleValidation(MethodArgumentNotValidException ex) {
        var errors = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResource("Hay campos inválidos", errors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class,
            InvalidCurrentPasswordException.class, InvalidPasswordResetTokenException.class})
    public ResponseEntity<ErrorResource> handleBadRequest(Exception ex) {
        var message = ex instanceof HttpMessageNotReadableException ? "El cuerpo de la petición no es válido" : ex.getMessage();
        return ResponseEntity.badRequest().body(ErrorResource.of(message));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResource> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResource.of(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResource> handleForbidden(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResource.of(ex.getMessage()));
    }

    @ExceptionHandler({UserNotFoundException.class, ProfileNotFoundException.class, ReportNotFoundException.class,
            AlertNotFoundException.class, NotificationNotFoundException.class, CommentNotFoundException.class,
            AbuseFlagNotFoundException.class})
    public ResponseEntity<ErrorResource> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResource.of(ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResource> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ErrorResource.of("Falta el parámetro " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResource> handleParameterType(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ErrorResource.of("El parámetro " + ex.getName() + " no es válido"));
    }

    @ExceptionHandler({InvalidReportStatusTransitionException.class, DuplicateAbuseFlagException.class,
            AbuseFlagAlreadyResolvedException.class})
    public ResponseEntity<ErrorResource> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResource.of(ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResource> handleDuplicatedEmail(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResource.of(ex.getMessage()));
    }

    /** Respaldo ante carreras: dos registros simultáneos con el mismo correo chocan en la restricción única. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResource> handleIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResource.of("El correo ya está registrado"));
    }
}
