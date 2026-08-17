package guru.interlis.mabillon.registraturplan;

import jakarta.servlet.http.HttpServletRequest;
import guru.interlis.mabillon.web.HtmxRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    String index(@RequestParam(required = false) String plan, Model model) {
        var plans = queryService.listPlans(false);
        model.addAttribute("title", "Registraturplan");
        model.addAttribute("active", "registraturplan");
        model.addAttribute("plans", plans);
        model.addAttribute("tree", plan == null && !plans.isEmpty()
                ? queryService.getTree(plans.getFirst().code())
                : plan == null ? null : queryService.getTree(plan));
        return "admin/registraturplan";
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
        adminService.movePosition(code, oberposition);
        return HtmxRequest.isRequest(request) ? index(null, model) : "redirect:/admin/registraturplan";
    }
}
