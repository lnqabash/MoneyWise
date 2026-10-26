package app.moneymate.common;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Produces the standard error envelope from docs/07-api-design.md. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorDetail(String field, String issue) {}
    public record ErrorBody(String code, String message, String traceId, List<ErrorDetail> details) {}
    public record ErrorEnvelope(ErrorBody error) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorEnvelope> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(envelope(ex.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .collect(Collectors.toList());
        return ResponseEntity.badRequest()
                .body(envelope("VALIDATION_ERROR", "Request validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500)
                .body(envelope("INTERNAL", "An unexpected error occurred", null));
    }

    private ErrorDetail toDetail(FieldError fe) {
        return new ErrorDetail(fe.getField(), fe.getDefaultMessage());
    }

    private ErrorEnvelope envelope(String code, String message, List<ErrorDetail> details) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return new ErrorEnvelope(new ErrorBody(code, message, traceId, details));
    }

    // Helper so callers can build Money-ish maps consistently if needed.
    public static Map<String, Object> money(long amountMinor, String currency) {
        return Map.of("amountMinor", amountMinor, "currency", currency);
    }
}
