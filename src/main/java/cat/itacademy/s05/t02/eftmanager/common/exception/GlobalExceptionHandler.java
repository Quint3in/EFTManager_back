package cat.itacademy.s05.t02.eftmanager.common.exception;

import cat.itacademy.s05.t02.eftmanager.admin.AdminSelfActionException;
import cat.itacademy.s05.t02.eftmanager.hideout.InvalidHideoutLevelException;
import cat.itacademy.s05.t02.eftmanager.hideout.StationNotFoundException;
import cat.itacademy.s05.t02.eftmanager.auth.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Modo inválido. Usa 'pvp' o 'pve'.");
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<Map<String, Object>> handleExternalApiException(ExternalApiException ex) {
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(StationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStationNotFound(StationNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidHideoutLevelException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidHideoutLevel(InvalidHideoutLevelException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(cat.itacademy.s05.t02.eftmanager.user.InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPassword(
            cat.itacademy.s05.t02.eftmanager.user.InvalidPasswordException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(cat.itacademy.s05.t02.eftmanager.admin.AdminSelfActionException.class)
    public ResponseEntity<Map<String, Object>> handleAdminSelfAction(
            cat.itacademy.s05.t02.eftmanager.admin.AdminSelfActionException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(cat.itacademy.s05.t02.eftmanager.task.TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFound(
            cat.itacademy.s05.t02.eftmanager.task.TaskNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(cat.itacademy.s05.t02.eftmanager.task.UserNotFoundForComparisonException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundForComparison(
            cat.itacademy.s05.t02.eftmanager.task.UserNotFoundForComparisonException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}