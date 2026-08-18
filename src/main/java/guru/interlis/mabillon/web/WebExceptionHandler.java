package guru.interlis.mabillon.web;

import java.util.List;

import guru.interlis.mabillon.domain.ConflictException;
import guru.interlis.mabillon.domain.FieldError;
import guru.interlis.mabillon.domain.NotFoundException;
import guru.interlis.mabillon.domain.ValidationException;
import guru.interlis.mabillon.security.AuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public final class WebExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ModelAndView notFound(NotFoundException failure, HttpServletRequest request) {
        return error(request, HttpStatus.NOT_FOUND, "Nicht gefunden", failure.getMessage(), List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ModelAndView noResourceFound(NoResourceFoundException failure, HttpServletRequest request) {
        return error(request, HttpStatus.NOT_FOUND, "Nicht gefunden",
                "Die angeforderte Seite wurde nicht gefunden.", List.of());
    }

    @ExceptionHandler(ValidationException.class)
    ModelAndView validation(ValidationException failure, HttpServletRequest request) {
        return error(request, HttpStatus.BAD_REQUEST, "Eingaben prüfen", failure.getMessage(), failure.errors());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ModelAndView missingParameter(MissingServletRequestParameterException failure, HttpServletRequest request) {
        FieldError fieldError = new FieldError(
                failure.getParameterName(), "required", "Dieses Feld ist erforderlich.");
        return error(request, HttpStatus.BAD_REQUEST, "Eingaben prüfen",
                "Bitte prüfen Sie die markierten Eingaben.", List.of(fieldError));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ModelAndView typeMismatch(MethodArgumentTypeMismatchException failure, HttpServletRequest request) {
        FieldError fieldError = new FieldError(
                failure.getName(), "type", "Der eingegebene Wert hat nicht das erwartete Format.");
        return error(request, HttpStatus.BAD_REQUEST, "Eingaben prüfen",
                "Bitte prüfen Sie die markierten Eingaben.", List.of(fieldError));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ModelAndView legacyValidation(IllegalArgumentException failure, HttpServletRequest request) {
        FieldError fieldError = new FieldError("_global", "invalid", failure.getMessage());
        return error(request, HttpStatus.BAD_REQUEST, "Eingaben prüfen",
                "Die Anfrage enthält einen ungültigen Wert.", List.of(fieldError));
    }

    @ExceptionHandler(ConflictException.class)
    ModelAndView conflict(ConflictException failure, HttpServletRequest request) {
        return error(request, HttpStatus.CONFLICT, "Aktion nicht möglich", failure.getMessage(), List.of());
    }

    @ExceptionHandler(AuthorizationException.class)
    ModelAndView forbidden(AuthorizationException failure, HttpServletRequest request) {
        return error(request, HttpStatus.FORBIDDEN, "Nicht berechtigt", failure.getMessage(), List.of());
    }

    private ModelAndView error(
            HttpServletRequest request,
            HttpStatus status,
            String title,
            String message,
            List<FieldError> errors) {
        ModelAndView view = new ModelAndView(HtmxRequest.isRequest(request) ? "error/_notice" : "error");
        view.setStatus(status);
        view.addObject("title", title);
        view.addObject("status", status.value());
        view.addObject("message", message == null || message.isBlank() ? status.getReasonPhrase() : message);
        view.addObject("errors", errors);
        return view;
    }
}
