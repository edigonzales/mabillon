package guru.interlis.mabillon.archivierung;

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

import guru.interlis.mabillon.numbering.ArchivAblieferungNumber;
import guru.interlis.mabillon.numbering.DossierNumber;

@Controller
@RequestMapping("/archivierung")
public final class ArchivierungController {

    private final AussonderungQueryService aussonderungQueryService;
    private final ArchivAblieferungService archivAblieferungService;
    private final SipService sipService;

    public ArchivierungController(
            AussonderungQueryService aussonderungQueryService,
            ArchivAblieferungService archivAblieferungService,
            SipService sipService) {
        this.aussonderungQueryService = aussonderungQueryService;
        this.archivAblieferungService = archivAblieferungService;
        this.sipService = sipService;
    }

    @ModelAttribute
    void csrfToken(HttpServletRequest request, Model model) {
        model.addAttribute("csrfToken", request.getAttribute(CsrfToken.class.getName()));
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("title", "Archivierung");
        model.addAttribute("active", "archive");
        model.addAttribute("eligible", aussonderungQueryService.eligible(0, 100));
        return "archivierung/index";
    }

    @GetMapping("/{number}")
    String detail(@PathVariable String number, Model model) {
        ArchivAblieferungNumber deliveryNumber = ArchivAblieferungNumber.parse(number);
        model.addAttribute("title", "Archivablieferung " + number);
        model.addAttribute("active", "archive");
        model.addAttribute("delivery", archivAblieferungService.get(deliveryNumber));
        return "archivierung/detail";
    }

    @PostMapping("/ablieferungen")
    String create(
            @RequestParam String organisationCode,
            @RequestParam String title,
            @RequestParam String archivempfaenger,
            @RequestParam(required = false) String bemerkung) {
        ArchivAblieferungView view = archivAblieferungService.create(
                new CreateArchivAblieferungCommand(organisationCode, title, archivempfaenger, bemerkung));
        return "redirect:/archivierung/" + view.deliveryNumber();
    }

    @PostMapping("/{number}/dossiers")
    String addDossier(@PathVariable String number, @RequestParam String dossierNumber) {
        archivAblieferungService.addDossier(ArchivAblieferungNumber.parse(number), DossierNumber.parse(dossierNumber));
        return "redirect:/archivierung/" + number;
    }

    @PostMapping("/{number}/bereit")
    String ready(@PathVariable String number) {
        archivAblieferungService.markReady(ArchivAblieferungNumber.parse(number));
        return "redirect:/archivierung/" + number;
    }

    @PostMapping("/{number}/sip-erzeugen")
    String generateSip(@PathVariable String number) {
        sipService.generate(ArchivAblieferungNumber.parse(number));
        return "redirect:/archivierung/" + number;
    }

    @PostMapping("/{number}/sip-validieren")
    String validateSip(@PathVariable String number, @RequestParam(required = false) Integer attempt) {
        ArchivAblieferungNumber deliveryNumber = ArchivAblieferungNumber.parse(number);
        if (attempt == null) {
            sipService.validateLatest(deliveryNumber);
        } else {
            sipService.validate(deliveryNumber, attempt);
        }
        return "redirect:/archivierung/" + number;
    }

    @PostMapping("/{number}/uebergeben")
    String transfer(@PathVariable String number, @RequestParam(required = false) String bemerkung) {
        archivAblieferungService.recordTransferred(ArchivAblieferungNumber.parse(number), bemerkung);
        return "redirect:/archivierung/" + number;
    }

    @PostMapping("/{number}/uebernehmen")
    String accept(@PathVariable String number, @RequestParam String archivsignatur,
            @RequestParam(required = false) String bemerkung) {
        archivAblieferungService.recordAccepted(ArchivAblieferungNumber.parse(number), archivsignatur, bemerkung);
        return "redirect:/archivierung/" + number;
    }

    @PostMapping("/{number}/ablehnen")
    String reject(@PathVariable String number, @RequestParam(required = false) String bemerkung) {
        archivAblieferungService.recordRejected(ArchivAblieferungNumber.parse(number), bemerkung);
        return "redirect:/archivierung/" + number;
    }
}
