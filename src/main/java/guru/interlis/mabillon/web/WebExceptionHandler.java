package guru.interlis.mabillon.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public final class WebExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    ModelAndView notFound() {
        ModelAndView view = new ModelAndView("error");
        view.setStatus(org.springframework.http.HttpStatus.NOT_FOUND);
        view.addObject("title", "Seite nicht gefunden");
        view.addObject("status", 404);
        view.addObject("message", "Die angeforderte Seite wurde nicht gefunden.");
        return view;
    }
}
