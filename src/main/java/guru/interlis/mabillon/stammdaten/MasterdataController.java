package guru.interlis.mabillon.stammdaten;

import guru.interlis.mabillon.web.HtmxRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/stammdaten")
public final class MasterdataController {

    private final OrganisationseinheitService organisationseinheitService;
    private final BenutzerService benutzerService;

    public MasterdataController(
            OrganisationseinheitService organisationseinheitService,
            BenutzerService benutzerService) {
        this.organisationseinheitService = organisationseinheitService;
        this.benutzerService = benutzerService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("title", "Stammdaten");
        model.addAttribute("active", "masterdata");
        model.addAttribute("organisationseinheiten", organisationseinheitService.list(true));
        model.addAttribute("benutzer", benutzerService.list(true));
        return "admin/masterdata";
    }

    @PostMapping("/organisationseinheiten")
    String createOrganisationseinheit(
            @RequestParam String kuerzel,
            @RequestParam String name,
            @RequestParam(required = false) String beschreibung,
            @RequestParam(required = false) String uebergeordneteEinheit,
            HttpServletRequest request,
            Model model) {
        organisationseinheitService.create(new OrganisationseinheitCreateCommand(
                kuerzel, name, beschreibung, blankToNull(uebergeordneteEinheit)));
        return HtmxRequest.isRequest(request) ? index(model) : "redirect:/admin/stammdaten";
    }

    @PostMapping("/organisationseinheiten/{kuerzel}")
    String updateOrganisationseinheit(
            @PathVariable String kuerzel,
            @RequestParam String name,
            @RequestParam(required = false) String beschreibung,
            @RequestParam(required = false) String uebergeordneteEinheit) {
        organisationseinheitService.update(new OrganisationseinheitUpdateCommand(
                kuerzel, name, beschreibung, blankToNull(uebergeordneteEinheit)));
        return "redirect:/admin/stammdaten";
    }

    @PostMapping("/organisationseinheiten/{kuerzel}/deactivate")
    String deactivateOrganisationseinheit(
            @PathVariable String kuerzel,
            HttpServletRequest request,
            Model model) {
        organisationseinheitService.deactivate(kuerzel);
        return HtmxRequest.isRequest(request) ? index(model) : "redirect:/admin/stammdaten";
    }

    @PostMapping("/benutzer")
    String createBenutzer(
            @RequestParam String username,
            @RequestParam String name,
            @RequestParam(required = false) String email,
            @RequestParam String organisationseinheit,
            HttpServletRequest request,
            Model model) {
        benutzerService.create(new BenutzerCreateCommand(username, name, email, organisationseinheit));
        return HtmxRequest.isRequest(request) ? index(model) : "redirect:/admin/stammdaten";
    }

    @PostMapping("/benutzer/{username}")
    String updateBenutzer(
            @PathVariable String username,
            @RequestParam String name,
            @RequestParam(required = false) String email,
            @RequestParam String organisationseinheit) {
        benutzerService.update(new BenutzerUpdateCommand(username, name, email, organisationseinheit));
        return "redirect:/admin/stammdaten";
    }

    @PostMapping("/benutzer/{username}/deactivate")
    String deactivateBenutzer(
            @PathVariable String username,
            HttpServletRequest request,
            Model model) {
        benutzerService.deactivate(username);
        return HtmxRequest.isRequest(request) ? index(model) : "redirect:/admin/stammdaten";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
