package guru.interlis.mabillon.registraturplan;

import java.time.LocalDate;

import guru.interlis.mabillon.web.HtmxRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/registraturplan")
public final class RegistraturplanController {

    private final RegistraturplanQueryService queryService;
    private final RegistraturplanAdminService adminService;

    public RegistraturplanController(
            RegistraturplanQueryService queryService,
            RegistraturplanAdminService adminService) {
        this.queryService = queryService;
        this.adminService = adminService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    String index(@RequestParam(required = false) String plan, Model model) {
        var plans = queryService.listPlans(true);
        model.addAttribute("title", "Registraturplan");
        model.addAttribute("active", "registraturplan");
        model.addAttribute("plans", plans);
        model.addAttribute("tree", plan == null && !plans.isEmpty()
                ? queryService.getTree(plans.getFirst().code())
                : plan == null ? null : queryService.getTree(plan));
        return "admin/registraturplan";
    }

    @PostMapping("/plaene")
    String createPlan(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam LocalDate gueltigVon,
            @RequestParam String organisationseinheit) {
        adminService.createPlan(new CreateRegistraturplanCommand(code, name, gueltigVon, organisationseinheit));
        return "redirect:/admin/registraturplan?plan=" + code;
    }

    @PostMapping("/plaene/{code}/activate")
    String activatePlan(@PathVariable String code) {
        adminService.activatePlan(code);
        return "redirect:/admin/registraturplan?plan=" + code;
    }

    @PostMapping("/plaene/{code}/replace")
    String replacePlan(@PathVariable String code, @RequestParam LocalDate gueltigBis) {
        adminService.replacePlan(code, gueltigBis);
        return "redirect:/admin/registraturplan?plan=" + code;
    }

    @PostMapping("/positionen")
    String createPosition(
            @RequestParam String planCode,
            @RequestParam String code,
            @RequestParam String titel,
            @RequestParam(required = false) String beschreibung,
            @RequestParam(required = false) String oberposition,
            @RequestParam String federfuehrendeEinheit) {
        adminService.createPosition(new CreatePositionCommand(
                planCode, code, titel, beschreibung, blankToNull(oberposition), federfuehrendeEinheit));
        return "redirect:/admin/registraturplan?plan=" + planCode;
    }

    @PostMapping("/positionen/{code}")
    String updatePosition(
            @PathVariable String code,
            @RequestParam String titel,
            @RequestParam(required = false) String beschreibung,
            @RequestParam String status) {
        adminService.updatePosition(new UpdatePositionCommand(code, titel, beschreibung, status));
        return "redirect:/admin/registraturplan";
    }

    @PostMapping("/positionen/{code}/deactivate")
    String deactivate(
            @PathVariable String code,
            HttpServletRequest request,
            Model model) {
        adminService.deactivatePosition(code);
        return HtmxRequest.isRequest(request) ? index(null, model) : "redirect:/admin/registraturplan";
    }

    @PostMapping("/positionen/{code}/verschieben")
    String move(
            @PathVariable String code,
            @RequestParam(required = false) String oberposition,
            HttpServletRequest request,
            Model model) {
        adminService.movePosition(code, blankToNull(oberposition));
        return HtmxRequest.isRequest(request) ? index(null, model) : "redirect:/admin/registraturplan";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
