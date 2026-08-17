package guru.interlis.mabillon.web;

import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public final class ErrorPageController {

    @GetMapping
    String error(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = status instanceof Number number ? number.intValue() : 500;
        model.addAttribute("title", statusCode >= 500 ? "Technischer Fehler" : "Anfrage nicht möglich");
        model.addAttribute("status", statusCode);
        model.addAttribute("message", statusCode == 404
                ? "Die angeforderte Seite wurde nicht gefunden."
                : statusCode == 403 ? "Für diese Aktion fehlt die erforderliche Berechtigung."
                        : "Die Anfrage konnte nicht verarbeitet werden.");
        model.addAttribute("errors", List.of());
        return "error";
    }
}
