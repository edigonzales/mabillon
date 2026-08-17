package guru.interlis.mabillon.aufgabe;

import java.time.LocalDate;
import java.util.UUID;

import guru.interlis.mabillon.numbering.GeschaeftNumber;
import guru.interlis.mabillon.security.CurrentActor;
import guru.interlis.mabillon.stammdaten.BenutzerService;
import guru.interlis.mabillon.stammdaten.OrganisationseinheitService;
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
@RequestMapping("/aufgaben")
public final class AufgabeController {

    private final AufgabeService aufgabeService;
    private final AufgabeQueryService queryService;
    private final CurrentActor currentActor;
    private final BenutzerService benutzerService;
    private final OrganisationseinheitService organisationseinheitService;

    public AufgabeController(
            AufgabeService aufgabeService,
            AufgabeQueryService queryService,
            CurrentActor currentActor,
            BenutzerService benutzerService,
            OrganisationseinheitService organisationseinheitService) {
        this.aufgabeService = aufgabeService;
        this.queryService = queryService;
        this.currentActor = currentActor;
        this.benutzerService = benutzerService;
        this.organisationseinheitService = organisationseinheitService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    String myTasks(Model model) {
        model.addAttribute("title", "Meine Aufgaben");
        model.addAttribute("active", "aufgaben");
        model.addAttribute("aufgaben", queryService.myOpenTasks(currentActor.username(), 100));
        return "aufgaben/index";
    }

    @GetMapping("/{tid}")
    String detail(@PathVariable UUID tid, Model model) {
        AufgabeView value = queryService.get(tid);
        model.addAttribute("title", value.titel());
        model.addAttribute("active", "aufgaben");
        model.addAttribute("aufgabe", value);
        model.addAttribute("benutzer", benutzerService.list(false));
        model.addAttribute("organisationseinheiten", organisationseinheitService.list(false));
        return "aufgaben/detail";
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

    @PostMapping("/{tid}")
    String update(
            @PathVariable UUID tid,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) Integer priority) {
        aufgabeService.update(new UpdateAufgabeCommand(tid, title, blankToNull(description), dueDate, priority));
        return "redirect:/aufgaben/" + tid;
    }

    @PostMapping("/{tid}/delegate")
    String delegate(
            @PathVariable UUID tid,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String organisationseinheit) {
        aufgabeService.delegate(new DelegateAufgabeCommand(
                tid, blankToNull(username), blankToNull(organisationseinheit)));
        return "redirect:/aufgaben/" + tid;
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
