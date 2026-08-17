package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;
import java.util.UUID;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.web.HtmxRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/aufgaben")
public final class AufgabeController {

    private final AufgabeService aufgabeService;

    public AufgabeController(AufgabeService aufgabeService) {
        this.aufgabeService = aufgabeService;
    }

    @PostMapping
    public String create(
            @RequestParam String geschaeftNumber,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String typCode,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String assignedUsername,
            @RequestParam(required = false) String assignedOrganisationseinheit) {
        AufgabeView created = aufgabeService.create(new CreateAufgabeCommand(
                GeschaeftNumber.parse(geschaeftNumber), title, description, typCode, dueDate,
                priority, blankToNull(assignedUsername), blankToNull(assignedOrganisationseinheit)));
        return "redirect:/geschaefte/" + created.geschaeftsnummer();
    }

    @PostMapping("/{tid}/start")
    public String start(@PathVariable UUID tid, HttpServletRequest request, Model model) {
        AufgabeView value = aufgabeService.start(tid);
        if (HtmxRequest.isRequest(request)) {
            model.addAttribute("aufgabe", value);
            return "geschaefte/_task-row";
        }
        return "redirect:/geschaefte/" + value.geschaeftsnummer();
    }

    @PostMapping("/{tid}/complete")
    public String complete(
            @PathVariable UUID tid,
            @RequestParam(required = false) String comment,
            HttpServletRequest request,
            Model model) {
        AufgabeView value = aufgabeService.complete(new CompleteAufgabeCommand(tid, comment));
        if (HtmxRequest.isRequest(request)) {
            model.addAttribute("aufgabe", value);
            return "geschaefte/_task-row";
        }
        return "redirect:/geschaefte/" + value.geschaeftsnummer();
    }

    @PostMapping("/{tid}/cancel")
    public String cancel(
            @PathVariable UUID tid,
            @RequestParam(required = false) String comment,
            HttpServletRequest request,
            Model model) {
        AufgabeView value = aufgabeService.cancel(new CancelAufgabeCommand(tid, comment));
        if (HtmxRequest.isRequest(request)) {
            model.addAttribute("aufgabe", value);
            return "geschaefte/_task-row";
        }
        return "redirect:/geschaefte/" + value.geschaeftsnummer();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
