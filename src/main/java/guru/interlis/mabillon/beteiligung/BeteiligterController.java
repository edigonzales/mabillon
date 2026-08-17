package guru.interlis.mabillon.beteiligung;

import java.util.UUID;

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
@RequestMapping("/beteiligte")
public final class BeteiligterController {

    private final BeteiligterService beteiligterService;

    public BeteiligterController(BeteiligterService beteiligterService) {
        this.beteiligterService = beteiligterService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    String list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String typ,
            @RequestParam(required = false) String externeReferenz,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("title", "Beteiligte");
        model.addAttribute("active", "beteiligte");
        model.addAttribute("name", name);
        model.addAttribute("typ", typ);
        model.addAttribute("externeReferenz", externeReferenz);
        model.addAttribute("result", beteiligterService.search(
                new BeteiligterSearchCriteria(blankToNull(name), blankToNull(typ), blankToNull(externeReferenz)),
                page, 25));
        return "beteiligte/index";
    }

    @GetMapping("/neu")
    String createForm(Model model) {
        model.addAttribute("title", "Beteiligten erfassen");
        model.addAttribute("active", "beteiligte");
        model.addAttribute("beteiligter", null);
        return "beteiligte/form";
    }

    @PostMapping
    String create(
            @RequestParam String typ,
            @RequestParam String name,
            @RequestParam(required = false) String vorname,
            @RequestParam(required = false) String organisation,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefon,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String externeReferenz) {
        BeteiligterView created = beteiligterService.create(new CreateBeteiligterCommand(
                typ, name, blankToNull(vorname), blankToNull(organisation), blankToNull(email),
                blankToNull(telefon), blankToNull(adresse), blankToNull(externeReferenz)));
        return "redirect:/beteiligte/" + created.tid();
    }

    @GetMapping("/{tid}")
    String detail(@PathVariable UUID tid, Model model) {
        BeteiligterView value = beteiligterService.get(tid);
        model.addAttribute("title", value.name());
        model.addAttribute("active", "beteiligte");
        model.addAttribute("beteiligter", value);
        return "beteiligte/detail";
    }

    @GetMapping("/{tid}/bearbeiten")
    String editForm(@PathVariable UUID tid, Model model) {
        BeteiligterView value = beteiligterService.get(tid);
        model.addAttribute("title", "Beteiligten bearbeiten");
        model.addAttribute("active", "beteiligte");
        model.addAttribute("beteiligter", value);
        return "beteiligte/form";
    }

    @PostMapping("/{tid}")
    String update(
            @PathVariable UUID tid,
            @RequestParam String typ,
            @RequestParam String name,
            @RequestParam(required = false) String vorname,
            @RequestParam(required = false) String organisation,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefon,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String externeReferenz) {
        beteiligterService.update(new UpdateBeteiligterCommand(
                tid, typ, name, blankToNull(vorname), blankToNull(organisation), blankToNull(email),
                blankToNull(telefon), blankToNull(adresse), blankToNull(externeReferenz)));
        return "redirect:/beteiligte/" + tid;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
